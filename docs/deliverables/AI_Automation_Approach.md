# AI and Automation Approach

**Project:** Plum Endorsement Management System
**Deliverable:** #5 -- How AI/Automation Is Leveraged
**Date:** March 14, 2026 (Updated)
**Version:** 3.0

---

## Table of Contents

- [Document Suite](#document-suite)
- [1. Problem Statement](#1-problem-statement)
- [2. Architecture: Port/Adapter Pattern for Swappable Intelligence](#2-architecture-portadapter-pattern-for-swappable-intelligence)
  - [2.1 The Five Intelligence Ports](#21-the-five-intelligence-ports)
  - [2.2 Why This Matters](#22-why-this-matters)
  - [2.3 Intelligence Feature Matrix — Current Status & Enhancement Path](#23-intelligence-feature-matrix--current-status--enhancement-path)
- [3. Pillar 1: Anomaly Detection](#3-pillar-1-anomaly-detection)
  - [3.1 Detection Rules](#31-detection-rules)
  - [3.2 Anomaly Status Workflow](#32-anomaly-status-workflow)
  - [3.3 Production Upgrade Path](#33-production-upgrade-path)
- [4. Pillar 2: Predictive EA Balance Forecasting](#4-pillar-2-predictive-ea-balance-forecasting)
  - [4.1 Forecasting Algorithm](#41-forecasting-algorithm)
  - [4.2 Seasonality Factors](#42-seasonality-factors)
  - [4.3 Confidence Scoring](#43-confidence-scoring)
  - [4.4 Shortfall Detection and Notification](#44-shortfall-detection-and-notification)
  - [4.5 Production Upgrade Path](#45-production-upgrade-path)
- [5. Pillar 3: Automated Error Resolution](#5-pillar-3-automated-error-resolution)
  - [5.1 Error Pattern Matching](#51-error-pattern-matching)
  - [5.2 Resolution Details](#52-resolution-details)
  - [5.3 Auto-Apply Decision Logic](#53-auto-apply-decision-logic)
  - [5.4 Auto-Apply Threshold Decision Matrix](#54-auto-apply-threshold-decision-matrix)
  - [5.5 Production Upgrade Path](#55-production-upgrade-path)
- [6. Pillar 4: Process Mining](#6-pillar-4-process-mining)
  - [6.1 Event Stream Analysis](#61-event-stream-analysis)
  - [6.2 Example: Transition Analysis Output](#62-example-transition-analysis-output)
  - [6.3 Bottleneck Detection](#63-bottleneck-detection)
  - [6.4 STP Rate Calculation](#64-stp-rate-calculation)
  - [6.5 Production Upgrade Path](#65-production-upgrade-path)
- [7. Pillar 5: Smart Batch Optimization](#7-pillar-5-smart-batch-optimization)
  - [7.1 Composite Scoring Algorithm](#71-composite-scoring-algorithm)
  - [7.2 Batch Assembly Strategy](#72-batch-assembly-strategy)
  - [7.3 Why DELETEs First?](#73-why-deletes-first)
  - [7.4 Integration with BatchAssemblyScheduler](#74-integration-with-batchassemblyscheduler)
  - [7.5 Production Upgrade Path](#75-production-upgrade-path)
- [8. Event-Driven Intelligence Integration](#8-event-driven-intelligence-integration)
  - [8.1 Intelligence Event Types](#81-intelligence-event-types)
  - [8.2 Event Flow](#82-event-flow)
- [9. Intelligence REST API](#9-intelligence-rest-api)
  - [9.1 Anomaly Detection Endpoints](#91-anomaly-detection-endpoints)
  - [9.2 Balance Forecast Endpoints](#92-balance-forecast-endpoints)
  - [9.3 Error Resolution Endpoints](#93-error-resolution-endpoints)
  - [9.4 Process Mining Endpoints](#94-process-mining-endpoints)
- [10. Production Upgrade Path: Rule-Based → Ollama/GenAI → ML/LLM](#10-production-upgrade-path-rule-based--ollamagenai--mlllm)
  - [10.1 The Swap — Now a Three-Stage Evolution](#101-the-swap--now-a-three-stage-evolution)
  - [10.2 Technology Stack — Three Stages](#102-technology-stack--three-stages)
  - [10.3 Rollout Strategy](#103-rollout-strategy)
- [11. Observability](#11-observability)
  - [11.1 Intelligence-Specific Metrics](#111-intelligence-specific-metrics)
  - [11.2 Grafana Dashboard: Intelligence Monitoring](#112-grafana-dashboard-intelligence-monitoring)
- [12. Design Trade-offs](#12-design-trade-offs)
- [13. Summary](#13-summary)
- [14. Four-Tier Evolution Strategy](#14-four-tier-evolution-strategy)
  - [14.1 Current Codebase Statistics](#141-current-codebase-statistics)
  - [14.2 Companion Documents](#142-companion-documents)

---

## Document Suite

This document is the **architectural foundation** for AI/automation in the Plum Endorsement system. Three companion documents provide actionable implementation details:

| Document | Scope | Items | Effort |
|----------|-------|-------|--------|
| **This Document** | Architecture, algorithms, design trade-offs, port/adapter pattern | 5 pillars | Reference |
| **[AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md)** | Enhancements implementable **now** with existing stack (Java 21, Spring Boot, PostgreSQL, Redis) | 24 items across 4 phases | ~34 dev-days |
| **[GenAI Augmentation Strategy](GenAI_Augmentation_Strategy.md)** | Ollama/LLM augmentation of the 24 enhancements — what to augment, what to keep rule-based | 22 items in 3 buckets | ~16-21 incremental days |
| **[AI Automation Vision](vision/AI_Automation_Vision.md)** | Future ML/AI capabilities requiring new infrastructure (Python sidecars, LLM APIs, vector DBs) | 15 capabilities across 5 pillars | 6-12 months |

**Reading order:**
1. **This document** -- understand the architecture and current algorithms
2. **Implementation Approach** -- what to build in the next 6-8 weeks (rule-based)
3. **GenAI Augmentation Strategy** -- which enhancements to augment with Ollama/LLM
4. **Vision** -- the long-term ML/AI roadmap (Python sidecars, full ML models)

---

## 1. Problem Statement

Insurance endorsement processing involves a set of tasks that are fundamentally repetitive, pattern-driven, and high-volume:

- **Anomaly detection**: Identifying fraudulent or erroneous endorsement patterns among millions of daily transactions
- **Balance forecasting**: Predicting when employer escrow accounts will run dry
- **Error correction**: Recognizing and fixing insurer-specific data format errors
- **Process optimization**: Finding bottlenecks across multi-insurer submission workflows
- **Batch sequencing**: Ordering endorsements to maximize throughput under balance constraints

At Plum's scale -- **100,000 employers, 1 million endorsements per day, 3+ insurer integrations** -- manual handling of any of these tasks is not feasible. A human operations team cannot review every endorsement for fraud, predict shortfalls across 100K escrow accounts, or fix formatting errors across three different insurer APIs in real time.

However, jumping straight to ML/LLM models carries its own risks:

```
Problem:  "We need AI" is not a strategy.

Risk 1:   ML models require training data we do not have on day one
Risk 2:   LLM inference latency (100-500ms) may violate SLA requirements
Risk 3:   Non-deterministic outputs are dangerous for financial transactions
Risk 4:   Model drift requires ongoing monitoring and retraining pipelines
Risk 5:   Vendor lock-in to specific ML platforms
```

**Our approach:** Start with deterministic, rule-based implementations that solve the problem _today_, architected behind port interfaces that allow swapping in ML/LLM backends _tomorrow_ -- without touching domain or application logic.

---

## 2. Architecture: Port/Adapter Pattern for Swappable Intelligence

The system uses hexagonal architecture's port/adapter pattern to define intelligence capabilities as **domain ports** (interfaces in the domain layer), with current **rule-based implementations as infrastructure adapters**. This is the same pattern we use for insurer integrations and database access -- intelligence is treated as an external capability.

```
+------------------------------------------------------------------+
|                        DOMAIN LAYER                               |
|                                                                   |
|  +--------------------+  +--------------------+                   |
|  | AnomalyDetection   |  | BalanceForecast    |                   |
|  | Port (interface)    |  | Port (interface)   |                   |
|  +--------+-----------+  +--------+-----------+                   |
|           |                       |                               |
|  +--------+-----------+  +--------+-----------+                   |
|  | ErrorResolution    |  | ProcessMining      |                   |
|  | Port (interface)    |  | Port (interface)   |                   |
|  +--------+-----------+  +--------+-----------+                   |
|           |                       |                               |
|  +--------+-----------+                                           |
|  | BatchOptimizer     |                                           |
|  | Port (interface)    |                                           |
|  +--------------------+                                           |
+------------------------------------------------------------------+
                    |                       |
                    v                       v
+------------------------------------------------------------------+
|                    INFRASTRUCTURE LAYER                            |
|                                                                   |
|  TODAY (Rule-Based)      NOW (Ollama/GenAI)    FUTURE (Full ML)   |
|  +------------------+    +------------------+  +----------------+ |
|  | RuleBasedAnomaly |    | OllamaAugmented  |  | IsolationForest| |
|  | Detector         | -> | AnomalyDetector  |->| Detector       | |
|  +------------------+    +------------------+  +----------------+ |
|  +------------------+    +------------------+  +----------------+ |
|  | StatisticalFore- |    | (planned)        |  | ProphetForecast| |
|  | castEngine       | -> | OllamaAugmented  |->| Engine         | |
|  +------------------+    | ForecastEngine   |  +----------------+ |
|                          +------------------+                     |
|  +------------------+    +------------------+  +----------------+ |
|  | SimulatedError   |    | OllamaError      |  | RAGError       | |
|  | Resolver         | -> | Resolver         |->| Resolver       | |
|  +------------------+    +------------------+  +----------------+ |
|  +------------------+    +------------------+  +----------------+ |
|  | EventStream      |    | (planned)        |  | PM4PyProcess   | |
|  | Analyzer         | -> | OllamaAugmented  |->| Analyzer       | |
|  +------------------+    | ProcessMiner     |  +----------------+ |
|                          +------------------+                     |
|  +------------------+    +------------------+  +----------------+ |
|  | ConstraintBatch  |    | (planned)        |  | ORToolsBatch   | |
|  | Optimizer        | -> | OllamaAugmented  |->| Optimizer      | |
|  +------------------+    | BatchOptimizer   |  +----------------+ |
|                          +------------------+                     |
+------------------------------------------------------------------+
```

### 2.1 The Five Intelligence Ports

Each port defines a single method with domain-level input/output records. Two ports already have **Ollama-augmented adapters** in addition to their rule-based implementations:

| Port Interface | Rule-Based Adapter | Ollama Adapter | Method Signature |
|---|---|---|---|
| `AnomalyDetectionPort` | `RuleBasedAnomalyDetector` | `OllamaAugmentedAnomalyDetector` | `analyzeEndorsement(Endorsement, List<Endorsement>)` --> `AnomalyResult` |
| `BalanceForecastPort` | `StatisticalForecastEngine` | -- | `generateForecast(UUID employerId, UUID insurerId, List<Endorsement>)` --> `ForecastResult` |
| `ErrorResolutionPort` | `SimulatedErrorResolver` | `OllamaErrorResolver` | `analyzeError(Endorsement, String errorMessage, UUID insurerId)` --> `ResolutionSuggestion` |
| `ProcessMiningPort` | `EventStreamAnalyzer` | -- | `analyzeWorkflow(List<EndorsementEvent>, UUID insurerId)` --> `List<ProcessMiningMetric>` |
| `BatchOptimizerPort` | `ConstraintBatchOptimizer` | -- | `optimizeBatch(List<Endorsement>, EAAccount, InsurerCapabilities)` --> `OptimizedBatchPlan` |

Adapter selection is controlled by `endorsement.intelligence.ollama.enabled` via `@ConditionalOnProperty`. When enabled, Ollama adapters take priority; they compute deterministic results first, then enrich via LLM. See [GenAI Augmentation Strategy](GenAI_Augmentation_Strategy.md) for the full adapter-by-adapter analysis.

### 2.2 Why This Matters

```
                    Three-Stage Adapter Evolution
                    =============================

  Application Service       Application Service       Application Service
  (no change)               (no change)               (no change)
       |                         |                         |
       v                         v                         v
  AnomalyDetectionPort     AnomalyDetectionPort     AnomalyDetectionPort
       |                         |                         |
       v                         v                         v
  RuleBasedAnomaly-    ===>  OllamaAugmented-    ===>  IsolationForest-
  Detector                   AnomalyDetector           Detector
  (deterministic)            (rules + LLM enrich)      (ML-backed)

  @ConditionalOnProperty    @ConditionalOnProperty    @Component @Primary
  (ollama.enabled=false,    (ollama.enabled=true)
   matchIfMissing=true)

  Zero changes to: Domain models, Application services, REST controllers,
                   Schedulers, Tests (port-level mocks still work)
```

**This is not theoretical — it is already working.** Two adapters (`OllamaAugmentedAnomalyDetector` and `OllamaErrorResolver`) are deployed and operational. Activating them requires a single environment variable: `SPRING_PROFILES_ACTIVE=ollama`. The `@ConditionalOnProperty` annotation on each adapter handles the swap. Feature flags allow gradual rollout.

### 2.3 Intelligence Feature Matrix — Current Status & Enhancement Path

The table below lists every intelligence feature across all 5 pillars, its current implementation approach, whether it uses Ollama/GenAI today, and what enhancement is available.

| # | Pillar | Feature | Current Approach | Ollama/GenAI Status | Enhancement Available |
|---|--------|---------|-----------------|--------------------|-----------------------|
| | **ANOMALY DETECTION** | | | | |
| 1 | Anomaly | Volume Spike detection | Rule-based (24h count vs 30-day avg × 5) | Rule drives scoring | ML: Isolation Forest for adaptive baselines |
| 2 | Anomaly | Add/Delete Cycling detection | Rule-based (same employee ADD+DELETE in 30 days) | Rule drives scoring | ML: Sequence model for complex cycling patterns |
| 3 | Anomaly | Suspicious Timing detection | Rule-based (coverage starts within 7 days) | Rule drives scoring | ML: Temporal clustering for claim-correlated timing |
| 4 | Anomaly | Unusual Premium detection | Rule-based (> 3 std devs from employer mean) | Rule drives scoring | ML: Per-employer distribution modeling |
| 5 | Anomaly | Dormancy Break detection | Rule-based (no activity for 90+ days) | Rule drives scoring | ML: Activity pattern learning per employee |
| 6 | Anomaly | **Anomaly explanation enrichment** | LLM generates business-context explanation | **DEPLOYED** (`OllamaAugmentedAnomalyDetector`) | Full ML: Auto-generated remediation playbooks |
| | **BALANCE FORECASTING** | | | | |
| 7 | Forecast | 30-day balance projection | Statistical (burn rate × seasonality) | Rule-based only | ML: Facebook Prophet time-series forecasting |
| 8 | Forecast | Day-of-week seasonality | Hardcoded factors (Mon 1.2x → Sun 0.2x) | Rule-based only | ML: Learned per-employer day-of-week patterns |
| 9 | Forecast | Monthly seasonality | Hardcoded Indian business cycle (Apr 1.4x, Oct 1.3x) | Rule-based only | ML: Auto-detected changepoints + holiday effects |
| 10 | Forecast | Shortfall detection & notification | Threshold-based (forecast > balance) | Rule-based only | ML: Probabilistic shortfall with confidence intervals |
| 11 | Forecast | **Actionable forecast narratives** | Template-based text | **PLANNED** (`OllamaAugmentedForecastEngine`) | Full ML: Contextual advice with scenario modeling |
| | **ERROR RESOLUTION** | | | | |
| 12 | Error | Date format correction | Keyword match → regex reformat (conf: 0.98) | Rule handles auto-apply | RAG: Historical resolution retrieval |
| 13 | Error | Member ID format correction | Keyword match → PLM- prefix (conf: 0.96) | Rule handles auto-apply | RAG: Insurer-specific format learning |
| 14 | Error | Missing field defaults | Keyword match → context defaults (conf: 0.90) | Rule suggests | RAG: Employer-specific field completion |
| 15 | Error | Premium mismatch adjustment | Keyword match → 5% adj (conf: 0.85) | Rule suggests | RAG: Insurer rate-table lookup |
| 16 | Error | Unknown error handling | No match → manual review (conf: 0.30) | Rule flags for review | RAG: Similar-error retrieval from vector DB |
| 17 | Error | **LLM-powered error analysis** | LLM classifies error, suggests fix with confidence | **DEPLOYED** (`OllamaErrorResolver`) | Full ML: RAG pipeline with vector store of past fixes |
| | **PROCESS MINING** | | | | |
| 18 | Process | State transition duration analysis | Event-pair timing (avg, P95, P99) | Rule-based only | ML: PM4Py conformance checking |
| 19 | Process | STP rate calculation | Count-based (confirmed / total × 100) | Rule-based only | ML: Predictive STP (will this endorsement go STP?) |
| 20 | Process | STP rate trend tracking | Daily snapshots in `stp_rate_snapshots` table | Rule-based only | ML: Trend forecasting with anomaly alerts |
| 21 | Process | Bottleneck detection | Threshold (P95 > 2× avg OR avg > 4h, min 5 samples) | Rule-based only | ML: Variant analysis + social network mining |
| 22 | Process | **Bottleneck recommendations** | Template-based insight text | **PLANNED** (`OllamaAugmentedProcessMiner`) | Full ML: Root-cause analysis with PM4Py |
| | **BATCH OPTIMIZATION** | | | | |
| 23 | Batch | Composite scoring (urgency + EA impact) | Weighted formula (60% urgency, 40% EA impact) | Rule-based only | ML: Reinforcement learning for dynamic weights |
| 24 | Batch | DELETEs-first strategy | Priority ordering (P0→P3) | Rule-based only | ML: Multi-objective optimization (OR-Tools) |
| 25 | Batch | Balance-constrained packing | Two-pass knapsack (DELETEs, then ADDs) | Rule-based only | ML: Linear programming with multiple constraints |
| 26 | Batch | **Optimization reports** | Basic savings summary | **PLANNED** (`OllamaAugmentedBatchOptimizer`) | Full ML: What-if scenario analysis |

**Summary:**

| Status | Count | Details |
|--------|-------|---------|
| **Ollama DEPLOYED** | 2 features | Anomaly explanation enrichment (#6), LLM-powered error analysis (#17) |
| **Ollama PLANNED** | 3 features | Forecast narratives (#11), Bottleneck recommendations (#22), Optimization reports (#26) |
| **Rule-based (ML-upgradeable)** | 21 features | All scoring, detection, computation, and analysis features — port interfaces ready for ML adapter swap |
| **Total** | 26 features | Across 5 intelligence pillars |

> **Key insight:** Ollama/GenAI augments the **narrative/explanation layer** (features #6, #11, #17, #22, #26). The **decision-making layer** (features #1-5, #7-10, #12-16, #18-21, #23-25) remains deterministic and rule-based — exactly where you want predictability for financial operations. Full ML replaces the decision-making layer itself.

---

## 3. Pillar 1: Anomaly Detection

**Adapter:** `RuleBasedAnomalyDetector` (implements `AnomalyDetectionPort`)
**Schedule:** Every 5 minutes (`0 */5 * * * *`)
**Location:** `infrastructure/intelligence/RuleBasedAnomalyDetector.java`

### 3.1 Detection Rules

Five rule functions run in parallel, and the **highest-scoring** rule wins:

```
+-------------------------------------------------------------------+
|                    ANOMALY DETECTION PIPELINE                      |
|                                                                    |
|  Endorsement + Recent History                                      |
|       |                                                            |
|       +---> checkVolumeSpike()       ---> ScoredAnomaly            |
|       |                                                            |
|       +---> checkAddDeleteCycling()  ---> ScoredAnomaly            |
|       |                                                            |
|       +---> checkSuspiciousTiming()  ---> ScoredAnomaly            |
|       |                                                            |
|       +---> checkUnusualPremium()    ---> ScoredAnomaly            |
|       |                                                            |
|       +---> checkDormancyBreak()     ---> ScoredAnomaly            |
|       |                                                            |
|       +---> MAX(score) across all rules                            |
|       |                                                            |
|       v                                                            |
|  AnomalyResult(type, score, explanation)                           |
|       |                                                            |
|       +---> score >= 0.7 ? FLAGGED : ignored                      |
+-------------------------------------------------------------------+
```

#### Rule 1: VOLUME_SPIKE

Flags when recent endorsement count for an employer exceeds the historical daily average by a significant margin. The rule uses a 24-hour window for recent count and a 30-day window for the baseline average.

```
Trigger conditions:
  - recentCount (24h) >= 10           (minimum count prevents false positives
                                       on low-volume employers)
  - recentCount > dailyAvg * 5       (5x the 30-day daily average)

Score = min(0.95, 0.5 + (recentCount / (dailyAvg * 10)))

Example:
  Employer XYZ: 30-day total = 150 endorsements --> dailyAvg = 5.0
  Today: 35 endorsements in 24h
  35 >= 10? YES.  35 > 5.0 * 5 = 25? YES.
  Score = min(0.95, 0.5 + (35 / 50)) = min(0.95, 1.2) = 0.95
  --> FLAGGED (score 0.95 >= threshold 0.7)
```

#### Rule 2: ADD_DELETE_CYCLING

Detects repeated add-then-delete patterns for the same employee within 30 days. This pattern suggests possible fraud (adding an employee to file a claim, then immediately removing them) or chronic data entry errors.

```
Window:    30 days
Grouping:  By employeeId + employerId
Trigger:   Same employee has BOTH an ADD and a DELETE endorsement in window
Score:     0.85 (fixed -- presence of the pattern is binary)

Example:
  Employee E-123 at Employer X:
    Mar 1:  ADD endorsement (employeeId=E-123)
    Mar 15: DELETE endorsement (employeeId=E-123)
  --> FLAGGED: "Add/delete cycling detected"
```

#### Rule 3: SUSPICIOUS_TIMING

Flags ADD endorsements where coverage starts within 7 days. This "late addition" pattern may indicate a pre-claim addition -- adding an employee just before a known medical event.

```
Applies to:  ADD endorsements only
Trigger:     coverageStartDate is 0-7 days from today
Score:       0.75

Example:
  Today: March 13
  Endorsement: ADD, coverageStartDate = March 15 (2 days away)
  --> FLAGGED: "Suspicious timing: ADD endorsement with coverage
               starting in 2 days (possible pre-claim addition)"
```

#### Rule 4: UNUSUAL_PREMIUM

Flags premiums that deviate more than 3 standard deviations from the employer's historical average. Uses Apache Commons Math `DescriptiveStatistics` for statistical calculation.

```
Minimum sample:  5 historical endorsements with premiums
Trigger:         |premium - mean| > 3 * stdDev
Score:           0.7

Example:
  Employer historical premiums: [5000, 5200, 4800, 5100, 4900]
  Mean = 5000, StdDev = 141.42
  New endorsement premium: 25000
  |25000 - 5000| = 20000 > 3 * 141.42 = 424.26
  --> FLAGGED: "Unusual premium: Rs.25000.00 is 141.4 standard
               deviations from employer average of Rs.5000.00"
```

#### Rule 5: DORMANCY_BREAK

Detects endorsements for employees who have had no endorsement activity for 90+ days. A sudden reappearance after prolonged inactivity may indicate fraud (pre-claim addition using a dormant employee record) or data entry errors.

```
Applies to:  All endorsement types
Trigger:     Employee's most recent prior endorsement was > 90 days ago
Score:       min(0.85, 0.6 + (daysSinceLastActivity / 365.0))

Example:
  Employee E-456 last endorsement: November 15, 2025
  New endorsement: March 14, 2026 (120 days gap)
  Score = min(0.85, 0.6 + (120 / 365)) = min(0.85, 0.929) = 0.85
  --> FLAGGED: "Dormancy break detected: employee E-456 had
               no activity for 120 days"
```

### 3.2 Anomaly Status Workflow

```
  +----------+     review()      +--------------+
  | FLAGGED  | ----------------> | UNDER_REVIEW |
  +----------+                   +--------------+
                                       |
                            +----------+----------+
                            |                     |
                            v                     v
                     +-----------+       +------------------+
                     | DISMISSED |       | CONFIRMED_FRAUD  |
                     +-----------+       +------------------+
```

### 3.3 Production Upgrade Path

```
Current:   RuleBasedAnomalyDetector
           - 5 deterministic rules, threshold-based scoring, zero ML dependencies

Deployed:  OllamaAugmentedAnomalyDetector (activate via SPRING_PROFILES_ACTIVE=ollama)
           - Same 5 rules via RuleBasedAnomalyScorer (deterministic scoring unchanged)
           - When score >= 0.7: enriches explanation via Ollama LLM
           - Fraud analyst persona prompt → 2-3 sentence business-context explanation
           - @CircuitBreaker + @Retry with fallback to rule-based explanation
           - Latency: <10ms (rules) + 1-3s (LLM enrichment, async-safe)

Future:    IsolationForestDetector (via Spring AI + Python sidecar)
           - Unsupervised anomaly detection
           - Learns "normal" endorsement patterns from data
           - Adapts to per-employer baselines automatically
           - Autoencoder neural network as second-stage classifier

Migration: 1. Enable Ollama profile → LLM-enriched explanations (available NOW)
           2. Collect 90 days of labeled anomaly data (FLAGGED -> CONFIRMED_FRAUD)
           3. Train Isolation Forest on historical endorsement features
           4. Deploy as new @Component @Primary adapter
           5. Run shadow mode: both adapters run, compare results
           6. Promote ML adapter when precision/recall meet targets
```

---

## 4. Pillar 2: Predictive EA Balance Forecasting

**Adapter:** `StatisticalForecastEngine` (implements `BalanceForecastPort`)
**Schedule:** Daily at 6:00 AM (`0 0 6 * * *`)
**Location:** `infrastructure/intelligence/StatisticalForecastEngine.java`

### 4.1 Forecasting Algorithm

The engine projects 30 days of EA (Endorsement Account) balance consumption using historical trends, day-of-week seasonality, and monthly seasonality calibrated to Indian business cycles.

```
+-------------------------------------------------------------------+
|                    FORECAST PIPELINE                                |
|                                                                    |
|  Input: 90 days of ADD endorsements for (employerId, insurerId)    |
|                                                                    |
|  Step 1: Calculate average premium per endorsement                 |
|           avgPremium = mean(premiums from ADD endorsements)         |
|                                                                    |
|  Step 2: Calculate daily endorsement rate                          |
|           dailyEndorsements = totalADDs / 90                       |
|                                                                    |
|  Step 3: Base daily burn rate                                      |
|           baseDailyBurnRate = avgPremium * dailyEndorsements        |
|                                                                    |
|  Step 4: For each of next 30 days:                                 |
|           dailyForecast = baseBurnRate                              |
|                         * dayOfWeekFactor(day)                     |
|                         * monthFactor(day)                         |
|                                                                    |
|  Step 5: Sum all 30 daily forecasts --> totalForecastedNeed        |
|                                                                    |
|  Step 6: Confidence = min(95%, 50% + (sampleSize * 0.5%))         |
+-------------------------------------------------------------------+
```

### 4.2 Seasonality Factors

**Day-of-week factors** -- endorsement volume correlates strongly with business days:

```
  Mon   Tue   Wed   Thu   Fri   Sat   Sun
  1.2x  1.15x 1.1x  1.05x 1.0x  0.3x  0.2x
   |     |     |     |     |     |     |
   #     #     #     #     #     .     .
   #     #     #     #     #
   #     #     #     #
   #     #     #
   #     #
```

**Monthly factors** -- calibrated to Indian business cycles:

```
  Jan  Feb  Mar  Apr  May  Jun  Jul  Aug  Sep  Oct  Nov  Dec
  0.9  0.95 1.1  1.4  1.1  1.0  1.0  0.95 1.05 1.3  1.05 0.85
                  |    ###              |              ###
                  |    ###  Hiring      |              ###  Appraisal
            Fiscal|    ###  Wave        |              ###  Cycle
            Year  |                     |
            End   |                September
                  April                starts
                  (new FY)             hiring review
```

### 4.3 Confidence Scoring

Confidence grows with sample size, reflecting the statistical validity of the forecast:

```
  Confidence = min(95%, 50% + (sampleSize * 0.5%))

  Samples:   0   10   20   50   90   100+
  Confidence: 50%  55%  60%  75%  95%  95%
              |    |    |    |    |    |
              Low  ---- Medium ------ High
```

### 4.4 Shortfall Detection and Notification

When the forecasted need exceeds the current EA balance, the system proactively notifies the employer:

```
  Current Balance: Rs.500,000
  Forecasted 30-day Need: Rs.750,000
  Shortfall: Rs.250,000
       |
       v
  BalanceForecastAlert event --> Kafka --> NotificationPort
       |
       v
  Employer receives: "Your EA account for Insurer X is projected
  to run short by Rs.2,50,000 within 30 days. Please top up."
```

### 4.5 Production Upgrade Path

```
Current:   StatisticalForecastEngine
           - Linear burn rate with seasonality multipliers
           - Day-of-week + month factors
           - Simple and interpretable

Planned:   OllamaAugmentedForecastEngine (see GenAI Augmentation Strategy §7.1)
           - Same statistical computation via StatisticalForecastComputer
           - LLM generates actionable narrative: shortfall timeline, top-up advice
           - Seasonal context: "April 1.4x factor due to fiscal year hiring wave"
           - @CircuitBreaker + @Retry with fallback to template-based narrative

Future:    ProphetForecastEngine (via Spring AI + Python sidecar)
           - Facebook Prophet for time-series forecasting
           - Automatic changepoint detection (e.g., employer growth spikes)
           - Holiday effects (Diwali, Holi cause processing pauses)
           - Uncertainty intervals (not just point estimates)
           - Alternative: ARIMA via Python microservice

Migration: 1. Enable Ollama profile → LLM-generated forecast narratives (near-term)
           2. Accumulate 6+ months of daily EA transaction history
           3. Train Prophet model per (employerId, insurerId) pair
           4. Validate against StatisticalForecastEngine predictions
           5. Deploy as @Primary adapter with A/B testing
```

---

## 5. Pillar 3: Automated Error Resolution

**Adapter:** `SimulatedErrorResolver` (implements `ErrorResolutionPort`)
**Trigger:** On endorsement REJECTED status from insurer
**Location:** `infrastructure/intelligence/SimulatedErrorResolver.java`

### 5.1 Error Pattern Matching

The resolver uses priority-ordered keyword matching against error messages from insurers. Each pattern has a fixed confidence score reflecting the reliability of the automated fix.

```
+-------------------------------------------------------------------+
|               ERROR RESOLUTION PIPELINE                            |
|                                                                    |
|  Input: (Endorsement, errorMessage, insurerId)                     |
|       |                                                            |
|       v                                                            |
|  Pattern Match (priority order):                                   |
|                                                                    |
|  1. "member" / "id format" / "invalid id"                          |
|     --> MEMBER_ID_FORMAT_ERROR    confidence: 0.96  AUTO-APPLY     |
|                                                                    |
|  2. "date" / "dob" / "format"                                      |
|     --> DATE_FORMAT_ERROR         confidence: 0.98  AUTO-APPLY     |
|                                                                    |
|  3. "required" / "missing" / "mandatory"                           |
|     --> MISSING_FIELD_ERROR       confidence: 0.90  MANUAL REVIEW  |
|                                                                    |
|  4. "premium" / "mismatch" / "amount"                              |
|     --> PREMIUM_MISMATCH_ERROR    confidence: 0.85  MANUAL REVIEW  |
|                                                                    |
|  5. (no pattern matched)                                           |
|     --> UNKNOWN_ERROR             confidence: 0.30  MANUAL REVIEW  |
+-------------------------------------------------------------------+
```

### 5.2 Resolution Details

**MEMBER_ID_FORMAT_ERROR** (confidence: 0.96)
```
  Problem:   Insurer requires PLM- prefixed, 8-character member IDs
  Fix:       "PLM-" + first 8 chars of employeeId (uppercased)
  Example:   "a1b2c3d4-e5f6..." --> "PLM-A1B2C3D4"
  Action:    Auto-applied (>= 0.95 threshold), endorsement resubmitted
```

**DATE_FORMAT_ERROR** (confidence: 0.98)
```
  Problem:   Insurer expects YYYY-MM-DD, got DD-MM-YYYY or DD/MM/YYYY
  Fix:       Regex extraction + reformat to ISO-8601
  Example:   "07-03-1990" --> "1990-03-07"
  Action:    Auto-applied (>= 0.95 threshold), endorsement resubmitted
```

**MISSING_FIELD_ERROR** (confidence: 0.90)
```
  Problem:   Required field missing from endorsement payload
  Fix:       Context-aware defaults based on field name:
             - email    --> "not-provided@employer.com"
             - phone    --> "0000000000"
             - address  --> "On file with employer"
             - gender   --> "Not Specified"
  Action:    Saved as suggestion, requires manual approval via API
```

**PREMIUM_MISMATCH_ERROR** (confidence: 0.85)
```
  Problem:   Premium does not match insurer's sum-insured table
  Fix:       Apply 5% adjustment multiplier (simulates table lookup)
  Example:   Rs.10,000 --> Rs.10,500
  Action:    Saved as suggestion, requires manual approval via API
```

**UNKNOWN_ERROR** (confidence: 0.30)
```
  Problem:   Error does not match any known pattern
  Fix:       None (manual review recommended)
  Action:    Logged with low confidence, requires human intervention
```

### 5.3 Auto-Apply Decision Logic

```
  Confidence >= 0.95?
       |
       +-- YES --> Auto-apply correction
       |           Resubmit endorsement to insurer
       |           Publish ErrorAutoResolved event
       |           Increment auto_resolved counter
       |           (max 2 auto-retries, configurable)
       |
       +-- NO  --> Save suggestion for manual review
                   Publish ErrorResolutionSuggested event
                   Increment suggested counter
                   Operator approves via PUT /api/v1/intelligence/error-resolutions/{id}/approve
```

### 5.4 Auto-Apply Threshold Decision Matrix

```
  +-------------------+------------+------------+-----------+
  | Error Type        | Confidence | Auto-Apply | Rationale |
  +-------------------+------------+------------+-----------+
  | MEMBER_ID_FORMAT  |    0.96    |    YES     | Format is |
  |                   |            |            | deterministic |
  +-------------------+------------+------------+-----------+
  | DATE_FORMAT       |    0.98    |    YES     | Date      |
  |                   |            |            | parsing is|
  |                   |            |            | exact     |
  +-------------------+------------+------------+-----------+
  | MISSING_FIELD     |    0.90    |    NO      | Default   |
  |                   |            |            | values may|
  |                   |            |            | be wrong  |
  +-------------------+------------+------------+-----------+
  | PREMIUM_MISMATCH  |    0.85    |    NO      | Financial |
  |                   |            |            | impact,   |
  |                   |            |            | needs     |
  |                   |            |            | human     |
  +-------------------+------------+------------+-----------+
  | UNKNOWN           |    0.30    |    NO      | No known  |
  |                   |            |            | fix       |
  +-------------------+------------+------------+-----------+
```

### 5.5 Production Upgrade Path

```
Current:   SimulatedErrorResolver
           - Priority-ordered keyword matching
           - Fixed corrections per error type
           - Deterministic and auditable

Deployed:  OllamaErrorResolver (activate via SPRING_PROFILES_ACTIVE=ollama)
           - Sends full endorsement context + error message to Ollama LLM
           - Expects structured JSON: { errorType, originalValue, correctedValue,
             resolution, confidence }
           - Handles novel/unknown error patterns the keyword matcher misses
           - @CircuitBreaker + @Retry with fallback to SimulatedErrorResolver
           - Falls back on JSON parse failure → rule-based result
           - Latency: 1-3s (acceptable for scheduled error processing)

Future:    RAGErrorResolver (via LangChain4j + vector DB)
           - RAG (Retrieval-Augmented Generation) pipeline:
             1. Embed all historical error resolutions into vector store
             2. On new error: retrieve K most similar past errors + their fixes
             3. Prompt LLM with error context + retrieved examples
             4. LLM generates correction with confidence score
           - Benefits:
             * Handles novel error types without code changes
             * Learns insurer-specific patterns from historical data
             * Confidence calibrated against actual fix success rate

Architecture:
  +----------+     +-----------+     +--------+     +---------+
  | Error    | --> | Vector    | --> | Top-K  | --> | LLM     |
  | Message  |     | Embedding |     | Search |     | (Claude |
  |          |     | (OpenAI)  |     | (Pinecone)   | or GPT) |
  +----------+     +-----------+     +--------+     +---------+
                                                        |
                                                        v
                                              +------------------+
                                              | ResolutionResult |
                                              | (correctedValue, |
                                              |  confidence,     |
                                              |  explanation)    |
                                              +------------------+
```

---

## 6. Pillar 4: Process Mining

**Adapter:** `EventStreamAnalyzer` (implements `ProcessMiningPort`)
**Schedule:** Daily at 3:00 AM (`0 0 3 * * *`)
**Location:** `infrastructure/intelligence/EventStreamAnalyzer.java`

### 6.1 Event Stream Analysis

The analyzer processes endorsement event streams to extract workflow metrics, identify bottlenecks, and calculate straight-through processing (STP) rates.

```
+-------------------------------------------------------------------+
|                    PROCESS MINING PIPELINE                          |
|                                                                    |
|  Input: All EndorsementEvent records for an insurer                |
|                                                                    |
|  Step 1: Group events by endorsementId                             |
|           { endorsement-1: [Created, Validated, Queued, ...],      |
|             endorsement-2: [Created, Rejected, RetryScheduled,...]} |
|                                                                    |
|  Step 2: Sort each group by occurredAt timestamp                   |
|                                                                    |
|  Step 3: Calculate transition durations                            |
|           For each consecutive pair (event[i], event[i+1]):        |
|             transitionKey = eventType[i] + " -> " + eventType[i+1] |
|             durationMs = occurredAt[i+1] - occurredAt[i]           |
|                                                                    |
|  Step 4: Aggregate per transition:                                 |
|           - avgDurationMs (mean)                                   |
|           - p95DurationMs (95th percentile)                        |
|           - p99DurationMs (99th percentile)                        |
|           - sampleCount                                            |
|           (uses Apache Commons Math DescriptiveStatistics)         |
|                                                                    |
|  Step 5: Happy path detection                                     |
|           If endorsement has NO "RETRY" or "REJECTED" events       |
|           --> count as happy path                                  |
|           happyPathPct = happyPathCount / totalEndorsements * 100  |
|                                                                    |
|  Step 6: Output: List<ProcessMiningMetric> per insurer             |
+-------------------------------------------------------------------+
```

### 6.2 Example: Transition Analysis Output

```
  Insurer: ICICI Lombard (real-time API)

  +------------------------------+---------+----------+----------+--------+
  | Transition                   | Avg     | P95      | P99      | Count  |
  +------------------------------+---------+----------+----------+--------+
  | CREATED -> VALIDATED         | 120ms   | 250ms    | 500ms    | 15,230 |
  | VALIDATED -> SUBMITTED_RT    | 45ms    | 100ms    | 200ms    | 14,890 |
  | SUBMITTED_RT -> CONFIRMED    | 180ms   | 350ms    | 800ms    | 14,200 |
  | SUBMITTED_RT -> REJECTED     | 150ms   | 300ms    | 600ms    |    690 |
  | REJECTED -> RETRY_SCHEDULED  | 2,000ms | 5,000ms  | 10,000ms |    690 |
  +------------------------------+---------+----------+----------+--------+

  Happy Path: 93.2% (14,200 / 15,230)
  STP Rate:   93.2%
```

### 6.3 Bottleneck Detection

The insight generation logic flags transitions that exhibit high variance or absolute slowness:

```
  Bottleneck criteria (OR logic):
    1. p95DurationMs > 2 * avgDurationMs  AND  sampleCount >= 5
       (high variance indicates inconsistent performance)

    2. avgDurationMs > 4 hours (14,400,000 ms)
       (absolute threshold for slow transitions)

  Example bottleneck alert:
    "Bottleneck detected: QUEUED_FOR_BATCH -> BATCH_SUBMITTED
     averages 6.2 hours (p95: 14.1 hours). Based on 342 samples."
```

### 6.4 STP Rate Calculation

STP (Straight-Through Processing) rate measures the percentage of endorsements that flow from creation to confirmation without any rejections or retries.

```
  Overall STP Rate:
    confirmed / (confirmed + rejected + failed + failed_permanent) * 100

  Per-Insurer STP Rate:
    Same formula, filtered by insurerId

  Exposed at:
    GET /api/v1/intelligence/process-mining/stp-rate
    GET /api/v1/intelligence/process-mining/stp-rate?insurerId={uuid}

  Response:
    {
      "overallStpRate": 87.5,
      "perInsurer": {
        "icici-uuid":  92.1,
        "bajaj-uuid":  85.3,
        "niva-uuid":   83.8
      },
      "totalEndorsements": 45230,
      "successfulEndorsements": 39576
    }

  STP Rate Trend (NEW — daily historical snapshots):
    GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={uuid}&days=30

  Response:
    {
      "insurerId": "icici-uuid",
      "dataPoints": [
        { "date": "2026-02-12", "stpRate": 91.2, "total": 1502, "stp": 1370 },
        { "date": "2026-02-13", "stpRate": 92.5, "total": 1480, "stp": 1369 },
        ...
      ],
      "currentRate": 92.1,
      "changePercent": 0.9
    }

  Stored in: stp_rate_snapshots table (V19 migration)
  Populated by: ProcessMiningScheduler (daily snapshot capture)
```

### 6.5 Production Upgrade Path

```
Current:   EventStreamAnalyzer
           - Sequential event pair analysis
           - Statistical aggregation (mean, p95, p99)
           - Basic happy path detection

Planned:   OllamaAugmentedProcessMiner (see GenAI Augmentation Strategy §7.2)
           - Same statistical computation via EventStreamComputer
           - LLM generates bottleneck recommendations with root cause analysis
           - Trend comparison: "Worsening +51% vs. prior 7 days"
           - Actionable suggestions: "Increase batch size limit or reduce interval"
           - @CircuitBreaker + @Retry with fallback to template insights

Future:    PM4PyProcessAnalyzer (via Python sidecar + gRPC)
           - Process mining library (PM4Py) integration
           - Conformance checking: compare actual vs expected process model
           - Variant analysis: discover all unique endorsement paths
           - Social network mining: identify handoff patterns between systems
           - Root cause analysis: correlate bottlenecks with attributes
             (employer size, endorsement type, time of day)

Example conformance check output:
  Expected: CREATED -> VALIDATED -> QUEUED -> BATCH_SUBMITTED -> CONFIRMED
  Variant 1 (78%): Matches expected (happy path)
  Variant 2 (12%): CREATED -> VALIDATED -> SUBMITTED_RT -> CONFIRMED
  Variant 3 (7%):  CREATED -> VALIDATED -> QUEUED -> REJECTED -> RETRY -> CONFIRMED
  Variant 4 (3%):  CREATED -> VALIDATED -> QUEUED -> REJECTED -> FAILED_PERMANENT
```

---

## 7. Pillar 5: Smart Batch Optimization

**Adapter:** `ConstraintBatchOptimizer` (implements `BatchOptimizerPort`)
**Trigger:** Integrated into `BatchAssemblyScheduler` (every 15 minutes)
**Location:** `infrastructure/intelligence/ConstraintBatchOptimizer.java`

### 7.1 Composite Scoring Algorithm

Each endorsement in the queue receives a composite score based on two weighted factors:

```
  compositeScore = urgencyScore * 0.6 + eaImpactScore * 0.4
                   |                     |
                   60% weight            40% weight
```

**Urgency Score** (60% weight):

```
  Two sub-components, averaged:

  1. Priority rank score:
     P0 (immediate) = 1.0     (4 - 0) / 4
     P1 (high)      = 0.75    (4 - 1) / 4
     P2 (normal)    = 0.5     (4 - 2) / 4
     P3 (low)       = 0.25    (4 - 3) / 4

  2. Time pressure (days until coverage start):
     timePressure = max(0, 1.0 - (daysUntilCoverage / 30))

     0 days  --> 1.0  (maximum urgency)
     7 days  --> 0.77
     15 days --> 0.5
     30 days --> 0.0  (no time pressure)
     >30 days -> 0.0

  urgencyScore = (priorityRankScore + timePressure) / 2
```

**EA Impact Score** (40% weight):

```
  DELETE endorsements:  1.0 (always highest -- they free balance)
  ADD/UPDATE:           max(0, 1.0 - (premiumAmount / availableBalance))

  Example:
    Available balance: Rs.100,000
    ADD premium Rs.5,000  --> impactScore = 1.0 - (5000/100000) = 0.95
    ADD premium Rs.80,000 --> impactScore = 1.0 - (80000/100000) = 0.20
    DELETE any amount     --> impactScore = 1.0
```

### 7.2 Batch Assembly Strategy

The optimizer uses a two-pass, balance-constrained knapsack approach:

```
+-------------------------------------------------------------------+
|                    BATCH OPTIMIZATION                               |
|                                                                    |
|  Phase 1: Score and sort all endorsements by composite score       |
|           (descending -- highest priority first)                   |
|                                                                    |
|  Phase 2: Pack DELETEs first                                       |
|           For each DELETE in scored order:                          |
|             if batch not full:                                     |
|               add to batch                                         |
|               runningBalance += premium (credit)                   |
|                                                                    |
|  Phase 3: Pack ADDs/UPDATEs constrained by balance                 |
|           For each ADD/UPDATE in scored order:                     |
|             if batch not full AND runningBalance >= premium:       |
|               add to batch                                         |
|               runningBalance -= premium (debit)                    |
|                                                                    |
|  Result: Optimized batch that:                                     |
|    - Processes most urgent endorsements first                      |
|    - Maximizes throughput within balance constraints                |
|    - Frees balance (via DELETEs) before consuming it (via ADDs)   |
+-------------------------------------------------------------------+
```

### 7.3 Why DELETEs First?

```
  Without optimization (naive FIFO):
    Balance: Rs.50,000
    Queue: [ADD Rs.40K, ADD Rs.30K, DELETE Rs.20K, ADD Rs.15K]
    Batch: [ADD Rs.40K]  --> balance Rs.10K, cannot fit ADD Rs.30K
    Result: 1 endorsement processed

  With optimization (DELETEs first):
    Balance: Rs.50,000
    Reordered: [DELETE Rs.20K, ADD Rs.40K, ADD Rs.30K, ADD Rs.15K]
    DELETE Rs.20K  --> balance Rs.70,000
    ADD Rs.40K     --> balance Rs.30,000
    ADD Rs.30K     --> balance Rs.0
    Result: 3 endorsements processed

  Savings: Rs.50,000 of additional endorsements processed per batch
```

### 7.4 Integration with BatchAssemblyScheduler

```
  BatchAssemblyScheduler (every 15 min)
       |
       v
  For each (employerId, insurerId) pair with queued endorsements:
       |
       +---> Fetch EAAccount balance
       |
       +---> Fetch InsurerCapabilities (maxBatchSize, etc.)
       |
       +---> BatchOptimizerPort.optimizeBatch(queue, account, capabilities)
       |          |
       |          +---> Returns OptimizedBatchPlan(endorsements, strategy,
       |                                           estimatedSavings, processingTimeMs)
       |
       +---> Submit optimized batch to insurer
       |
       +---> Publish BatchOptimized event with savings amount
       |
       +---> Record metrics: savings summary, optimization duration timer
```

The optimizer is integrated with a **graceful fallback**: if the optimization fails for any reason, the scheduler falls back to FIFO ordering rather than failing the entire batch.

### 7.5 Production Upgrade Path

```
Current:   ConstraintBatchOptimizer
           - Composite scoring with weighted factors
           - Two-pass balance-constrained packing
           - O(n log n) sorting + O(n) packing

Planned:   OllamaAugmentedBatchOptimizer (see GenAI Augmentation Strategy §7.3)
           - Same DP knapsack via ConstraintKnapsackSolver (deterministic, fast)
           - LLM generates rich optimization reports:
             * Why certain endorsements were deferred (business-language)
             * Actionable recommendations for deferred items
             * Balance utilization analysis with next-batch preview
           - @CircuitBreaker + @Retry with fallback to template report

Future:    ORToolsBatchOptimizer (via Google OR-Tools)
           - Linear programming for multi-constraint optimization:
             * Balance constraint (hard)
             * Batch size constraint (hard)
             * Insurer processing window constraint (hard)
             * Urgency objective (maximize)
             * EA balance utilization objective (maximize)
           - Or genetic algorithm for non-linear constraints
           - Handles constraints the heuristic approach cannot:
             * "Process employer A before employer B" dependencies
             * "Maximum premium per batch" insurer limits
             * "Even distribution across time windows" fairness
```

---

## 8. Event-Driven Intelligence Integration

All five intelligence pillars publish events to Kafka, enabling downstream consumers (dashboards, alerting systems, audit logs) to react in real time.

### 8.1 Intelligence Event Types

Six event types are defined as records in the `EndorsementEvent` sealed interface:

```
+------------------------------------------------------------------+
|  INTELLIGENCE EVENTS (published to Kafka topic: endorsement-events)|
+------------------------------------------------------------------+
|                                                                    |
|  1. AnomalyDetected                                               |
|     Fields: endorsementId, employerId, anomalyType, anomalyScore, |
|             explanation                                            |
|     Published by: AnomalyDetectionService                         |
|     Trigger: score >= 0.7 threshold                               |
|                                                                    |
|  2. ForecastGenerated                                              |
|     Fields: endorsementId, employerId, forecastedNeed, daysAhead,  |
|             narrative                                              |
|     Published by: BalanceForecastService                           |
|     Trigger: daily forecast generation                             |
|                                                                    |
|  3. BatchOptimized                                                 |
|     Fields: endorsementId, employerId, batchId,                    |
|             optimizationStrategy, savedAmount                      |
|     Published by: BatchAssemblyScheduler                           |
|     Trigger: batch optimization completes                          |
|                                                                    |
|  4. ErrorAutoResolved                                              |
|     Fields: endorsementId, employerId, errorType, resolution,      |
|             autoApplied (boolean)                                   |
|     Published by: ErrorResolutionService                           |
|     Trigger: confidence >= 0.95 (auto-applied)                     |
|                                                                    |
|  5. ErrorResolutionSuggested                                       |
|     Fields: endorsementId, employerId, errorType, suggestedFix,    |
|             confidence                                             |
|     Published by: ErrorResolutionService                           |
|     Trigger: confidence < 0.95 (needs manual review)               |
|                                                                    |
|  6. ProcessMiningInsight                                           |
|     Fields: endorsementId, employerId, insightType, insight        |
|     Published by: ProcessMiningService                             |
|     Trigger: daily analysis detects bottleneck or STP anomaly      |
+------------------------------------------------------------------+
```

### 8.2 Event Flow

```
  Intelligence Adapter
       |
       v
  Application Service (AnomalyDetectionService, etc.)
       |
       +---> Persist result to PostgreSQL
       |
       +---> EventPublisher.publish(event)
                  |
                  v
            KafkaEventPublisher
                  |
                  +---> Serialize to JSON
                  |
                  +---> Send to "endorsement-events" topic
                  |     Key: employerId (for partition affinity)
                  |
                  +---> Increment endorsement.kafka.publish counter
                        (tagged: result=success/failure, eventType=...)
```

---

## 9. Intelligence REST API

Fourteen endpoints are exposed under `/api/v1/intelligence/`, served by the `IntelligenceController`.

### 9.1 Anomaly Detection Endpoints

```
  GET  /api/v1/intelligence/anomalies
       Query params: ?employerId={uuid}  OR  ?status={FLAGGED|UNDER_REVIEW|...}
       Default: returns FLAGGED anomalies
       Response: List<AnomalyDetectionResponse>

  GET  /api/v1/intelligence/anomalies/{id}
       Response: AnomalyDetectionResponse

  PUT  /api/v1/intelligence/anomalies/{id}/review
       Body: { "status": "DISMISSED" | "CONFIRMED_FRAUD", "notes": "..." }
       Response: AnomalyDetectionResponse
```

### 9.2 Balance Forecast Endpoints

```
  GET  /api/v1/intelligence/forecasts
       Query params: ?employerId={uuid}&insurerId={uuid}  (both required)
       Response: BalanceForecastResponse (latest forecast)

  GET  /api/v1/intelligence/forecasts/history
       Query params: ?employerId={uuid}
       Response: List<ForecastHistoryResponse>

  POST /api/v1/intelligence/forecasts/generate
       Query params: ?employerId={uuid}&insurerId={uuid}
       Response: BalanceForecastResponse (newly generated)
```

### 9.3 Error Resolution Endpoints

```
  GET  /api/v1/intelligence/error-resolutions
       Query params: ?endorsementId={uuid}
       Response: List<ErrorResolutionResponse>

  GET  /api/v1/intelligence/error-resolutions/stats
       Response: { total, autoApplied, suggested, autoApplyRate,
                   successCount, failureCount, successRate }

  POST /api/v1/intelligence/error-resolutions/resolve
       Query params: ?endorsementId={uuid}&errorMessage={string}
       Response: ErrorResolutionResponse

  POST /api/v1/intelligence/error-resolutions/{id}/approve
       Response: 200 OK
```

### 9.4 Process Mining Endpoints

```
  GET  /api/v1/intelligence/process-mining/metrics
       Query params: ?insurerId={uuid}  (optional, returns all if omitted)
       Response: List<ProcessMiningMetricResponse>

  GET  /api/v1/intelligence/process-mining/insights
       Response: List<ProcessMiningInsightResponse>
       (bottleneck alerts with insurer name, description, timestamp)

  GET  /api/v1/intelligence/process-mining/stp-rate
       Query params: ?insurerId={uuid}  (optional)
       Response: StpRateResponse { overallStpRate, perInsurer, total, successful }

  GET  /api/v1/intelligence/process-mining/stp-rate/trend
       Query params: ?insurerId={uuid}&days={30}  (both optional, defaults: all insurers, 30 days)
       Response: StpRateTrendResponse { insurerId, dataPoints[], currentRate, changePercent }

  POST /api/v1/intelligence/process-mining/analyze
       Triggers on-demand analysis across all insurers
       Response: 202 Accepted
```

---

## 10. Production Upgrade Path: Rule-Based → Ollama/GenAI → ML/LLM

> **Detailed implementation guidance:**
> - For enhancements within the current rule-based system, see **[AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md)** (24 items, Java-only)
> - For Ollama/GenAI augmentation of those enhancements, see **[GenAI Augmentation Strategy](GenAI_Augmentation_Strategy.md)** (22 items, 3 buckets)
> - For the full ML/LLM adapter swap path, see **[AI Automation Vision](vision/AI_Automation_Vision.md)** (15 capabilities, new infrastructure)

### 10.1 The Swap — Now a Three-Stage Evolution

The key architectural insight is that **upgrading intelligence requires zero changes to domain or application code**. Only new adapter implementations in the infrastructure layer. With Ollama integration, we now have a concrete **intermediate stage** between rule-based and full ML:

```
  STAGE 1: RULE-BASED            STAGE 2: OLLAMA/GenAI           STAGE 3: FULL ML
  (Current Default)              (Deployed, opt-in)              (Future)
  =====================          =====================          =====================

  Domain Layer                   Domain Layer                   Domain Layer
  (unchanged)                    (unchanged)                    (unchanged)
       |                              |                              |
       v                              v                              v
  AnomalyDetectionPort          AnomalyDetectionPort          AnomalyDetectionPort
       |                              |                              |
       v                              v                              v
  @ConditionalOnProperty         @ConditionalOnProperty         @Component @Primary
  (ollama.enabled=false,         (ollama.enabled=true)          IsolationForestDetector
   matchIfMissing=true)               |                         (ML-backed)
  RuleBasedAnomalyDetector       OllamaAugmentedAnomaly              |
  (deterministic, <10ms)         Detector                      Fallback:
                                 (rules + LLM enrich)          OllamaAugmented or
                                      |                        RuleBasedDetector
                                 Fallback:                     (circuit breaker)
                                 rule-based explanation
```

**Stage 2 is already operational** for anomaly detection and error resolution. Activate with `SPRING_PROFILES_ACTIVE=ollama`.

### 10.2 Technology Stack — Three Stages

**Stage 2: Ollama/GenAI (Available Now)**

```
  +---------------------+-------------------------------+----------------------------+
  | Pillar              | Technology                    | Purpose                    |
  +---------------------+-------------------------------+----------------------------+
  | All 5 Pillars       | Spring AI + Ollama            | Local LLM (llama3.2)       |
  |                     | (spring-ai-ollama-starter)    | No API keys, no cost       |
  |                     |                               | Temp 0.3, 512 tokens       |
  +---------------------+-------------------------------+----------------------------+
  | Anomaly Detection   | OllamaAugmentedAnomaly-       | Enriched explanations      |
  |                     | Detector (DEPLOYED)           | with business context      |
  +---------------------+-------------------------------+----------------------------+
  | Error Resolution    | OllamaErrorResolver           | Structured JSON resolution |
  |                     | (DEPLOYED)                    | for novel error patterns   |
  +---------------------+-------------------------------+----------------------------+
  | Balance Forecasting | OllamaAugmentedForecast-      | Actionable narratives      |
  |                     | Engine (PLANNED)              | with shortfall timelines   |
  +---------------------+-------------------------------+----------------------------+
  | Process Mining      | OllamaAugmentedProcess-       | Bottleneck recommendations |
  |                     | Miner (PLANNED)               | with trend analysis        |
  +---------------------+-------------------------------+----------------------------+
  | Batch Optimization  | OllamaAugmentedBatch-         | Rich optimization reports  |
  |                     | Optimizer (PLANNED)           | with deferral explanations |
  +---------------------+-------------------------------+----------------------------+
```

**Stage 3: Full ML/LLM (Future)**

```
  +---------------------+-------------------------------+----------------------------+
  | Pillar              | Technology                    | Purpose                    |
  +---------------------+-------------------------------+----------------------------+
  | Anomaly Detection   | Spring AI + scikit-learn      | Isolation Forest model     |
  |                     | (served via Python sidecar)   | for unsupervised anomaly   |
  |                     |                               | detection                  |
  +---------------------+-------------------------------+----------------------------+
  | Balance Forecasting | Spring AI + Facebook Prophet  | Time-series forecasting    |
  |                     | (or ARIMA via statsmodels)    | with changepoint detection |
  +---------------------+-------------------------------+----------------------------+
  | Error Resolution    | LangChain4j + Pinecone        | RAG pipeline: embed past   |
  |                     | + Claude/GPT-4                | errors, retrieve similar,  |
  |                     |                               | LLM generates fix          |
  +---------------------+-------------------------------+----------------------------+
  | Process Mining      | PM4Py (Python library)        | Conformance checking,      |
  |                     | via gRPC sidecar              | variant analysis, social   |
  |                     |                               | network mining             |
  +---------------------+-------------------------------+----------------------------+
  | Batch Optimization  | Google OR-Tools (Java API)    | Linear programming for     |
  |                     | or jMetal (genetic algorithms) | multi-constraint           |
  |                     |                               | optimization               |
  +---------------------+-------------------------------+----------------------------+
```

### 10.3 Rollout Strategy

```
  Phase 0: Ollama Augmentation (available NOW)
  +-------------------------------------------+
  | Enable SPRING_PROFILES_ACTIVE=ollama      |
  | Rule-based scoring unchanged              |
  | LLM enriches explanations/narratives      |
  | Circuit breaker → fallback to rules       |
  | Zero risk: rules still make all decisions |
  +-------------------------------------------+
           |
           v
  Phase 1: Shadow Mode (weeks 1-4)
  +-------------------------------------------+
  | Both ML + rule-based adapters run          |
  | Rule-based: serves traffic                |
  | ML-based: runs in background              |
  | Compare results, log differences          |
  +-------------------------------------------+
           |
           v
  Phase 2: Canary (weeks 5-8)
  +-------------------------------------------+
  | ML adapter serves 10% of traffic          |
  | Monitor precision/recall/latency          |
  | Feature flag per employer                 |
  +-------------------------------------------+
           |
           v
  Phase 3: Graduated Rollout (weeks 9-12)
  +-------------------------------------------+
  | ML adapter serves 50% -> 90%              |
  | Ollama adapter as first fallback          |
  | Rule-based as second fallback             |
  | Circuit breaker chain: ML → Ollama → Rules|
  +-------------------------------------------+
           |
           v
  Phase 4: Full Migration
  +-------------------------------------------+
  | ML adapter is @Primary (100% traffic)     |
  | Ollama + rule-based retained as fallbacks |
  | Continuous monitoring: daily accuracy     |
  +-------------------------------------------+
```

---

## 11. Observability

### 11.1 Intelligence-Specific Metrics

All intelligence adapters and services emit Micrometer metrics, scraped by Prometheus and visualized in Grafana.

```
  +-------------------------------+----------+-----------------------------------+
  | Metric                        | Type     | Tags                              |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.anomaly.detected  | Counter  | anomalyType, employerId           |
  | endorsement.anomaly.score     | Summary  | anomalyType                       |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.forecast.generated| Counter  | employerId                        |
  | endorsement.forecast.shortfall| Counter  | employerId                        |
  |   .detected                   |          |                                   |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.error.auto_resolved| Counter | errorType, insurerId              |
  | endorsement.error.suggested   | Counter  | errorType                         |
  | endorsement.error.resolution  | Summary  | errorType                         |
  |   .confidence                 |          |                                   |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.batch.optimization| Summary  | strategy                          |
  |   .savings                    |          |                                   |
  | endorsement.batch.optimization| Timer    | (none)                            |
  |   .duration                   |          |                                   |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.process.stp_rate  | Gauge    | insurerId                         |
  | endorsement.process           | Gauge    | insurerId                         |
  |   .avg_lifecycle_hours        |          |                                   |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.scheduler.duration| Timer    | scheduler={anomaly_detection,     |
  |                               |          |   balance_forecast, process_mining}|
  |                               |          | result={success, failure}         |
  +-------------------------------+----------+-----------------------------------+
  | endorsement.scheduler         | Counter  | scheduler, result                 |
  |   .execution                  |          |                                   |
  +-------------------------------+----------+-----------------------------------+
```

### 11.2 Grafana Dashboard: Intelligence Monitoring

A dedicated Grafana dashboard (`intelligence-monitoring.json`) provides real-time visibility:

```
  +-------------------------------------------------------------------+
  |                 INTELLIGENCE MONITORING DASHBOARD                   |
  +-------------------------------------------------------------------+
  |                                                                    |
  |  Row 1: Anomaly Detection                                         |
  |  +------------------+  +-------------------+  +-----------------+ |
  |  | Anomalies/hour   |  | Score Distribution|  | By Type         | |
  |  | (rate graph)     |  | (histogram)       |  | (pie chart)     | |
  |  +------------------+  +-------------------+  +-----------------+ |
  |                                                                    |
  |  Row 2: Error Resolution                                          |
  |  +------------------+  +-------------------+  +-----------------+ |
  |  | Auto-Resolve Rate|  | Confidence Dist.  |  | By Error Type   | |
  |  | (gauge)          |  | (histogram)       |  | (stacked bar)   | |
  |  +------------------+  +-------------------+  +-----------------+ |
  |                                                                    |
  |  Row 3: Process Mining                                            |
  |  +------------------+  +-------------------+  +-----------------+ |
  |  | STP Rate         |  | Avg Lifecycle     |  | Bottleneck      | |
  |  | per Insurer      |  | Duration (hours)  |  | Alerts          | |
  |  | (gauge panel)    |  | (time series)     |  | (alert list)    | |
  |  +------------------+  +-------------------+  +-----------------+ |
  |                                                                    |
  |  Row 4: Batch Optimization                                        |
  |  +------------------+  +-------------------+  +-----------------+ |
  |  | Savings per Batch|  | Optimization      |  | Throughput      | |
  |  | (bar chart)      |  | Duration (ms)     |  | Improvement (%) | |
  |  +------------------+  +-------------------+  +-----------------+ |
  |                                                                    |
  |  Row 5: Scheduler Health                                          |
  |  +------------------+  +-------------------+  +-----------------+ |
  |  | Execution Count  |  | Duration per Run  |  | Failure Rate    | |
  |  | (counter)        |  | (timer histogram) |  | (percentage)    | |
  |  +------------------+  +-------------------+  +-----------------+ |
  +-------------------------------------------------------------------+
```

---

## 12. Design Trade-offs

| Decision | Trade-off | Rationale |
|---|---|---|
| **Rule-based first, ML later** | Lower accuracy initially vs. faster time-to-market | We need intelligence on day one. ML requires training data that does not exist yet. Rule-based systems are auditable and predictable -- critical for financial transactions. |
| **Port/adapter for intelligence** | Extra abstraction layer vs. swappability | The interface overhead is minimal (one interface + one record per pillar). The payoff is enormous: swap ML backends without touching domain logic, run shadow mode comparisons, circuit-break to fallback. |
| **0.95 auto-apply threshold** | Conservative (only DATE_FORMAT and MEMBER_ID auto-apply) vs. higher automation rate | Financial impact of incorrect auto-corrections is severe. Better to require manual review for lower-confidence fixes and gradually lower the threshold as trust builds. |
| **Scheduled batch analysis (not real-time)** | 5-minute detection latency for anomalies vs. lower resource consumption | Real-time per-endorsement anomaly detection at 1M/day = 11.6 calls/second. Batch analysis every 5 minutes amortizes cost and allows aggregate pattern detection. |
| **Apache Commons Math over ML libraries** | Limited statistical capabilities vs. zero ML infrastructure dependency | DescriptiveStatistics, standard deviation, percentile calculations cover 90% of our needs. No Python sidecar, no model serving, no GPU requirements. |
| **Day-of-week + month seasonality (hardcoded)** | Does not adapt to per-employer patterns vs. simplicity | Good enough for MVP. Per-employer seasonality requires 6+ months of data per employer. The hardcoded India-market factors (April hiring wave, October appraisals) capture the dominant patterns. |
| **Two-pass batch optimization (DELETEs first)** | Not globally optimal vs. O(n log n) performance | True optimal knapsack is NP-hard. Our heuristic (DELETEs first to free balance, then score-sorted ADDs) achieves near-optimal results in polynomial time. |
| **Kafka for intelligence events** | Additional infrastructure vs. decoupled observability | Intelligence events must not block endorsement processing. Async Kafka publication ensures zero-latency impact on the critical path. Downstream consumers (dashboards, alerts) process independently. |
| **Fixed confidence scores per error type** | Not adaptive vs. deterministic behavior | In the rule-based phase, confidence is a function of the fix type, not the specific error instance. This is intentional: DATE_FORMAT fixes are always 98% reliable, regardless of the specific date. ML phase will produce per-instance confidence. |
| **Simulated processing latency (Thread.sleep)** | Unrealistic vs. realistic integration testing | Each adapter includes simulated latency (50-150ms) to model real-world ML inference times. This ensures our timeout handling, circuit breakers, and scheduler budgets work correctly before ML backends arrive. |

---

## 13. Summary

```
+-------------------------------------------------------------------+
|              AI/AUTOMATION STRATEGY SUMMARY                        |
+-------------------------------------------------------------------+
|                                                                    |
|  Architecture:  Port/Adapter pattern for swappable intelligence    |
|  Stage 1:       5 rule-based adapters (deterministic, auditable)   |
|  Stage 2:       2 Ollama adapters deployed, 3 planned             |
|  Stage 3:       ML/LLM adapters via Python sidecars + LangChain4j |
|  Migration:     Ollama -> shadow -> canary -> graduated -> full    |
|                                                                    |
|  +-------------------+-------------------+-----------------------+ |
|  | Pillar            | Schedule          | Ollama Adapter        | |
|  +-------------------+-------------------+-----------------------+ |
|  | Anomaly Detection | Every 5 min       | DEPLOYED              | |
|  | Balance Forecast  | Daily 6 AM        | Planned               | |
|  | Error Resolution  | On rejection      | DEPLOYED              | |
|  | Process Mining    | Daily 3 AM        | Planned               | |
|  | Batch Optimizer   | Every 15 min      | Planned               | |
|  +-------------------+-------------------+-----------------------+ |
|                                                                    |
|  Events:  6 intelligence event types --> Kafka                     |
|  API:     14 REST endpoints under /api/v1/intelligence/            |
|  Metrics: 12+ Micrometer metrics --> Prometheus --> Grafana         |
|                                                                    |
|  Key insight: We do not need ML to deliver intelligence.           |
|  Ollama/GenAI augmentation is ALREADY deployed for 2 pillars,     |
|  proving the adapter swap works. The architecture ensures that     |
|  upgrading to full ML is a deployment-time adapter swap, not       |
|  an application rewrite.                                           |
+-------------------------------------------------------------------+
```

---

## 14. Four-Tier Evolution Strategy

The AI/automation strategy is organized into four tiers, each documented separately:

```
+-------------------------------------------------------------------+
|                    FOUR-TIER INTELLIGENCE EVOLUTION                 |
+-------------------------------------------------------------------+
|                                                                    |
|  TIER 1: CURRENT STATE (This Document)                            |
|  ======================================                            |
|  5 rule-based adapters, fully operational                          |
|  5 anomaly rules, linear forecasting, keyword error matching       |
|  Event pair process mining, 0-1 knapsack batch optimization        |
|  14 REST endpoints, 6 Kafka event types, 12+ Prometheus metrics    |
|  Status: DEPLOYED AND TESTED (800+ tests, all passing)             |
|                                                                    |
|  TIER 1.5: OLLAMA/GenAI AUGMENTATION (GenAI Augmentation Strategy)|
|  ================================================================= |
|  2 Ollama adapters DEPLOYED (anomaly enrichment, error resolution) |
|  3 Ollama adapters PLANNED (forecast, process mining, batch)       |
|  Rules make decisions; LLM enriches explanations/narratives        |
|  Spring AI + local Ollama (llama3.2) — no API keys, no cost       |
|  @CircuitBreaker + @Retry → fallback to rule-based on LLM failure |
|  22 enhancements assessed: 8 high-value, 6 marginal, 8 keep rules |
|  Effort: ~16-21 incremental days on top of Tier 2                 |
|  Status: PARTIALLY DEPLOYED, STRATEGY DOCUMENTED                   |
|                                                                    |
|  TIER 2: NEAR-TERM ENHANCEMENTS (Implementation Approach)         |
|  =========================================================         |
|  24 enhancements using existing Java/Spring Boot stack              |
|  Phase 1: Feedback loops (self-calibrating thresholds, MAPE)       |
|  Phase 2: Enhanced rules (dormancy, cross-insurer, correlation)    |
|  Phase 3: Operational (ShedLock, graceful shutdown, retention)      |
|  Phase 4: Rich narratives and explanations                         |
|  Effort: ~34 developer-days | Dependencies: shedlock only          |
|  Status: READY TO IMPLEMENT                                        |
|                                                                    |
|  TIER 3: ML/AI VISION (Vision Document)                           |
|  =========================================                         |
|  15 capabilities requiring new infrastructure                      |
|  Q1: RAG error resolver + A/B testing framework                    |
|  Q2: Prophet forecasting + Isolation Forest anomaly detection      |
|  Q3: PM4Py process mining + OR-Tools optimizer + MLOps             |
|  Q4+: LSTM, Ensemble, Graph fraud, RL optimizer                   |
|  Effort: 6-12 months | Dependencies: Python, LLM APIs, vector DB  |
|  Status: ARCHITECTURALLY READY (port interfaces are stable)        |
|                                                                    |
+-------------------------------------------------------------------+
|                                                                    |
|  The four tiers are ADDITIVE, not SEQUENTIAL.                     |
|  Tier 1.5 Ollama augments the rule-based system with LLM          |
|           narratives. Rules still make all decisions.              |
|  Tier 2 enhancements improve the rule-based algorithms.           |
|  Tier 3 ML adapters replace the rule-based scoring/analysis.      |
|  All paths are enabled by the port/adapter architecture.          |
|                                                                    |
+-------------------------------------------------------------------+
```

### 14.1 Current Codebase Statistics

```
Intelligence Source Code:
  5 domain ports (interfaces)
  5 infrastructure adapters (rule-based implementations)
  2 infrastructure adapters (Ollama/GenAI — anomaly + error resolution)
  5 application services (orchestration)
  8 schedulers (periodic analysis + cleanup)
  1 REST controller (14 endpoints)
  16 DTOs (request/response records)
  8 domain models (entities + enums, including StpRateSnapshot)
  6 Flyway migrations (anomaly, forecasts, errors, process mining, stp_rate_snapshots, error_resolution_outcome)

Ollama/GenAI Integration:
  Spring AI dependency (spring-ai-ollama-spring-boot-starter)
  application-ollama.yml (llama3.2, temperature 0.3, 512 tokens)
  Ollama service in docker-compose.yml
  @ConditionalOnProperty toggle on all 7 adapters
  Resilience4j circuit breaker + retry instances for Ollama
  RuleBasedAnomalyScorer extracted as shared component

Intelligence Test Coverage:
  Unit tests:        Testing all 7 adapters, 5 services, 8 schedulers, 1 controller
  API tests:         Testing all 13 intelligence REST endpoints
  BDD tests:         Feature files for all 5 intelligence pillars
  E2E tests:         Playwright tests for Intelligence dashboard tabs
  Performance tests: Gatling simulations for intelligence endpoints

Observability:
  12+ Micrometer metrics (counters, timers, gauges, summaries)
  5 Grafana dashboards (intelligence + application + infra + business + scheduler)
  Structured JSON logging with traceId/spanId/endorsementId correlation
  OpenTelemetry distributed tracing (100% sampling)
```

### 14.2 Companion Documents

| Document | Purpose | Key Sections |
|----------|---------|-------------|
| [AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md) | 24 near-term enhancements | Feedback loops, enhanced rules, operational improvements, rich narratives |
| [GenAI Augmentation Strategy](GenAI_Augmentation_Strategy.md) | Ollama/LLM augmentation assessment | 3-bucket classification, new adapter designs, prompt engineering, effort analysis |
| [AI Automation Vision](vision/AI_Automation_Vision.md) | 15 future ML/AI capabilities | Isolation Forest, Prophet, RAG+LLM, PM4Py, OR-Tools, MLOps, Feature Store |
