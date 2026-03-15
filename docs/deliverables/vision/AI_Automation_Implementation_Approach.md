# AI Automation Implementation Approach

**Project:** Plum Endorsement Management System
**Date:** March 14, 2026 (Updated)
**Scope:** Implementable AI/automation enhancements using existing codebase, infrastructure, and data
**Companion:** [GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md) -- which of these 24 enhancements to augment with Ollama/LLM

---

## Table of Contents

- [1. Executive Summary](#1-executive-summary)
- [2. Current State: What's Already Implemented](#2-current-state-whats-already-implemented)
  - [2.1 The Five Intelligence Pillars](#21-the-five-intelligence-pillars)
  - [2.2 Infrastructure Already in Place](#22-infrastructure-already-in-place)
- [3. Phase 1: Feedback Loops & Self-Calibration (Week 1-2)](#3-phase-1-feedback-loops--self-calibration-week-1-2)
  - [3.1 Anomaly False Positive Tracking](#31-anomaly-false-positive-tracking)
  - [3.2 Forecast Accuracy Backtesting](#32-forecast-accuracy-backtesting)
  - [3.3 Error Resolution Success Tracking](#33-error-resolution-success-tracking)
  - [3.4 STP Rate Trending with Daily Snapshots](#34-stp-rate-trending-with-daily-snapshots)
- [4. Phase 2: Enhanced Detection Rules (Week 3-4)](#4-phase-2-enhanced-detection-rules-week-3-4)
  - [4.1 Configurable Anomaly Thresholds](#41-configurable-anomaly-thresholds)
  - [4.2 New Anomaly Rule: Employer Dormancy Break](#42-new-anomaly-rule-employer-dormancy-break)
  - [4.3 New Anomaly Rule: Cross-Insurer Duplication](#43-new-anomaly-rule-cross-insurer-duplication)
  - [4.4 Enhanced Error Patterns: Insurer-Specific Rules](#44-enhanced-error-patterns-insurer-specific-rules)
  - [4.5 Anomaly Correlation: Multi-Signal Detection](#45-anomaly-correlation-multi-signal-detection)
- [5. Phase 3: Operational Intelligence (Week 5-6)](#5-phase-3-operational-intelligence-week-5-6)
  - [5.1 Distributed Scheduler Coordination (ShedLock)](#51-distributed-scheduler-coordination-shedlock)
  - [5.2 Graceful Shutdown for Intelligence Schedulers](#52-graceful-shutdown-for-intelligence-schedulers)
  - [5.3 Data Retention for Intelligence Results](#53-data-retention-for-intelligence-results)
  - [5.4 Intelligence Health Indicators](#54-intelligence-health-indicators)
  - [5.5 Enhanced Batch Optimizer: Configurable Weights](#55-enhanced-batch-optimizer-configurable-weights)
  - [5.6 Process Mining: Variant Detection](#56-process-mining-variant-detection)
- [6. Phase 4: Enhanced Narratives & Explanations (Week 7-8)](#6-phase-4-enhanced-narratives--explanations-week-7-8)
  - [6.1 Rich Anomaly Explanations](#61-rich-anomaly-explanations)
  - [6.2 Forecast Narratives with Actionable Advice](#62-forecast-narratives-with-actionable-advice)
  - [6.3 Error Resolution Explanation Chains](#63-error-resolution-explanation-chains)
  - [6.4 Process Mining Bottleneck Recommendations](#64-process-mining-bottleneck-recommendations)
  - [6.5 Batch Optimization Reports](#65-batch-optimization-reports)
- [7. Implementation Summary](#7-implementation-summary)
- [8. Architecture: No Changes Required](#8-architecture-no-changes-required)

---

## 1. Executive Summary

The Plum Endorsement system has a working intelligence layer built on **5 rule-based adapters** behind hexagonal port interfaces. All 5 pillars -- anomaly detection, balance forecasting, error resolution, process mining, and batch optimization -- are operational with deterministic, auditable algorithms.

This document identifies **24 concrete enhancements** that can be implemented **now** using the existing technology stack (Java 21, Spring Boot 3, PostgreSQL, Redis, Kafka, Apache Commons Math). No new infrastructure, ML frameworks, Python sidecars, or external AI services are required. Each enhancement improves accuracy, automation rate, or observability within the current architecture.

### What "Implementable Now" Means

- Uses existing Java/Spring Boot codebase -- no new languages or runtimes
- Uses existing infrastructure (PostgreSQL, Redis, Kafka, Prometheus/Grafana)
- Uses existing port/adapter architecture -- enhances adapters, not interfaces
- Requires no training data, ML models, or external AI APIs
- Can be deployed incrementally behind feature flags (`@ConditionalOnProperty`)
- Estimated total effort: 6-8 developer-weeks across 4 phases

### GenAI Augmentation (New)

Each of the 24 enhancements below has been assessed for **Ollama/LLM augmentation** in the companion [GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md). The assessment classifies each into one of three buckets:

| Bucket | Tag | Meaning | Extra Effort |
|--------|-----|---------|-------------|
| **GenAI adds real value** | `[GenAI: HIGH]` | LLM enrichment significantly improves output quality | +1-2 days per item |
| **GenAI adds marginal value** | `[GenAI: MARGINAL]` | Narrative overlay, rules still make decisions | +0.5-1 day per item |
| **Keep rule-based** | `[GenAI: NONE]` | Deterministic/infrastructure — GenAI adds no value | 0 days |

Two Ollama adapters are already deployed: `OllamaAugmentedAnomalyDetector` and `OllamaErrorResolver`. Activate via `SPRING_PROFILES_ACTIVE=ollama`.

---

## 2. Current State: What's Already Implemented

### 2.1 The Five Intelligence Pillars

| Pillar | Adapter | Algorithm | Schedule | Status |
|--------|---------|-----------|----------|--------|
| Anomaly Detection | `RuleBasedAnomalyDetector` | 5 scoring rules (volume spike, add/delete cycling, suspicious timing, unusual premium, dormancy break) | Every 5 min | **Operational** |
| Balance Forecasting | `StatisticalForecastEngine` | 90-day linear extrapolation with day-of-week + monthly seasonality | Daily 6 AM | **Operational** |
| Error Resolution | `SimulatedErrorResolver` | Priority-ordered keyword matching (5 patterns) | On rejection | **Operational** |
| Process Mining | `EventStreamAnalyzer` | Event pair analysis with percentile aggregation | Daily 3 AM | **Operational** |
| Batch Optimization | `ConstraintBatchOptimizer` | 0-1 knapsack DP with composite scoring | Every 15 min | **Operational** |

### 2.2 Infrastructure Already in Place

```
Available Now:
  [x] 5 port interfaces in domain layer (AnomalyDetectionPort, BalanceForecastPort, etc.)
  [x] 5 rule-based adapters in infrastructure layer
  [x] 5 application services orchestrating the adapters
  [x] 5 schedulers driving periodic analysis
  [x] 14 REST endpoints under /api/v1/intelligence/
  [x] 6 intelligence event types in EndorsementEvent sealed interface
  [x] 12+ Micrometer metrics (counters, timers, gauges, summaries)
  [x] Grafana dashboard (intelligence-monitoring.json)
  [x] Kafka event publishing for all intelligence outputs
  [x] PostgreSQL persistence for all results
  [x] @ConditionalOnProperty feature flags on all schedulers
  [x] Thread.sleep simulation of ML latency (validates timeout handling)
  [x] Apache Commons Math for statistical operations
```

---

## 3. Phase 1: Feedback Loops & Self-Calibration (Week 1-2)

These enhancements make the existing intelligence **learn from its own outputs** without any ML models.

### 3.1 Anomaly False Positive Tracking `[GenAI: MARGINAL]`

**Problem**: Anomaly detection flags items at a fixed 0.7 threshold. The system never learns which flags were useful and which were noise. Reviewers dismiss anomalies but that feedback is discarded.

**Current flow**:
```
Endorsement → RuleBasedAnomalyDetector → score ≥ 0.7 → FLAGGED → (reviewer) → DISMISSED/CONFIRMED
                                                                                    ↑
                                                                        Feedback is lost
```

**Enhanced flow**:
```
Endorsement → RuleBasedAnomalyDetector → score ≥ threshold → FLAGGED → (reviewer) → DISMISSED/CONFIRMED
                                              ↑                                            |
                                              |                                            v
                                        Threshold adjusts                     Track per-rule precision
                                        based on 30-day                       falsePositiveRate =
                                        precision metric                      dismissed / (dismissed + confirmed)
```

**Implementation**:

**Modify** `AnomalyDetectionService.java`:
- After reviewer dismisses/confirms anomaly, compute running precision per `AnomalyType`
- Store in Redis: `anomaly:precision:{VOLUME_SPIKE}` = `{ confirmed: 23, dismissed: 87, total: 110, precision: 0.209 }`
- Update precision on every review action

**Modify** `RuleBasedAnomalyDetector.java`:
- Accept a `Map<AnomalyType, Double>` of current precision rates (injected from service)
- Rules with precision < 20% over 30-day window: raise their minimum score threshold by 0.05
- Rules with precision > 80%: lower their minimum score threshold by 0.05
- Bounds: threshold never goes below 0.5 or above 0.95

**Modify** `application.yml`:
```yaml
endorsement.intelligence.anomaly-detection:
  feedback-enabled: true
  precision-window-days: 30
  threshold-adjustment-step: 0.05
  min-threshold: 0.5
  max-threshold: 0.95
```

**New metrics**:
- `endorsement.anomaly.precision` (Gauge, tags: anomalyType) -- current 30-day precision
- `endorsement.anomaly.threshold` (Gauge, tags: anomalyType) -- current effective threshold

**Impact**: Reduces alert fatigue by 30-60% over first 90 days as low-precision rules auto-tighten.

### 3.2 Forecast Accuracy Backtesting `[GenAI: MARGINAL]`

**Problem**: `BalanceForecastRecord` has `forecastedAmount` and `actualAmount` fields, but `actualAmount` is never populated. The system generates forecasts but never checks if they were right.

**Current flow**:
```
Day 1: Generate forecast (₹750,000 needed in 30 days) → save to DB
Day 31: ... nothing happens. Forecast accuracy is never computed.
```

**Enhanced flow**:
```
Day 1:  Generate forecast (₹750,000 needed in 30 days) → save to DB
Day 31: Backtester runs → calculates actual spend from endorsement history
        actual = sum(ADD premiums) - sum(DELETE credits) over the 30-day window
        accuracy = 1 - |actual - forecasted| / forecasted
        → Update BalanceForecastRecord.actualAmount and accuracy
        → Compute MAPE (Mean Absolute Percentage Error) across all forecasts
        → If MAPE > 30%, log warning and recommend seasonality factor review
```

**Implementation**:

**Create** scheduler `ForecastBacktestScheduler.java` in `application/scheduler/`:
- Runs daily at 7 AM (after forecast generation at 6 AM)
- Queries `BalanceForecastRecord` where `forecastDate + 30 days <= today` and `actualAmount IS NULL`
- For each record: compute actual spend from endorsement history, update record
- Compute rolling 30-day MAPE per (employerId, insurerId) pair
- Store MAPE in Redis for dashboard display

**Modify** `BalanceForecastService.java`:
- Add method `backfillActuals(BalanceForecastRecord record)` that queries endorsement history
- Compute accuracy and persist

**New metrics**:
- `endorsement.forecast.mape` (Gauge, tags: employerId, insurerId) -- Mean Absolute Percentage Error
- `endorsement.forecast.accuracy.distribution` (Summary) -- accuracy distribution

**Impact**: Enables data-driven seasonality factor tuning. If March accuracy drops to 60%, the March factor (1.1) needs adjustment.

### 3.3 Error Resolution Success Tracking `[GenAI: MARGINAL]` — **✅ COMPLETED**

> **Implementation Status**: Fully implemented with V20 migration adding `outcome`, `outcome_at`, `outcome_endorsement_status` columns to `error_resolutions` table. `ErrorResolutionService.trackOutcome()` hooks into `ProcessEndorsementHandler` at CONFIRMED/REJECTED/FAILED_PERMANENT transitions. Stats endpoint enhanced with `successCount`, `failureCount`, `successRate`. Covered by 5 unit tests, 1 API test, 1 BDD scenario, 1 E2E test.

**Problem**: When errors are auto-resolved (confidence >= 0.95), the system resubmits the endorsement but never checks if the resubmission succeeded. If the fix was wrong, the endorsement fails again silently.

**Enhanced flow**:
```
Error detected → SimulatedErrorResolver → confidence 0.96 → auto-apply fix
                                                                    |
                                                                    v
                                                          Resubmit to insurer
                                                                    |
                                                         +----------+-----------+
                                                         |                      |
                                                         v                      v
                                                    Confirmed                Rejected again
                                                         |                      |
                                                         v                      v
                                                    Track: fix_success++    Track: fix_failure++
                                                                                |
                                                                                v
                                                                    If fix_failure_rate > 20%:
                                                                      raise auto-apply threshold
                                                                      for this error type
```

**Implementation**:

**Modify** `ErrorResolutionService.java`:
- After auto-applying a fix, store the resolution ID in the endorsement's metadata
- Listen for subsequent `Confirmed` or `Rejected` events on that endorsement
- Update the resolution record with `fixSucceeded: true/false`
- Compute per-errorType success rate in Redis

**Modify** `application.yml`:
```yaml
endorsement.intelligence.error-resolution:
  track-fix-outcomes: true
  success-rate-window-days: 30
  min-success-rate-for-auto-apply: 0.80  # Disable auto-apply if success rate drops below 80%
```

**New metrics**:
- `endorsement.error.fix.outcome` (Counter, tags: errorType, outcome=success|failure)
- `endorsement.error.fix.success_rate` (Gauge, tags: errorType)

**Impact**: Prevents cascading bad fixes. If DATE_FORMAT auto-corrections start failing (e.g., an insurer changes their format), the system auto-disables auto-apply within 30 days.

### 3.4 STP Rate Trending with Daily Snapshots `[GenAI: MARGINAL]` — **✅ COMPLETED**

> **Implementation Status**: Fully implemented with V19 migration creating `stp_rate_snapshots` table. New `StpRateSnapshot` domain model, `StpRateSnapshotRepository` port, JPA adapter. New endpoint `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days={30}` returning `StpRateTrendResponse` with data points, current rate, and change percentage. `ProcessMiningScheduler` captures daily snapshots. Covered by 4 unit tests, 1 API test, 1 BDD scenario, 1 E2E test.

**Problem**: Process mining computes STP rates daily but only stores the latest value. No historical trend is available -- operators cannot tell if STP is improving or degrading.

**Implementation**:

**Create** entity `StpRateSnapshot.java` in `domain/model/`:
```java
@Getter @Setter @Builder
public class StpRateSnapshot {
    private UUID id;
    private UUID insurerId;
    private BigDecimal stpRate;
    private int totalEndorsements;
    private int successfulEndorsements;
    private LocalDate snapshotDate;
    private Instant createdAt;
}
```

**Create** Flyway migration `V16__create_stp_rate_snapshots.sql`:
```sql
CREATE TABLE stp_rate_snapshots (
    id UUID PRIMARY KEY,
    insurer_id UUID NOT NULL,
    stp_rate DECIMAL(5,2) NOT NULL,
    total_endorsements INTEGER NOT NULL,
    successful_endorsements INTEGER NOT NULL,
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(insurer_id, snapshot_date)
);
```

**Modify** `ProcessMiningService.java`:
- After computing STP rates, save a snapshot per insurer per day
- Add query method: `getStpRateTrend(insurerId, startDate, endDate)` returning `List<StpRateSnapshot>`

**Add** API endpoint `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days=30`

**Impact**: Enables trend visualization on the Intelligence dashboard. Operators can see if process improvements are working.

---

## 4. Phase 2: Enhanced Detection Rules (Week 3-4)

These enhancements add new detection rules and pattern matching within the existing rule-based architecture.

### 4.1 Configurable Anomaly Thresholds `[GenAI: NONE]`

**Problem**: All 5 anomaly rules have hardcoded thresholds in Java code. Operators cannot tune them without a code deployment.

**Implementation**:

**Modify** `application.yml`:
```yaml
endorsement.intelligence.anomaly-detection:
  rules:
    volume-spike:
      enabled: true
      min-count-24h: 10              # Minimum endorsements to trigger
      multiplier: 5.0                # X times daily average
      base-score: 0.5
      max-score: 0.95
    add-delete-cycling:
      enabled: true
      window-days: 30
      score: 0.85
    suspicious-timing:
      enabled: true
      days-ahead-threshold: 7
      score: 0.75
    unusual-premium:
      enabled: true
      std-dev-multiplier: 3.0       # Number of standard deviations
      min-samples: 5
      score: 0.70
```

**Modify** `RuleBasedAnomalyDetector.java`:
- Inject all thresholds via `@Value` annotations with defaults matching current hardcoded values
- Allow each rule to be individually disabled via `enabled: false`

**Impact**: Operators can tune detection sensitivity without code changes. A new insurer with different volume patterns can be configured independently.

### 4.2 New Anomaly Rule: Employer Dormancy Break `[GenAI: HIGH]` — **✅ COMPLETED**

> **Implementation Status**: Fully implemented as `checkDormancyBreak()` in `RuleBasedAnomalyScorer.java`. Detects employees with no endorsement activity for 90+ days. Score: `Math.min(0.85, 0.6 + (daysSinceLastActivity / 365.0))`. `DORMANCY_BREAK` added to `AnomalyType` enum. Existing `OllamaAugmentedAnomalyDetector` automatically enriches dormancy break explanations via LLM. Covered by 5 unit tests, 1 API test, 1 BDD scenario, 1 E2E test.

**Problem**: When an employer that hasn't submitted endorsements in 60+ days suddenly submits a batch, this is suspicious but not caught by the volume spike rule (which compares to 30-day average, which is 0).

**Implementation**:

**Add** to `RuleBasedAnomalyDetector.java`:
```java
private Optional<AnomalyResult> checkDormancyBreak(Endorsement endorsement, List<Endorsement> history) {
    // Find most recent endorsement before current one
    Instant lastActivity = history.stream()
        .filter(e -> e.getCreatedAt().isBefore(endorsement.getCreatedAt()))
        .map(Endorsement::getCreatedAt)
        .max(Instant::compareTo)
        .orElse(null);

    if (lastActivity == null) return Optional.empty(); // First ever endorsement

    long daysSinceLastActivity = ChronoUnit.DAYS.between(lastActivity, endorsement.getCreatedAt());
    if (daysSinceLastActivity >= dormancyThresholdDays) { // default: 60
        double score = Math.min(0.85, 0.6 + (daysSinceLastActivity / 365.0));
        return Optional.of(new AnomalyResult(
            "DORMANCY_BREAK", score,
            String.format("Employer dormant for %d days then submitted endorsement. " +
                "Last activity: %s", daysSinceLastActivity, lastActivity)
        ));
    }
    return Optional.empty();
}
```

**Add** `DORMANCY_BREAK` to `AnomalyType.java` enum.

**Config**:
```yaml
endorsement.intelligence.anomaly-detection.rules:
  dormancy-break:
    enabled: true
    dormancy-threshold-days: 60
    base-score: 0.60
    max-score: 0.85
```

### 4.3 New Anomaly Rule: Cross-Insurer Duplication `[GenAI: HIGH]`

**Problem**: The same employee being added to multiple insurers simultaneously could indicate coverage stacking fraud or data entry errors. Currently not detected.

**Implementation**:

**Add** to `RuleBasedAnomalyDetector.java`:
```java
private Optional<AnomalyResult> checkCrossInsurerDuplication(
        Endorsement endorsement, List<Endorsement> history) {
    if (endorsement.getType() != EndorsementType.ADD) return Optional.empty();

    long duplicateCount = history.stream()
        .filter(e -> e.getType() == EndorsementType.ADD)
        .filter(e -> e.getEmployeeId().equals(endorsement.getEmployeeId()))
        .filter(e -> !e.getInsurerId().equals(endorsement.getInsurerId()))
        .filter(e -> e.getCreatedAt().isAfter(Instant.now().minus(30, ChronoUnit.DAYS)))
        .count();

    if (duplicateCount > 0) {
        return Optional.of(new AnomalyResult(
            "CROSS_INSURER_DUPLICATE", 0.80,
            String.format("Employee %s has ADD endorsements across %d different insurers in 30 days",
                endorsement.getEmployeeId(), duplicateCount + 1)
        ));
    }
    return Optional.empty();
}
```

**Add** `CROSS_INSURER_DUPLICATE` to `AnomalyType.java` enum.

### 4.4 Enhanced Error Patterns: Insurer-Specific Rules `[GenAI: HIGH]`

**Problem**: `SimulatedErrorResolver` uses the same 5 patterns for all insurers. In reality, each insurer has unique error formats, field requirements, and response structures.

**Implementation**:

**Create** `InsurerErrorPatterns.java` in `infrastructure/intelligence/`:
```java
@Component
public class InsurerErrorPatterns {
    // Per-insurer pattern overrides loaded from configuration
    private final Map<String, List<ErrorPattern>> insurerPatterns;

    record ErrorPattern(String keyword, String errorType, double confidence,
                        Function<String, String> corrector) {}

    // ICICI Lombard: expects "PLMICICI-" prefix, different date format
    // Niva Bupa: batch CSV column order errors, "column mismatch" pattern
    // Bajaj Allianz: SOAP fault codes, XML validation errors
}
```

**Modify** `SimulatedErrorResolver.java`:
- Accept `insurerId` parameter (already in the method signature but unused)
- Look up insurer-specific patterns first; fall back to generic patterns if no match
- Add insurer-specific member ID formats (ICICI: "PLMICICI-", Bajaj: "BAJ-", Niva: no prefix)

**Config**:
```yaml
endorsement.intelligence.error-resolution:
  insurer-patterns:
    icici-lombard:
      member-id-prefix: "PLMICICI-"
      date-format: "yyyy-MM-dd"
    bajaj-allianz:
      member-id-prefix: "BAJ-"
      date-format: "dd/MM/yyyy"
      soap-fault-patterns:
        - keyword: "SOAP-Fault"
          type: "SOAP_VALIDATION_ERROR"
          confidence: 0.92
    niva-bupa:
      member-id-prefix: ""
      csv-column-patterns:
        - keyword: "column mismatch"
          type: "CSV_FORMAT_ERROR"
          confidence: 0.94
```

**Impact**: Increases auto-resolution rate by handling insurer-specific quirks. Estimated improvement: auto-apply rate from ~40% to ~60%.

### 4.5 Anomaly Correlation: Multi-Signal Detection `[GenAI: HIGH]`

**Problem**: Each anomaly rule runs independently. An endorsement that triggers both VOLUME_SPIKE and UNUSUAL_PREMIUM simultaneously is much more suspicious than either alone, but the system just takes the max score.

**Implementation**:

**Modify** `RuleBasedAnomalyDetector.java`:
```java
// After running all individual rules, check for correlations
if (firedRules.size() >= 2) {
    double correlatedScore = Math.min(0.98, maxScore + (0.05 * (firedRules.size() - 1)));
    return new AnomalyResult(
        "CORRELATED_ANOMALY",
        correlatedScore,
        String.format("Multiple anomaly signals detected: %s. " +
            "Individual scores: %s. Correlated score: %.2f",
            firedRules.stream().map(AnomalyResult::anomalyType).collect(joining(", ")),
            firedRules.stream().map(r -> String.format("%s=%.2f", r.anomalyType(), r.score())).collect(joining(", ")),
            correlatedScore)
    );
}
```

**Add** `CORRELATED_ANOMALY` to `AnomalyType.java` enum.

**Impact**: Reduces false negatives. An endorsement with volume spike (0.65, below threshold) + unusual premium (0.55, below threshold) individually would be missed. With correlation: 0.65 + 0.05 = 0.70 -- flagged.

---

## 5. Phase 3: Operational Intelligence (Week 5-6)

These enhancements improve the operational aspects of the intelligence system.

### 5.1 Distributed Scheduler Coordination (ShedLock) `[GenAI: NONE]`

**Problem**: Running 2+ backend instances causes duplicate batch submissions and anomaly analysis. All `@Scheduled` beans have no distributed lock.

**Implementation**:

**Add** dependency to `build.gradle.kts`:
```kotlin
implementation("net.javacrumbs.shedlock:shedlock-spring:5.10.0")
implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:5.10.0")
```

**Create** Flyway migration `V16__create_shedlock_table.sql`:
```sql
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMPTZ NOT NULL,
    locked_at TIMESTAMPTZ NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
```

**Create** `SchedulerLockConfig.java`:
```java
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT14M")
public class SchedulerLockConfig {
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
```

**Modify** all 7 schedulers -- add `@SchedulerLock`:
```java
@Scheduled(cron = "${endorsement.batch.schedule-cron}")
@SchedulerLock(name = "batchAssembly", lockAtLeastFor = "PT1M", lockAtMostFor = "PT14M")
public void assembleAndSubmitBatches() { ... }
```

**Impact**: Enables safe multi-instance deployment. Critical for production scale-out.

### 5.2 Graceful Shutdown for Intelligence Schedulers `[GenAI: NONE]`

**Problem**: No `server.shutdown: graceful` configured. When a pod is killed, in-flight scheduler runs and Kafka commits may be lost.

**Implementation**:

**Modify** `application.yml`:
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Modify** all schedulers -- add interrupt checking:
```java
public void runBatchAnalysis() {
    List<Endorsement> batch = repository.findRecentEndorsements(since);
    for (Endorsement e : batch) {
        if (Thread.currentThread().isInterrupted()) {
            log.warn("Scheduler interrupted during shutdown, processed {} of {}", processed, batch.size());
            break;
        }
        analyzeEndorsement(e);
        processed++;
    }
}
```

### 5.3 Data Retention for Intelligence Results `[GenAI: NONE]`

**Problem**: `DataRetentionScheduler` exists but only archives endorsements. Anomaly detections, forecast records, error resolutions, and process mining metrics accumulate indefinitely.

**Implementation**:

**Modify** `DataRetentionScheduler.java`:
- Add retention policies for intelligence tables:
```java
// Dismissed anomalies: retain 90 days
anomalyRepo.deleteByStatusAndFlaggedAtBefore(DISMISSED, Instant.now().minus(90, DAYS));

// Forecast records with backtested accuracy: retain 180 days
forecastRepo.deleteByCreatedAtBefore(Instant.now().minus(180, DAYS));

// Error resolutions: retain 90 days
errorResolutionRepo.deleteByCreatedAtBefore(Instant.now().minus(90, DAYS));

// Process mining metrics: retain 365 days (for yearly trend analysis)
processMiningRepo.deleteByCalculatedAtBefore(Instant.now().minus(365, DAYS));
```

**Config**:
```yaml
endorsement.intelligence.data-retention:
  anomaly-days: 90
  forecast-days: 180
  error-resolution-days: 90
  process-mining-days: 365
```

### 5.4 Intelligence Health Indicators `[GenAI: NONE]`

**Problem**: `/actuator/health` reports database and Kafka health but not intelligence subsystem health. If anomaly detection hasn't run in 15 minutes (scheduler stuck), no alert fires.

**Implementation**:

**Create** `IntelligenceHealthIndicator.java`:
```java
@Component
public class IntelligenceHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        Instant lastAnomalyRun = redis.get("scheduler:lastRun:anomalyDetection");
        Instant lastForecastRun = redis.get("scheduler:lastRun:balanceForecast");

        boolean anomalyStale = lastAnomalyRun.isBefore(Instant.now().minus(15, MINUTES));
        boolean forecastStale = lastForecastRun.isBefore(Instant.now().minus(25, HOURS));

        if (anomalyStale || forecastStale) {
            return Health.down()
                .withDetail("anomalyDetection", anomalyStale ? "STALE" : "OK")
                .withDetail("balanceForecast", forecastStale ? "STALE" : "OK")
                .build();
        }
        return Health.up().build();
    }
}
```

**Modify** all schedulers -- record last run timestamp in Redis after each execution.

**Impact**: Kubernetes liveness probe detects stuck schedulers and restarts the pod.

### 5.5 Enhanced Batch Optimizer: Configurable Weights `[GenAI: NONE]`

**Problem**: Urgency weight (0.6) and EA impact weight (0.4) are hardcoded. Different business contexts may need different priorities (e.g., during enrollment season, urgency should be 0.8).

**Implementation**:

**Modify** `ConstraintBatchOptimizer.java`:
```java
@Value("${endorsement.intelligence.batch-optimizer.urgency-weight:0.6}")
private double urgencyWeight;

@Value("${endorsement.intelligence.batch-optimizer.ea-impact-weight:0.4}")
private double eaImpactWeight;
```

**Config**:
```yaml
endorsement.intelligence.batch-optimizer:
  urgency-weight: 0.6
  ea-impact-weight: 0.4
  # During enrollment season, override:
  # urgency-weight: 0.8
  # ea-impact-weight: 0.2
```

### 5.6 Process Mining: Variant Detection `[GenAI: MARGINAL]`

**Problem**: `EventStreamAnalyzer` treats all endorsement paths equally. It cannot tell you "78% follow the happy path, 12% go through real-time submission, 7% require retries, 3% fail permanently."

**Implementation**:

**Modify** `EventStreamAnalyzer.java`:
- After grouping events by endorsementId, extract the full state sequence as a "variant" string
- Count occurrences of each variant
- Store top-10 variants per insurer

```java
// Extract variant
String variant = events.stream()
    .map(EndorsementEvent::eventType)
    .collect(Collectors.joining(" → "));
// Example: "Created → Validated → QueuedForBatch → BatchSubmitted → Confirmed"

variantCounts.merge(variant, 1, Integer::sum);
```

**Create** entity `ProcessVariant.java`:
```java
public class ProcessVariant {
    private UUID id;
    private UUID insurerId;
    private String variant;        // "Created → Validated → ... → Confirmed"
    private int occurrences;
    private BigDecimal percentage;
    private boolean isHappyPath;
    private Instant calculatedAt;
}
```

**Add** API endpoint: `GET /api/v1/intelligence/process-mining/variants?insurerId={id}`

**Impact**: Enables conformance checking -- operators can see which process paths are most common and investigate unusual variants.

---

## 6. Phase 4: Enhanced Narratives & Explanations (Week 7-8)

These enhancements make the intelligence system more **interpretable** and **actionable** for operators.

### 6.1 Rich Anomaly Explanations `[GenAI: HIGH]` — *Already deployed as `OllamaAugmentedAnomalyDetector`*

**Problem**: Current anomaly explanations are generic: "Volume spike detected: 15 endorsements in 24h vs average of 2.5/day." A human reviewer needs more context to decide whether to dismiss or confirm.

**Implementation**:

**Modify** `RuleBasedAnomalyDetector.java` -- enhance explanation generation:

```
Current:  "Volume spike detected: 15 endorsements in 24h vs average of 2.5/day"

Enhanced: "Volume spike detected for employer {employerName}:
           - 15 endorsements in last 24 hours (6x the 30-day average of 2.5/day)
           - Previous spikes: 2 in last 90 days (both dismissed as legitimate hiring events)
           - Endorsement types in spike: 12 ADD, 2 UPDATE, 1 DELETE
           - Total premium impact: Rs. 1,87,500
           - Similar employers (same industry/size) average: 4.2/day
           Recommended action: Review if this aligns with known hiring activity."
```

**Data needed** (all available in existing tables):
- Previous anomalies for the same employer (query `AnomalyDetection` table)
- Endorsement type breakdown (already in the history list)
- Premium sum (compute from endorsement data)
- Review history (from anomaly status transitions)

### 6.2 Forecast Narratives with Actionable Advice `[GenAI: HIGH]`

**Problem**: Current forecast narrative is informational. Operators see "shortfall of Rs. 2,50,000" but don't know what to do or how urgent it is.

**Enhanced narrative**:
```
Current:  "Based on 90-day trends, employer will need approximately Rs. 1,12,500
           over the next 30 days. Daily burn rate: Rs. 3,750."

Enhanced: "EA Account Status for {employerName} with {insurerName}:

           Current available balance:     Rs. 5,00,000
           Projected 30-day requirement:  Rs. 7,50,000
           Projected shortfall:           Rs. 2,50,000

           Timeline:
           - Balance will cover endorsements for approximately 20 days
           - Shortfall begins around April 3, 2026
           - At current rate, Rs. 2,50,000 top-up is needed by April 1

           Confidence: 75% (based on 45 endorsements over 90 days)
           Seasonality note: April factor = 1.4x (fiscal year hiring wave)

           Action required: Notify employer to top up EA account by Rs. 2,50,000
           before April 1 to avoid endorsement processing delays."
```

**Implementation**: Enhance `StatisticalForecastEngine.generateForecast()` to compute `daysUntilShortfall` and include actionable text in the narrative.

### 6.3 Error Resolution Explanation Chains `[GenAI: HIGH]` — *Already deployed as `OllamaErrorResolver`*

**Problem**: When an error is auto-resolved, the explanation says "Date format mismatch corrected." The operator reviewing the fix needs to understand the full chain of reasoning.

**Enhanced explanation**:
```
Current:  "Date format mismatch. Corrected: '07-03-1990' → '1990-03-07'"

Enhanced: "Error Resolution Chain:
           1. Insurer rejected endorsement with error: 'Invalid DOB format: 07-03-1990'
           2. Pattern matched: DATE_FORMAT_ERROR (keyword: 'dob', 'format')
           3. Extracted date '07-03-1990' via regex \\d{2}[-/]\\d{2}[-/]\\d{4}
           4. Parsed as DD-MM-YYYY → reformatted to ISO-8601: 1990-03-07
           5. Confidence: 98% (DATE_FORMAT corrections succeed 97.3% historically)
           6. Auto-applied (confidence ≥ 95% threshold)
           7. Endorsement resubmitted to insurer at 2026-03-13T10:45:23Z"
```

**Implementation**: Build explanation strings incrementally as the resolver processes each step. Store the full chain in the `resolution` field.

### 6.4 Process Mining Bottleneck Recommendations `[GenAI: HIGH]`

**Problem**: Bottleneck insights say "averages 6.2 hours" but don't suggest what to do about it.

**Enhanced insight**:
```
Current:  "Bottleneck detected: QUEUED_FOR_BATCH → BATCH_SUBMITTED averages 6.2 hours"

Enhanced: "Bottleneck detected: QUEUED_FOR_BATCH → BATCH_SUBMITTED

           Metrics:
           - Average duration: 6.2 hours (up from 4.1 hours last week)
           - P95 duration: 14.1 hours
           - P99 duration: 22.3 hours
           - Sample count: 342 (last 7 days)

           Possible causes:
           - Batch assembly runs every 15 minutes, but queue depth exceeds batch size limit
           - Insurer batch API may have rate limiting or processing delays
           - Consider: Increase batch size limit or reduce assembly interval

           Trend: Worsening (+51% avg duration vs. prior 7 days)
           Impact: 342 endorsements experienced delays"
```

**Implementation**: Compare current metrics to prior period (stored in `ProcessMiningMetric` history). Add trend calculation and static recommendation mapping based on transition type.

### 6.5 Batch Optimization Reports `[GenAI: HIGH]`

**Problem**: The optimizer returns a savings estimate but no breakdown of why certain endorsements were included or deferred.

**Enhanced output**:
```
Current:  "Strategy: KNAPSACK_WITH_DELETES_FIRST, Savings: Rs. 15,000"

Enhanced: "Batch Optimization Report:
           Strategy: KNAPSACK_WITH_DELETES_FIRST

           Included (12 endorsements):
           - 3 DELETEs processed first (freed Rs. 45,000 balance)
           - 9 ADDs selected by composite score (top 9 of 15 queued)
           - Total premium: Rs. 1,85,000

           Deferred (3 endorsements):
           - 2 low-priority ADDs (P3, coverage starts in 25+ days)
           - 1 high-premium ADD (Rs. 80,000, exceeds remaining balance)

           Savings vs. FIFO: Rs. 15,000
           Balance utilization: 92% (Rs. 1,85,000 of Rs. 2,00,000 available)
           Next batch in: 15 minutes"
```

**Implementation**: Modify `ConstraintBatchOptimizer.optimizeBatch()` to return a `BatchReport` record alongside the `OptimizedBatchPlan`, containing included/deferred lists with reasons.

---

## 7. Implementation Summary

### All 24 Enhancement Items

| # | Enhancement | Phase | Effort | GenAI Bucket | GenAI +Effort | Impact | Status |
|---|------------|-------|--------|-------------|--------------|--------|--------|
| 1 | Anomaly false positive tracking | Phase 1 | 2d | MARGINAL | +0.5d | Self-calibrating thresholds |
| 2 | Forecast accuracy backtesting | Phase 1 | 2d | MARGINAL | +0.5d | MAPE-driven factor tuning |
| 3 | Error resolution success tracking | Phase 1 | 2d | MARGINAL | +0.5d | Auto-disable bad fixes | **COMPLETED** |
| 4 | STP rate trending with snapshots | Phase 1 | 1d | MARGINAL | +0.5d | Historical trend analysis | **COMPLETED** |
| 5 | Configurable anomaly thresholds | Phase 2 | 1d | NONE | — | No-code tuning |
| 6 | Dormancy break anomaly rule | Phase 2 | 1d | HIGH | +1.5d | New fraud pattern | **COMPLETED** |
| 7 | Cross-insurer duplication rule | Phase 2 | 1d | HIGH | +1.5d | New fraud pattern |
| 8 | Insurer-specific error patterns | Phase 2 | 3d | HIGH | +2d | Higher auto-resolve rate |
| 9 | Multi-signal anomaly correlation | Phase 2 | 1d | HIGH | +1.5d | Fewer false negatives |
| 10 | Distributed scheduler locks | Phase 3 | 2d | NONE | — | Multi-instance safety |
| 11 | Graceful shutdown | Phase 3 | 1d | NONE | — | Zero data loss on restart |
| 12 | Intelligence data retention | Phase 3 | 1d | NONE | — | Controlled table growth |
| 13 | Intelligence health indicators | Phase 3 | 1d | NONE | — | Scheduler staleness alerts |
| 14 | Configurable optimizer weights | Phase 3 | 0.5d | NONE | — | Seasonal tuning |
| 15 | Process variant detection | Phase 3 | 2d | MARGINAL | +1d | Conformance checking |
| 16 | Rich anomaly explanations | Phase 4 | 2d | HIGH | **DEPLOYED** | Faster reviewer decisions |
| 17 | Forecast actionable narratives | Phase 4 | 1d | HIGH | +1.5d | Clear operator instructions |
| 18 | Error resolution explanation chains | Phase 4 | 1d | HIGH | **DEPLOYED** | Full audit trail |
| 19 | Bottleneck recommendations | Phase 4 | 1d | HIGH | +1.5d | Actionable process insights |
| 20 | Batch optimization reports | Phase 4 | 1d | HIGH | +1.5d | Transparency on batch decisions |
| 21 | Shadow mode framework | Phase 4 | 2d | HIGH | +1d | Safe adapter comparison |
| 22 | Per-rule anomaly metrics dashboard | Phase 4 | 1d | MARGINAL | +0.5d | Rule performance visibility |
| 23 | Forecast factor auto-tuning | Phase 4 | 2d | NONE | — | Data-driven seasonality |
| 24 | Error pattern frequency ranking | Phase 4 | 1d | NONE | — | Prioritize pattern development |

> **GenAI column legend**: HIGH = LLM enrichment significantly improves output. MARGINAL = narrative overlay, rules still make decisions. NONE = deterministic/infrastructure — no GenAI value. DEPLOYED = Ollama adapter already operational.
>
> See **[GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md)** for detailed per-enhancement analysis, prompt templates, and architecture.

### Estimated Timeline

```
                                    Rule-Based    + GenAI Augmentation
Phase 1 (Week 1-2):  Feedback       ~7 days        + 2 days (marginal overlays)
Phase 2 (Week 3-4):  Detection      ~7 days        + 6.5 days (high-value enrichment)
Phase 3 (Week 5-6):  Operational    ~8 days        + 1 day (variant narrative)
Phase 4 (Week 7-8):  Narratives     ~12 days       + 6 days (3 new Ollama adapters)

Rule-based total:     ~34 developer-days
GenAI incremental:    ~16-21 developer-days
Combined total:       ~50-55 developer-days (10-11 weeks with testing)
```

> **Note**: Items #16 (rich anomaly explanations) and #18 (error resolution chains) are already deployed as Ollama adapters. Their GenAI effort is 0 — the work is done.

### New Dependencies Required

| Dependency | Purpose | Size |
|-----------|---------|------|
| `shedlock-spring` | Distributed scheduler locking | ~50KB |
| `shedlock-provider-jdbc-template` | JDBC-based lock storage | ~20KB |

No new languages, runtimes, ML frameworks, or external services needed.

### Testing Requirements

Per the project rules, each phase includes:
- Unit tests for all modified/new components
- API tests for new/changed endpoints
- BDD scenarios for new intelligence behaviors
- E2E tests for new frontend Intelligence tab features
- Updated Allure report

---

## 8. Architecture: No Changes Required

All 24 enhancements work within the existing hexagonal architecture:

```
Domain Layer (unchanged):
  - Port interfaces remain the same
  - Domain models gain 2 new entities (StpRateSnapshot, ProcessVariant)
  - No new ports needed

Application Layer (enhanced):
  - Services gain feedback loop methods
  - 1 new scheduler (ForecastBacktestScheduler)
  - Health indicator added

Infrastructure Layer (enhanced):
  - Adapters gain new rules, configurable thresholds, richer explanations
  - ShedLock configuration added
  - Redis used for precision/success rate caching

API Layer (extended):
  - 2 new endpoints (STP rate trend, process variants)
  - Enhanced response payloads (richer narratives)
```

The key principle remains: **domain and application code stay clean**. All intelligence logic improvements happen in infrastructure adapters. The port interfaces remain stable, ensuring that Ollama and future ML adapter swaps remain zero-effort.

**Ollama/GenAI layer** (no architecture changes required):
- 2 Ollama adapters already deployed (`OllamaAugmentedAnomalyDetector`, `OllamaErrorResolver`)
- 3 additional Ollama adapters planned (forecast, process mining, batch optimizer)
- All use the same `@ConditionalOnProperty` toggle pattern
- All compute deterministic results first, then enrich via LLM
- See **[GenAI Augmentation Strategy](../GenAI_Augmentation_Strategy.md)** for full implementation details
