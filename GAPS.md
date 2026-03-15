# Gap Analysis: Current System vs Design Challenge Requirements

**Date:** March 13, 2026
**System Version:** 0.3.0-SNAPSHOT (Phase 3 Complete + HIGH + MEDIUM Gap Fixes)
**Total Tests:** ~673+ (368 unit + 102 API + 74 BDD + 112 E2E + 6 perf) — 0 failures
**HIGH Gaps Remaining:** 0 of 4 (all fixed)
**MEDIUM Gaps Remaining:** 0 of 9 (all fixed/acknowledged)
**LOW Gaps Remaining:** 0 of 7 (all fixed/acknowledged)

---

## Design Challenge Requirements (from PDF)

The Architect Design Problem PDF specifies 6 functional requirements, 4 assumptions, and 6 deliverables. This document maps every gap between the current implementation and those requirements.

---

## Requirement 1: Real-Time and Batch Execution

> *"Executes endorsements in either real-time or batch, as defined by the insurer."*

### What's Implemented

- `InsurerPort` interface with `submitRealTime()`, `submitBatch()`, and capability detection via `InsurerCapabilities`
- 4 insurer adapters: Mock (RT+Batch), ICICI Lombard (RT-only), Niva Bupa (Batch-only), Bajaj Allianz (RT+Batch)
- `InsurerRouter` factory pattern for runtime adapter resolution
- `BatchAssemblyScheduler` groups queued endorsements by insurer, respects `maxBatchSize`, uses per-insurer `batchSlaHours` for SLA deadline
- `BatchStatusPollerScheduler` polls insurer status every 60s, handles confirmations/rejections, detects SLA breaches
- Resilience4j circuit breakers and retry policies per insurer

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 1.1 | ~~**No "one batch at a time per insurer" enforcement**~~ | ~~**HIGH**~~ **FIXED** | **FIXED:** Added guard at the top of `assembleBatchForInsurer()` (line 78) that calls `batchRepository.existsByInsurerIdAndStatusIn(insurerId, [ASSEMBLING, SUBMITTED, PROCESSING, PARTIAL_COMPLETE])`. If any active batch exists, skips that insurer with a warning log and increments `endorsement.batch.skipped.active` metric. 2 new unit tests: `SkipsInsurerWithActiveBatch`, `ProceedsWhenPreviousBatchComplete`. |
| 1.2 | ~~**Batch progress not visible to employers**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Added `GET /api/v1/endorsements/employers/{employerId}/batches` endpoint with paginated `BatchProgressResponse` DTO (batchId, insurerId, status, endorsementCount, totalPremium, submittedAt, slaDeadline, insurerBatchRef). Added `findByEmployerId` to `BatchRepository` port, `JpaBatchRepositoryAdapter`, and `SpringDataBatchRepository` (JPQL join through endorsements table). `EndorsementQueryHandler.findBatchesByEmployerId()` delegates to paginated query. |

---

## Requirement 2: No Loss of Coverage

> *"Ensures employees receive uninterrupted coverage from the moment they are eligible — no gaps in medical cover."*

### What's Implemented

- Provisional coverage granted immediately on ADD endorsement creation (`CreateEndorsementHandler:92-101`)
- 11-state endorsement machine with `ProvisionalCoverage` domain model (`isActive()`, `confirm()`, `expire()`)
- 5 coverage gaps identified and **all 5 fixed** in code with 10 new unit tests:
  - **Gap 1 (FIXED):** `handleRejection()` expires coverage on FAILED_PERMANENT, publishes `ProvisionalCoverageExpired` event, calls `notifyCoverageAtRisk()` (`ProcessEndorsementHandler:229-266`)
  - **Gap 2 (FIXED):** `ProvisionalCoverageCleanupScheduler` checks endorsement status via `isTerminal()` before expiring — skips active endorsements
  - **Gap 3 (FIXED):** `ProvisionalCoverageExpired` event published from cleanup scheduler + `notifyCoverageExpired()` notification sent
  - **Gap 4 (FIXED):** `StuckEndorsementRetryScheduler` polls every 5 min and resubmits RETRY_PENDING endorsements
  - **Gap 5 (FIXED):** `handleConfirmation()` publishes `ProvisionalCoverageConfirmed` event when provisional coverage exists

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 2.1 | ~~**No proactive "coverage expiring soon" warning**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Added `warnExpiringCoverages()` method to `ProvisionalCoverageCleanupScheduler` that runs before expiry logic. Queries `findActiveExpiringBefore(warningCutoff, staleCutoff)` for coverages within the warning window (configurable `endorsement.provisional-coverage.warning-days-before-expiry: 2`). Calls `notificationPort.notifyCoverageAtRisk()` for each with days-remaining message. Added `findActiveExpiringBefore` to `ProvisionalCoverageRepository` port + `JpaProvisionalCoverageRepositoryAdapter` + `SpringDataProvisionalCoverageRepository`. 2 new unit tests: `WarnsAboutExpiringCoverages`, `DoesNotWarnForFreshCoverages`. Increments `endorsement.coverage.warning` metric. |
| 2.2 | ~~**Insufficient EA balance doesn't block endorsement creation**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** Made blocking configurable via `@Value("${endorsement.ea.block-on-insufficient-balance:false}")`. Default `false` preserves coverage-first behavior. When set to `true`, throws `InsufficientBalanceException` on insufficient balance. Explicit constructor injection in `CreateEndorsementHandler`. 1 new unit test: `InsufficientBalanceBlocking_ShouldThrow`. |

---

## Requirement 3: EA Balance Optimization

> *"Optimizes endorsement processing so employers can maintain a minimum required balance in their endorsement account."*

### What's Implemented

- `EAAccount` domain model with `reserve()` / `debit()` / `credit()` lifecycle and `availableBalance()` calculation
- `EABalanceCalculator.sequenceForOptimalBalance()` — processes deletions before additions to free balance first
- `ConstraintBatchOptimizer` — composite scoring (60% urgency / 40% EA impact), **0-1 knapsack DP** (deletions first, then DP-optimal subset of additions within balance constraint)
- `StatisticalForecastEngine` — 30-day forecast with **dual-layer seasonality** (7 day-of-week factors + 12 monthly factors calibrated to Indian business cycles like April hiring waves and October appraisal cycles)
- `BalanceForecastService` — shortfall detection with `notifyInsufficientBalance()` called on shortfall, `BalanceForecastAlert` event published

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 3.1 | ~~**Batch optimizer uses greedy algorithm, not mathematical optimization**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** Upgraded `ConstraintBatchOptimizer` from greedy to 0-1 knapsack DP. Deletions are still processed first (always beneficial), then `solveKnapsack()` finds the mathematically optimal subset of additions/updates that maximizes total composite score within the available balance constraint. DP table uses integer pennies to avoid floating-point precision issues, capped at 1M entries to prevent memory explosion. Backtracking reconstructs the optimal set. 1 new unit test: `DPKnapsack_PicksOptimalSubset`. |
| 3.2 | ~~**Safety margin is hardcoded at 10%**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** Extracted `SAFETY_MARGIN` constant to constructor-injected `@Value("${endorsement.ea.safety-margin-pct:0.10}")` in `EABalanceCalculator`. Default 10% preserved. Configurable in `application.yml` under `endorsement.ea.safety-margin-pct`. Updated all test constructors. |
| 3.3 | ~~**Delete credit timing not factored into forecast**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** `StatisticalForecastEngine` now accepts `@Value("${endorsement.ea.credit-delay-days:30}")`. DELETE endorsements tracked separately — credits only count for forecast days beyond `creditDelayDays`. Net forecast = gross burn - delayed credits. Narrative includes credit impact. 1 new unit test: `FactorsInCreditDelay` verifying short-delay < long-delay forecasted need. |

---

## Requirement 4: Real-Time Visibility

> *"Provides real-time visibility to stakeholders about endorsement execution status, including account balance, outstanding items, and errors."*

### What's Implemented

- React frontend with 8 pages: Dashboard, Endorsement List (filterable/paginated), Endorsement Detail (status timeline), Create Endorsement, EA Account Lookup, EA Balance Optimization, Multi-Insurer View, Intelligence Dashboard (4 tabs)
- 27 REST API endpoints across 5 controllers with filtering, pagination, and RFC 7807 error responses
- 7 Grafana dashboards (application overview, endorsement business, infrastructure health, scheduler monitoring, multi-insurer, reconciliation, intelligence)
- ELK stack (Elasticsearch + Logstash + Kibana) for centralized structured logging
- Jaeger for distributed tracing with `endorsementId` and `employerId` baggage propagation
- Swagger/OpenAPI documentation at `/swagger-ui`
- Prometheus metrics with percentile histograms

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 4.1 | ~~**No WebSocket or Server-Sent Events (SSE) for push-based updates**~~ | ~~**HIGH**~~ **FIXED** | **FIXED:** Implemented STOMP/SockJS WebSocket infrastructure: `WebSocketConfig` (`/ws` endpoint, `/topic` broker), `WebSocketEventBroadcaster` (broadcasts all domain events to `/topic/employer/{employerId}` with event type, endorsement ID, employer ID, and timestamp). Integrated into `KafkaEventPublisher` as optional dependency — after every Kafka publish, event is broadcast to WebSocket subscribers. `SecurityConfig` updated to permit `/ws/**`. 3 new unit tests for broadcaster (employer topic routing, confirmed event, null employerId graceful skip). |
| 4.2 | ~~**No dedicated "outstanding items" endpoint**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Added `GET /api/v1/endorsements/employers/{employerId}/outstanding` endpoint returning paginated `EndorsementResponse` for all non-terminal statuses (CREATED, VALIDATED, PROVISIONALLY_COVERED, SUBMITTED_TO_INSURER, INSURER_PROCESSING, QUEUED_FOR_BATCH, RETRY_PENDING). `EndorsementQueryHandler.findOutstandingByEmployerId()` delegates to existing `findByEmployerIdAndStatusIn` repository method. |
| 4.3 | ~~**Notifications are logging-only**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Created `WebhookNotificationAdapter` implementing all 10 `NotificationPort` methods via HTTP POST to configurable `endorsement.notifications.webhook.url`. Each method annotated with `@CircuitBreaker(name = "webhookNotification")` and `@Retry(name = "webhookNotification")` with fallback logging. Activated via `@ConditionalOnProperty(endorsement.notifications.webhook.enabled=true)`. `LoggingNotificationAdapter` becomes fallback via `@ConditionalOnMissingBean`. Resilience4j circuit breaker + retry configs added to `application.yml`. 4 unit tests. |
| 4.4 | ~~**Two notification methods defined but never called from service code**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Added `notificationPort.notifyAnomalyDetected(employerId, anomalyType, score, explanation)` in `AnomalyDetectionService.analyzeEndorsement()` inside the `if (result.isFlagged())` block. Added `notificationPort.notifyForecastShortfall(employerId, shortfall, daysAhead)` in `BalanceForecastService.generateForecast()` after `notifyInsufficientBalance()`. Both verified in existing tests with `verify(notificationPort).notifyAnomalyDetected(...)` and `verify(notificationPort).notifyForecastShortfall(...)`. |

---

## Requirement 5: AI/Automation

> *"Utilizes AI/automation tools wherever appropriate (e.g., process optimization, anomaly detection, reconciliation, prediction)."*

### What's Implemented

- 5 intelligence pillars, all behind port/adapter interfaces for swappable ML backends:
  - **Anomaly Detection**: `RuleBasedAnomalyDetector` — 4 rules (volume spike, add/delete cycling, suspicious timing, unusual premium) with configurable thresholds
  - **Balance Forecasting**: `StatisticalForecastEngine` — 90-day lookback, day-of-week factors (Mon 1.2x → Sun 0.2x), monthly seasonality (Apr 1.4x, Oct 1.3x), configurable credit delay for deletions, confidence scoring
  - **Error Resolution**: `SimulatedErrorResolver` — 5 error patterns (member ID, date format, missing field, premium mismatch, unknown) with auto-apply at >= 95% confidence
  - **Process Mining**: `EventStreamAnalyzer` — STP rate calculation, bottleneck detection (p95 > 2x avg OR avg > 4 hours), happy path percentage
  - **Batch Optimization**: `ConstraintBatchOptimizer` — 0-1 knapsack DP with composite scoring (60% urgency / 40% EA impact), priority-aware sequencing (P0 DELETE → P3 PREMIUM_UPDATE)
- Automated reconciliation via `ReconciliationEngine` with `ReconciliationScheduler` (every 15 min)
- 13 intelligence REST API endpoints
- All 5 intelligence pillars have @ConditionalOnProperty feature flags

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 5.1 | **All intelligence is rule-based, not ML** | **MEDIUM** — **ACKNOWLEDGED** | **ACKNOWLEDGED: Intentional design decision.** Rule-based intelligence is the correct approach for MVP. The `AI_Automation_Approach.md` deliverable documents the production upgrade path: Spring AI for anomaly detection (Isolation Forest), LangChain4j for error resolution (RAG pipeline), PM4Py for process mining, OR-Tools for batch optimization. The port/adapter pattern ensures ML upgrades are adapter swaps, not rewrites. **Interview framing**: "Rule-based first to establish baselines and contracts, ML when we have training data." |
| 5.2 | ~~**No feedback loop from anomaly review decisions**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** `AnomalyDetectionService.reviewAnomaly()` now computes false positive rate per anomaly type on terminal review decisions. Records `endorsement.anomaly.review` counter (outcome: false_positive/true_positive) and `endorsement.anomaly.false_positive_rate.{type}` gauge. Added `countByAnomalyTypeAndStatus()` and `countByAnomalyType()` to `AnomalyDetectionRepository` port + JPA adapter + Spring Data repo. 2 new unit tests: `Dismissed_RecordsFalsePositiveRate`, `ConfirmedFraud_RecordsTruePositive`. |
| 5.3 | ~~**Reconciliation doesn't verify against insurer's records**~~ | ~~**HIGH**~~ **FIXED** | **FIXED:** `ReconciliationEngine` now injects `BatchRepository` and verifies batch endorsements against insurer records. For endorsements with a `batchId`, looks up the batch's `insurerBatchRef`, calls `insurerPort.checkBatchStatus()`, and cross-references per-endorsement results: confirmed → MATCH + `handleConfirmation()`, rejected → INSURER_REJECTED + `handleRejection()`, not found → PARTIAL_MATCH + notification. Graceful fallback to local-state matching when `checkBatchStatus()` throws. Added `ReconciliationOutcome.INSURER_REJECTED` enum value. 5 new unit tests: `ConfirmedByInsurer`, `RejectedByInsurer`, `NotFoundInInsurerResults`, `CheckBatchStatusFails_FallsBack`, updated `BatchMatch_Confirmed`. |
| 5.4 | ~~**Simulated processing latency in intelligence adapters**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** Removed `Thread.sleep()` calls from `StatisticalForecastEngine` (100ms), `ConstraintBatchOptimizer` (75ms), and `RuleBasedAnomalyDetector` (50ms). Intelligence analysis is now purely computational with no artificial delays. |

---

## Requirement 6: Scalability

> *"Is architected for scalability (100K employers, average 10 employee changes per employer per day, 10 different insurance providers)."*

**Target throughput**: 100K employers × 10 changes/day = **1M endorsements/day** ≈ 11.5 endorsements/sec (steady state, peaks ~5x)

### What's Implemented

- Java 21 virtual threads enabled (`spring.threads.virtual.enabled: true`)
- Kafka KRaft mode with **88 partitions** across 4 topics (32 events + 32 commands + 8 notifications + 16 reconciliation)
- `employerId` as Kafka partition key for ordering guarantees per employer
- Redis distributed caching (60s TTL) with `@Cacheable` on `InsurerRegistry`
- Resilience4j circuit breakers per insurer (configurable sliding window, failure rate threshold, wait duration)
- PostgreSQL with 6 indexes on the endorsements table
- Kubernetes manifests for container orchestration (`k8s/` directory with namespace, deployments, services, configmaps, secrets, PVCs)
- Docker Compose with health checks for all 9 infrastructure services
- Idempotency keys on endorsement creation to prevent duplicates
- MDC-based structured logging with correlation IDs

### Gaps

| # | Gap | Severity | How to Address |
|---|-----|----------|----------------|
| 6.1 | ~~**No explicit HikariCP connection pool configuration**~~ | ~~**HIGH**~~ **FIXED** | **FIXED:** Added explicit HikariCP config to `application.yml`: `maximum-pool-size: 30`, `minimum-idle: 5`, `connection-timeout: 10000`, `idle-timeout: 300000`, `max-lifetime: 1680000`, `leak-detection-threshold: 60000`. Added Hibernate batch settings: `jdbc.batch_size: 25`, `jdbc.fetch_size: 50`, `order_inserts: true`, `order_updates: true`. `application-railway.yml` updated with production values (`maximum-pool-size: 20`, no leak detection). |
| 6.2 | ~~**No HorizontalPodAutoscaler (HPA) or PodDisruptionBudget (PDB)**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Created `k8s/backend/hpa.yaml` — `HorizontalPodAutoscaler` targeting `backend` deployment with `minReplicas: 2`, `maxReplicas: 8`, CPU target 70%, memory target 80%, scale-up stabilization 60s (max 2 pods/60s), scale-down stabilization 300s (max 1 pod/120s). Created `k8s/backend/pdb.yaml` — `PodDisruptionBudget` with `minAvailable: 1`. Both auto-applied by existing `kubectl apply -f k8s/backend/` in `k8s-start.sh`. |
| 6.3 | ~~**No data retention or archival strategy**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Created `V15__create_archive_tables.sql` migration (creates `endorsements_archive` and `endorsement_events_archive` tables). Created `DataRetentionScheduler` with `@Scheduled(cron = endorsement.retention.archive-cron)` running weekly. Moves terminal endorsements (CONFIRMED, REJECTED, CANCELLED) older than `endorsement.retention.days: 365` to archive tables using batch INSERT INTO...SELECT + DELETE pattern. Events archived before endorsements to respect FK constraints. Records `endorsement.archive.count` metrics by type. Configurable `batch-size: 1000`. 3 unit tests: archives terminal, skips delete when nothing to archive, records metrics. |
| 6.4 | ~~**No rate limiting on API ingress**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Created `RateLimitingFilter` (extends `OncePerRequestFilter`) implementing token-bucket algorithm per client IP. Configurable via `endorsement.rate-limit.requests-per-second: 50` and `endorsement.rate-limit.burst-size: 100`. Returns 429 Too Many Requests with `Retry-After: 1` header when exhausted. Activated via `@ConditionalOnProperty(endorsement.rate-limit.enabled=true)`. Supports `X-Forwarded-For` header for proxied requests. 4 unit tests: within-limit passes, exceeding-limit rejects, tokens replenish, max burst cap. |
| 6.5 | ~~**Caching is heap-only, not distributed**~~ | ~~**LOW**~~ **FIXED** | **FIXED:** Switched `application.yml` from `spring.cache.type: caffeine` to `spring.cache.type: redis` with `time-to-live: 60s`, `key-prefix: "endorsement:"`, `use-key-prefix: true`. Test profile retains Caffeine for unit tests (no Redis required). Redis was already provisioned in Docker Compose and K8s. All instances now share a single distributed cache. |
| 6.6 | ~~**No optimistic locking on EA Account for concurrent updates**~~ | ~~**MEDIUM**~~ **FIXED** | **FIXED:** Added `private Long version` field to `EAAccount` domain model. Added `@Version private Long version` to `EAAccountEntity` JPA entity. Created `V14__add_ea_account_version.sql` Flyway migration (`ALTER TABLE ea_accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0`). Updated `EndorsementMapper` to map version field in both `toDomain` and `toEntity` for EAAccount. JPA optimistic locking now throws `OptimisticLockException` on concurrent updates. 1 new unit test: `version_settableAndGettableViaBuilder`. |

---

## Assumption Gaps

The PDF lists 4 assumptions. Compliance status:

| # | PDF Assumption | Status | Gap Reference |
|---|---------------|--------|---------------|
| A1 | *"Insurer provides batch and real-time APIs for endorsement processing"* | **Fully Implemented** | 4 adapters with `InsurerCapabilities` detection, `InsurerRouter` factory |
| A2 | *"Insurer can process one batch at a time, will have varying SLAs for batch (few hours to few days)"* | **Fully Implemented** | SLA deadlines set per insurer. One-batch-at-a-time constraint enforced via `existsByInsurerIdAndStatusIn()` guard in `BatchAssemblyScheduler` (Gap 1.1 FIXED). |
| A3 | *"Endorsement failures must be handled with retries and clear error communication"* | **Fully Implemented** | Resilience4j retry (3 attempts, exponential backoff), `ErrorResolutionService` auto-resolve, `StuckEndorsementRetryScheduler` for RETRY_PENDING, `notifyEndorsementRejected()` for clear error communication |
| A4 | *"Real-time insight, notifications, and automated reconciliation are required"* | **Fully Implemented** | Reconciliation verifies against insurer records (Gap 5.3 FIXED). WebSocket push-based real-time updates (Gap 4.1 FIXED). `WebhookNotificationAdapter` for production webhook delivery (Gap 4.3 FIXED). All notification methods wired (Gap 4.4 FIXED). |

---

## Deliverable Status

| # | PDF Deliverable | Status | Location |
|---|----------------|--------|----------|
| 1 | High-level architecture and system components (illustrated) | **DONE** | `docs/deliverables/High_Level_Architecture.md` (61 KB) |
| 2 | Approach for ensuring no loss of coverage at any stage | **DONE** | `docs/deliverables/No_Loss_of_Coverage_Approach.md` (39 KB) — all 5 coverage gaps fixed in code |
| 3 | Algorithm or approach for minimizing EA balance requirements | **DONE** | `docs/deliverables/EA_Balance_Minimization_Algorithm.md` (35 KB) |
| 4 | User flows or example screens/dashboards for real-time visibility | **DONE** | `docs/deliverables/Real_Time_Visibility_User_Flows.md` (94 KB) |
| 5 | How AI/automation is leveraged (describe or demo) | **DONE** | `docs/deliverables/AI_Automation_Approach.md` (58 KB) |
| 6 | Code/prototype (demo if time allows) | **DONE** | 673+ tests passing, `./start.sh` one-click demo |

---

## Summary: All Gaps by Severity

### HIGH (0 remaining — all 4 fixed)

| # | Gap | Status | What Was Done |
|---|-----|--------|---------------|
| 1.1 | One-batch-at-a-time per insurer not enforced | **FIXED** | Guard in `BatchAssemblyScheduler` + `endorsement.batch.skipped.active` metric + 2 tests |
| 4.1 | No WebSocket/SSE for real-time push updates | **FIXED** | `WebSocketConfig` + `WebSocketEventBroadcaster` + `KafkaEventPublisher` integration + 3 tests |
| 5.3 | Reconciliation doesn't verify against insurer records | **FIXED** | `ReconciliationEngine` calls `checkBatchStatus()`, cross-references, graceful fallback + `INSURER_REJECTED` outcome + 5 tests |
| 6.1 | No explicit HikariCP connection pool configuration | **FIXED** | HikariCP pool (30 dev / 20 prod) + Hibernate batching (batch_size=25, fetch_size=50, order_inserts/updates) |

### MEDIUM (0 remaining — all 9 fixed/acknowledged)

| # | Gap | Status | What Was Done |
|---|-----|--------|---------------|
| 1.2 | Batch progress not visible to employers | **FIXED** | `GET /api/v1/endorsements/employers/{id}/batches` endpoint + `BatchProgressResponse` DTO + `findByEmployerId` in BatchRepository |
| 2.1 | No proactive coverage-expiring-soon warning | **FIXED** | `warnExpiringCoverages()` in scheduler + `findActiveExpiringBefore` repo method + configurable warning days + 2 tests |
| 4.2 | No dedicated outstanding items endpoint | **FIXED** | `GET /api/v1/endorsements/employers/{id}/outstanding` endpoint + `findOutstandingByEmployerId` query handler |
| 4.3 | Notifications are logging-only | **FIXED** | `WebhookNotificationAdapter` with circuit breaker + retry + conditional activation + 4 tests |
| 4.4 | Two notification methods never called | **FIXED** | Wired `notifyAnomalyDetected()` in AnomalyDetectionService + `notifyForecastShortfall()` in BalanceForecastService |
| 5.1 | All AI/intelligence is rule-based, not ML | **ACKNOWLEDGED** | Intentional design decision — rule-based first, ML via port/adapter swap |
| 6.2 | No HPA/PDB in Kubernetes manifests | **FIXED** | `hpa.yaml` (min 2, max 8, CPU 70%, mem 80%) + `pdb.yaml` (minAvailable: 1) |
| 6.3 | No data retention/archival strategy | **FIXED** | Archive tables migration + `DataRetentionScheduler` (weekly, 365-day retention) + 3 tests |
| 6.4 | No rate limiting on API ingress | **FIXED** | `RateLimitingFilter` token-bucket per IP + 429 responses + configurable limits + 4 tests |
| 6.6 | No optimistic locking on EA Account | **FIXED** | `@Version` on EAAccountEntity + Flyway V14 migration + mapper updates + 1 test |

### LOW (0 remaining — all 7 fixed/acknowledged)

| # | Gap | Status | What Was Done |
|---|-----|--------|---------------|
| 2.2 | Insufficient balance doesn't block creation | **FIXED** | Configurable `block-on-insufficient-balance` flag + `InsufficientBalanceException` + 1 test |
| 3.1 | Batch optimizer is greedy, not optimal | **FIXED** | 0-1 knapsack DP in `ConstraintBatchOptimizer.solveKnapsack()` + 1 test |
| 3.2 | Safety margin hardcoded at 10% | **FIXED** | `@Value("${endorsement.ea.safety-margin-pct:0.10}")` in `EABalanceCalculator` |
| 3.3 | Delete credit timing not factored into forecast | **FIXED** | `creditDelayDays` parameter in `StatisticalForecastEngine` + delayed credit calculation + 1 test |
| 5.2 | No feedback loop from anomaly reviews | **FIXED** | False positive rate gauge per anomaly type + review outcome counter + 2 tests |
| 5.4 | Thread.sleep() in intelligence adapters | **FIXED** | Removed from 3 intelligence adapters |
| 6.5 | Caching is heap-only, not distributed | **FIXED** | Switched to Redis cache in `application.yml`, Caffeine retained for test profile |

---

## Interview Talking Points

### Overall Posture

- **673+ automated tests** (368 unit + 102 API + 74 BDD + 112 E2E + 6 perf) — **0 failures, 100% pass rate**
- **0 HIGH gaps, 0 MEDIUM gaps, 0 LOW gaps remaining** — all 20 gaps fixed or acknowledged
- **All 6 design challenge deliverables complete** with standalone documents in `docs/deliverables/`
- One-click demo: `./start.sh` (Docker Compose + backend + frontend), full Allure report via `./run-all-tests.sh`

### Key Architecture Decisions

| Decision | Rationale | Talking Point |
|----------|-----------|---------------|
| **Hexagonal / Ports & Adapters** | Domain has zero infrastructure imports. 18 port interfaces, all implemented by infrastructure adapters. | "Adding a new insurer like Digit or Star Health is a new adapter class + one DB row. Zero handler changes, zero domain changes. The `InsurerRouter` auto-discovers it." |
| **Strategy pattern for insurer diversity** | 4 adapters (REST/JSON, CSV/SFTP, SOAP/XML, Mock) with different protocols, capabilities, and SLA profiles. | "ICICI Lombard is REST/JSON real-time only. Niva Bupa is CSV/SFTP batch-only. Bajaj Allianz is SOAP/XML with both. Each gets its own circuit breaker tuned to its latency profile." |
| **11-state lifecycle with state machine** | Transitions validated at domain level. `canTransitionTo()` prevents invalid state changes. | "The state machine is in the domain model, not scattered across handlers. Every transition is validated — you can't go from CREATED to CONFIRMED without going through VALIDATED first." |
| **CQRS** | Separate command handlers (mutate + publish events) from query handler (read-only, @Transactional(readOnly=true)). | "Query handlers can route to read replicas. Command handlers own the write path and event publishing. They never mix." |
| **Event-carried state transfer** | 24 Kafka event types as sealed Java records. Each event carries all data consumers need. | "Consumers never query back to the source. A `Confirmed` event carries the `insurerReference`. A `Rejected` event carries the `reason`. The sealed interface gives compile-time exhaustiveness." |
| **Provisional coverage (no-loss guarantee)** | Coverage granted immediately at creation, before insurer confirms. 5 edge cases identified and all fixed. | "Coverage is granted optimistically. If the insurer rejects, coverage is expired with notification. If an endorsement gets stuck, the retry scheduler resubmits it. The employee is never uncovered." |
| **AI/intelligence behind port interfaces** | 5 pillars (anomaly, forecast, error resolution, process mining, batch optimization) — rule-based now, ML-swappable. | "Rule-based first to establish contracts and baselines. Spring AI for anomaly detection, LangChain4j for error resolution — it's an adapter swap, not a rewrite." |

### Gap-Specific Talking Points

#### Completed Gaps (proof of thoroughness)

| Gap | What Was Done | Talking Point |
|-----|---------------|---------------|
| **1.1** One batch per insurer | Guard in `BatchAssemblyScheduler` + metric + 2 tests | "The PDF says 'insurer can process one batch at a time'. We enforce this with a DB query before assembly — if any active batch exists for that insurer, we skip and log." |
| **4.1** Real-time push updates | WebSocket (STOMP/SockJS) broadcasting all domain events | "Every Kafka event is also broadcast to WebSocket subscribers at `/topic/employer/{employerId}`. The frontend gets real-time status updates without polling." |
| **5.3** Reconciliation vs insurer | `ReconciliationEngine` calls `checkBatchStatus()`, cross-references results | "Reconciliation isn't just checking our own DB. We call the insurer's batch status API and cross-reference per-endorsement results — confirmed, rejected, or not found." |
| **6.1** Connection pool tuning | HikariCP (30 dev / 20 prod), Hibernate batching (batch_size=25) | "HikariCP with leak detection in dev, Hibernate batch inserts/updates for throughput. Production pool is smaller because Railway has connection limits." |
| **6.6** Optimistic locking | `@Version` on EAAccount + Flyway migration | "EA Account balance updates use optimistic locking. Concurrent debit/credit operations get `OptimisticLockException` instead of lost updates." |
| **6.4** Rate limiting | Token-bucket per IP, configurable, 429 with Retry-After | "Rate limiting is off by default for dev but production-ready. 50 req/sec per IP with burst to 100, returns 429 with `Retry-After` header." |
| **6.3** Data retention | Archive tables + weekly scheduler, 365-day retention | "Terminal endorsements older than a year get archived. Events first (FK constraint), then endorsements. Batch size 1000 to avoid long transactions." |
| **6.2** K8s autoscaling | HPA (2-8 pods, CPU 70%, mem 80%) + PDB (minAvailable: 1) | "HPA scales from 2 to 8 pods based on CPU and memory. PDB ensures at least 1 pod survives during rolling updates." |

#### Recently Fixed LOW Gaps (additional depth)

| Gap | Talking Point |
|-----|--------------|
| **2.2** (Balance blocking now configurable) | "Default is coverage-first (non-blocking). Production operators can enable blocking via `block-on-insufficient-balance: true`. The employee is never left uncovered by default, but finance teams can opt into stricter controls." |
| **3.1** (DP knapsack optimizer) | "Upgraded from greedy to 0-1 knapsack DP. Deletions first (always beneficial), then `solveKnapsack()` finds the mathematically optimal subset of additions. Uses integer pennies for the DP table — no floating-point drift." |
| **3.3** (Delete credit delay in forecast) | "The forecast engine now factors in configurable `credit-delay-days`. Credits from deletions only count for forecast days beyond the delay period. A 30-day delay means deletion credits don't help in the first month of forecast." |
| **5.2** (Anomaly feedback loop) | "Review decisions now compute false positive rate per anomaly type. `endorsement.anomaly.false_positive_rate.volume_spike` gauge feeds into Grafana dashboards. This creates labeled training data for future ML model training." |
| **5.1** (AI is rule-based) | "Rule-based first to establish contracts and baselines. The port/adapter pattern means upgrading to ML (Spring AI for Isolation Forest anomaly detection, LangChain4j for RAG-based error resolution) is an adapter swap at deployment time." |

### Numbers to Know Cold

| Metric | Value |
|--------|-------|
| Total automated tests | 673+ (368 unit + 102 API + 74 BDD + 112 E2E + 6 perf) |
| Test pass rate | 100% |
| REST endpoints | 27 |
| Kafka event types | 24 (sealed interface) |
| Kafka partitions | 88 across 4 topics |
| Database tables | 15 (13 operational + 2 archive) |
| Flyway migrations | 15 |
| Endorsement states | 11 |
| Insurer adapters | 4 (Mock, ICICI Lombard, Niva Bupa, Bajaj Allianz) |
| Intelligence adapters | 5 |
| Port interfaces | 18 |
| Scheduled jobs | 9 |
| Grafana dashboards | 7 |
| Frontend pages | 8 |
| Gatling p50 / p95 / p99 | 8ms / 181ms / 200ms |
| Scalability target | 1M endorsements/day (100K employers x 10 changes) |
| HPA range | 2-8 pods (CPU 70%, memory 80%) |
| HikariCP pool | 30 dev / 20 prod |
| Data retention | 365 days before archive |

