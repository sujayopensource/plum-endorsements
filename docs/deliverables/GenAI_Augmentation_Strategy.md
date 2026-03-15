# GenAI Augmentation Strategy: Rule-Based to LLM-Augmented Intelligence

**Project:** Plum Endorsement Management System
**Deliverable:** #9 -- GenAI Augmentation Strategy for Intelligence Layer
**Date:** March 14, 2026
**Version:** 1.0
**Companion:** [AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md) (24 enhancements, rule-based)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current State: What Exists Today](#2-current-state-what-exists-today)
   - 2.1 [Five Intelligence Pillars](#21-five-intelligence-pillars)
   - 2.2 [Existing Ollama Integration (2 Adapters)](#22-existing-ollama-integration-2-adapters)
   - 2.3 [Established Integration Pattern](#23-established-integration-pattern)
   - 2.4 [Infrastructure Already in Place](#24-infrastructure-already-in-place)
3. [Assessment Framework: Where GenAI Adds Value](#3-assessment-framework-where-genai-adds-value)
   - 3.1 [Decision Criteria](#31-decision-criteria)
   - 3.2 [Three-Bucket Classification](#32-three-bucket-classification)
4. [Bucket 1: GenAI Adds Real Value (8 Items)](#4-bucket-1-genai-adds-real-value-8-items)
   - 4.1 [Forecast Narratives with Actionable Advice (Enhancement 6.2)](#41-forecast-narratives-with-actionable-advice-enhancement-62)
   - 4.2 [Process Mining Bottleneck Recommendations (Enhancement 6.4)](#42-process-mining-bottleneck-recommendations-enhancement-64)
   - 4.3 [Batch Optimization Reports (Enhancement 6.5)](#43-batch-optimization-reports-enhancement-65)
   - 4.4 [Dormancy Break Anomaly Rule (Enhancement 4.2)](#44-dormancy-break-anomaly-rule-enhancement-42)
   - 4.5 [Cross-Insurer Duplication Detection (Enhancement 4.3)](#45-cross-insurer-duplication-detection-enhancement-43)
   - 4.6 [Insurer-Specific Error Patterns (Enhancement 4.4)](#46-insurer-specific-error-patterns-enhancement-44)
   - 4.7 [Multi-Signal Anomaly Correlation (Enhancement 4.5)](#47-multi-signal-anomaly-correlation-enhancement-45)
   - 4.8 [Shadow Mode Framework (Enhancement 21)](#48-shadow-mode-framework-enhancement-21)
5. [Bucket 2: GenAI Adds Marginal Value -- Narrative Overlay (6 Items)](#5-bucket-2-genai-adds-marginal-value--narrative-overlay-6-items)
   - 5.1 [Anomaly False Positive Tracking (Enhancement 3.1)](#51-anomaly-false-positive-tracking-enhancement-31)
   - 5.2 [Forecast Accuracy Backtesting (Enhancement 3.2)](#52-forecast-accuracy-backtesting-enhancement-32)
   - 5.3 [Error Resolution Success Tracking (Enhancement 3.3)](#53-error-resolution-success-tracking-enhancement-33)
   - 5.4 [STP Rate Trending (Enhancement 3.4)](#54-stp-rate-trending-enhancement-34)
   - 5.5 [Process Variant Detection (Enhancement 5.6)](#55-process-variant-detection-enhancement-56)
   - 5.6 [Per-Rule Anomaly Metrics Dashboard (Enhancement 22)](#56-per-rule-anomaly-metrics-dashboard-enhancement-22)
6. [Bucket 3: Keep Rule-Based -- No GenAI Value (8 Items)](#6-bucket-3-keep-rule-based--no-genai-value-8-items)
7. [New Ollama Adapter Implementations](#7-new-ollama-adapter-implementations)
   - 7.1 [OllamaAugmentedForecastEngine](#71-ollamaaugmentedforecastengine)
   - 7.2 [OllamaAugmentedProcessMiner](#72-ollamaaugmentedprocessminer)
   - 7.3 [OllamaAugmentedBatchOptimizer](#73-ollamaaugmentedbatchoptimizer)
   - 7.4 [Anomaly & Error Resolution -- Existing Adapters Extended](#74-anomaly--error-resolution--existing-adapters-extended)
8. [Synchronous vs. Asynchronous GenAI: The Latency Decision](#8-synchronous-vs-asynchronous-genai-the-latency-decision)
   - 8.1 [The Problem: LLM Latency in Real-Time Paths](#81-the-problem-llm-latency-in-real-time-paths)
   - 8.2 [Architecture: Fast Path + Async Enrichment](#82-architecture-fast-path--async-enrichment)
   - 8.3 [Port Interface Extension](#83-port-interface-extension)
   - 8.4 [Async Enrichment via Kafka](#84-async-enrichment-via-kafka)
9. [Prompt Engineering Strategy](#9-prompt-engineering-strategy)
   - 9.1 [Prompt Template Externalization](#91-prompt-template-externalization)
   - 9.2 [Prompt Design Principles](#92-prompt-design-principles)
   - 9.3 [Prompt Catalog (All 5 Adapters)](#93-prompt-catalog-all-5-adapters)
10. [Resilience & Fallback Architecture](#10-resilience--fallback-architecture)
    - 10.1 [Circuit Breaker Configuration](#101-circuit-breaker-configuration)
    - 10.2 [Fallback Chain](#102-fallback-chain)
    - 10.3 [Degradation Modes](#103-degradation-modes)
11. [Infrastructure Requirements](#11-infrastructure-requirements)
    - 11.1 [Ollama Service Configuration](#111-ollama-service-configuration)
    - 11.2 [Spring AI Configuration](#112-spring-ai-configuration)
    - 11.3 [Docker Compose Extension](#113-docker-compose-extension)
    - 11.4 [Kubernetes Deployment](#114-kubernetes-deployment)
    - 11.5 [Cloud-Hosted LLM Alternative](#115-cloud-hosted-llm-alternative)
12. [Effort Estimation & Implementation Roadmap](#12-effort-estimation--implementation-roadmap)
    - 12.1 [Per-Enhancement Effort Table](#121-per-enhancement-effort-table)
    - 12.2 [Effort Summary by Bucket](#122-effort-summary-by-bucket)
    - 12.3 [Week-by-Week Implementation Plan](#123-week-by-week-implementation-plan)
13. [Testing Strategy](#13-testing-strategy)
    - 13.1 [Unit Testing with Mock ChatClient](#131-unit-testing-with-mock-chatclient)
    - 13.2 [Integration Testing with Ollama Testcontainer](#132-integration-testing-with-ollama-testcontainer)
    - 13.3 [Shadow Mode Validation](#133-shadow-mode-validation)
    - 13.4 [Test Count Estimate](#134-test-count-estimate)
14. [Risk Matrix](#14-risk-matrix)
15. [Decision Log](#15-decision-log)

---

## 1. Executive Summary

The Plum Endorsement system's intelligence layer comprises **5 rule-based adapters** behind hexagonal port interfaces, with **24 planned enhancements** documented in the [AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md). Two of these (anomaly explanation enrichment and error resolution) are already augmented with Ollama LLM integration.

This document assesses the remaining **22 enhancements** for GenAI augmentation -- specifically, the effort and architectural approach to make each one use **Ollama (or any LLM) by default with rule-based fallback**.

### Key Findings

- **8 of 22** enhancements genuinely benefit from GenAI (natural language generation, contextual reasoning, pattern recognition in unstructured data)
- **6 of 22** benefit from a lightweight GenAI narrative overlay on top of deterministic computation
- **8 of 22** should remain purely rule-based/infrastructure -- adding GenAI would be over-engineering
- **3 new Ollama adapter classes** are needed (forecast, process mining, batch optimizer)
- **0 new port interfaces** -- the hexagonal architecture already handles adapter switching via `@ConditionalOnProperty`
- The critical design decision is **sync vs. async**: use rule-based for real-time API paths, GenAI for scheduled/background processing

### Effort Summary

| Bucket | Items | GenAI Augmentation Effort | Base Rule-Based Effort |
|--------|-------|--------------------------|----------------------|
| GenAI adds real value | 8 | 12-16 days | 10 days |
| GenAI adds marginal value (narrative overlay) | 6 | 4-5 days | 7 days |
| Keep rule-based (no GenAI) | 8 | 0 days | 17 days |
| **Total** | **22** | **16-21 days incremental** | **34 days base** |

**Combined total**: 34 days (base) + 16-21 days (GenAI) = **~50-55 developer-days** for everything with GenAI where it adds value.

---

## 2. Current State: What Exists Today

### 2.1 Five Intelligence Pillars

| Pillar | Port Interface | Rule-Based Adapter | Ollama Adapter | Status |
|--------|---------------|-------------------|----------------|--------|
| Anomaly Detection | `AnomalyDetectionPort` | `RuleBasedAnomalyDetector` | `OllamaAugmentedAnomalyDetector` | Both operational |
| Balance Forecasting | `BalanceForecastPort` | `StatisticalForecastEngine` | -- | Rule-based only |
| Error Resolution | `ErrorResolutionPort` | `SimulatedErrorResolver` | `OllamaErrorResolver` | Both operational |
| Process Mining | `ProcessMiningPort` | `EventStreamAnalyzer` | -- | Rule-based only |
| Batch Optimization | `BatchOptimizerPort` | `ConstraintBatchOptimizer` | -- | Rule-based only |

**3 pillars lack Ollama adapters**: Balance Forecasting, Process Mining, Batch Optimization.

### 2.2 Existing Ollama Integration (2 Adapters)

**`OllamaAugmentedAnomalyDetector`** (`infrastructure/intelligence/OllamaAugmentedAnomalyDetector.java`):
- Delegates scoring to `RuleBasedAnomalyScorer` (deterministic, fast)
- If score >= threshold (0.7): enriches explanation via Ollama LLM prompt
- If score < threshold: returns rule-based explanation unchanged
- `@CircuitBreaker(name = "ollamaAnomalyDetection")` + `@Retry` with fallback to rule explanation
- Prompt: fraud analyst persona, 2-3 sentence enriched explanation

**`OllamaErrorResolver`** (`infrastructure/intelligence/OllamaErrorResolver.java`):
- Sends full endorsement context + error message to Ollama
- Expects structured JSON response: `{ errorType, originalValue, correctedValue, resolution, confidence }`
- Parses JSON, falls back to `SimulatedErrorResolver` on parse failure
- `@CircuitBreaker(name = "ollamaErrorResolution")` + `@Retry`

### 2.3 Established Integration Pattern

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Domain Port Interface                         │
│                    (e.g., AnomalyDetectionPort)                      │
└───────────────┬────────────────────────────┬─────────────────────────┘
                │                            │
    ┌───────────▼───────────┐    ┌───────────▼───────────┐
    │   Rule-Based Adapter   │    │    Ollama Adapter      │
    │                        │    │                        │
    │ @ConditionalOnProperty │    │ @ConditionalOnProperty │
    │ (ollama.enabled=false, │    │ (ollama.enabled=true)  │
    │  matchIfMissing=true)  │    │                        │
    │                        │    │ ┌────────────────────┐ │
    │ Deterministic rules    │    │ │ Rule-based scorer  │ │
    │ <10ms latency          │    │ │ (fast, deterministic│ │
    │ Always available       │    │ │  — same as left)   │ │
    │                        │    │ └────────┬───────────┘ │
    │                        │    │          │              │
    │                        │    │ ┌────────▼───────────┐ │
    │                        │    │ │ ChatClient → Ollama│ │
    │                        │    │ │ @CircuitBreaker     │ │
    │                        │    │ │ @Retry              │ │
    │                        │    │ │ 1-3s latency       │ │
    │                        │    │ └────────┬───────────┘ │
    │                        │    │          │              │
    │                        │    │ Fallback: rule-based   │
    └────────────────────────┘    └──────────────────────────┘
```

**Key properties of this pattern**:
1. Spring Boot activates exactly one adapter per port based on `endorsement.intelligence.ollama.enabled`
2. The Ollama adapter always computes the deterministic result first, then enriches via LLM
3. Every LLM call has `@CircuitBreaker` + `@Retry` with a typed fallback method
4. Fallback always returns the rule-based result -- never throws, never returns null
5. Per-adapter cost to follow this pattern: **~1 day** (adapter class + prompt + parsing + fallback + test + config)

### 2.4 Infrastructure Already in Place

```
Already configured:
  [x] Spring AI dependency in build.gradle.kts (spring-ai-ollama-spring-boot-starter)
  [x] Ollama service in docker-compose.yml
  [x] application-ollama.yml (model: llama3.2, temperature: 0.3, num-predict: 512)
  [x] @ConditionalOnProperty toggle on all 5 rule-based adapters
  [x] @ConditionalOnProperty toggle on 2 Ollama adapters
  [x] ChatClient.Builder auto-configured by Spring AI
  [x] Resilience4j circuit breaker + retry instances for Ollama
  [x] RuleBasedAnomalyScorer extracted as shared component (used by both adapters)
```

---

## 3. Assessment Framework: Where GenAI Adds Value

### 3.1 Decision Criteria

An enhancement benefits from GenAI augmentation when it meets **2+ of these criteria**:

| Criterion | Description | Example |
|-----------|-------------|---------|
| **Natural language generation** | Output is human-readable narrative that benefits from fluency and contextual phrasing | Anomaly explanations, forecast advice, bottleneck recommendations |
| **Contextual reasoning** | Decision depends on correlating multiple signals that can't be captured in static rules | Dormancy break (is 90-day gap suspicious or seasonal?), cross-insurer duplication (fraud or legitimate?) |
| **Unstructured input parsing** | Input is freeform text (error messages, SOAP faults) that doesn't match keyword patterns | Insurer-specific error messages in the "unknown error" long tail |
| **Explainability** | Users need to understand *why* an algorithmic decision was made, in business language | Batch optimizer explaining why it deferred 3 endorsements |

An enhancement does **NOT** benefit from GenAI when:

| Anti-Criterion | Description | Example |
|----------------|-------------|---------|
| **Deterministic computation** | Output is a number, boolean, or precise state change | MAPE calculation, threshold adjustment, precision tracking |
| **Infrastructure concern** | Change is about locking, shutdown, retention, or health checks | ShedLock, graceful shutdown, data retention |
| **Configuration** | Change moves a constant to `application.yml` | Configurable weights, configurable thresholds |
| **Latency-sensitive path** | Operation is on the synchronous API request path and must complete in <50ms | Real-time anomaly scoring during endorsement creation |

### 3.2 Three-Bucket Classification

```
22 Remaining Enhancements
│
├── Bucket 1: GenAI Adds Real Value (8 items)
│   ├── 6.2  Forecast narratives with actionable advice
│   ├── 6.4  Process mining bottleneck recommendations
│   ├── 6.5  Batch optimization reports
│   ├── 4.2  Dormancy break anomaly rule ✅ COMPLETED
│   ├── 4.3  Cross-insurer duplication detection
│   ├── 4.4  Insurer-specific error patterns
│   ├── 4.5  Multi-signal anomaly correlation
│   └── 21   Shadow mode framework
│
├── Bucket 2: GenAI Adds Marginal Value — Narrative Overlay (6 items)
│   ├── 3.1  Anomaly false positive tracking
│   ├── 3.2  Forecast accuracy backtesting
│   ├── 3.3  Error resolution success tracking ✅ COMPLETED
│   ├── 3.4  STP rate trending with snapshots ✅ COMPLETED
│   ├── 5.6  Process variant detection
│   └── 22   Per-rule anomaly metrics dashboard
│
└── Bucket 3: Keep Rule-Based — No GenAI Value (8 items)
    ├── 4.1  Configurable anomaly thresholds
    ├── 5.1  Distributed scheduler locks (ShedLock)
    ├── 5.2  Graceful shutdown
    ├── 5.3  Data retention for intelligence results
    ├── 5.4  Intelligence health indicators
    ├── 5.5  Configurable optimizer weights
    ├── 23   Forecast factor auto-tuning
    └── 24   Error pattern frequency ranking
```

---

## 4. Bucket 1: GenAI Adds Real Value (8 Items)

### 4.1 Forecast Narratives with Actionable Advice (Enhancement 6.2)

**Rule-based approach**: `String.format()` template in `StatisticalForecastEngine`:
```
"Based on 90-day trends (45 ADD endorsements, avg premium ₹12,500),
 employer will need approximately ₹7,50,000 over the next 30 days.
 Daily burn rate: ₹25,000. Seasonality-adjusted. Confidence: 72%."
```

**GenAI-augmented approach**: LLM generates context-aware, actionable narrative:
```
"EA Account Alert for Acme Corp with ICICI Lombard:

 Current available balance:     ₹5,00,000
 Projected 30-day requirement:  ₹7,50,000
 Projected shortfall:           ₹2,50,000

 Timeline:
 - Balance will cover endorsements for approximately 20 days
 - Shortfall begins around April 3, 2026
 - At current rate, ₹2,50,000 top-up is needed by April 1

 April is historically your highest-volume month (fiscal year hiring wave).
 Last April, Acme processed 2.1x their monthly average. Factor this into
 your top-up planning.

 Action required: Notify employer to top up EA account by ₹2,50,000
 before April 1 to avoid endorsement processing delays."
```

**Why GenAI wins**: The LLM correlates seasonality data, historical patterns, and business context to produce advice that a static template cannot. It generates different advice for a tech company (continuous hiring) vs. a retail company (seasonal spikes).

**Implementation**: New `OllamaAugmentedForecastEngine` adapter. See [Section 7.1](#71-ollamaaugmentedforecastengine).

**Effort**: 1.5 days

### 4.2 Process Mining Bottleneck Recommendations (Enhancement 6.4)

**Rule-based approach**: Static recommendation map:
```
"Bottleneck detected: QUEUED_FOR_BATCH → BATCH_SUBMITTED averages 6.2 hours"
```

**GenAI-augmented approach**: LLM reasons about multiple signals:
```
"Bottleneck detected: QUEUED_FOR_BATCH → BATCH_SUBMITTED

 Metrics:
 - Average duration: 6.2 hours (up from 4.1 hours last week)
 - P95 duration: 14.1 hours
 - P99 duration: 22.3 hours
 - Sample count: 342 (last 7 days)

 Analysis: The 51% increase in average duration correlates with the batch
 size limit (100) being consistently hit — queue depth has averaged 140
 endorsements per assembly cycle. The Niva Bupa CSV/SFTP adapter is the
 primary contributor (batch-only, slower processing).

 Recommendations:
 1. Increase batch assembly frequency from every 15 min to every 10 min
 2. Consider splitting Niva Bupa batches into 50-item chunks for faster SFTP transfer
 3. Monitor: if P95 exceeds 18 hours, escalate to insurer SLA review

 Impact: 342 endorsements experienced delays affecting 28 employers."
```

**Why GenAI wins**: Can correlate queue depth, insurer-specific latency, and SLA data to produce novel recommendations that a static map cannot anticipate.

**Implementation**: New `OllamaAugmentedProcessMiner` adapter. See [Section 7.2](#72-ollamaaugmentedprocessminer).

**Effort**: 2 days

### 4.3 Batch Optimization Reports (Enhancement 6.5)

**Rule-based approach**: Summary string from `ConstraintBatchOptimizer`:
```
"DP knapsack optimization: 12 of 15 endorsed, deletions first,
 0-1 knapsack for additions. Savings: ₹15,000"
```

**GenAI-augmented approach**: LLM narrates optimizer decisions in business language:
```
"Batch Optimization Report for ICICI Lombard (Batch #47):

 Strategy: KNAPSACK_WITH_DELETES_FIRST

 Included (12 endorsements):
 - 3 DELETEs processed first (freed ₹45,000 balance)
 - 9 ADDs selected by composite score (top 9 of 15 queued)
 - Total premium: ₹1,85,000

 Deferred (3 endorsements):
 - 2 low-priority ADDs (P3, coverage starts in 25+ days) — will be
   picked up in the next batch cycle in 15 minutes
 - 1 high-premium ADD (₹80,000 for employee Rajesh Kumar) — exceeds
   remaining balance. Requires EA top-up before processing.

 Why this order: Processing the 3 deletions first freed ₹45,000 in EA
 balance, which allowed 2 additional ADD endorsements to fit within the
 balance constraint that would have been deferred under FIFO ordering.

 Savings vs. FIFO: ₹15,000
 Balance utilization: 92% (₹1,85,000 of ₹2,00,000 available)"
```

**Why GenAI wins**: Makes the DP knapsack algorithm's decisions interpretable to non-technical operations staff. Explains *why* specific endorsements were deferred.

**Implementation**: New `OllamaAugmentedBatchOptimizer` adapter. See [Section 7.3](#73-ollamaaugmentedbatchoptimizer).

**Effort**: 1.5 days

### 4.4 Dormancy Break Anomaly Rule (Enhancement 4.2) — **✅ COMPLETED**

> **Implementation Status**: `checkDormancyBreak()` added to `RuleBasedAnomalyScorer.java`. `DORMANCY_BREAK` added to `AnomalyType` enum. The existing `OllamaAugmentedAnomalyDetector` automatically enriches dormancy break explanations via LLM. Covered by 5 unit tests, 1 API test, 1 BDD scenario, 1 E2E test.

**Rule-based approach**: Score = `0.6 + (daysSinceLastActivity / 365.0)`, capped at 0.85.

**GenAI-augmented approach**: LLM evaluates whether dormancy is suspicious given employer context:
- A 90-day gap for a seasonal retail business → likely legitimate (off-season)
- A 90-day gap for an IT services company → suspicious (IT hires year-round)
- A 120-day gap followed by 15 ADDs in one day → highly suspicious

**Implementation**: Handled by existing `OllamaAugmentedAnomalyDetector`. The `RuleBasedAnomalyScorer` computes the dormancy score, and the Ollama adapter enriches the explanation with employer-context reasoning. **No new adapter class needed** -- dormancy rule added to `RuleBasedAnomalyScorer` and the existing Ollama enrichment pipeline handles it.

**Effort**: 2 days (1d for rule in scorer, 1d for prompt tuning + tests)

### 4.5 Cross-Insurer Duplication Detection (Enhancement 4.3)

**Rule-based approach**: Binary check -- employee has ADD endorsements across multiple insurers within 30 days → score 0.80.

**GenAI-augmented approach**: LLM evaluates whether duplication is fraud vs. legitimate:
- Employee transferred between insurers (coverage overlap period) → legitimate
- Employee added to 3+ insurers simultaneously → likely fraud or data entry error
- Employee added to new insurer while DELETE pending on old insurer → legitimate transition

**Implementation**: Same as 4.4 -- add rule to `RuleBasedAnomalyScorer`, existing `OllamaAugmentedAnomalyDetector` enriches the explanation. **No new adapter class.**

**Effort**: 2 days

### 4.6 Insurer-Specific Error Patterns (Enhancement 4.4)

**Rule-based approach**: Per-insurer keyword maps in YAML config. Handles known patterns but returns `UNKNOWN_ERROR` with 0.3 confidence for unrecognized errors.

**GenAI-augmented approach**: The existing `OllamaErrorResolver` already handles this -- it parses freeform error messages and returns structured JSON with error type, corrected value, and confidence. The enhancement is to **improve the prompt** with insurer-specific context:

```
"You are resolving an error from ICICI Lombard (REST/JSON API).
 ICICI Lombard uses member ID format 'PLMICICI-XXXXXXXX' and
 date format 'yyyy-MM-dd'. Common ICICI errors include:
 - 'Invalid Member ID' — prefix mismatch
 - 'Date parse error' — DD/MM/YYYY sent instead of YYYY-MM-DD
 ..."
```

**Implementation**: Modify existing `OllamaErrorResolver.buildPrompt()` to include insurer-specific context from `InsurerConfiguration`. **No new adapter class.**

**Effort**: 2 days (prompt engineering + insurer context injection + tests)

### 4.7 Multi-Signal Anomaly Correlation (Enhancement 4.5)

**Rule-based approach**: `maxScore + 0.05 * (firedRules - 1)` — arithmetic score boost when multiple rules fire.

**GenAI-augmented approach**: LLM explains *why* correlated signals are more suspicious:
```
"Multiple anomaly signals detected for employer Acme Corp:
 - VOLUME_SPIKE (score: 0.65): 15 endorsements in 24h vs. avg 2.5/day
 - UNUSUAL_PREMIUM (score: 0.55): ₹95,000 premium is 3.8σ above employer mean
 - SUSPICIOUS_TIMING (score: 0.60): coverage starts in 3 days

 Correlated assessment: These three signals together strongly suggest
 possible pre-claim manipulation. The volume spike of high-premium
 endorsements with imminent coverage dates is a classic pattern for
 adding employees who already have pending medical claims. Recommend
 immediate review before insurer submission."
```

**Implementation**: Add multi-signal detection to `RuleBasedAnomalyScorer` (fires when 2+ rules exceed individual sub-thresholds). Existing `OllamaAugmentedAnomalyDetector` enriches the correlated explanation. **No new adapter class.**

**Effort**: 1.5 days

### 4.8 Shadow Mode Framework (Enhancement 21)

**What it is**: A framework that runs **both** rule-based and Ollama adapters in parallel, logs both results, but only uses the rule-based result for actual business decisions. This enables safe A/B comparison of GenAI accuracy before switching to GenAI-primary.

**Implementation**:

```java
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.shadow-mode.enabled",
                        havingValue = "true")
public class ShadowModeAnomalyDetector implements AnomalyDetectionPort {

    private final RuleBasedAnomalyScorer scorer;
    private final ChatClient.Builder chatClientBuilder;
    private final MeterRegistry meterRegistry;

    @Override
    public AnomalyResult analyzeEndorsement(Endorsement endorsement,
                                             List<Endorsement> recentHistory) {
        // 1. Always compute rule-based result (this is the "source of truth")
        ScoringResult ruleResult = scorer.score(endorsement, recentHistory);

        // 2. Async: also compute Ollama result (for comparison only)
        CompletableFuture.runAsync(() -> {
            try {
                String ollamaExplanation = enrichWithLlm(endorsement, ruleResult);
                // Log comparison — never used for business logic
                log.info("SHADOW_MODE comparison for endorsement {}: " +
                         "rule_type={}, rule_score={}, ollama_explanation_length={}",
                         endorsement.getId(), ruleResult.anomalyType(),
                         ruleResult.score(), ollamaExplanation.length());
                meterRegistry.counter("endorsement.shadow.comparison",
                        "anomalyType", ruleResult.anomalyType()).increment();
            } catch (Exception e) {
                log.debug("Shadow mode Ollama call failed (non-critical): {}",
                          e.getMessage());
            }
        });

        // 3. Return rule-based result (Ollama result is logged only)
        return new AnomalyResult(ruleResult.anomalyType(), ruleResult.score(),
                ruleResult.ruleExplanation());
    }
}
```

**Why it matters**: Critical for validating GenAI accuracy before switching any adapter from rule-based-primary to GenAI-primary. Without shadow mode, switching to GenAI is a leap of faith.

**Effort**: 1.5 days (generic framework applicable to all 5 ports)

---

## 5. Bucket 2: GenAI Adds Marginal Value -- Narrative Overlay (6 Items)

These enhancements have a **deterministic core** that must stay rule-based. GenAI adds an optional narrative summary layer on top.

**Pattern**: Keep the computation. Add an optional `enrichNarrative(data)` call to Ollama that runs *after* the computation, gated by configuration.

```java
// Pattern for all Bucket 2 items
String narrative = computeDeterministicResult(data);  // Always runs
if (ollamaEnabled && !isRealTimePath) {
    narrative = enrichNarrative(data, narrative);       // Optional LLM overlay
}
```

### 5.1 Anomaly False Positive Tracking (Enhancement 3.1)

**Deterministic core** (must stay rule-based):
- Precision = dismissed / (dismissed + confirmed) per AnomalyType
- Threshold adjustment: precision < 20% → raise threshold by 0.05; precision > 80% → lower by 0.05
- Stored in Redis: `anomaly:precision:{VOLUME_SPIKE}` = `{ confirmed: 23, dismissed: 87 }`

**GenAI narrative overlay** (optional):
- Weekly report: "Volume Spike rule has 20.9% precision this month. 87 of 110 flagged anomalies were dismissed. The rule is triggering on legitimate hiring batches for IT employers. Consider raising the minimum endorsement count from 10 to 15."

**Effort**: 1 day (0.5d deterministic + 0.5d narrative)

### 5.2 Forecast Accuracy Backtesting (Enhancement 3.2)

**Deterministic core** (must stay rule-based):
- MAPE = mean(|actual - forecast| / forecast) per (employerId, insurerId)
- Backfill `actualAmount` from endorsement history for mature forecasts
- Store MAPE in Redis for dashboard display

**GenAI narrative overlay** (optional):
- "March forecasts for Acme Corp had 68% accuracy (MAPE: 32%). The primary error source was underestimating April hiring: the March→April seasonality factor (1.4) was insufficient for Acme's 2.3x actual spike. Recommend setting a company-specific April factor of 2.0."

**Effort**: 1 day (0.5d deterministic + 0.5d narrative)

### 5.3 Error Resolution Success Tracking (Enhancement 3.3) — **✅ COMPLETED**

> **Implementation Status**: V20 migration adds `outcome`, `outcome_at`, `outcome_endorsement_status` columns to `error_resolutions`. `ErrorResolutionService.trackOutcome()` hooks into `ProcessEndorsementHandler` at CONFIRMED/REJECTED/FAILED_PERMANENT transitions. Stats endpoint returns `successCount`, `failureCount`, `successRate`. Covered by 5 unit + 1 API + 1 BDD + 1 E2E tests.

**Deterministic core** (rule-based — implemented):
- Track fix outcomes: after auto-applying a fix, listen for subsequent Confirmed/Rejected events
- success_rate = fix_success / (fix_success + fix_failure) per errorType
- If success_rate < 80% → disable auto-apply for that error type

**GenAI narrative overlay** (optional — not yet implemented):
- "DATE_FORMAT auto-corrections have dropped to 72% success rate this week (down from 97% last month). 8 of 28 corrections were rejected by ICICI Lombard. Analysis: ICICI appears to have changed their accepted date format from yyyy-MM-dd to dd-MMM-yyyy. Recommend updating the ICICI date format pattern."

**Effort**: 1 day (0.5d deterministic + 0.5d narrative)

### 5.4 STP Rate Trending (Enhancement 3.4) — **✅ COMPLETED**

> **Implementation Status**: V19 migration creates `stp_rate_snapshots` table. `StpRateSnapshot` domain model, `StpRateSnapshotRepository` port, JPA adapter all implemented. New endpoint `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days=30` returns `StpRateTrendResponse`. `ProcessMiningScheduler` captures daily snapshots. Covered by 4 unit + 1 API + 1 BDD + 1 E2E tests.

**Deterministic core** (rule-based — implemented):
- Daily snapshot: `StpRateSnapshot(insurerId, stpRate, totalEndorsements, snapshotDate)`
- Stored in `stp_rate_snapshots` table
- Trend API: `GET /api/v1/intelligence/process-mining/stp-rate/trend?insurerId={id}&days=30`

**GenAI narrative overlay** (optional — not yet implemented):
- "ICICI Lombard STP rate improved from 87.2% to 92.1% over the past 14 days (+4.9 percentage points). This improvement correlates with the batch size reduction deployed on March 1. Bajaj Allianz STP rate declined 2.3% — investigate SOAP timeout errors."

**Effort**: 0.5 day (0.25d deterministic snapshot already designed + 0.25d narrative)

### 5.5 Process Variant Detection (Enhancement 5.6)

**Deterministic core** (must stay rule-based):
- Group events by endorsementId → extract state sequence → count variants
- "Created → Validated → QueuedForBatch → BatchSubmitted → Confirmed" = variant #1 (78%)
- Store top-10 variants per insurer in `ProcessVariant` entity

**GenAI narrative overlay** (optional):
- LLM classifies each variant: "normal" (happy path), "degraded" (retries involved), "anomalous" (unexpected state sequence). Explains business impact: "Variant #3 (7% of endorsements) involves a REJECTED → RETRY_PENDING → SUBMITTED cycle. Average lifecycle: 4.2 days vs. 0.8 days for happy path. These 47 endorsements affected 12 employers."

**Effort**: 1 day (0.5d variant extraction + 0.5d LLM classification)

### 5.6 Per-Rule Anomaly Metrics Dashboard (Enhancement 22)

**This is purely visualization** -- Grafana dashboard JSON panels showing per-rule precision, threshold, and fire rate. No GenAI value.

**Effort**: 0.5 day (Grafana dashboard only, no LLM)

---

## 6. Bucket 3: Keep Rule-Based -- No GenAI Value (8 Items)

These are infrastructure, configuration, or pure numerical operations. Adding LLM calls would introduce latency and non-determinism with zero benefit.

| # | Enhancement | Why GenAI Doesn't Help |
|---|-------------|----------------------|
| **4.1** | Configurable anomaly thresholds | Moving hardcoded constants to `@Value("${...}")` in YAML. There is nothing to "generate." |
| **5.1** | Distributed scheduler locks (ShedLock) | Database row locking (`SELECT ... FOR UPDATE`). Pure infrastructure. |
| **5.2** | Graceful shutdown | `server.shutdown: graceful` + `Thread.currentThread().isInterrupted()` checks. Infrastructure. |
| **5.3** | Data retention for intelligence results | `DELETE FROM anomaly_detections WHERE flagged_at < ?`. Retention policy is deterministic. |
| **5.4** | Intelligence health indicators | `HealthIndicator` checking Redis for scheduler last-run timestamps. Infrastructure. |
| **5.5** | Configurable optimizer weights | Moving `URGENCY_WEIGHT = 0.6` to `@Value("${endorsement.intelligence.batch-optimizer.urgency-weight:0.6}")`. Configuration. |
| **23** | Forecast factor auto-tuning | Numerical optimization: `new_factor = old_factor * (actual / forecast)`. MAPE-driven, deterministic adjustment. |
| **24** | Error pattern frequency ranking | `SELECT error_type, COUNT(*) FROM error_resolutions GROUP BY error_type ORDER BY COUNT(*) DESC`. SQL aggregation. |

**Recommendation**: Implement these exactly as specified in the [AI Automation Implementation Approach](vision/AI_Automation_Implementation_Approach.md). No architectural changes needed.

---

## 7. New Ollama Adapter Implementations

### 7.1 OllamaAugmentedForecastEngine

**File**: `infrastructure/intelligence/OllamaAugmentedForecastEngine.java`

**Implements**: `BalanceForecastPort`

```java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled",
                        havingValue = "true")
public class OllamaAugmentedForecastEngine implements BalanceForecastPort {

    private final StatisticalForecastComputer computer;  // Extracted from StatisticalForecastEngine
    private final ChatClient.Builder chatClientBuilder;

    @Override
    public ForecastResult generateForecast(UUID employerId, UUID insurerId,
                                            List<Endorsement> history) {
        // 1. Always compute deterministic forecast (fast, reliable)
        ForecastResult baseResult = computer.compute(employerId, insurerId, history);

        // 2. Enrich narrative via LLM
        String enrichedNarrative = enrichNarrative(employerId, insurerId, baseResult);

        return new ForecastResult(
                baseResult.forecastedNeed(), baseResult.forecastDays(),
                baseResult.dailyBurnRate(), baseResult.shortfall(),
                baseResult.topUpRequired(), enrichedNarrative);
    }

    @CircuitBreaker(name = "ollamaForecast", fallbackMethod = "enrichNarrativeFallback")
    @Retry(name = "ollamaForecast")
    private String enrichNarrative(UUID employerId, UUID insurerId,
                                    ForecastResult baseResult) {
        ChatClient client = chatClientBuilder.build();
        String prompt = buildForecastPrompt(employerId, insurerId, baseResult);
        String enriched = client.prompt().user(prompt).call().content();
        return enriched != null && !enriched.isBlank()
                ? enriched : baseResult.narrative();
    }

    @SuppressWarnings("unused")
    private String enrichNarrativeFallback(UUID employerId, UUID insurerId,
                                            ForecastResult baseResult, Throwable t) {
        log.warn("Ollama forecast narrative fallback for employer {}: {}",
                 employerId, t.getMessage());
        return baseResult.narrative();
    }
}
```

**Refactoring required**: Extract the computation logic from `StatisticalForecastEngine` into a shared `StatisticalForecastComputer` component (similar to how `RuleBasedAnomalyScorer` was extracted from the anomaly detector). Both the rule-based and Ollama adapters delegate to this computer.

### 7.2 OllamaAugmentedProcessMiner

**File**: `infrastructure/intelligence/OllamaAugmentedProcessMiner.java`

**Implements**: `ProcessMiningPort`

**Strategy**: `EventStreamAnalyzer` computes metrics (avg/p95/p99 durations, happy path %, transition counts). The Ollama adapter:
1. Runs the same computation via an extracted `EventStreamComputer`
2. Sends metrics to LLM with prompt: "Analyze these process mining metrics. Identify bottlenecks, compare to prior period, and recommend operational changes."
3. Attaches LLM-generated insights as a `narrative` field on each `ProcessMiningMetric`

**Refactoring required**: Extract computation from `EventStreamAnalyzer` into `EventStreamComputer`.

### 7.3 OllamaAugmentedBatchOptimizer

**File**: `infrastructure/intelligence/OllamaAugmentedBatchOptimizer.java`

**Implements**: `BatchOptimizerPort`

**Strategy**: `ConstraintBatchOptimizer` runs the DP knapsack algorithm. The Ollama adapter:
1. Runs the same optimization via an extracted `ConstraintKnapsackSolver`
2. Sends the `OptimizedBatchPlan` (included list, deferred list, savings) to LLM
3. LLM generates a human-readable report explaining each decision

**Refactoring required**: Extract the DP solver from `ConstraintBatchOptimizer` into `ConstraintKnapsackSolver`.

### 7.4 Anomaly & Error Resolution -- Existing Adapters Extended

**No new adapter classes needed.** The existing `OllamaAugmentedAnomalyDetector` and `OllamaErrorResolver` absorb the new rules (dormancy break, cross-insurer duplication, multi-signal correlation, insurer-specific patterns) because:

1. New anomaly rules are added to `RuleBasedAnomalyScorer` (shared component)
2. The Ollama anomaly adapter already calls `scorer.score()` and enriches any result above threshold
3. Insurer-specific error patterns are handled by modifying `OllamaErrorResolver.buildPrompt()` to include insurer context

**Changes to existing adapters**:

| Adapter | Change | Effort |
|---------|--------|--------|
| `OllamaAugmentedAnomalyDetector` | None -- automatically enriches new rule types | 0 |
| `RuleBasedAnomalyScorer` | Add 3 new rules: dormancy break, cross-insurer duplication, multi-signal correlation | 3 days |
| `OllamaErrorResolver` | Add insurer context to prompt (query `InsurerConfiguration` for format preferences) | 1 day |

---

## 8. Synchronous vs. Asynchronous GenAI: The Latency Decision

### 8.1 The Problem: LLM Latency in Real-Time Paths

```
LLM call latency: 1-3 seconds (Ollama local, llama3.2)
API response budget: <200ms (P95 target)

Current real-time paths that invoke intelligence ports:
├── POST /api/v1/endorsements (CreateEndorsementHandler)
│   └── AnomalyDetectionPort.analyzeEndorsement()    ← Called inline
│   └── ErrorResolutionPort.analyzeError()            ← Called on rejection
│
Current scheduled paths (background, latency-tolerant):
├── AnomalyDetectionScheduler (every 5 min)
├── BalanceForecastScheduler (daily 6 AM)
├── ProcessMiningScheduler (daily 3 AM)
├── BatchAssemblyScheduler (every 15 min)
└── DataRetentionScheduler (daily)
```

**Scheduled paths**: LLM latency is acceptable. Users are not waiting. Use Ollama directly.

**Real-time paths**: LLM latency is unacceptable. A 2-second anomaly enrichment during `POST /endorsements` would violate SLA.

### 8.2 Architecture: Fast Path + Async Enrichment

```
POST /api/v1/endorsements
│
▼
CreateEndorsementHandler
│
├── 1. AnomalyDetectionPort.analyzeEndorsementFast()     ← FAST: rule-based only (<10ms)
│      Returns: AnomalyResult(type, score, ruleExplanation)
│
├── 2. If anomaly detected: publish AnomalyFlagged event to Kafka
│
▼
(Handler returns HTTP 201 within 200ms)


Background (async, via Kafka consumer):
│
├── AnomalyEnrichmentConsumer listens for AnomalyFlagged events
├── Calls OllamaAugmentedAnomalyDetector.enrichExplanation()
├── Updates anomaly_detections record with enriched explanation
└── Broadcasts WebSocket notification with enriched explanation
```

### 8.3 Port Interface Extension

Add a `default` fast-path method to intelligence ports that need real-time invocation:

```java
public interface AnomalyDetectionPort {
    // Full analysis (may include LLM enrichment — used by schedulers)
    AnomalyResult analyzeEndorsement(Endorsement endorsement,
                                      List<Endorsement> recentHistory);

    // Fast analysis (rule-based only — used by real-time API handlers)
    default AnomalyResult analyzeEndorsementFast(Endorsement endorsement,
                                                  List<Endorsement> recentHistory) {
        return analyzeEndorsement(endorsement, recentHistory);
    }
}
```

The rule-based adapter's implementation of both methods is identical (fast by nature). The Ollama adapter overrides `analyzeEndorsementFast()` to skip the LLM call:

```java
// In OllamaAugmentedAnomalyDetector
@Override
public AnomalyResult analyzeEndorsementFast(Endorsement endorsement,
                                             List<Endorsement> recentHistory) {
    // Rule-based scoring only — no LLM call
    ScoringResult result = scorer.score(endorsement, recentHistory);
    return new AnomalyResult(result.anomalyType(), result.score(),
                              result.ruleExplanation());
}
```

### 8.4 Async Enrichment via Kafka

```java
@Component
@ConditionalOnProperty(name = "endorsement.intelligence.ollama.enabled",
                        havingValue = "true")
public class AnomalyEnrichmentConsumer {

    @KafkaListener(topics = "endorsement-events",
                   groupId = "anomaly-enrichment")
    public void onAnomalyFlagged(EndorsementEvent.AnomalyFlagged event) {
        // Fetch endorsement + history
        // Call OllamaAugmentedAnomalyDetector.analyzeEndorsement() (full, with LLM)
        // Update anomaly record with enriched explanation
        // Broadcast via WebSocket
    }
}
```

**Impact on existing handlers**: `CreateEndorsementHandler` changes one method call:
```java
// Before
anomalyDetectionPort.analyzeEndorsement(endorsement, history);
// After
anomalyDetectionPort.analyzeEndorsementFast(endorsement, history);
```

---

## 9. Prompt Engineering Strategy

### 9.1 Prompt Template Externalization

Move prompts from inline Java strings to external template files for easier iteration:

```
src/main/resources/prompts/
├── anomaly-enrichment.txt          ← Anomaly explanation enrichment
├── error-resolution.txt            ← Error analysis + structured JSON output
├── forecast-narrative.txt          ← Balance forecast actionable advice
├── bottleneck-recommendation.txt   ← Process mining bottleneck analysis
├── batch-optimization-report.txt   ← Batch optimizer decision explanation
└── variant-classification.txt      ← Process variant classification
```

Load via `@Value("classpath:prompts/anomaly-enrichment.txt")` or Spring's `Resource` abstraction.

### 9.2 Prompt Design Principles

| Principle | Rationale | Example |
|-----------|-----------|---------|
| **Domain persona** | Grounds the LLM in insurance operations context | "You are a fraud analyst for an Indian health insurance platform (Plum)." |
| **Structured input** | Provide all relevant data fields explicitly | "Anomaly Type: VOLUME_SPIKE, Score: 0.82, Employer: Acme Corp, ..." |
| **Output format constraint** | Prevent markdown, JSON wrapping, or verbose preamble | "Respond with ONLY the enriched explanation text (no JSON, no markdown)." |
| **Length constraint** | Keep LLM output concise and predictable | "Provide a concise analysis (3-5 sentences)." |
| **Actionable output** | Every response must include a recommended action | "End with a specific action the operations team should take." |
| **Low temperature** | Minimize hallucination for factual/analytical tasks | `temperature: 0.3` (already configured) |
| **Token budget** | Prevent runaway generation | `num-predict: 512` (768 for forecast narratives) |

### 9.3 Prompt Catalog (All 5 Adapters)

| Adapter | Prompt Goal | Input Data | Output Format | Token Budget |
|---------|-------------|------------|---------------|-------------|
| Anomaly Enrichment | Business-context explanation + recommended action | anomalyType, score, ruleExplanation, endorsement details | Plain text (2-3 sentences) | 512 |
| Error Resolution | Classify error, suggest correction, rate confidence | endorsement details, errorMessage, insurerId | Structured JSON | 512 |
| Forecast Narrative | Actionable balance advice with timeline | forecastedNeed, dailyBurnRate, shortfall, seasonality factors | Plain text (5-8 sentences) | 768 |
| Bottleneck Recommendation | Root cause + operational recommendations | transition metrics (avg/p95/p99), trend vs. prior period, queue depth | Plain text (5-10 sentences) | 768 |
| Batch Optimization Report | Explain optimizer decisions in business language | included list, deferred list (with reasons), savings, balance utilization | Plain text (8-12 sentences) | 768 |

---

## 10. Resilience & Fallback Architecture

### 10.1 Circuit Breaker Configuration

```yaml
# application-ollama.yml additions
resilience4j:
  circuitbreaker:
    instances:
      ollamaAnomalyDetection:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
      ollamaErrorResolution:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
      ollamaForecast:                    # NEW
        slidingWindowSize: 5
        failureRateThreshold: 40
        waitDurationInOpenState: 120s     # Longer — forecast runs daily
      ollamaProcessMining:               # NEW
        slidingWindowSize: 5
        failureRateThreshold: 40
        waitDurationInOpenState: 120s
      ollamaBatchOptimizer:              # NEW
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s

  retry:
    instances:
      ollamaAnomalyDetection:
        maxAttempts: 2
        waitDuration: 500ms
      ollamaErrorResolution:
        maxAttempts: 2
        waitDuration: 500ms
      ollamaForecast:
        maxAttempts: 3
        waitDuration: 1s
      ollamaProcessMining:
        maxAttempts: 3
        waitDuration: 1s
      ollamaBatchOptimizer:
        maxAttempts: 2
        waitDuration: 500ms
```

### 10.2 Fallback Chain

Every Ollama adapter follows a strict fallback chain:

```
1. Try LLM call (with @Retry, up to N attempts with backoff)
       │
       ▼ (on any exception)
2. @CircuitBreaker triggers fallback method
       │
       ▼
3. Fallback returns rule-based result
       │
       ▼
4. Circuit opens after failureRateThreshold% failures
       │
       ▼
5. All subsequent calls go directly to fallback (skip LLM)
       │
       ▼ (after waitDurationInOpenState)
6. Half-open: try one LLM call
       │
       ├── Success → close circuit, resume LLM calls
       └── Failure → re-open circuit, extend wait
```

**Critical invariant**: The system **never fails** due to LLM unavailability. If Ollama is down, restarting, or overloaded, every adapter seamlessly degrades to rule-based behavior. No operator intervention required.

### 10.3 Degradation Modes

| Mode | Ollama Status | Behavior | User-Visible Impact |
|------|--------------|----------|-------------------|
| **Full GenAI** | Healthy | LLM enriches all outputs | Rich explanations, actionable narratives |
| **Partial degradation** | Intermittent failures | Some calls use LLM, some fall back | Mixed explanation quality (acceptable) |
| **Circuit open** | Down or overloaded | All calls use rule-based fallback | Standard explanations (same as before GenAI) |
| **Ollama disabled** | Not deployed | `ollama.enabled=false`, rule-based adapters activated | Identical to pre-GenAI behavior |

---

## 11. Infrastructure Requirements

### 11.1 Ollama Service Configuration

| Parameter | Current | Recommended for Full Augmentation |
|-----------|---------|----------------------------------|
| Model | `llama3.2` (3B params) | `llama3.2` for anomaly/error; consider `llama3.1:8b` for forecast/process mining narratives |
| Temperature | 0.3 | 0.3 (analytical tasks) |
| `num-predict` | 512 | 512 (anomaly, error) / 768 (forecast, process mining, batch) |
| GPU | Optional | Recommended for <1s latency; CPU-only works but 3-5s per call |
| Memory | ~4GB for 3B model | ~4GB (3B) or ~8GB (8B model) |
| Concurrent requests | Default (1) | Set `OLLAMA_NUM_PARALLEL=4` for scheduler bursts |

### 11.2 Spring AI Configuration

```yaml
# application-ollama.yml — extended
endorsement:
  intelligence:
    ollama:
      enabled: true

spring:
  ai:
    model:
      chat: ollama
    ollama:
      base-url: ${OLLAMA_URL:http://localhost:11434}
      chat:
        model: ${OLLAMA_MODEL:llama3.2}
        options:
          temperature: 0.3
          num-predict: ${OLLAMA_MAX_TOKENS:512}

# Per-adapter model override (optional — use larger model for narratives)
endorsement.intelligence.forecast.ollama-model: ${OLLAMA_FORECAST_MODEL:llama3.2}
endorsement.intelligence.process-mining.ollama-model: ${OLLAMA_MINING_MODEL:llama3.2}
```

### 11.3 Docker Compose Extension

The existing `docker-compose.yml` already includes an Ollama service. For the full augmentation, ensure the Ollama service pre-pulls the required model:

```yaml
# docker-compose.yml — ollama service (already present)
ollama:
  image: ollama/ollama:latest
  ports:
    - "11434:11434"
  volumes:
    - ollama_data:/root/.ollama
  deploy:
    resources:
      reservations:
        devices:
          - driver: nvidia           # Optional: GPU acceleration
            count: 1
            capabilities: [gpu]
  healthcheck:
    test: ["CMD", "ollama", "list"]
    interval: 10s
    timeout: 5s
    retries: 5
```

Add a model-pull init container or startup script:
```bash
# In start.sh, after docker-compose up:
docker exec plum-ollama ollama pull llama3.2
```

### 11.4 Kubernetes Deployment

```yaml
# k8s/ollama/deployment.yaml (NEW)
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ollama
  namespace: plum
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ollama
  template:
    spec:
      containers:
        - name: ollama
          image: ollama/ollama:latest
          ports:
            - containerPort: 11434
          resources:
            requests:
              memory: "4Gi"
              cpu: "2"
            limits:
              memory: "6Gi"
              cpu: "4"
          env:
            - name: OLLAMA_NUM_PARALLEL
              value: "4"
          volumeMounts:
            - name: ollama-data
              mountPath: /root/.ollama
      volumes:
        - name: ollama-data
          persistentVolumeClaim:
            claimName: ollama-pvc
```

### 11.5 Cloud-Hosted LLM Alternative

The Spring AI `ChatClient` abstraction supports swapping Ollama for any cloud LLM provider without changing adapter code:

| Provider | Spring AI Starter | Config Change | Latency | Cost |
|----------|------------------|--------------|---------|------|
| Ollama (local) | `spring-ai-ollama-spring-boot-starter` | `spring.ai.ollama.base-url` | 1-3s (CPU), <1s (GPU) | Free (self-hosted) |
| AWS Bedrock (Claude) | `spring-ai-bedrock-ai-spring-boot-starter` | `spring.ai.bedrock.anthropic.*` | 0.5-2s | ~$0.003/1K tokens |
| OpenAI (GPT-4o) | `spring-ai-openai-spring-boot-starter` | `spring.ai.openai.api-key` | 0.5-1.5s | ~$0.005/1K tokens |
| Azure OpenAI | `spring-ai-azure-openai-spring-boot-starter` | `spring.ai.azure.openai.*` | 0.5-1.5s | ~$0.005/1K tokens |
| Google Vertex AI (Gemini) | `spring-ai-vertex-ai-gemini-spring-boot-starter` | `spring.ai.vertex.ai.*` | 0.5-2s | ~$0.004/1K tokens |

**Zero adapter code changes required** -- only `application-{profile}.yml` configuration changes. This is a direct benefit of programming to the `ChatClient` interface (Strategy pattern).

---

## 12. Effort Estimation & Implementation Roadmap

### 12.1 Per-Enhancement Effort Table

| # | Enhancement | Base (Rule-Based) | GenAI Augmentation | Total | New Adapter? |
|---|-------------|-------------------|-------------------|-------|-------------|
| | **Bucket 1: GenAI Adds Real Value** | | | | |
| 6.2 | Forecast narratives | 1d | 1.5d | 2.5d | `OllamaAugmentedForecastEngine` (NEW) |
| 6.4 | Bottleneck recommendations | 1d | 2d | 3d | `OllamaAugmentedProcessMiner` (NEW) |
| 6.5 | Batch optimization reports | 1d | 1.5d | 2.5d | `OllamaAugmentedBatchOptimizer` (NEW) |
| 4.2 | Dormancy break rule | 1d | 1d | 2d | Existing `OllamaAugmentedAnomalyDetector` |
| 4.3 | Cross-insurer duplication | 1d | 1d | 2d | Existing `OllamaAugmentedAnomalyDetector` |
| 4.4 | Insurer-specific error patterns | 3d | 1d | 4d | Existing `OllamaErrorResolver` |
| 4.5 | Multi-signal correlation | 1d | 0.5d | 1.5d | Existing `OllamaAugmentedAnomalyDetector` |
| 21 | Shadow mode framework | 0d | 1.5d | 1.5d | Generic framework (all ports) |
| | **Bucket 1 Subtotal** | **9d** | **10d** | **19d** | |
| | **Bucket 2: Narrative Overlay** | | | | |
| 3.1 | Anomaly false positive tracking | 2d | 0.5d | 2.5d | None (inline enrichNarrative call) |
| 3.2 | Forecast accuracy backtesting | 2d | 0.5d | 2.5d | None |
| 3.3 | Error resolution success tracking | 2d | 0.5d | 2.5d | None |
| 3.4 | STP rate trending | 1d | 0.25d | 1.25d | None |
| 5.6 | Process variant detection | 2d | 0.5d | 2.5d | None |
| 22 | Per-rule anomaly metrics dashboard | 1d | 0d | 1d | None (Grafana only) |
| | **Bucket 2 Subtotal** | **10d** | **2.25d** | **12.25d** | |
| | **Bucket 3: Rule-Based Only** | | | | |
| 4.1 | Configurable anomaly thresholds | 1d | 0d | 1d | -- |
| 5.1 | Distributed scheduler locks | 2d | 0d | 2d | -- |
| 5.2 | Graceful shutdown | 1d | 0d | 1d | -- |
| 5.3 | Data retention | 1d | 0d | 1d | -- |
| 5.4 | Intelligence health indicators | 1d | 0d | 1d | -- |
| 5.5 | Configurable optimizer weights | 0.5d | 0d | 0.5d | -- |
| 23 | Forecast factor auto-tuning | 2d | 0d | 2d | -- |
| 24 | Error pattern frequency ranking | 1d | 0d | 1d | -- |
| | **Bucket 3 Subtotal** | **9.5d** | **0d** | **9.5d** | |
| | **Refactoring (extract shared computers)** | | **3d** | **3d** | |
| | **Infrastructure (K8s Ollama, prompts, config)** | | **2d** | **2d** | |
| | | | | | |
| | **GRAND TOTAL** | **28.5d** | **17.25d** | **~46d** | |

### 12.2 Effort Summary by Bucket

| Bucket | Items | Base Effort | GenAI Effort | Combined | New Adapters |
|--------|-------|-------------|-------------|----------|-------------|
| 1: GenAI adds real value | 8 | 9 days | 10 days | 19 days | 3 new + 2 modified |
| 2: Narrative overlay | 6 | 10 days | 2.25 days | 12.25 days | 0 (inline calls) |
| 3: Rule-based only | 8 | 9.5 days | 0 days | 9.5 days | 0 |
| Refactoring | -- | -- | 3 days | 3 days | -- |
| Infrastructure | -- | -- | 2 days | 2 days | -- |
| **Total** | **22** | **28.5 days** | **17.25 days** | **~46 days** | **3 new** |

### 12.3 Week-by-Week Implementation Plan

```
Phase A: Foundation & Refactoring (Week 1)
├── Day 1-2:  Extract shared computation components
│             ├── StatisticalForecastComputer (from StatisticalForecastEngine)
│             ├── EventStreamComputer (from EventStreamAnalyzer)
│             └── ConstraintKnapsackSolver (from ConstraintBatchOptimizer)
├── Day 3:    Shadow mode framework (generic, all 5 ports)
├── Day 4:    K8s Ollama deployment + prompt template externalization
└── Day 5:    Infrastructure: Resilience4j config for 3 new circuit breakers

Phase B: Bucket 3 — Rule-Based Only (Week 2)
├── Day 1:    Configurable anomaly thresholds (4.1) + optimizer weights (5.5)
├── Day 2-3:  ShedLock distributed scheduler locks (5.1) + graceful shutdown (5.2)
├── Day 4:    Data retention (5.3) + health indicators (5.4)
└── Day 5:    Forecast auto-tuning (23) + error frequency ranking (24)

Phase C: Bucket 1 — New Anomaly Rules + GenAI Enrichment (Week 3)
├── Day 1:    Dormancy break rule in RuleBasedAnomalyScorer (4.2)
├── Day 2:    Cross-insurer duplication rule (4.3)
├── Day 3:    Multi-signal correlation (4.5) + prompt tuning for new rules
├── Day 4-5:  Insurer-specific error patterns (4.4) — OllamaErrorResolver prompt update

Phase D: Bucket 1 — New Ollama Adapters (Week 4)
├── Day 1-2:  OllamaAugmentedForecastEngine (6.2) — adapter + prompt + tests
├── Day 2-3:  OllamaAugmentedProcessMiner (6.4) — adapter + prompt + tests
├── Day 4-5:  OllamaAugmentedBatchOptimizer (6.5) — adapter + prompt + tests

Phase E: Bucket 2 — Feedback Loops + Narrative Overlays (Week 5)
├── Day 1:    Anomaly false positive tracking (3.1)
├── Day 2:    Forecast accuracy backtesting (3.2)
├── Day 3:    Error resolution success tracking (3.3)
├── Day 4:    STP rate trending (3.4) + process variant detection (5.6)
└── Day 5:    Per-rule anomaly metrics Grafana dashboard (22)

Phase F: Sync/Async Architecture + Integration Testing (Week 6)
├── Day 1:    Port interface extension (analyzeEndorsementFast)
├── Day 2:    Kafka async enrichment consumer
├── Day 3-4:  Integration testing (all adapters with Ollama Testcontainer)
└── Day 5:    Shadow mode validation run + documentation
```

**Total**: ~6 weeks (30 working days), parallelizable to ~4 weeks with 2 developers.

---

## 13. Testing Strategy

### 13.1 Unit Testing with Mock ChatClient

Every Ollama adapter test mocks `ChatClient.Builder` to return deterministic responses:

```java
@ExtendWith(MockitoExtension.class)
class OllamaAugmentedForecastEngineTest {

    @Mock private ChatClient.Builder chatClientBuilder;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.CallPromptResponseSpec responseSpec;
    @InjectMocks private OllamaAugmentedForecastEngine engine;

    @Test
    void generateForecast_ollamaAvailable_shouldReturnEnrichedNarrative() {
        // Arrange
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(/* ... */);
        when(responseSpec.content()).thenReturn("Enriched forecast narrative...");

        // Act
        ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        // Assert
        assertThat(result.narrative()).contains("Enriched forecast narrative");
    }

    @Test
    void generateForecast_ollamaDown_shouldFallbackToRuleBasedNarrative() {
        // Arrange
        when(chatClientBuilder.build()).thenThrow(new RuntimeException("Ollama unavailable"));

        // Act
        ForecastResult result = engine.generateForecast(employerId, insurerId, history);

        // Assert — fallback to rule-based narrative
        assertThat(result.narrative()).contains("Based on 90-day trends");
    }
}
```

### 13.2 Integration Testing with Ollama Testcontainer

```java
@Testcontainers
@SpringBootTest(properties = "endorsement.intelligence.ollama.enabled=true")
class OllamaIntegrationTest {

    @Container
    static GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama:latest")
            .withExposedPorts(11434)
            .waitingFor(Wait.forHttp("/api/tags").forStatusCode(200));

    @DynamicPropertySource
    static void ollamaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.ollama.base-url",
                () -> "http://localhost:" + ollama.getMappedPort(11434));
    }

    // Integration tests that verify end-to-end LLM calls
}
```

### 13.3 Shadow Mode Validation

Run shadow mode for 7 days in staging, then analyze:

```sql
-- Compare rule-based vs. Ollama explanations
SELECT
    anomaly_type,
    COUNT(*) as total,
    AVG(LENGTH(rule_explanation)) as avg_rule_length,
    AVG(LENGTH(ollama_explanation)) as avg_ollama_length,
    -- Manual review: sample 10% for quality scoring
FROM shadow_mode_comparisons
WHERE created_at > NOW() - INTERVAL '7 days'
GROUP BY anomaly_type;
```

### 13.4 Test Count Estimate

| Category | New Tests | Description |
|----------|-----------|-------------|
| Unit (Ollama adapters) | 18 | 6 per new adapter (happy path, fallback, circuit breaker, prompt format, parse error, empty response) |
| Unit (new anomaly rules) | 12 | 4 per new rule (dormancy, cross-insurer, correlation) |
| Unit (feedback loops) | 16 | 4 per feedback enhancement (precision tracking, backtesting, success tracking, STP trending) |
| Unit (infrastructure) | 12 | ShedLock, graceful shutdown, data retention, health indicators |
| API integration | 8 | Ollama-augmented endpoints (with Testcontainer) |
| BDD | 6 | Ollama enrichment scenarios (enabled vs. disabled vs. fallback) |
| Shadow mode validation | 4 | Parallel execution, comparison logging, metrics recording |
| **Total** | **~76** | |

---

## 14. Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| Ollama latency spikes under load | MEDIUM | LOW | Circuit breaker opens → rule-based fallback. Scheduled paths are latency-tolerant. |
| LLM hallucination in explanations | MEDIUM | MEDIUM | Low temperature (0.3), structured prompts, domain persona grounding. Explanations are advisory — scoring is always rule-based. |
| LLM generates inconsistent JSON (error resolver) | LOW | MEDIUM | JSON parse error → fallback to `SimulatedErrorResolver`. Already implemented in existing `OllamaErrorResolver`. |
| Ollama model too large for K8s memory limits | LOW | HIGH | Use 3B model (4GB RAM). Monitor pod OOMKills. Fallback: switch to cloud-hosted LLM. |
| Prompt drift after model upgrade | MEDIUM | LOW | Version-pin Ollama model in config. Test prompts against new models in shadow mode before switching. |
| GenAI augmentation delays critical scheduler runs | LOW | HIGH | LLM calls have per-call timeout (5s). Total scheduler budget enforced via `@SchedulerLock(lockAtMostFor)`. Fallback ensures scheduler always completes. |
| Regulatory concern about AI-generated explanations | LOW | HIGH | All scoring is deterministic (rule-based). LLM only generates explanations, never makes decisions. Audit trail shows both rule score and LLM narrative. |

---

## 15. Decision Log

| # | Decision | Rationale | Alternatives Considered |
|---|----------|-----------|----------------------|
| 1 | **Ollama (local) as default LLM** | Zero external dependencies, no API keys, data stays on-premises, free | Cloud LLMs (faster, higher quality) — available as drop-in replacement via Spring AI config |
| 2 | **Rule-based scoring + LLM explanation** (not LLM scoring) | Deterministic, auditable scores. LLM adds narrative value, not decision-making authority. | LLM-only scoring — rejected due to non-determinism, latency, and auditability concerns |
| 3 | **`@ConditionalOnProperty` adapter switching** | Clean separation, zero runtime overhead when disabled, Spring Boot idiom | Runtime feature flag (LaunchDarkly) — overkill for infrastructure toggle |
| 4 | **Extract shared computation components** (Scorer, Computer, Solver) | Avoids code duplication between rule-based and Ollama adapters | Copy-paste computation into Ollama adapters — rejected for DRY violation |
| 5 | **Sync fast path + async enrichment** for real-time APIs | Preserves <200ms API SLA while still delivering enriched explanations | Synchronous LLM in API path — rejected (1-3s latency unacceptable) |
| 6 | **Shadow mode before full switch** | De-risks GenAI adoption. Validates quality before any business logic depends on LLM output. | Direct switch — rejected (no quality baseline) |
| 7 | **8 of 22 items stay rule-based** | Infrastructure, configuration, and pure computation do not benefit from LLM. Adding GenAI would be over-engineering. | Augment everything — rejected (adds latency and complexity with no value) |
| 8 | **3B model (llama3.2)** over larger models | Fits in 4GB RAM, <1s on GPU, acceptable quality for analytical prompts | 8B model — higher quality but 2x memory; recommended only for forecast narratives if 3B quality is insufficient |
