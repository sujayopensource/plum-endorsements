# Deliverable #3: Algorithm for Minimizing EA Balance Requirements

**Plum Endorsement Management System -- Design Challenge**

---

## Table of Contents

1. [Problem Statement](#1-problem-statement)
2. [EA Account Domain Model](#2-ea-account-domain-model)
3. [Reserve-Debit-Credit Lifecycle](#3-reserve-debit-credit-lifecycle)
4. [Priority-Aware Batch Optimization](#4-priority-aware-batch-optimization-eabalancecalculator)
5. [Smart Batch Optimization](#5-smart-batch-optimization-constraintbatchoptimizer)
6. [Predictive Balance Forecasting](#6-predictive-balance-forecasting)
7. [Balance Minimization Formula](#7-balance-minimization-formula)
8. [Observability](#8-observability)
9. [Design Trade-offs](#9-design-trade-offs)

---

## 1. Problem Statement

In group health insurance, every employer maintains an **Employer Advance (EA) account** with each insurer they work with. This is a prepaid float used to fund endorsement processing -- adding employees to policies (ADD), removing them (DELETE), or modifying coverage (UPDATE).

The core tension:

```
  Balance too HIGH           Balance too LOW
  +-----------------+       +-----------------+
  | Idle capital     |       | Endorsements    |
  | tied up with     |       | BLOCKED --      |
  | insurer.         |       | employees lack  |
  | Opportunity cost |       | coverage.       |
  | for employer.    |       | Compliance risk.|
  +-----------------+       +-----------------+
         |                          |
         +--- GOAL: Find the ------+
              minimum balance
              that NEVER blocks
              endorsement processing
```

**Constraints:**
- Employers may have EA accounts with 3+ insurers simultaneously (Bajaj Allianz, ICICI Lombard, Niva Bupa, etc.)
- ADD endorsements consume balance; DELETE endorsements free it
- Endorsements arrive continuously but insurers process in batches (some support real-time)
- Indian business seasonality creates predictable demand spikes (April hiring wave, October appraisals)
- Balance top-ups are not instantaneous -- NEFT/RTGS delays of 1-4 hours

**Goal:** Minimize the balance an employer must maintain while guaranteeing that no endorsement is ever blocked due to insufficient funds.

---

## 2. EA Account Domain Model

The `EAAccount` is a domain entity that models the prepaid float with balance-reservation semantics. It enforces financial invariants at the domain level, preventing any operation that would lead to negative available balance.

```java
public class EAAccount {

    private UUID employerId;
    private UUID insurerId;
    private BigDecimal balance;      // Total deposited amount
    private BigDecimal reserved;     // Amount locked for pending endorsements
    private Instant updatedAt;

    // Effective spendable balance
    public BigDecimal availableBalance() {
        return balance.subtract(reserved);
    }

    // Can this endorsement be funded?
    public boolean canFund(BigDecimal amount) {
        return availableBalance().compareTo(amount) >= 0;
    }

    // Lock funds for a pending endorsement (prevents double-spending)
    public void reserve(BigDecimal amount) {
        this.reserved = this.reserved.add(amount);
        if (availableBalance().compareTo(BigDecimal.ZERO) < 0) {
            this.reserved = this.reserved.subtract(amount);  // Rollback
            throw new IllegalStateException(
                "Insufficient available balance to reserve " + amount);
        }
        this.updatedAt = Instant.now();
    }

    // Release a reservation (endorsement confirmed or cancelled)
    public void releaseReservation(BigDecimal amount) {
        this.reserved = this.reserved.subtract(amount);
        this.updatedAt = Instant.now();
    }

    // Actual charge against balance (on insurer confirmation)
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    // Refund to balance (DELETE endorsement confirmed)
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedAt = Instant.now();
    }
}
```

**Key invariant:** `availableBalance() >= 0` at all times. The `reserve()` method enforces this with an optimistic check-and-rollback pattern -- it adds to reserved, checks the invariant, and rolls back if violated. This is simpler and less error-prone than a separate check-then-act, which would be vulnerable to TOCTOU races without external synchronization.

**Memory layout:**

```
EA Account State
+-----------------------------+
|  balance:   100,000         |    <-- Total deposited
|  reserved:   35,000         |    <-- Locked for pending ADDs
|  -------------------------  |
|  available:  65,000         |    <-- Can fund new endorsements
+-----------------------------+
```

---

## 3. Reserve-Debit-Credit Lifecycle

The EA account participates in a three-phase financial flow that tracks each endorsement from creation through settlement.

```
Phase 1: CREATION                Phase 2: CONFIRMATION           Phase 3: DELETION
(Endorsement Created)            (Insurer Confirms)              (DELETE Confirmed)

+-------------------+            +-------------------+           +-------------------+
| reserve(premium)  |            | debit(premium)    |           | credit(premium)   |
|                   |            | releaseReserve()  |           |                   |
| Locks funds so    |            | Converts reserved |           | Refunds balance   |
| other endorsements|            | into actual charge|           | when employee     |
| cannot spend them |            | against balance   |           | removed from      |
|                   |            |                   |           | policy            |
+-------------------+            +-------------------+           +-------------------+
      |                                |                               |
      v                                v                               v
balance:  100,000               balance:   65,000               balance:   73,000
reserved:  35,000  (+35k)       reserved:        0  (-35k)      reserved:        0
available: 65,000               available: 65,000               available: 73,000
```

**Why three phases instead of direct debit?**

Direct debit on creation would mean: if the insurer rejects the endorsement, we need to reverse it. Reversals are messy -- they create reconciliation noise and audit trail complications. The reservation pattern gives us:

| Property | Direct Debit | Reserve-then-Debit |
|---|---|---|
| Double-spend protection | Requires distributed lock | Built into domain model |
| Rejection handling | Requires reversal transaction | Just release reservation |
| Balance visibility | True balance unknown until confirmation | `availableBalance()` always accurate |
| Audit trail | Debit + possible reversal | Reserve -> Debit (clean, linear) |

**How it integrates with the endorsement lifecycle:**

```
CreateEndorsementHandler.handle():
  1. Save endorsement (status = CREATED)
  2. Validate -> VALIDATED
  3. Grant provisional coverage -> PROVISIONALLY_COVERED
  4. If type == ADD:
       account = findEAAccount(employerId, insurerId)
       if account.canFund(premiumAmount):
           account.reserve(premiumAmount)        // <-- Phase 1
           saveTransaction(RESERVE, amount)
       else:
           log warning (endorsement still proceeds,
                        but batch optimizer will handle
                        balance-constrained sequencing)
```

The reservation happens at endorsement creation time. The actual debit happens when the insurer confirms the endorsement (via `ProcessEndorsementHandler`). If the insurer rejects, the reservation is released.

---

## 4. Priority-Aware Batch Optimization (EABalanceCalculator)

The `EABalanceCalculator` is a domain service that sequences endorsements to maximize the number that can be funded from the current balance. The key insight: **if you process deletions first, they free up balance that can then fund additions.**

### 4.1 Priority Classification

```java
public enum EndorsementPriority {
    P0_DELETION(0),        // DELETE -- frees balance, always process first
    P1_COST_NEUTRAL(1),    // UPDATE with premiumAmount == 0 -- no EA impact
    P2_ADDITION(2),        // ADD -- consumes balance, sorted by urgency
    P3_PREMIUM_UPDATE(3);  // UPDATE with premium change -- lowest priority

    public static EndorsementPriority classify(Endorsement endorsement) {
        return switch (endorsement.getType()) {
            case DELETE -> P0_DELETION;
            case UPDATE -> {
                if (endorsement.getPremiumAmount() != null
                        && endorsement.getPremiumAmount().signum() == 0)
                    yield P1_COST_NEUTRAL;
                yield P3_PREMIUM_UPDATE;
            }
            case ADD -> P2_ADDITION;
        };
    }
}
```

**Rationale for ordering:**
- **P0 (DELETE):** Every deletion returns premium to the balance. Processing them first maximizes the pool available for additions. This is the single most impactful optimization.
- **P1 (Cost-neutral UPDATE):** Zero EA impact, so they can be included freely. Getting them processed keeps the queue short.
- **P2 (ADD):** Sorted by `coverageStartDate` ascending -- employees whose coverage starts soonest are processed first. This prevents coverage gaps.
- **P3 (Premium UPDATE):** Lowest urgency. Premium adjustments rarely affect employee coverage.

### 4.2 Optimal Sequencing

```java
public List<Endorsement> sequenceForOptimalBalance(List<Endorsement> endorsements) {
    List<Endorsement> sorted = new ArrayList<>(endorsements);
    sorted.sort(Comparator
        .comparingInt((Endorsement e) -> EndorsementPriority.classify(e).getRank())
        .thenComparing(e -> e.getCoverageStartDate() != null
            ? e.getCoverageStartDate() : LocalDate.MAX));
    return sorted;
}
```

Two-level sort: first by priority rank (P0 < P1 < P2 < P3), then by coverage start date within each tier.

### 4.3 Constrained Batch Construction

```java
public BatchPlan constructOptimizedBatch(List<Endorsement> endorsements,
                                          EAAccount account, int maxBatchSize) {
    List<Endorsement> sequenced = sequenceForOptimalBalance(endorsements);
    List<Endorsement> included = new ArrayList<>();
    List<Endorsement> deferred = new ArrayList<>();
    BigDecimal runningBalance = account.availableBalance();

    for (Endorsement e : sequenced) {
        if (included.size() >= maxBatchSize) {
            deferred.add(e);
            continue;
        }

        BigDecimal impact = calculateImpact(e);

        if (impact.signum() <= 0) {
            // Deletions / cost-neutral: always include (they free balance)
            included.add(e);
            runningBalance = runningBalance.subtract(impact);
        } else if (runningBalance.compareTo(impact) >= 0) {
            // Additions/updates: only if running balance supports them
            included.add(e);
            runningBalance = runningBalance.subtract(impact);
        } else {
            deferred.add(e);
        }
    }

    return new BatchPlan(included, deferred, runningBalance);
}
```

**Walk-through example:**

```
EA Account: balance=50,000  reserved=0  available=50,000
Queue: 5 endorsements

Sequenced by priority:
  [1] DELETE  employee-A   premium=12,000   (P0)
  [2] DELETE  employee-B   premium= 8,000   (P0)
  [3] UPDATE  employee-C   premium=     0   (P1, cost-neutral)
  [4] ADD     employee-D   premium=25,000   (P2, starts 2026-03-15)
  [5] ADD     employee-E   premium=35,000   (P2, starts 2026-03-20)

Processing:
  Step 1: DELETE -12,000 -> running = 62,000  [INCLUDED]
  Step 2: DELETE  -8,000 -> running = 70,000  [INCLUDED]
  Step 3: UPDATE      0  -> running = 70,000  [INCLUDED]
  Step 4: ADD   +25,000  -> running = 45,000  [INCLUDED]  (70k >= 25k)
  Step 5: ADD   +35,000  -> running < 0       [DEFERRED]  (45k < 35k)

Result: BatchPlan(included=[1,2,3,4], deferred=[5], projectedBalance=45,000)

Without optimization (FIFO order): only 1-2 endorsements might fit.
With optimization: 4 of 5 endorsements processed.
```

### 4.4 Balance Forecast (Domain-Level)

```java
public BalanceForecast forecastBalance(EAAccount account, List<Endorsement> endorsements) {
    // Sum all costs (ADD, UPDATE with premium > 0)
    BigDecimal totalRequired = sum of positive impacts;

    // Sum all credits (DELETE)
    BigDecimal expectedCredits = sum of negative impacts (absolute value);

    // Net requirement
    BigDecimal netRequired = totalRequired - expectedCredits;

    // 10% safety margin
    BigDecimal safetyAmount = netRequired * 0.10;
    BigDecimal requiredMinimum = netRequired + safetyAmount;

    // Shortfall = how much more the employer needs to deposit
    BigDecimal shortfall = max(0, requiredMinimum - account.availableBalance());

    return new BalanceForecast(requiredMinimum, shortfall, shortfall > 0);
}
```

The 10% safety margin accounts for:
- Premium amounts that may change between creation and insurer confirmation
- Timing mismatches between reservation and actual processing
- Endorsements that arrive between forecast runs

---

## 5. Smart Batch Optimization (ConstraintBatchOptimizer)

The `ConstraintBatchOptimizer` is an infrastructure-layer implementation of `BatchOptimizerPort` that uses a composite scoring function to sequence endorsements. While `EABalanceCalculator` provides the domain-level priority logic, the `ConstraintBatchOptimizer` adds a weighted multi-factor score to maximize throughput under balance constraints.

### 5.1 Composite Scoring Function

Each endorsement receives a score from 0.0 to 1.0:

```
compositeScore = (urgencyScore x 0.60) + (eaImpactScore x 0.40)
```

**Urgency Score (60% weight):**

```java
private double calculateUrgencyScore(Endorsement e) {
    // Priority contributes: P0=1.0, P1=0.75, P2=0.5, P3=0.25
    int rank = EndorsementPriority.classify(e).getRank();
    double priorityScore = (4 - rank) / 4.0;

    // Time pressure: how close is coverage start date?
    if (e.getCoverageStartDate() != null) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(),
                                                   e.getCoverageStartDate());
        double timePressure = max(0, 1.0 - (daysUntil / 30.0));
        return (priorityScore + timePressure) / 2.0;
    }
    return priorityScore;
}
```

```
Urgency Score Breakdown:
+---------------------------------------------------+
| Priority Component     | Time Pressure Component  |
| P0 DELETE  -> 1.00     | 0 days away  -> 1.00     |
| P1 NEUTRAL -> 0.75     | 7 days away  -> 0.77     |
| P2 ADD     -> 0.50     | 15 days away -> 0.50     |
| P3 UPDATE  -> 0.25     | 30+ days away-> 0.00     |
+---------------------------------------------------+
Final urgency = average of both components
```

**EA Impact Score (40% weight):**

```java
private double calculateEAImpactScore(Endorsement e, EAAccount account) {
    if (e.getType() == EndorsementType.DELETE)
        return 1.0;   // Always beneficial -- frees balance

    if (account == null || e.getPremiumAmount() == null)
        return 0.5;

    BigDecimal available = account.availableBalance();
    if (available.signum() <= 0) return 0.0;

    // Lower cost relative to balance = higher score
    double impactRatio = e.getPremiumAmount() / available;
    return max(0, 1.0 - impactRatio);
}
```

The EA impact score penalizes endorsements that consume a large fraction of the available balance. A 5,000 ADD against a 100,000 balance scores 0.95 (barely uses the pool). A 90,000 ADD against the same balance scores 0.10 (consumes most of the pool). Deletions always score 1.0 because they increase the pool.

### 5.2 Two-Pass Packing Algorithm

```
Pass 1: Process all DELETEs (sorted by composite score, descending)
        Each deletion INCREASES runningBalance

Pass 2: Process ADDs and UPDATEs (sorted by composite score, descending)
        Each addition DECREASES runningBalance
        Skip if runningBalance < cost (defer to next batch)

                   +--- DELETEs processed first ---+
                   |                                |
                   v                                v
           +-------------+                 +---------------+
           | runningBal  |  -- credits --> | runningBal    |
           | = available |                 | = available   |
           |             |                 |   + credits   |
           +-------------+                 +------|--------+
                                                  |
                                   ADDs consume from
                                   the enlarged pool
                                                  |
                                                  v
                                           +---------------+
                                           | Final batch:  |
                                           | max items     |
                                           | within balance|
                                           +---------------+
```

### 5.3 Savings Calculation

```java
BigDecimal naiveCost = queue.stream()
    .limit(maxBatchSize)
    .filter(e -> type == ADD || type == UPDATE)
    .map(Endorsement::getPremiumAmount)
    .reduce(ZERO, BigDecimal::add);

BigDecimal optimizedCost = optimized.stream()
    .filter(e -> type == ADD || type == UPDATE)
    .map(Endorsement::getPremiumAmount)
    .reduce(ZERO, BigDecimal::add);

BigDecimal savings = max(0, naiveCost - optimizedCost);
```

Naive cost assumes FIFO ordering (no priority awareness). Optimized cost is what the optimized batch actually requires. The difference is the savings -- balance that the employer does NOT need to have deposited for this batch to succeed.

### 5.4 Integration with Batch Assembly

The `BatchAssemblyScheduler` runs on a cron schedule and delegates to the optimizer:

```
BatchAssemblyScheduler.assembleAndSubmitBatches()
  |
  |-- Group queued endorsements by insurerId
  |
  +-- For each insurer:
        |
        |-- if optimizerEnabled:
        |     plan = batchOptimizer.optimizeBatch(endorsements, account, capabilities)
        |     use plan.endorsements() as the optimized queue
        |     publish BatchOptimized event if savings > 0
        |
        |-- else:
        |     use original FIFO order
        |
        +-- Chunk into maxBatchSize groups and submit
```

The optimizer is behind a feature flag (`endorsement.intelligence.batch-optimizer.enabled`) and falls back gracefully to FIFO ordering on any failure.

---

## 6. Predictive Balance Forecasting

### 6.1 Architecture

```
+---------------------+       +-------------------------+       +--------------------+
| BalanceForecast     |       | StatisticalForecast     |       | BalanceForecast   |
| Service             |------>| Engine                  |------>| Repository        |
| (Application Layer) |       | (Infrastructure Layer)  |       | (Persistence)     |
+---------------------+       +-------------------------+       +--------------------+
        |                              |
        | Gathers history              | Implements BalanceForecastPort
        | Computes shortfall           | 90-day lookback
        | Publishes events             | Day-of-week seasonality
        | Sends notifications          | Monthly seasonality
        |                              | Confidence scoring
        v                              v
+---------------------+       +-------------------------+
| EndorsementRepo     |       | ForecastResult          |
| EAAccountRepo       |       | { forecastedNeed,       |
| EventPublisher      |       |   daysAhead,            |
| NotificationPort    |       |   dailyBurnRate,        |
+---------------------+       |   narrative }           |
                               +-------------------------+
```

### 6.2 StatisticalForecastEngine

The forecast engine projects 30-day EA balance needs using historical endorsement patterns with dual-layer seasonality adjustments.

**Step 1: Calculate base daily burn rate**

```
historyCutoff = now - 90 days

relevantAdds = endorsements.filter(
    employer == target AND insurer == target
    AND type == ADD
    AND createdAt > historyCutoff
)

avgPremium = mean(relevantAdds.map(premiumAmount))
dailyEndorsements = relevantAdds.size() / 90
baseDailyBurnRate = avgPremium * dailyEndorsements
```

**Step 2: Apply dual-layer seasonality**

Day-of-week factors (normalized to weekday activity):

```
+------+------+------+------+------+------+------+
| Mon  | Tue  | Wed  | Thu  | Fri  | Sat  | Sun  |
| 1.20 | 1.15 | 1.10 | 1.05 | 1.00 | 0.30 | 0.20 |
+------+------+------+------+------+------+------+
  ^                                          ^
  Peak: HR teams process         Weekend: minimal
  Monday backlog                 endorsement activity
```

Monthly factors (Indian business calendar):

```
Jan  Feb  Mar  Apr  May  Jun  Jul  Aug  Sep  Oct  Nov  Dec
0.9  0.95 1.1  1.4  1.1  1.0  1.0  0.95 1.05 1.3  1.05 0.85
               ^^^                        ^^^
               April: New fiscal     October: Appraisal
               year hiring wave      cycle hiring wave
```

**Step 3: Project 30 days**

```
totalForecastedNeed = 0
for day in 1..30:
    forecastDate = today + day
    dayFactor   = DAY_OF_WEEK_FACTORS[forecastDate.dayOfWeek]
    monthFactor = MONTH_FACTORS[forecastDate.month]
    totalForecastedNeed += baseDailyBurnRate * dayFactor * monthFactor
```

**Step 4: Confidence scoring**

```
confidence = min(95, 50 + (sampleSize * 0.5))
```

| Sample Size (90-day ADDs) | Confidence |
|---|---|
| 0 | 50% (baseline, no data) |
| 20 | 60% |
| 50 | 75% |
| 90+ | 95% (cap) |

The confidence score is communicated in the forecast narrative. Low confidence forecasts prompt the employer to maintain a higher buffer manually.

### 6.3 BalanceForecastService (Shortfall Detection)

The application-layer service orchestrates forecasting with alerting:

```
BalanceForecastService.generateForecast(employerId, insurerId):
  |
  |-- 1. Gather endorsement history (CREATED, VALIDATED,
  |      CONFIRMED, PROVISIONALLY_COVERED)
  |
  |-- 2. Call forecastEngine.generateForecast()
  |      Returns: forecastedNeed, dailyBurnRate, narrative
  |
  |-- 3. Load current EA account balance
  |      shortfall = forecastedNeed - availableBalance
  |      topUpRequired = (shortfall > 0)
  |
  |-- 4. Persist BalanceForecastRecord
  |
  |-- 5. Publish ForecastGenerated event
  |
  |-- 6. If topUpRequired:
  |      a. Increment endorsement.forecast.shortfall.detected counter
  |      b. Publish BalanceForecastAlert event
  |      c. notificationPort.notifyInsufficientBalance(
  |             employerId, forecastedNeed, currentAvailable)
  |      d. Log warning
  |
  +-- 7. Return forecast record
```

**Alert flow:**

```
Forecast detects shortfall
        |
        v
+--------------------+     +----------------------+     +------------------+
| BalanceForecast    |---->| Kafka Event:         |---->| Downstream       |
| Alert Event        |     | BalanceForecastAlert |     | consumers        |
+--------------------+     +----------------------+     | (email, Slack,   |
        |                                               |  dashboard)      |
        v                                               +------------------+
+--------------------+
| NotificationPort   |
| .notifyInsufficient|
| Balance()          |
+--------------------+
```

### 6.4 Scheduled Execution

The `BalanceForecastScheduler` runs periodically to generate forecasts for all active employer-insurer pairs, ensuring proactive detection of upcoming shortfalls before they cause endorsement processing delays.

---

## 7. Balance Minimization Formula

### 7.1 Without Optimization (Naive Approach)

```
Minimum Required Balance = sum(premiumAmount for all pending ADD endorsements)
```

This is the worst case. Every ADD must be fully funded upfront, with no consideration for DELETEs that will return funds.

### 7.2 With Priority-Aware Optimization

```
Minimum Required Balance =
    max(runningBalance[i] for i in 0..N)
    where runningBalance is computed over the priority-sorted sequence
```

This is the **high-water mark** of the running balance across the optimized sequence. Because deletions are processed first, the balance is replenished before additions consume it.

### 7.3 With Full Forecasting + Optimization

```
Minimum Required Balance =
    (30-day forecasted additions based on historical burn rate)
  - (30-day forecasted deletions/credits)
  + 10% safety margin
  - (optimization savings from batch sequencing)
```

### 7.4 Worked Example

**Scenario:** 100 pending endorsements

```
60 ADD endorsements  x $1,000 avg premium = $60,000 total cost
40 DELETE endorsements x $800 avg premium = $32,000 total credits
```

**Naive approach (no optimization):**

```
Required balance = $60,000
(Must have all ADD premiums upfront. DELETE credits arrive later,
 but cannot be counted on until processed.)
```

**Priority-aware optimization (deletions first):**

```
Batch processing order:
  Step 1-40:  Process 40 DELETEs -> balance increases by $32,000
  Step 41-100: Process 60 ADDs   -> balance decreases by $60,000

Starting balance needed:
  After DELETEs:   startBalance + $32,000
  After ADDs:      startBalance + $32,000 - $60,000 >= 0
  Solve:           startBalance >= $28,000

Required balance = $28,000
```

**Savings:**

```
+------------------------------------------+
| Approach       | Required Balance | Saving |
|----------------|-----------------|--------|
| Naive (FIFO)   | $60,000         |   --   |
| Priority-aware | $28,000         |  53%   |
+------------------------------------------+
```

**Visual: Running balance through optimized sequence**

```
Balance ($)
  |
60k|
   |
50k|
   |
40k|. . . . . . . . . . . . . . . . . . . . . . . HIGH-WATER MARK
   |                                                (without optimization)
32k|           *
   |         * |
28k|--------*--+---------- REQUIRED MINIMUM -------  <-- with optimization
   |      *    |          (high-water mark of
   |    *      |           optimized sequence)
   |  *        |
   |*          |
 0 +-----|-----|---------|--> Endorsement sequence
         40    |         100
    DELETEs    ADDs
    processed  processed
    first      second
```

### 7.5 Compounding Effect Over Time

The optimization effect compounds across batch cycles:

```
Batch Cycle 1:  Save 53% -> freed capital reinvested or held for Cycle 2
Batch Cycle 2:  Credits from Cycle 1 DELETEs arrive -> even lower requirement
Batch Cycle 3:  Steady state reached where employer maintains ~40% of naive requirement
```

For an employer with 500 endorsements/month across 3 insurers, annual capital savings:

```
Naive:     500 x $1,000 avg x 12 months = $6,000,000 peak balance needed
Optimized: $6,000,000 x 0.47 (53% reduction) = $2,820,000 peak balance needed
Savings:   $3,180,000 in freed working capital per year
```

---

## 8. Observability

All optimization activity is instrumented with Micrometer metrics exposed to Prometheus/Grafana.

### 8.1 Metrics Catalog

| Metric | Type | Tags | Purpose |
|---|---|---|---|
| `endorsement.ea.reservation` | Counter | `result={success,insufficient}` | Track reservation success rate. High `insufficient` rate signals systemic underfunding |
| `endorsement.active.count` | Gauge | `status={11 states}` | Per-status endorsement counts. Rising QUEUED_FOR_BATCH indicates processing bottleneck |
| `endorsement.forecast.generated` | Counter | `employerId` | Forecast generation rate |
| `endorsement.forecast.shortfall.detected` | Counter | `employerId` | Shortfall frequency. Sustained counts indicate employer needs permanent balance increase |
| `endorsement.batch.optimization.savings` | Summary | `strategy` | Distribution of savings per batch. Mean/p99 show optimization effectiveness |
| `endorsement.batch.optimization.duration` | Timer | -- | Optimizer latency. Alerts if >500ms (impacts batch cycle time) |
| `endorsement.batch.size` | Summary | -- | Items per batch. Low values may indicate balance constraints limiting batch fill |
| `endorsement.scheduler.duration` | Timer | `scheduler, result` | End-to-end batch assembly time |

### 8.2 Key Dashboards

**EA Balance Health (per employer + insurer):**

```
+-------------------------------------------------------+
| EA Balance Overview              [employer] [insurer]  |
|-------------------------------------------------------|
|  Current Balance:    $85,000                           |
|  Reserved:           $22,000                           |
|  Available:          $63,000                           |
|  30-day Forecast:    $71,000 needed                    |
|  Shortfall:          $8,000 (top-up recommended)       |
|  Optimization Savings (last 7d avg): $14,200/batch     |
+-------------------------------------------------------+
```

**Optimization Effectiveness:**

```
+-------------------------------------------------------+
| Batch Optimization Metrics         [last 30 days]     |
|-------------------------------------------------------|
|  Avg savings per batch:  $12,400                      |
|  Total savings:          $248,000                     |
|  Avg batch fill rate:    87%                          |
|  Endorsements deferred:  4.2%                         |
|  Optimizer p99 latency:  180ms                        |
+-------------------------------------------------------+
```

### 8.3 Alerting Rules

| Alert | Condition | Action |
|---|---|---|
| EA Shortfall Imminent | `endorsement.forecast.shortfall.detected` > 0 for employer | Notify employer via email/webhook |
| High Deferral Rate | deferred / total > 20% over 24h | Investigate: insufficient balance or optimizer issue |
| Optimizer Degradation | `endorsement.batch.optimization.savings` p50 drops >50% | Check optimizer configuration, data quality |
| Reservation Failures | `endorsement.ea.reservation{result=insufficient}` > 10/hour | Alert ops team, check employer balance |

---

## 9. Design Trade-offs

| Decision | Alternative Considered | Rationale |
|---|---|---|
| **Reservation-based locking** over pessimistic DB locks | `SELECT ... FOR UPDATE` on EA account row | Reservation semantics are domain-explicit and testable. DB locks would leak infrastructure concerns into the domain and create contention under concurrent endorsement creation. |
| **Optimistic invariant check** in `reserve()` (add then validate) over check-then-act | `if (canFund(amount)) then reserve(amount)` | Check-then-act is vulnerable to TOCTOU races without external synchronization. The optimistic pattern atomically validates and rolls back within the same method call. |
| **Priority enum with `classify()` static method** over strategy pattern | `EndorsementPriorityStrategy` interface with implementations per type | Four priorities are stable and exhaustively determined by type + premium. A strategy pattern adds indirection without benefit. If priorities become configurable, this can evolve. |
| **Composite scoring (60/40 urgency/EA)** over pure priority ordering | Strict priority-only ordering as in `EABalanceCalculator` | Pure priority ignores the magnitude of EA impact. A $100 ADD should be preferred over a $50,000 ADD if both are P2 and balance is tight. The composite score captures this nuance. |
| **60% urgency / 40% EA impact weight split** over equal weighting | 50/50, 70/30, or configurable weights | Coverage gaps (urgency) are more costly than suboptimal balance use. 60/40 biases toward not missing coverage deadlines while still materially optimizing balance. Weights should be tunable in production. |
| **Deletions always first (two-pass)** in ConstraintBatchOptimizer over single-pass score-sorted | Process all endorsements in strict composite-score order | Deletions have zero downside (they free balance). Processing them first unconditionally maximizes the pool for subsequent additions. A single-pass approach might interleave a high-score ADD before a deletion, reducing the pool unnecessarily. |
| **10% safety margin** over dynamic margin | Margin based on premium amount variance (stddev) | 10% is a pragmatic starting point. Dynamic margin requires enough historical data (>100 endorsements) to compute meaningful variance. The system can evolve to use `DescriptiveStatistics.getStandardDeviation()` from the existing Apache Commons Math dependency. |
| **90-day lookback** for burn rate over 30 or 180 days | 30-day (more recent), 180-day (more stable) | 90 days balances recency and stability. 30-day is too volatile (one large employer onboarding skews the rate). 180-day dilutes seasonal patterns. The confidence score already adjusts for low sample sizes. |
| **Feature flag on optimizer** (`endorsement.intelligence.batch-optimizer.enabled`) | Always-on optimizer | New optimization logic can introduce regressions. The flag allows instant rollback to FIFO ordering. In production, we would A/B test optimizer-on vs. optimizer-off and measure deferral rate and savings. |
| **Graceful fallback to FIFO** on optimizer failure | Fail the entire batch assembly on optimizer error | Batch processing must not stop because the optimizer throws an exception. FIFO is suboptimal but correct. The optimizer failure is logged and metrics alert on degradation. |
| **Shortfall alert via events + notifications** over synchronous blocking | Block endorsement creation if shortfall predicted | Blocking endorsement creation on a *prediction* would cause false positives. Instead, the system processes endorsements (with provisional coverage) and alerts the employer asynchronously. The batch optimizer handles balance constraints at batch assembly time. |
| **Per-endorsement reservation** over batch-level reservation | Reserve total batch cost at batch assembly time | Per-endorsement reservation provides finer-grained accounting. If one endorsement is rejected, only its reservation is released. Batch-level reservation would require partial release logic. |

---

## Appendix: Source File Reference

| Component | File |
|---|---|
| EA Account domain model | `src/main/java/.../domain/model/EAAccount.java` |
| Endorsement Priority enum | `src/main/java/.../domain/model/EndorsementPriority.java` |
| EA Balance Calculator | `src/main/java/.../domain/service/EABalanceCalculator.java` |
| Constraint Batch Optimizer | `src/main/java/.../infrastructure/intelligence/ConstraintBatchOptimizer.java` |
| Statistical Forecast Engine | `src/main/java/.../infrastructure/intelligence/StatisticalForecastEngine.java` |
| Balance Forecast Service | `src/main/java/.../application/service/BalanceForecastService.java` |
| Batch Assembly Scheduler | `src/main/java/.../application/scheduler/BatchAssemblyScheduler.java` |
| Create Endorsement Handler | `src/main/java/.../application/handler/CreateEndorsementHandler.java` |
| Metrics Config | `src/main/java/.../infrastructure/config/MetricsConfig.java` |
| Gauge Registrar | `src/main/java/.../infrastructure/config/EndorsementGaugeRegistrar.java` |
| BatchOptimizerPort (port) | `src/main/java/.../domain/port/BatchOptimizerPort.java` |
| BalanceForecastPort (port) | `src/main/java/.../domain/port/BalanceForecastPort.java` |
