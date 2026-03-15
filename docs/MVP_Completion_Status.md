# Project Completion Status

**Project:** Plum Endorsement Management System
**Date:** March 14, 2026
**Version:** 0.1.0-SNAPSHOT
**Phases Completed:** MVP + Phase 2 (Multi-Insurer & Optimization) + Phase 3 (Intelligence & AI/Automation)

---

## Executive Summary

The platform is **fully functional across 3 phases** and production-ready for demo. All core endorsement lifecycle operations work end-to-end: creation with provisional coverage, real-time and batch insurer submission, confirmation/rejection with retry logic, EA balance management, multi-insurer routing (4 adapters with per-insurer circuit breakers), automated reconciliation, and a full intelligence layer (anomaly detection with 5 rules, balance forecasting, error resolution with outcome tracking, process mining with STP rate trending, employer health scoring, insurer benchmarking). The system includes a React frontend with 10+ pages, REST API with 38 endpoints and Swagger documentation, **800+ automated tests (100% pass rate)** across 5 test layers (unit, API, BDD, E2E, performance), full observability stack (Prometheus, Grafana, Jaeger, ELK), one-click setup, and cloud deployment configuration.

**Next priority:** CI/CD pipeline (Harness or Jenkins) with ArgoCD GitOps, JaCoCo code coverage, SonarQube quality gates, and automated environment promotion (Dev → Staging → Canary → Production). See [Phase 4 — CI/CD Pipeline & GitOps](#phase-4--cicd-pipeline--gitops-next-priority) and [Next Items to Pick Up](#next-items-to-pick-up-priority-order).

---

## Codebase Metrics

| Metric | Count |
|--------|-------|
| Java source files | 163 |
| Java source lines | 9,340 |
| Unit test files | 48 |
| Unit tests | 420 |
| API integration test files | 21 |
| API integration tests | 124 |
| Behaviour test feature files | 20 |
| Behaviour test scenarios (Cucumber) | 92 |
| E2E test files (Playwright) | 28 |
| E2E tests (flow + Storybook) | 158 |
| Performance test simulations (Gatling) | 14 |
| Performance tests | 6 |
| Storybook story files | 8 |
| Storybook stories | ~30 |
| Frontend files (TSX/TS/CSS) | 87 |
| SQL migrations (Flyway V1-V20) | 20 |
| Database tables | 18 |
| REST endpoints | 38 |
| REST controllers | 6 |
| Kafka topics | 5 |
| Kafka partitions (total) | 88 |
| Scheduled jobs | 9 |
| Domain models | 21 |
| Domain ports (interfaces) | 18 |
| Domain services | 3 |
| Application services | 8 |
| JPA entities | 14 |
| Grafana dashboards | 7 |
| **Total automated tests** | **800+ (100% passing)** |

---

## Architecture

### Hexagonal (Ports & Adapters)

```
┌──────────────────────────────────────────────────────────────┐
│                        API Layer (6 controllers)             │
│   EndorsementController (8)      IntelligenceController (16) │
│   InsurerConfigController (5)    ReconciliationController (3)│
│   EAAccountController (1)        AuditLogController (1)      │
│   GlobalExceptionHandler (RFC 7807)   22 DTOs (records)      │
├──────────────────────────────────────────────────────────────┤
│                    Application Layer                         │
│   Handlers (3):  Create / Process / Query (CQRS)             │
│   Services (8):  AnomalyDetection, BalanceForecast,          │
│                  EmployerHealthScore, ErrorResolution,        │
│                  InsurerBenchmark, ProcessMining,             │
│                  ReconciliationEngine, AuditLog               │
│   Schedulers (9): Batch, Poller, Cleanup, Reconciliation,    │
│                   Anomaly, Forecast, ProcessMining,           │
│                   StuckRetry, DataRetention                   │
├──────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│   Models (21):  Endorsement, EAAccount, EndorsementBatch,    │
│                 ProvisionalCoverage, InsurerConfiguration,   │
│                 AnomalyDetection, BalanceForecastRecord,     │
│                 ErrorResolution, ProcessMiningMetric,         │
│                 ReconciliationRun/Item, AuditLog, + enums    │
│   EndorsementEvent (22 types, sealed interface)              │
│   Services (3): StateMachine, BalanceCalculator, Registry    │
│   Ports (18):   Repository + Port interfaces                 │
├──────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                       │
│   JPA Adapters (10)      14 JPA Entities + Mapper            │
│   14 Spring Data Repos   KafkaEventPublisher + Config        │
│   Insurer Adapters (4):  Mock, ICICI, NivaBupa, Bajaj        │
│   InsurerRouter (Factory) + InsurerRegistry (@Cacheable)     │
│   Intelligence (5):      Anomaly, Forecast, ErrorResolver,   │
│                          BatchOptimizer, ProcessMiner         │
│   Observability:         MdcFilter, RequestLogging, Metrics  │
│   Config:                Security, ShedLock, RateLimit, WS   │
└──────────────────────────────────────────────────────────────┘
```

### Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 (virtual threads) |
| Framework | Spring Boot | 3.4.3 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | (managed by Spring Boot) |
| Cache | Caffeine | 3.1.8 |
| Messaging | Apache Kafka | 3.7.0 (KRaft) |
| Cache Infra | Redis | 7 |
| Tracing | OpenTelemetry + Jaeger | 1.55 |
| Metrics | Micrometer + Prometheus | (managed by Spring Boot) |
| Resilience | Resilience4j | 2.2.0 |
| API Docs | SpringDoc OpenAPI | 2.8.4 |
| Frontend | React + TypeScript | 19.2 + 5.9 |
| Build (FE) | Vite | 7.3.1 |
| Styling | Tailwind CSS + shadcn/ui | 4.2.1 + 4.0.0 |
| Data Fetching | TanStack Query | 5.x |
| E2E Testing | Playwright | 1.50.x |
| Component Dev | Storybook | 10.2.x |
| Build (BE) | Gradle (Kotlin DSL) | 8.10.2 |

---

## Feature Completion Matrix

### Core Endorsement Engine

| Feature | Status | Details |
|---------|--------|---------|
| Create endorsement (ADD/DELETE/UPDATE) | Done | With validation, idempotency key, employee data (JSONB) |
| State machine (11 states) | Done | CREATED -> VALIDATED -> PROVISIONALLY_COVERED -> ... -> CONFIRMED/FAILED_PERMANENT |
| Idempotency guarantee | Done | Unique key per endorsement, duplicate returns existing record |
| Get endorsement by ID | Done | Full response with all fields |
| List endorsements (paginated) | Done | Filter by employerId, statuses; page/size params |
| Event sourcing | Done | 12 event types persisted to endorsement_events table + published to Kafka |

### Insurer Submission

| Feature | Status | Details |
|---------|--------|---------|
| Real-time submission path | Done | PROVISIONALLY_COVERED -> SUBMITTED_REALTIME -> CONFIRMED |
| Batch submission path | Done | QUEUED_FOR_BATCH -> BATCH_SUBMITTED -> INSURER_PROCESSING |
| Batch assembly scheduler | Done | Runs every 15 min, groups by insurer, chunks at max 100 |
| Batch status poller | Done | Polls every 60s, handles confirmations/rejections |
| SLA monitoring | Done | 24-hour deadline per batch, breach notifications |
| Confirm endorsement | Done | Sets insurerReference, transitions to CONFIRMED |
| Reject endorsement | Done | Retry logic: retryCount < 3 -> RETRY_PENDING, >= 3 -> FAILED_PERMANENT |
| Mock insurer adapter | Done | Always succeeds, 100ms latency, synthetic references (INS-RT-*, INS-BATCH-*) |

### Provisional Coverage

| Feature | Status | Details |
|---------|--------|---------|
| Grant on creation (ADD type) | Done | Immediate coverage before insurer confirmation |
| Confirm on endorsement confirmation | Done | coverageType: PROVISIONAL -> CONFIRMED |
| Stale cleanup | Done | Daily at 2 AM, expires coverages older than 30 days |
| Skip for DELETE/UPDATE types | Done | No coverage record created |

### EA Balance Management

| Feature | Status | Details |
|---------|--------|---------|
| Account lookup | Done | By employerId + insurerId composite key |
| Balance reservation (ADD type) | Done | Reserves premiumAmount, reduces availableBalance |
| Skip reservation (DELETE/UPDATE) | Done | No balance impact for non-ADD types |
| Insufficient balance handling | Done | Endorsement still created, notification logged |
| Transaction ledger | Done | ea_transactions table, audit trail for all movements |

### REST API (38 Endpoints)

#### Endorsement Operations (8 endpoints)

| Method | Endpoint | Status |
|--------|----------|--------|
| POST | `/api/v1/endorsements` | Done |
| GET | `/api/v1/endorsements/{id}` | Done |
| GET | `/api/v1/endorsements?employerId=&statuses=&page=&size=` | Done |
| POST | `/api/v1/endorsements/{id}/submit` | Done |
| POST | `/api/v1/endorsements/{id}/confirm?insurerReference=` | Done |
| POST | `/api/v1/endorsements/{id}/reject?reason=` | Done |
| GET | `/api/v1/endorsements/{id}/coverage` | Done |
| GET | `/api/v1/endorsements/batches/{batchId}/progress` | Done |

#### EA Account (1 endpoint)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/ea-accounts?employerId=&insurerId=` | Done |

#### Insurer Configuration (5 endpoints)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/insurers` | Done |
| GET | `/api/v1/insurers/{id}` | Done |
| GET | `/api/v1/insurers/{id}/capabilities` | Done |
| POST | `/api/v1/insurers` | Done |
| PUT | `/api/v1/insurers/{id}` | Done |

#### Reconciliation (3 endpoints)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/reconciliation/runs?insurerId=` | Done |
| GET | `/api/v1/reconciliation/runs/{runId}/items` | Done |
| POST | `/api/v1/reconciliation/trigger?insurerId=` | Done |

#### Intelligence (16 endpoints)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/intelligence/anomalies` | Done |
| GET | `/api/v1/intelligence/anomalies/{id}` | Done |
| PUT | `/api/v1/intelligence/anomalies/{id}/review` | Done |
| GET | `/api/v1/intelligence/forecasts` | Done |
| GET | `/api/v1/intelligence/forecasts/history` | Done |
| POST | `/api/v1/intelligence/forecasts/generate` | Done |
| GET | `/api/v1/intelligence/error-resolutions` | Done |
| GET | `/api/v1/intelligence/error-resolutions/stats` | Done |
| POST | `/api/v1/intelligence/error-resolutions/{id}/approve` | Done |
| GET | `/api/v1/intelligence/process-mining/metrics` | Done |
| GET | `/api/v1/intelligence/process-mining/insights` | Done |
| GET | `/api/v1/intelligence/process-mining/stp-rate` | Done |
| GET | `/api/v1/intelligence/process-mining/stp-rate/trend` | Done |
| POST | `/api/v1/intelligence/process-mining/analyze` | Done |
| GET | `/api/v1/intelligence/employers/{id}/health-score` | Done |
| GET | `/api/v1/intelligence/benchmarks` | Done |

#### Audit (1 endpoint)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/audit-logs` | Done |

Error responses follow RFC 7807 ProblemDetail format with `title`, `detail`, `status`, and optional `errors[]` for validation failures.

### Frontend (10+ Pages)

| Page | Route | Status | Features |
|------|-------|--------|----------|
| Dashboard | `/` | Done | Stats cards, status distribution bar, recent endorsements, EA balance widget |
| Endorsements List | `/endorsements` | Done | TanStack Table, employer/status filters, sorting, pagination, CSV export, skeleton loading |
| Create Endorsement | `/endorsements/new` | Done | react-hook-form + zod validation, structured employee data fields, error handling (409/422/400) |
| Endorsement Detail | `/endorsements/:id` | Done | Status timeline (real-time vs batch path), key-value grid, action buttons, coverage card |
| EA Accounts | `/ea-accounts` | Done | Lookup form, balance/reserved/available cards, progress bar |
| Insurers List | `/insurers` | Done | All active insurers, capabilities display, adapter type badges |
| Insurer Detail | `/insurers/:id` | Done | Configuration details, capabilities, rate limits, SLA settings |
| Intelligence Dashboard | `/intelligence` | Done | 4 tabs: Anomalies, Forecasts, Error Resolution, Process Mining |
| Reconciliation | `/reconciliation` | Done | Reconciliation runs, items with outcome display, manual trigger |
| Batch Progress | `/batches/:id` | Done | Batch status, endorsement list, SLA tracking |

**Frontend infrastructure:**
- Vite dev server with proxy (`/api` -> `localhost:8080`)
- Responsive layout: sidebar (desktop) + sheet menu (mobile)
- Breadcrumb navigation
- Toast notifications (sonner)
- 20 shadcn/ui base components
- TanStack Query for data fetching with caching
- Storybook 10.x with 8 story files (~30 stories) for component development and testing

### Infrastructure

| Component | Status | Details |
|-----------|--------|---------|
| PostgreSQL 16 | Done | 18 tables, 20 Flyway migrations (V1-V20), composite keys, JSONB, indexes, archive tables |
| Redis 7 | Done | Spring Data Redis for `@Cacheable` (InsurerRegistry); Caffeine fallback (1000 entries, 60s TTL) |
| Kafka (KRaft) | Done | 5 topics, 88 partitions: endorsement-events (32p), endorsement-commands (32p), endorsement-notifications (8p), endorsement-reconciliation (16p) |
| Jaeger | Done | OpenTelemetry OTLP export, 100% sampling, baggage propagation, UI at :16686 |
| Prometheus | Done | 20+ custom metrics (counters, timers, gauges, summaries) + Actuator endpoints |
| Grafana | Done | 7 dashboards (application overview, endorsement business, infrastructure, intelligence, multi-insurer, reconciliation, scheduler) |
| ELK Stack | Done | Logstash pipeline + Elasticsearch + Kibana for structured log aggregation |
| Docker Compose | Done | All services with health checks (PostgreSQL, Redis, Kafka, Jaeger, Elasticsearch, Logstash, Kibana, Grafana, Prometheus) |
| Kubernetes (local) | Done | Plain YAML manifests in `k8s/` with ConfigMaps, Secrets, PVCs, HPA, PDB |

### Security

| Feature | Status | Details |
|---------|--------|---------|
| Spring Security | Done | `SessionCreationPolicy.STATELESS`, CSRF disabled (API-only) |
| Request authorization | Done | `permitAll()` — auth not required by design challenge |
| Rate limiting | Done | IP-based sliding window via `RateLimitingFilter` |
| Audit logging | Done | `AuditLoggingAspect` captures mutations to `audit_logs` table; `GET /api/v1/audit-logs` API |
| ShedLock (distributed locks) | Done | `shedlock` table (V16) + `ShedLockConfig` for multi-instance scheduler coordination |
| AuthZ/AuthN strategy | Documented | JWT + OAuth2 + RBAC (5 roles) architecture in [Product Evolution Vision](deliverables/vision/Product_Evolution_Vision.md#9-authentication--authorization-architecture) |

### Deployment

| Feature | Status | Details |
|---------|--------|---------|
| Multi-stage Dockerfile | Done | Build: temurin:21-jdk-alpine, Runtime: temurin:21-jre-alpine, non-root user, ZGC, healthcheck |
| Railway.app config | Done | railway.toml + application-railway.yml with env var substitution |
| One-click start script | Done | `./start.sh` — checks prereqs, starts containers, builds, seeds data, launches both servers |
| One-click stop | Done | `./start.sh stop` — kills servers, docker compose down |
| K8s local dev launcher | Done | `./k8s-start.sh` — builds image, deploys to local K8s, port-forwards, starts frontend |
| K8s local dev stop | Done | `./k8s-start.sh stop` — kills port-forwards, deletes `plum` namespace |
| K8s manifests | Done | 17 YAML files in `k8s/` — namespace, deployments, services, configmaps, secret, PVC, seed job |

### Testing

| Category | Tests | Status |
|----------|-------|--------|
| Unit tests (JUnit 5 + Mockito + AssertJ) | 420 | All passing |
| API integration tests (RestAssured + Testcontainers) | 124 | All passing |
| Behaviour tests (Cucumber BDD + Testcontainers) | 92 | All passing |
| E2E tests (Playwright — flow + Storybook) | 158 | All passing |
| Performance tests (Gatling) | 6 | All passing |
| **Total** | **800+** | **100% pass rate** |

**Test Reporting:**

| Report | Command | Access |
|--------|---------|--------|
| Combined Allure (all suites) | `./run-all-tests.sh` | Allure Docker at :5050 |
| API tests only | `./run-api-tests.sh` | Allure Docker at :5050 |
| BDD tests only | `./run-behaviour-tests.sh` | Allure Docker at :5051 |
| E2E tests only | `./run-e2e-tests.sh` | Allure Docker at :5052 |
| Performance tests | `./run-perf-tests.sh` | Gatling HTML reports |
| Cucumber HTML/JSON | (auto-generated) | `behaviour-tests/build/reports/cucumber/` |

**Allure report segregation** — all test suites are post-processed with `parentSuite` and `epic` labels for clean grouping:

| Tab | Section | Tests |
|-----|---------|-------|
| Suites → Parent Suite | API Tests | 124 RestAssured integration tests |
| Suites → Parent Suite | BDD Tests | 92 Cucumber behaviour scenarios |
| Suites → Parent Suite | E2E Tests | 158 Playwright tests (flow + Storybook) |
| Behaviors → Epic | Endorsement API | API integration tests |
| Behaviors → Epic | Endorsement BDD | Cucumber BDD tests |
| Behaviors → Epic | Endorsement E2E | E2E + Storybook component tests |

### Documentation

| Document | Location |
|----------|----------|
| Architecture design | `docs/plum/Scalable_Insurance_Platform_Architecture.md` |
| Hexagonal architecture guide | `docs/Architecture_Hexagonal_Ports_and_Adapters.md` |
| Execution plan (4 phases) | `docs/Endorsement_System_Execution_Plan.md` |
| Functional specification | `docs/Functional_Specification.md` |
| Installation & usage guide | `docs/MVP_Installation_Startup_Usage_Guide.md` |
| Performance test plan | `docs/Performance_Test_Plan.md` |
| Phase 2 completion status | `docs/Phase2_Completion_Status.md` |
| Phase 3 completion status | `docs/Phase3_Completion_Status.md` |
| Project completion status | `docs/MVP_Completion_Status.md` (this document) |
| API test suite README | `api-tests/README.md` |
| Original problem statement | `docs/Architect Design Problem.pdf` |

**Deliverables:**

| Document | Location |
|----------|----------|
| High-level architecture | `docs/deliverables/High_Level_Architecture.md` |
| EA balance minimization algorithm | `docs/deliverables/EA_Balance_Minimization_Algorithm.md` |
| No-loss-of-coverage approach | `docs/deliverables/No_Loss_of_Coverage_Approach.md` |
| Real-time visibility user flows | `docs/deliverables/Real_Time_Visibility_User_Flows.md` |
| Product usage guide | `docs/deliverables/Product_Usage_Guide.md` |
| Demo script (15 min) | `docs/deliverables/Demo_Script_15min.md` |

**Vision & Strategy:**

| Document | Location |
|----------|----------|
| Product evolution vision (Phase 4) | `docs/deliverables/vision/Product_Evolution_Vision.md` |
| AI automation vision | `docs/deliverables/vision/AI_Automation_Vision.md` |
| AI automation approach | `docs/deliverables/vision/AI_Automation_Approach.md` |
| AI automation implementation | `docs/deliverables/vision/AI_Automation_Implementation_Approach.md` |
| UX improvement strategy | `docs/deliverables/vision/UX_Improvement_Strategy.md` |

---

## Database Schema (18 Tables)

```
┌─────────────────────┐     ┌──────────────────────┐
│    endorsements      │────>│  endorsement_events   │
│ (PK: id UUID)        │     │ (FK: endorsement_id)  │
│ employer_id          │     │ event_type, event_data │
│ employee_id          │     └──────────────────────┘
│ insurer_id           │
│ policy_id            │     ┌──────────────────────┐
│ type, status         │────>│ provisional_coverages  │
│ employee_data (JSONB)│     │ (FK: endorsement_id)  │
│ premium_amount       │     │ coverage_type          │
│ batch_id (FK)        │     │ confirmed_at           │
│ insurer_reference    │     └──────────────────────┘
│ idempotency_key (UQ) │
│ version (optimistic) │     ┌──────────────────────┐
└─────────────────────┘     │ endorsement_batches    │
                             │ (PK: id UUID)          │
┌─────────────────────┐     │ insurer_batch_ref      │
│    ea_accounts       │     │ sla_deadline           │
│ (PK: employer_id +   │     └──────────────────────┘
│      insurer_id)     │
│ balance, reserved    │     ┌──────────────────────┐
│ version (opt. lock)  │────>│   ea_transactions      │
│ updated_at           │     │ (FK: endorsement_id)  │
└─────────────────────┘     │ type, amount           │
                             │ balance_after          │
┌──────────────────────┐    └──────────────────────┘
│ insurer_configurations│
│ (PK: insurer_id UUID)│    ┌──────────────────────┐
│ insurer_name, code   │    │  reconciliation_runs  │
│ adapter_type, active │───>│ (PK: id UUID)         │
│ supports_rt/batch    │    │ matched, missing      │
│ rate_limit, sla_hours│    └──────────────────────┘
└──────────────────────┘         │
                             ┌──────────────────────┐
┌──────────────────────┐    │ reconciliation_items  │
│  anomaly_detections  │    │ (FK: run_id)          │
│ employer_id, type    │    │ outcome, action_taken │
│ score, explanation   │    └──────────────────────┘
│ status (state machine)│
└──────────────────────┘    ┌──────────────────────┐
                             │  balance_forecasts    │
┌──────────────────────┐    │ employer/insurer_id   │
│  error_resolutions   │    │ forecasted_amount     │
│ error_type, confidence│    │ narrative             │
│ auto_applied         │    └──────────────────────┘
└──────────────────────┘
                             ┌──────────────────────┐
┌──────────────────────┐    │ process_mining_metrics │
│     audit_logs       │    │ insurer_id, from/to   │
│ action, entity_type  │    │ avg/p95/p99_ms        │
│ entity_id, details   │    │ happy_path_pct        │
│ timestamp            │    └──────────────────────┘
└──────────────────────┘
                             ┌──────────────────────┐
                             │  stp_rate_snapshots   │
                             │ insurer_id            │
                             │ stp_rate, sample_count│
                             │ snapshot_date         │
                             └──────────────────────┘

                             ┌──────────────────────┐
┌──────────────────────┐    │     shedlock          │
│  *_archive (3 tables)│    │ name (PK), lock_until │
│ endorsements_archive │    │ locked_at, locked_by  │
│ ea_transactions_arch │    └──────────────────────┘
│ events_archive       │
└──────────────────────┘
```

---

## State Machine

```
CREATED
  │
  ▼
VALIDATED
  │
  ▼
PROVISIONALLY_COVERED ─────────────────────────────────┐
  │                                                     │
  ├──► SUBMITTED_REALTIME ──► INSURER_PROCESSING ──┐   │
  │                                                 │   │
  └──► QUEUED_FOR_BATCH ──► BATCH_SUBMITTED ────────┘   │
                                    │                    │
                          ┌─────────┴─────────┐         │
                          ▼                   ▼         │
                      CONFIRMED          REJECTED       │
                      (terminal)            │           │
                                   ┌────────┴────────┐  │
                                   ▼                 ▼  │
                            RETRY_PENDING    FAILED_PERMANENT
                                   │          (terminal)
                                   └─── (back to submit) ──┘
```

**Terminal states:** CONFIRMED, FAILED_PERMANENT
**Retry policy:** Up to 3 attempts (configurable via `endorsement.retry.max-attempts`)
**Stuck endorsement recovery:** `StuckEndorsementRetryScheduler` detects and retries endorsements stuck in intermediate states

---

## Insurer Adapters (4 Implementations)

All four adapters implement the `InsurerPort` domain interface. Each is a simulated implementation with realistic latency and data format handling, routed at runtime by `InsurerRouter` (Factory pattern).

| Adapter | Protocol | Modes | Latency | Key Behavior |
|---------|----------|-------|---------|-------------|
| `MockInsurerAdapter` | REST/JSON | RT + Batch | 100ms | Always succeeds, synthetic references (INS-RT-*, INS-BATCH-*) |
| `IciciLombardAdapter` | REST/JSON | RT only | 150ms | `@CircuitBreaker(name="iciciLombard")`, `@Retry`, JSON field mapping via `IciciLombardDataMapper` |
| `NivaBupaAdapter` | CSV/SFTP | Batch only | 200ms | `submitRealTime()` throws `UnsupportedOperationException`; CSV generation via `NivaBupaCsvMapper` |
| `BajajAllianzAdapter` | SOAP/XML | RT + Batch | 250ms | `@CircuitBreaker(name="bajajAllianz")`, XML envelope via `BajajAllianzXmlMapper` |

**Production swap:** Replace `Thread.sleep()` with real HTTP/SFTP/SOAP clients. Zero changes needed in domain, handlers, or routing logic — the Strategy pattern ensures insurer-specific behavior is fully encapsulated.

---

## Scheduled Jobs (9)

| Job | Schedule | Purpose |
|-----|----------|---------|
| Batch Assembly | Every 15 min (`0 */15 * * * *`) | Groups QUEUED_FOR_BATCH endorsements by insurer, chunks into batches of 100, submits to insurer |
| Batch Status Poller | Every 60 sec (fixed delay) | Polls insurer for batch results, handles confirmations/rejections, checks SLA breaches |
| Coverage Cleanup | Daily at 2 AM (`0 0 2 * * *`) | Expires provisional coverages older than 30 days |
| Reconciliation | Every 15 min | Reconciles INSURER_PROCESSING endorsements against insurer records per active insurer |
| Anomaly Detection | Every 5 min (configurable) | Scans recent endorsements for suspicious patterns (volume spikes, add/delete cycling, suspicious timing, unusual premiums, dormancy breaks) |
| Balance Forecast | Daily at 6 AM | 30-day balance projection for all active EA accounts with shortfall alerting |
| Process Mining | Daily at 3 AM | Event stream analysis for STP rates, bottleneck detection, lifecycle metrics per insurer |
| Stuck Endorsement Retry | Configurable cron | Detects and retries endorsements stuck in non-terminal intermediate states |
| Data Retention | Configurable cron | Archives aged records to archive tables, cleans expired forecasts and metrics |

---

## Observability

| Capability | Implementation | Access |
|------------|---------------|--------|
| Distributed tracing | OpenTelemetry -> Jaeger (100% sampling, baggage propagation) | http://localhost:16686 |
| Metrics | Micrometer -> Prometheus (20+ custom metrics) | http://localhost:8080/actuator/prometheus |
| Dashboards | Grafana (7 dashboards, 60+ panels) | http://localhost:3000 |
| Log aggregation | Logstash -> Elasticsearch -> Kibana (structured JSON) | http://localhost:5601 |
| Health checks | Spring Actuator (DB, Redis, Kafka, circuit breakers) | http://localhost:8080/actuator/health |
| API documentation | SpringDoc OpenAPI | http://localhost:8080/swagger-ui |
| Request logging | `RequestLoggingFilter` (method, URI, status, duration, body size) | Application logs |
| MDC context | `MdcRequestFilter` (traceId, spanId, requestId, endorsementId, employerId) | All log entries |
| Test reports (combined) | Allure (Docker) | http://localhost:5050 via `./run-all-tests.sh` |
| Test reports (API) | Allure (Docker) | http://localhost:5050 via `./run-api-tests.sh` |
| Test reports (BDD) | Allure (Docker) | http://localhost:5051 via `./run-behaviour-tests.sh` |
| Test reports (E2E) | Allure (Docker) | http://localhost:5052 via `./run-e2e-tests.sh` |
| Performance reports | Gatling HTML | via `./run-perf-tests.sh` |
| Storybook | Component explorer | http://localhost:6006 via `cd frontend && npm run storybook` |

---

## Quick Start

```bash
# Option A: Docker Compose (requires Java 21+, Node 18+, Docker)
./start.sh

# Option B: Kubernetes (requires Java 21+, Node 18+, Docker, kubectl, K8s cluster)
./k8s-start.sh

# Access points (same for both options)
# Frontend:      http://localhost:5173
# Backend API:   http://localhost:8080/api/v1
# Swagger UI:    http://localhost:8080/swagger-ui
# Jaeger:        http://localhost:16686
# Grafana:       http://localhost:3000 (7 dashboards)
# Kibana:        http://localhost:5601
# Prometheus:    http://localhost:9090

# Demo data (pre-seeded)
# Employer ID:   11111111-1111-1111-1111-111111111111
# Insurer ID:    22222222-2222-2222-2222-222222222222
# EA Balance:    500,000

# Run all tests (API + BDD + E2E + Perf) with combined Allure report
./run-all-tests.sh

# Or run individually
./run-api-tests.sh              # API tests only (Allure at :5050)
./run-behaviour-tests.sh        # BDD tests only (Allure at :5051)
./run-e2e-tests.sh              # E2E + Storybook tests only (Allure at :5052)
./run-perf-tests.sh             # Performance tests (Gatling)

# Allure combined report navigation:
#   Suites tab -> Parent Suite:
#     API Tests  — 124 RestAssured integration tests
#     BDD Tests  — 92 Cucumber behaviour scenarios
#     E2E Tests  — 158 Playwright tests (flow + Storybook)
#   Behaviors tab -> Epic:
#     Endorsement API / Endorsement BDD / Endorsement E2E

# Stop everything
./start.sh stop        # Docker Compose
./k8s-start.sh stop    # Kubernetes
./run-all-tests.sh --stop   # Allure server
```

---

## Project Structure

```
plum-endorsements/
├── src/main/java/com/plum/endorsements/
│   ├── EndorsementApplication.java
│   ├── api/                          # REST controllers, DTOs, exception handler
│   │   ├── controller/               # 6 controllers (Endorsement, EAAccount, Insurer, Intelligence, Reconciliation, AuditLog)
│   │   ├── dto/                      # 22 DTOs (requests, responses — all Java records)
│   │   └── exception/                # GlobalExceptionHandler (RFC 7807)
│   ├── application/                  # Use cases, orchestration
│   │   ├── handler/                  # 3 handlers (Create, Process, Query — CQRS)
│   │   ├── service/                  # 8 services (AnomalyDetection, BalanceForecast, EmployerHealthScore, ErrorResolution, InsurerBenchmark, ProcessMining, ReconciliationEngine, AuditLog)
│   │   ├── scheduler/                # 9 schedulers (Batch, Poller, Cleanup, Reconciliation, Anomaly, Forecast, ProcessMining, StuckRetry, DataRetention)
│   │   └── exception/                # 4 domain exceptions
│   ├── domain/                       # Pure business logic (ZERO infrastructure imports)
│   │   ├── model/                    # 21 models + enums (Endorsement, EAAccount, InsurerConfiguration, AnomalyDetection, etc.)
│   │   ├── port/                     # 18 port interfaces (repositories + adapters)
│   │   └── service/                  # 3 services (StateMachine, BalanceCalculator, InsurerRegistry)
│   └── infrastructure/               # Framework adapters
│       ├── persistence/              # 10 adapters, 14 entities, 1 mapper, 14 Spring Data repos
│       ├── messaging/                # KafkaConfig (5 topics, 88 partitions), KafkaEventPublisher, WebSocketEventBroadcaster
│       ├── insurer/                  # InsurerRouter (Factory) + 4 adapters: Mock, ICICI, NivaBupa, Bajaj + 3 data mappers
│       ├── intelligence/             # 5 engines: AnomalyDetector, ForecastEngine, ErrorResolver, BatchOptimizer, ProcessMiner
│       ├── notification/             # LoggingNotificationAdapter, WebhookNotificationAdapter
│       └── config/                   # SecurityConfig, ShedLockConfig, MetricsConfig, RateLimitingFilter, MdcRequestFilter, WebSocketConfig, etc.
├── src/main/resources/
│   ├── application.yml               # Main config (200+ properties)
│   ├── application-railway.yml       # Railway deployment (env var substitution)
│   ├── application-test.yml          # Unit test profile
│   ├── logback-spring.xml            # Structured logging (JSON for prod, human-readable for dev)
│   └── db/migration/                 # V1-V20 Flyway migrations (18 tables)
├── src/test/java/                    # 48 unit test classes (420 tests)
├── api-tests/                        # 21 API integration test classes (124 tests)
├── behaviour-tests/                  # Cucumber BDD tests (92 scenarios)
│   └── src/test/
│       ├── java/.../bdd/             # Runner, config, 20 step def classes, support
│       └── resources/features/       # 20 Gherkin feature files
├── e2e-tests/                        # Playwright E2E + Storybook component tests (158 tests)
│   ├── playwright.config.ts          # 2 projects: storybook (:6006) + e2e (:5173)
│   └── tests/
│       ├── e2e/                      # 21 E2E flow test files
│       ├── storybook/                # 7 Storybook component test files
│       └── fixtures/                 # API helpers for test data seeding
├── performance-tests/                # Gatling performance tests
│   └── src/gatling/scala/            # 14 simulations, 5 scenarios, 3 request classes
├── frontend/                         # React + TypeScript + Vite (87 files)
│   ├── .storybook/                   # Storybook config (main.ts, preview.ts)
│   └── src/
│       ├── pages/                    # 10+ pages (Dashboard, List, Detail, Create, EAAccounts, Intelligence, Insurers, Reconciliation, etc.)
│       ├── components/               # Layout, Shared, UI (20 shadcn/ui), Feature components
│       │   └── **/*.stories.tsx      # 8 Storybook story files (~30 stories)
│       ├── hooks/                    # Custom hooks (useEndorsements, useIntelligence, etc.)
│       ├── lib/                      # API client, query keys, constants, utils
│       ├── types/                    # Type definition files
│       └── routes/                   # Router config + root layout
├── observability/                    # Full observability stack
│   ├── grafana/                      # 7 dashboards + provisioning (datasources, dashboards)
│   ├── prometheus/                   # prometheus.yml scrape config
│   └── logstash/                     # Logstash pipeline config
├── docker-compose.yml                # PostgreSQL, Redis, Kafka, Jaeger, Elasticsearch, Logstash, Kibana, Grafana, Prometheus
├── Dockerfile                        # Multi-stage (temurin:21-jdk-alpine build, jre-alpine runtime, non-root, ZGC, healthcheck)
├── railway.toml                      # Railway deployment config
├── k8s-start.sh                      # One-click K8s installer & launcher
├── k8s/                              # Kubernetes manifests (17 YAML files)
│   ├── namespace.yaml                # plum namespace
│   ├── postgres/                     # ConfigMap, Secret, PVC, Deployment, Service
│   ├── redis/                        # Deployment, Service
│   ├── kafka/                        # ConfigMap, Deployment, Service
│   ├── elasticsearch/                # Deployment, PVC, Service
│   ├── jaeger/                       # Deployment, Service
│   ├── grafana/                      # ConfigMap, Deployment, Service
│   ├── prometheus/                   # ConfigMap, Deployment, Service
│   ├── logstash/                     # ConfigMap, Deployment, Service
│   ├── kibana/                       # Deployment, Service
│   ├── backend/                      # ConfigMap, Deployment, Service, HPA, PDB
│   └── seed-job.yaml                 # K8s Job for demo data seeding
├── run-all-tests.sh                  # Combined runner + merged Allure report (:5050)
├── run-api-tests.sh                  # API test runner + Allure report (:5050)
├── run-behaviour-tests.sh            # BDD test runner + Allure report (:5051)
├── run-e2e-tests.sh                  # E2E test runner + Allure report (:5052)
├── run-perf-tests.sh                 # Performance test runner
├── scripts/
│   ├── allure-post-process.sh        # Injects parentSuite + epic labels into Allure results
│   ├── allure-label-filter.jq        # jq filter for label injection
│   ├── allure-categories.json        # Allure result classification rules
│   └── gatling-to-allure.sh          # Converts Gatling results to Allure format
├── docs/                             # Architecture, specs, guides
│   ├── deliverables/                 # Technical deliverable documents
│   │   └── vision/                   # Vision & strategy documents
│   └── plum/                         # Platform architecture & interview prep
└── CLAUDE.md                         # AI coding assistant instructions (design patterns & conventions)
```

**163 Java source files | 151+ test files | 20 feature files | 87 frontend files | 8 story files | 20 migrations | 800+ tests**

---

## Phase 2 — Scale: Multi-Insurer & Optimization (Completed)

**Date:** March 8, 2026

### Multi-Insurer Architecture

| Feature | Status | Details |
|---------|--------|---------|
| Insurer Configuration model | Done | `insurer_configurations` table (V6 migration), domain model with `toCapabilities()` |
| InsurerRegistry | Done | @Cacheable lookup by insurerId/insurerCode, @CacheEvict on update |
| InsurerRouter | Done | Factory pattern — collects InsurerPort beans by `getAdapterType()`, resolves per endorsement |
| ICICI Lombard adapter | Done | REST/JSON, real-time only, @CircuitBreaker, 150ms simulated latency |
| Niva Bupa adapter | Done | CSV batch-only, `submitRealTime` throws UnsupportedOperationException |
| Bajaj Allianz adapter | Done | SOAP/XML, real-time + batch, @CircuitBreaker |
| Multi-insurer handler routing | Done | ProcessEndorsementHandler, BatchAssemblyScheduler, BatchStatusPollerScheduler all use InsurerRouter |
| Per-insurer resilience4j config | Done | Circuit breakers: iciciLombard, bajajAllianz; retry instances configured |

### EA Balance Optimization

| Feature | Status | Details |
|---------|--------|---------|
| EndorsementPriority enum | Done | P0_DELETION, P1_COST_NEUTRAL, P2_ADDITION, P3_PREMIUM_UPDATE with `classify()` |
| Optimal batch sequencing | Done | `sequenceForOptimalBalance()` — deletions first to free balance |
| Optimized batch construction | Done | `constructOptimizedBatch()` — respects balance constraints, returns `BatchPlan` |
| Balance forecasting | Done | `forecastBalance()` — projects shortfall with 10% safety margin |

### Kafka Event Enhancement

| Feature | Status | Details |
|---------|--------|---------|
| Employer-based partitioning | Done | Partition key changed from endorsementId to employerId |
| Scaled partitions | Done | endorsement-events 3→32, endorsement-commands 3→32, endorsement-notifications 1→8 |
| Reconciliation topic | Done | endorsement-reconciliation (16 partitions) |
| New event types | Done | ReconciliationMatched, ReconciliationDiscrepancy, ReconciliationMissing, BalanceForecastAlert |

### Automated Reconciliation

| Feature | Status | Details |
|---------|--------|---------|
| Reconciliation engine | Done | Checks INSURER_PROCESSING endorsements against insurer refs |
| Reconciliation outcomes | Done | MATCH, PARTIAL_MATCH, REJECTED, MISSING |
| Scheduled runs | Done | Every 15 minutes, iterates all active insurers |
| Reconciliation API | Done | GET /runs, GET /runs/{id}/items, POST /trigger |
| Database tables | Done | reconciliation_runs, reconciliation_items (V8 migration) |

### New REST Endpoints (Phase 2)

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/insurers` | Done |
| GET | `/api/v1/insurers/{id}` | Done |
| GET | `/api/v1/insurers/{id}/capabilities` | Done |
| GET | `/api/v1/reconciliation/runs?insurerId=` | Done |
| GET | `/api/v1/reconciliation/runs/{runId}/items` | Done |
| POST | `/api/v1/reconciliation/trigger?insurerId=` | Done |

### Observability (Phase 2)

| Feature | Status | Details |
|---------|--------|---------|
| Multi-insurer monitoring dashboard | Done | Per-insurer latency, circuit breaker state, error rates |
| Reconciliation monitoring dashboard | Done | Run frequency, outcome distribution, SLA breaches |
| Active insurer count gauge | Done | `endorsement.insurer.active.count` |
| Per-insurer submission metrics | Done | Timer per adapter (mock, icici, nivabupa, bajaj) |

### Phase 2 Database Schema Additions

```
┌──────────────────────────┐     ┌──────────────────────────┐
│ insurer_configurations    │     │   reconciliation_runs     │
│ (PK: insurer_id UUID)    │     │ (PK: id UUID)             │
│ insurer_name, insurer_code│     │ insurer_id, status        │
│ adapter_type, active      │     │ total_checked, matched    │
│ supports_real_time/batch  │     │ started_at, completed_at  │
│ max_batch_size, sla_hours │     └──────────────────────────┘
│ rate_limit_per_min        │              │
│ circuit_breaker_config    │     ┌──────────────────────────┐
└──────────────────────────┘     │  reconciliation_items     │
                                  │ (FK: run_id, endorsement) │
                                  │ outcome, action_taken     │
                                  │ sent_data, confirmed_data │
                                  └──────────────────────────┘
```

### Demo Data (Phase 2)

| Insurer | Insurer ID | Protocol | Modes |
|---------|-----------|----------|-------|
| Mock | `22222222-2222-2222-2222-222222222222` | In-memory | Real-time + Batch |
| ICICI Lombard | `33333333-3333-3333-3333-333333333333` | REST/JSON | Real-time only |
| Niva Bupa | `44444444-4444-4444-4444-444444444444` | CSV | Batch only |
| Bajaj Allianz | `55555555-5555-5555-5555-555555555555` | SOAP/XML | Real-time + Batch |

---

## Phase 3 — Intelligence: AI/Automation & Predictive (Completed)

**Date:** March 8, 2026

### Anomaly Detection

| Feature | Status | Details |
|---------|--------|---------|
| Rule-based anomaly detector | Done | 5 rules: volume spike, add/delete cycling, suspicious timing, unusual premium, dormancy break |
| AnomalyDetectionService | Done | Per-endorsement analysis + scheduled batch analysis |
| AnomalyDetectionScheduler | Done | Configurable cron (default every 5 min), @ConditionalOnProperty |
| Anomaly API endpoints | Done | GET /anomalies, GET /anomalies/{id}, PUT /anomalies/{id}/review |
| Hook into CreateEndorsementHandler | Done | Non-blocking anomaly check after endorsement validation |

### Predictive EA Balance Forecasting

| Feature | Status | Details |
|---------|--------|---------|
| StatisticalForecastEngine | Done | Commons-math3, day-of-week + monthly seasonality (Indian hiring waves Apr/Oct) |
| BalanceForecastService | Done | 30-day projection, shortfall alerts, narrative generation |
| BalanceForecastScheduler | Done | Daily at 6 AM, iterates all active EA accounts |
| Forecast API endpoints | Done | GET /forecasts, GET /forecasts/history, POST /forecasts/generate |

### Smart Batch Optimization

| Feature | Status | Details |
|---------|--------|---------|
| ConstraintBatchOptimizer | Done | Composite scoring (urgency 60% + EA impact 40%), deletions before additions |
| BatchAssemblyScheduler integration | Done | Optimizer called before chunking, graceful fallback on failure |

### Automated Error Resolution

| Feature | Status | Details |
|---------|--------|---------|
| SimulatedErrorResolver | Done | 5 error types: date format (0.98), missing field (0.90), member ID (0.96), premium mismatch (0.85), unknown (0.3) |
| ErrorResolutionService | Done | Auto-apply at >= 95% confidence, suggest below threshold |
| Error resolution outcome tracking | Done | `trackOutcome()` hooks into ProcessEndorsementHandler at CONFIRMED/REJECTED/FAILED_PERMANENT transitions; stats enhanced with successCount, failureCount, successRate |
| ProcessEndorsementHandler integration | Done | Error resolution attempted before rejection/retry in both submit and reject paths |
| Error resolution API endpoints | Done | GET /error-resolutions, GET /stats (with outcome metrics), POST /{id}/approve |

### Process Mining

| Feature | Status | Details |
|---------|--------|---------|
| EventStreamAnalyzer | Done | Time-in-state metrics, happy path %, bottleneck detection (p95 > 2x avg) |
| ProcessMiningService | Done | Per-insurer analysis, STP rate calculation, insight generation |
| ProcessMiningScheduler | Done | Daily at 3 AM; captures daily STP rate snapshots to `stp_rate_snapshots` table |
| Process mining API endpoints | Done | GET /metrics, GET /insights, GET /stp-rate, GET /stp-rate/trend, POST /analyze |

### Phase 3 REST Endpoints

| Method | Endpoint | Status |
|--------|----------|--------|
| GET | `/api/v1/intelligence/anomalies` | Done |
| GET | `/api/v1/intelligence/anomalies/{id}` | Done |
| PUT | `/api/v1/intelligence/anomalies/{id}/review` | Done |
| GET | `/api/v1/intelligence/forecasts` | Done |
| GET | `/api/v1/intelligence/forecasts/history` | Done |
| POST | `/api/v1/intelligence/forecasts/generate` | Done |
| GET | `/api/v1/intelligence/error-resolutions` | Done |
| GET | `/api/v1/intelligence/error-resolutions/stats` | Done |
| POST | `/api/v1/intelligence/error-resolutions/{id}/approve` | Done |
| GET | `/api/v1/intelligence/process-mining/metrics` | Done |
| GET | `/api/v1/intelligence/process-mining/insights` | Done |
| GET | `/api/v1/intelligence/process-mining/stp-rate` | Done |
| GET | `/api/v1/intelligence/process-mining/stp-rate/trend` | Done |
| POST | `/api/v1/intelligence/process-mining/analyze` | Done |

### Frontend (Phase 3)

| Page | Route | Status | Features |
|------|-------|--------|----------|
| Intelligence Dashboard | `/intelligence` | Done | 4 tabs: Anomalies, Forecasts, Error Resolution, Process Mining |
| Sidebar update | — | Done | "Intelligence" nav item with Sparkles icon |

### Phase 3 Database Schema Additions

```
┌──────────────────────────┐     ┌──────────────────────────┐
│   anomaly_detections      │     │    balance_forecasts      │
│ (PK: id UUID)             │     │ (PK: id UUID)             │
│ endorsement_id, employer_id│    │ employer_id, insurer_id   │
│ anomaly_type, score        │     │ forecasted_amount         │
│ explanation, status        │     │ actual_amount, accuracy   │
│ flagged_at, reviewed_at    │     │ narrative, created_at     │
└──────────────────────────┘     └──────────────────────────┘

┌──────────────────────────┐     ┌──────────────────────────┐
│   error_resolutions       │     │ process_mining_metrics    │
│ (PK: id UUID)             │     │ (PK: id UUID)             │
│ endorsement_id, error_type│     │ insurer_id                │
│ original_value             │     │ from_status, to_status    │
│ corrected_value, resolution│     │ avg/p95/p99_duration_ms  │
│ confidence, auto_applied   │     │ sample_count              │
│ outcome, outcome_at        │     │ happy_path_pct            │
│ outcome_endorsement_status │     └──────────────────────────┘
│ created_at                 │
└──────────────────────────┘     ┌──────────────────────────┐
                                  │  stp_rate_snapshots       │
                                  │ (PK: id UUID)             │
                                  │ insurer_id, stp_rate      │
                                  │ sample_count              │
                                  │ snapshot_date, created_at │
                                  └──────────────────────────┘
```

### Observability (Phase 3)

| Feature | Status | Details |
|---------|--------|---------|
| Intelligence monitoring dashboard | Done | Grafana: anomaly rate, score distribution, forecast rate, shortfall detections, auto-resolution gauge, STP trend, lifecycle hours, batch savings, scheduler counts |
| Anomaly detection metrics | Done | endorsement.anomaly.detected (counter), endorsement.anomaly.score (summary) |
| Forecast metrics | Done | endorsement.forecast.generated (counter), endorsement.forecast.shortfall.detected (counter) |
| Error resolution metrics | Done | endorsement.error.auto_resolved (counter), endorsement.error.suggested (counter) |
| Process mining metrics | Done | endorsement.process.stp_rate (gauge), endorsement.process.avg_lifecycle_hours (gauge) |
| Batch optimization metrics | Done | endorsement.batch.optimization.savings (summary) |

### Phase 3 Testing Summary

| Category | New Tests | Updated Tests | Current Total |
|----------|-----------|---------------|---------------|
| Unit tests | 126 | 2 modified | 420 |
| API integration tests | 42 | 1 modified | 124 |
| BDD Cucumber tests | 30 scenarios | 1 modified | 92 |
| E2E Playwright tests | 30 | 1 modified | 158 |
| Performance tests | 2 simulations | — | 6 |
| **Total** | **~230** | **5 modified** | **800+** |

### Phase 3 File Count

| Category | New Files | Modified Files |
|----------|-----------|----------------|
| Domain models + enums | 6 | 1 (EndorsementEvent.java) |
| Domain ports | 9 | 1 (EAAccountRepository.java) |
| Application services | 4 | — |
| Application schedulers | 3 | 1 (BatchAssemblyScheduler.java) |
| Application handlers | — | 2 (Create + Process handlers) |
| Infrastructure intelligence | 5 | — |
| Infrastructure persistence | 12 (entity + repo + adapter) | 1 (JpaEndorsementRepositoryAdapter) |
| API controller + DTOs | 9 | — |
| DB migrations | 5 (V9-V13) | — |
| Configuration | — | 2 (application.yml, build.gradle.kts) |
| Frontend | 4 | 2 (Sidebar, routes) |
| Grafana dashboards | 1 | — |
| Unit tests | 16 | 2 |
| API tests | 4 | 1 |
| BDD tests | 9 (features + steps + helper) | 1 |
| E2E tests | 4 | 1 |
| Performance tests | 3 | — |
| Scripts | — | 1 (start.sh) |
| **Total** | **~94 new** | **~16 modified** |

---

## Deployment Strategies — Railway & AWS

### Current Deployment Options

The platform supports **3 deployment modes** today, plus a documented AWS production strategy:

| Mode | Command | Target | Use Case |
|---|---|---|---|
| Docker Compose | `./start.sh` | Local machine | Development, demo, testing |
| Kubernetes (local) | `./k8s-start.sh` | Docker Desktop K8s / minikube | K8s development, manifests validation |
| Railway (PaaS) | `git push` / Railway CLI | Railway cloud | Staging, quick cloud deployment |
| AWS (EKS) | Terraform + ArgoCD | AWS multi-region | Production (documented, not yet deployed) |

---

### Railway Deployment (Current — PaaS)

Railway is a developer-centric PaaS that deploys directly from a Dockerfile. The platform is fully configured for Railway deployment.

#### What's Already Configured

| File | Purpose |
|---|---|
| `railway.toml` | Build config: Dockerfile path, health check endpoint (`/actuator/health`), restart policy (ON_FAILURE, max 5 retries), 300s health check timeout |
| `application-railway.yml` | Spring profile: env var substitution for `DATABASE_URL`, `REDIS_URL`, `KAFKA_URL`, `PORT`; HikariCP tuned (pool 20, idle 5); production logging (INFO, Kafka/Hibernate at WARN); Flyway enabled; actuator health + metrics + prometheus exposed |
| `Dockerfile` | Multi-stage build: `eclipse-temurin:21-jdk-alpine` (build) → `eclipse-temurin:21-jre-alpine` (runtime); non-root user `plum`; virtual threads enabled; ZGC; 75% max RAM; `wget` health check |

#### Railway Service Topology

```
Railway Project: plum-endorsements
│
├── Backend Service (Spring Boot)
│   ├── Build: Dockerfile (multi-stage, temurin:21-alpine)
│   ├── Profile: SPRING_PROFILES_ACTIVE=railway
│   ├── Health: /actuator/health (300s timeout)
│   ├── Restart: ON_FAILURE (max 5 retries)
│   ├── JVM: Virtual threads + ZGC + 75% RAM
│   └── Port: $PORT (injected by Railway)
│
├── PostgreSQL Service (Railway plugin)
│   ├── Version: 16
│   ├── Connection: $DATABASE_URL (auto-injected)
│   ├── Migrations: Flyway V1-V20 (run on startup)
│   └── Pool: HikariCP (max 20, min idle 5)
│
├── Redis Service (Railway plugin)
│   ├── Version: 7
│   ├── Connection: $REDIS_URL (auto-injected)
│   └── Use: @Cacheable (InsurerRegistry), Spring session
│
├── Kafka Service (Railway plugin or private networking)
│   ├── Connection: $KAFKA_URL (auto-injected)
│   ├── Topics: 5 (endorsement-events, commands, notifications, reconciliation)
│   └── Partitions: 88 total
│
└── Frontend Service (Vite static or separate deploy)
    ├── Build: npm run build → dist/
    ├── Serve: nginx or Railway static site
    └── API proxy: /api → Backend service (via Railway networking)
```

#### Railway Deployment Flow

```
1. Developer pushes to main (or Railway-linked branch)
          │
          ▼
2. Railway detects Dockerfile, triggers build
   ├── Stage 1 (build): gradlew bootJar -x test (~2 min)
   └── Stage 2 (runtime): copy JAR, set non-root user (~30s)
          │
          ▼
3. Railway deploys container
   ├── Injects env vars: DATABASE_URL, REDIS_URL, KAFKA_URL, PORT
   ├── Sets SPRING_PROFILES_ACTIVE=railway
   └── Starts: java -jar app.jar (virtual threads, ZGC)
          │
          ▼
4. Health check loop (/actuator/health)
   ├── Initial: up to 300s for Flyway migrations + Spring context
   ├── Ongoing: health check interval
   └── Failure: restart (max 5, then alert)
          │
          ▼
5. Service is live → Railway assigns public URL
   └── Custom domain: endorsements.plum.com (via Railway DNS)
```

#### Railway Environment Variables

| Variable | Source | Example |
|---|---|---|
| `DATABASE_URL` | Railway PostgreSQL plugin | `jdbc:postgresql://...railway.app:5432/railway` |
| `REDIS_URL` | Railway Redis plugin | `redis://default:...@...railway.app:6379` |
| `KAFKA_URL` | Railway Kafka or private network | `kafka.railway.internal:9092` |
| `PORT` | Railway platform | `8080` (assigned at deploy) |
| `SPRING_PROFILES_ACTIVE` | Manual config | `railway` |

#### Railway Limitations & When to Move to AWS

| Limitation | Impact | When It Matters |
|---|---|---|
| **Single region** | No geographic redundancy | When serving users in multiple countries |
| **No multi-AZ** | Single point of failure for DB | When SLA requires > 99.9% uptime |
| **Limited Kafka** | Single broker, no replication | When processing > 10K endorsements/day |
| **No VPC peering** | Cannot connect to on-premises insurer APIs | When real insurer integrations require VPN |
| **Container resource limits** | Max 8 vCPU, 32GB RAM per service | When single-instance scaling is insufficient |
| **No canary/blue-green** | All-or-nothing deploys | When zero-downtime deployment is mandatory |
| **No HIPAA/SOC2** | Compliance limitations | When handling PII under regulatory oversight |
| **Cost at scale** | $20-50/service/month becomes expensive at 10+ services | When infrastructure cost exceeds $500/month |

**Recommendation:** Railway for MVP/demo/staging (current). Migrate to AWS when any 2+ limitations above are triggered.

---

### AWS Deployment (Production Strategy)

The AWS deployment strategy transforms the single-region Railway deployment into a production-grade, multi-AZ, auto-scaling infrastructure using managed services for every component.

#### Architecture Overview

```
AWS Account: plum-production
│
├── VPC: 10.0.0.0/16 (ap-south-1, Mumbai)
│   ├── Public Subnets (3 AZs)
│   │   ├── 10.0.1.0/24 (ap-south-1a) — ALB, NAT Gateway
│   │   ├── 10.0.2.0/24 (ap-south-1b) — ALB, NAT Gateway
│   │   └── 10.0.3.0/24 (ap-south-1c) — ALB (standby)
│   │
│   ├── Private Subnets — Application (3 AZs)
│   │   ├── 10.0.11.0/24 (1a) — EKS worker nodes
│   │   ├── 10.0.12.0/24 (1b) — EKS worker nodes
│   │   └── 10.0.13.0/24 (1c) — EKS worker nodes
│   │
│   └── Private Subnets — Data (3 AZs)
│       ├── 10.0.21.0/24 (1a) — RDS primary, ElastiCache, MSK broker 1
│       ├── 10.0.22.0/24 (1b) — RDS standby, ElastiCache, MSK broker 2
│       └── 10.0.23.0/24 (1c) — ElastiCache, MSK broker 3
│
├── EKS Cluster: plum-endorsements
│   ├── Control Plane: AWS-managed (multi-AZ)
│   ├── Node Group: m6i.xlarge (4 vCPU, 16GB)
│   │   ├── Min: 2 nodes, Max: 8 nodes (Cluster Autoscaler)
│   │   └── Spot instances for non-critical workloads (30-40% savings)
│   ├── Backend Deployment
│   │   ├── Replicas: 3 (HPA: min 3, max 10)
│   │   ├── CPU request: 500m, limit: 2000m
│   │   ├── Memory request: 1Gi, limit: 2Gi
│   │   ├── Readiness: /actuator/health (30s initial, 10s period)
│   │   ├── Liveness: /actuator/health (60s initial, 15s period)
│   │   └── PDB: minAvailable 2 (always at least 2 pods running)
│   └── Frontend Deployment (nginx serving static build)
│       ├── Replicas: 2 (HPA: min 2, max 5)
│       └── Served via CloudFront CDN
│
├── RDS PostgreSQL 16 (Multi-AZ)
│   ├── Instance: db.r6g.xlarge (4 vCPU, 32GB)
│   ├── Storage: gp3 (100GB, 3000 IOPS, auto-scaling to 500GB)
│   ├── Multi-AZ: Synchronous standby in different AZ
│   ├── Backup: Automated daily (35-day retention) + PITR
│   ├── Encryption: AES-256 at rest (AWS KMS)
│   ├── Connection: via pgBouncer sidecar or RDS Proxy
│   │   ├── Max connections: 200 (managed by RDS Proxy)
│   │   └── IAM authentication (no password in config)
│   └── Parameter Group:
│       ├── shared_buffers: 8GB (25% of RAM)
│       ├── effective_cache_size: 24GB (75%)
│       ├── work_mem: 64MB
│       └── max_wal_size: 4GB
│
├── Amazon MSK (Kafka)
│   ├── Brokers: 3 (kafka.m5.large, one per AZ)
│   ├── Storage: 100GB per broker (auto-scaling)
│   ├── Topics: 5 (matching current configuration)
│   │   ├── endorsement-events: 32 partitions, RF=3
│   │   ├── endorsement-commands: 32 partitions, RF=3
│   │   ├── endorsement-notifications: 8 partitions, RF=3
│   │   ├── endorsement-reconciliation: 16 partitions, RF=3
│   │   └── endorsement-deadletter: 8 partitions, RF=3
│   ├── Replication factor: 3 (data on all 3 brokers)
│   ├── min.insync.replicas: 2
│   ├── Encryption: TLS in transit + KMS at rest
│   └── Monitoring: MSK metrics → Prometheus via MSK exporter
│
├── ElastiCache Redis 7 (Cluster Mode)
│   ├── Node type: cache.r6g.large (2 vCPU, 13GB)
│   ├── Cluster: 1 shard, 2 replicas (multi-AZ)
│   ├── Use: @Cacheable (InsurerRegistry, 60s TTL), ShedLock
│   ├── Encryption: in-transit (TLS) + at-rest (KMS)
│   └── Failover: automatic to replica in < 30s
│
├── OpenSearch Service (ELK replacement)
│   ├── Instance: r6g.large.search (3 nodes)
│   ├── Storage: 200GB gp3 per node
│   ├── Use: structured log aggregation (replaces Elasticsearch + Kibana)
│   ├── Fine-grained access control: IAM-based
│   └── Dashboard: OpenSearch Dashboards (replaces Kibana)
│
├── Application Load Balancer (ALB)
│   ├── Target Groups:
│   │   ├── /api/* → EKS backend service (port 8080)
│   │   ├── /ws/* → EKS backend (WebSocket upgrade)
│   │   └── /* → CloudFront (frontend static assets)
│   ├── SSL: ACM certificate (*.plum-endorsements.com)
│   ├── WAF: AWS WAF v2 (OWASP top 10 rules, rate limiting)
│   └── Access Logs: S3 bucket (90-day retention)
│
├── CloudFront CDN
│   ├── Origin: S3 bucket (frontend build artifacts)
│   ├── Edge locations: India, Singapore, UAE
│   ├── Cache policy: 24h for static assets, no-cache for index.html
│   └── SSL: ACM certificate (edge-optimized)
│
├── Route 53
│   ├── endorsements.plum.com → ALB (ap-south-1)
│   ├── api.endorsements.plum.com → ALB (latency-based)
│   └── Health checks: /actuator/health (30s interval, 3 failures → failover)
│
├── Monitoring & Observability
│   ├── Amazon Managed Prometheus (AMP)
│   │   └── Scrapes /actuator/prometheus from all EKS pods
│   ├── Amazon Managed Grafana (AMG)
│   │   ├── 7 dashboards (imported from existing JSON files)
│   │   └── Data source: AMP + CloudWatch + MSK metrics
│   ├── AWS X-Ray (or Jaeger on EKS)
│   │   └── OpenTelemetry OTLP → X-Ray collector
│   ├── CloudWatch
│   │   ├── EKS control plane logs
│   │   ├── RDS performance insights
│   │   ├── MSK broker metrics
│   │   └── ALB access logs + 5xx alarms
│   └── CloudWatch Alarms
│       ├── RDS CPU > 80% → SNS → PagerDuty
│       ├── EKS pod restart count > 3 → SNS → Slack
│       ├── MSK under-replicated partitions > 0 → SNS → PagerDuty
│       ├── ALB 5xx rate > 1% → SNS → Slack
│       └── Redis evictions > 0 → SNS → Slack
│
└── Security
    ├── IAM Roles for Service Accounts (IRSA)
    │   └── EKS pods assume IAM roles (no AWS keys in config)
    ├── Secrets Manager
    │   ├── RDS credentials (auto-rotated every 30 days)
    │   ├── Redis auth token
    │   └── MSK SASL credentials
    ├── KMS
    │   ├── RDS encryption key
    │   ├── MSK encryption key
    │   ├── S3 encryption key
    │   └── ElastiCache encryption key
    ├── Security Groups
    │   ├── ALB: 443 inbound from 0.0.0.0/0
    │   ├── EKS nodes: 8080 from ALB SG only
    │   ├── RDS: 5432 from EKS SG only
    │   ├── MSK: 9092 from EKS SG only
    │   ├── Redis: 6379 from EKS SG only
    │   └── OpenSearch: 443 from EKS SG only
    └── VPC Endpoints (no internet traversal for AWS services)
        ├── S3 gateway endpoint
        ├── ECR interface endpoints (dkr + api)
        ├── STS interface endpoint
        └── Secrets Manager interface endpoint
```

#### Infrastructure-as-Code (Terraform)

```
terraform/
├── environments/
│   ├── staging/
│   │   ├── main.tf          — Module invocations with staging-specific vars
│   │   ├── variables.tf     — Smaller instances, single-AZ where allowed
│   │   └── terraform.tfvars — instance_type="db.r6g.large", min_replicas=2
│   └── production/
│       ├── main.tf          — Module invocations with production-specific vars
│       ├── variables.tf     — Multi-AZ, larger instances, HA configuration
│       └── terraform.tfvars — instance_type="db.r6g.xlarge", min_replicas=3
│
├── modules/
│   ├── vpc/
│   │   ├── main.tf          — VPC, subnets (public/private/data), NAT GW, route tables
│   │   ├── variables.tf     — CIDR blocks, AZ count
│   │   └── outputs.tf       — vpc_id, subnet_ids, security_group_ids
│   │
│   ├── eks/
│   │   ├── main.tf          — EKS cluster, node group, IRSA, OIDC provider
│   │   ├── variables.tf     — node_instance_type, min/max/desired nodes
│   │   └── outputs.tf       — cluster_endpoint, cluster_ca, kubeconfig
│   │
│   ├── rds/
│   │   ├── main.tf          — RDS PostgreSQL 16, Multi-AZ, parameter group, subnet group
│   │   ├── variables.tf     — instance_class, storage, backup_retention
│   │   └── outputs.tf       — endpoint, port, secret_arn
│   │
│   ├── msk/
│   │   ├── main.tf          — MSK cluster, 3 brokers, topic auto-creation
│   │   ├── variables.tf     — broker_instance_type, ebs_volume_size
│   │   └── outputs.tf       — bootstrap_brokers, zookeeper_connect
│   │
│   ├── elasticache/
│   │   ├── main.tf          — ElastiCache Redis cluster, multi-AZ replication
│   │   ├── variables.tf     — node_type, num_cache_clusters
│   │   └── outputs.tf       — primary_endpoint, reader_endpoint
│   │
│   ├── opensearch/
│   │   ├── main.tf          — OpenSearch domain, fine-grained access control
│   │   ├── variables.tf     — instance_type, instance_count, volume_size
│   │   └── outputs.tf       — domain_endpoint, dashboard_endpoint
│   │
│   ├── alb/
│   │   ├── main.tf          — ALB, target groups, listener rules, WAF association
│   │   ├── variables.tf     — certificate_arn, waf_acl_arn
│   │   └── outputs.tf       — alb_dns_name, alb_zone_id
│   │
│   ├── cloudfront/
│   │   ├── main.tf          — CloudFront distribution, S3 origin, cache policy
│   │   └── outputs.tf       — distribution_domain_name
│   │
│   ├── monitoring/
│   │   ├── main.tf          — AMP workspace, AMG workspace, CloudWatch alarms
│   │   └── outputs.tf       — prometheus_endpoint, grafana_url
│   │
│   └── security/
│       ├── main.tf          — KMS keys, Secrets Manager secrets, IAM roles
│       └── outputs.tf       — kms_key_arns, secret_arns, role_arns
│
└── backend.tf               — S3 + DynamoDB state backend (locking)
```

**Terraform execution strategy:**

```bash
# Initialize (once per environment)
cd terraform/environments/production
terraform init

# Plan (review changes before applying)
terraform plan -out=tfplan

# Apply (create/update infrastructure)
terraform apply tfplan

# Estimated apply time: ~25 min (RDS: 10 min, MSK: 15 min, EKS: 10 min — parallel)
```

#### Managed Services Mapping

| Current (Docker Compose / K8s local) | AWS Managed Service | Key Differences |
|---|---|---|
| PostgreSQL 16 (Docker container) | **Amazon RDS PostgreSQL 16** | Multi-AZ, automated backups (PITR), IAM auth, Performance Insights, auto-scaling storage |
| Redis 7 (Docker container) | **Amazon ElastiCache Redis 7** | Cluster mode, multi-AZ replication, automatic failover in < 30s, encryption at rest + in transit |
| Apache Kafka 3.7 (Docker container) | **Amazon MSK** | 3 brokers (multi-AZ), managed ZooKeeper-less (KRaft), auto-scaling storage, TLS + SASL auth |
| Elasticsearch 8.12 (Docker container) | **Amazon OpenSearch Service** | Managed Elasticsearch-compatible, fine-grained access, dashboards built-in, auto-scaling |
| Logstash (Docker container) | **Fluent Bit sidecar** or **Firehose** | Lighter-weight log forwarding; Logstash pipeline config maps to Fluent Bit parsers |
| Kibana (Docker container) | **OpenSearch Dashboards** | Built into OpenSearch Service, no separate deployment |
| Prometheus (Docker container) | **Amazon Managed Prometheus (AMP)** | HA, long-term storage, integrated with EKS; existing `prometheus.yml` scrape configs reusable |
| Grafana (Docker container) | **Amazon Managed Grafana (AMG)** | SSO integration, managed upgrades; existing 7 dashboard JSON files importable directly |
| Jaeger (Docker container) | **AWS X-Ray** or **Jaeger on EKS** | X-Ray: zero-infra, IAM-based; Jaeger on EKS: full compatibility with existing OTLP export |
| Spring Boot (Docker container) | **EKS Pod** | Same Dockerfile, same JVM flags; K8s manifests from `k8s/` apply with minimal changes |
| React frontend (Vite dev server) | **S3 + CloudFront** | `npm run build` → upload to S3 → CloudFront CDN with edge caching; API calls via ALB |

#### Application Configuration Per Environment

```yaml
# application-staging.yml (new file to create)
spring:
  datasource:
    url: jdbc:postgresql://${RDS_ENDPOINT}:5432/endorsements
    username: ${RDS_USERNAME}    # from Secrets Manager
    password: ${RDS_PASSWORD}    # from Secrets Manager
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
  data:
    redis:
      url: rediss://${ELASTICACHE_ENDPOINT}:6379  # rediss:// for TLS
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
      sasl.jaas.config: >
        org.apache.kafka.common.security.scram.ScramLoginModule required
        username="${MSK_USERNAME}" password="${MSK_PASSWORD}";

server:
  port: 8080
  shutdown: graceful

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  tracing:
    sampling:
      probability: 0.1  # 10% sampling in staging (cost control)

logging:
  level:
    root: INFO
    com.plum: INFO
    org.springframework.kafka: WARN
```

```yaml
# application-production.yml (new file to create)
spring:
  datasource:
    url: jdbc:postgresql://${RDS_ENDPOINT}:5432/endorsements
    username: ${RDS_USERNAME}
    password: ${RDS_PASSWORD}
    hikari:
      maximum-pool-size: 50     # Higher for production load
      minimum-idle: 10
      connection-timeout: 5000
      leak-detection-threshold: 30000
  data:
    redis:
      url: rediss://${ELASTICACHE_ENDPOINT}:6379
      timeout: 2s
  kafka:
    bootstrap-servers: ${MSK_BOOTSTRAP_SERVERS}
    producer:
      acks: all               # Already configured
      retries: 3
    properties:
      security.protocol: SASL_SSL
      sasl.mechanism: SCRAM-SHA-512
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50       # Larger batches for production throughput
          fetch_size: 100

server:
  port: 8080
  shutdown: graceful
  tomcat:
    threads:
      max: 200
    accept-count: 100

management:
  tracing:
    sampling:
      probability: 0.01  # 1% sampling in production (cost control)

endorsement:
  rate-limit:
    enabled: true
    requests-per-second: 100
    burst-size: 200
  intelligence:
    anomaly-detection:
      schedule-cron: "0 */5 * * * *"
    balance-forecast:
      schedule-cron: "0 0 6 * * *"
    process-mining:
      schedule-cron: "0 0 3 * * *"

logging:
  level:
    root: WARN
    com.plum: INFO
```

#### Auto-Scaling Strategy

```
EKS Pod Scaling (Horizontal Pod Autoscaler):
├── Backend
│   ├── Min replicas: 3 (always across 3 AZs)
│   ├── Max replicas: 10
│   ├── Scale-up trigger: CPU > 70% avg across pods (3 min window)
│   ├── Scale-down trigger: CPU < 30% avg (10 min stabilization)
│   └── Custom metric: endorsement.processing.queue.size > 100 → scale up
│
├── EKS Node Scaling (Cluster Autoscaler):
│   ├── Min nodes: 2 (cost baseline)
│   ├── Max nodes: 8 (capacity ceiling)
│   ├── Scale-up: when pod pending due to insufficient resources
│   ├── Scale-down: when node utilization < 50% for 10 min
│   └── Spot instances: 50% of nodes (with on-demand fallback)
│
├── RDS Scaling:
│   ├── Storage: auto-scaling (100GB → 500GB, gp3)
│   ├── Read replicas: add when read IOPS > 5000 sustained
│   └── Instance: manual upgrade (db.r6g.xlarge → db.r6g.2xlarge) during maintenance window
│
├── MSK Scaling:
│   ├── Storage: auto-scaling per broker (100GB → 1TB)
│   ├── Brokers: manual (3 → 6) if partition lag > threshold
│   └── Topic partitions: increase via admin CLI if consumer lag grows
│
└── ElastiCache Scaling:
    ├── Read replicas: 1 → 2 if read ops > 50K/sec
    └── Node type: upgrade during maintenance window if memory > 80%
```

#### Migration Strategy — Railway to AWS

```
Phase 1: Parallel Infrastructure (Week 1-2)
├── Stand up AWS infrastructure via Terraform (RDS, MSK, ElastiCache, EKS)
├── Configure application-staging.yml for AWS endpoints
├── Deploy backend to EKS staging namespace
├── Verify: health check, Flyway migrations, Kafka connectivity
└── Run full test suite against AWS staging

Phase 2: Data Migration (Week 3)
├── Export PostgreSQL data from Railway: pg_dump --format=custom
├── Import to RDS: pg_restore --no-owner --no-acl
├── Verify row counts match across all 18 tables
├── Verify Flyway schema version matches (V20)
└── Verify EA account balances are consistent

Phase 3: Traffic Cutover (Week 4)
├── DNS: Update Route 53 to point to AWS ALB
│   ├── Weighted routing: 10% AWS, 90% Railway (canary)
│   ├── Monitor: error rates, latency, database connections
│   ├── If stable for 24h: 50/50 split
│   ├── If stable for 48h: 100% AWS
│   └── If issues: revert to 100% Railway (< 5 min via DNS)
├── Kafka: Switch producer/consumer to MSK
│   ├── Drain Railway Kafka consumers (process remaining messages)
│   └── No data loss: consumer offsets tracked per partition
└── Redis: Cache is ephemeral — no migration needed
    └── InsurerRegistry cache rebuilds automatically on first access

Phase 4: Decommission Railway (Week 5)
├── Verify zero traffic to Railway (check Railway metrics)
├── Final pg_dump from Railway as backup
├── Delete Railway services
├── Archive Railway project
└── Update documentation and runbooks
```

#### Cost Estimation (Monthly)

| Service | Staging | Production | Notes |
|---|---|---|---|
| EKS control plane | $73 | $73 | Fixed cost per cluster |
| EKS nodes (2-8 m6i.xlarge) | $140 | $420 | 50% spot for staging |
| RDS PostgreSQL (r6g.xlarge, Multi-AZ) | $280 | $560 | Multi-AZ doubles cost |
| MSK (3 kafka.m5.large brokers) | $250 | $250 | Same config staging/prod |
| ElastiCache Redis (r6g.large, 2 nodes) | $180 | $180 | Cluster mode |
| OpenSearch (3 r6g.large.search) | $300 | $300 | 3-node cluster |
| ALB | $20 | $40 | Per-request pricing |
| CloudFront | $5 | $20 | India edge locations |
| S3 (frontend + backups) | $5 | $10 | Minimal storage |
| Route 53 | $1 | $1 | Per hosted zone |
| Secrets Manager | $2 | $2 | Per secret per month |
| CloudWatch / AMP / AMG | $30 | $60 | Metrics + logs + dashboards |
| NAT Gateway (2 AZs) | $65 | $130 | Data processing charges |
| **Total** | **~$1,350/mo** | **~$2,050/mo** | |

**Cost optimization opportunities:**
- Spot instances for EKS worker nodes: 30-40% savings on compute
- Reserved instances for RDS and ElastiCache: 30-50% savings on 1-year commitment
- S3 Intelligent-Tiering for log archives: automatic cost reduction for infrequently accessed data
- MSK serverless (when available in region): pay-per-partition-hour instead of fixed broker cost

#### Disaster Recovery

| Scenario | RTO | RPO | Strategy |
|---|---|---|---|
| Single pod crash | < 30s | 0 | K8s auto-restarts; HPA replaces; PDB ensures min 2 pods |
| Single AZ failure | < 5 min | 0 | RDS Multi-AZ failover; EKS pods reschedule to other AZs; ElastiCache replica promotion |
| Region failure | < 1 hour | < 5 min | Cross-region RDS read replica promotion; MSK MirrorMaker; DNS failover via Route 53 health checks |
| Database corruption | < 30 min | < 5 min | RDS Point-in-Time Recovery to any second within backup window; Flyway re-runs migrations |
| Application rollback | < 2 min | 0 | ArgoCD reverts to previous K8s manifest version; previous Docker image available in ECR |

---

## What's NOT Yet Implemented (Phase 4+)

| Area | Current State | Future Plan |
|------|--------------|-------------|
| Authentication | Open (permitAll) — strategy documented | JWT + OAuth2 + RBAC (5 roles) — see [Product Evolution Vision §9](deliverables/vision/Product_Evolution_Vision.md) |
| Notifications | Logging only (`LoggingNotificationAdapter`) | Email (SES/SendGrid), webhook, Slack/Teams adapters behind `NotificationPort` |
| Full-text search | Filter by employerId + status only | Elasticsearch for employee name, policy search |
| CI/CD pipeline | Manual deploy (`./gradlew`, `./run-all-tests.sh`) | Harness or Jenkins pipeline with ArgoCD GitOps — see [Product Evolution Vision §3.9](deliverables/vision/Product_Evolution_Vision.md#39-cicd-pipeline--harness--jenkins) |
| Test coverage reporting | No coverage tool | JaCoCo (80% line, 70% branch) + SonarQube quality gates |
| File attachments | Not supported | S3 for supporting documents |
| Multi-currency | Single currency (INR) | ISO 4217 currency fields with defaults — see [Product Evolution Vision §3.2](deliverables/vision/Product_Evolution_Vision.md) |
| Regulatory compliance | No country-specific rules | `CompliancePort` + per-country adapters (IRDAI, DHIC, MAS) |
| Self-service onboarding | Manual insurer setup | API spec parser + field mapping UI + test sandbox |
| Multi-region deployment | Single-region (Railway) | AWS multi-region (Mumbai, Singapore, Bahrain) |

**Previously NOT implemented, now DONE:**

| Area | Status | Details |
|------|--------|---------|
| Rate limiting | Done | IP-based `RateLimitingFilter` with sliding window |
| Audit logging | Done | `AuditLoggingAspect` + `audit_logs` table + `GET /api/v1/audit-logs` API |
| WebSocket config | Done | `WebSocketConfig` + `WebSocketEventBroadcaster` for real-time event broadcast |
| Scheduler coordination | Done | ShedLock table (V16) + `ShedLockConfig` for distributed locking |
| Data archival tables | Done | Archive tables for endorsements, transactions, events (V15) |

---

## Phase 4 — CI/CD Pipeline & GitOps (Next Priority)

> **Status:** NOT IMPLEMENTED — documented in [Product Evolution Vision §3.9](deliverables/vision/Product_Evolution_Vision.md#39-cicd-pipeline--harness--jenkins)
> **Effort:** 2 weeks (10 days)
> **Priority:** P0 — prerequisite for every other Phase 4 capability

### Why CI/CD First

Every remaining Phase 4 capability (multi-currency, localized insurers, compliance, mobile app) produces new code that needs to be compiled, tested, scanned, containerized, and deployed. Without a CI/CD pipeline, each capability introduces risk of broken builds merging to `main`, untested code reaching production, and unscanned dependencies introducing vulnerabilities. CI/CD is the foundation that makes all subsequent work safe.

### Current State (Manual)

```
Developer Workflow Today:
├── Code → local `./gradlew test` (developer may forget)
├── Merge to main → no automated gate (broken code can merge)
├── Deploy → manual `docker build` + `docker-compose up` or Railway CLI
├── Test → manual `./run-all-tests.sh` (takes 18 min, often skipped)
├── Security → no scanning (OWASP CVEs may be present)
└── Rollback → manual investigation + manual revert
```

### Target State (Automated)

```
CI/CD Pipeline (11 stages, ~18 min, fully automated):

PR / Push → Compile → ┬─ Unit Tests ────────┬→ Quality Gate → Docker Build
                       ├─ API Tests          │                      │
                       ├─ BDD Tests          │                      ▼
                       ├─ SonarQube SAST     │              Deploy Staging
                       ├─ OWASP Dep Check    │                      │
                       └─ Trivy Container ───┘                      ▼
                                                      ┬─ Smoke Tests
                                                      ├─ E2E Tests (Playwright)
                                                      └─ Perf Tests (Gatling)
                                                               │
                                                               ▼
                                                     Canary Deploy (10%)
                                                     Automated Verification
                                                               │
                                                               ▼
                                                    Manual Approval Gate
                                                               │
                                                               ▼
                                                     Production Rolling Deploy
```

### Pipeline Tool Options

Both tools are fully documented with ready-to-use pipeline definitions:

| Tool | Pipeline File | Best For |
|---|---|---|
| **Harness** (recommended) | `.harness/pipelines/endorsement-pipeline.yaml` | Cloud-native teams; zero infrastructure maintenance; built-in test intelligence, canary verification, security scanning |
| **Jenkins** | `Jenkinsfile` at repo root | On-premises teams; existing Jenkins infrastructure; maximum customization; extensive plugin ecosystem |

### ArgoCD GitOps Integration

The existing `k8s/` directory (17 YAML files) is ready for GitOps adoption:

```
GitOps Deployment Flow:
├── CI pipeline updates k8s/backend/deployment.yaml with new image tag
├── ArgoCD detects manifest change in git (polling or webhook)
├── ArgoCD syncs desired state to target K8s cluster
├── ArgoCD verifies health (readinessProbe + livenessProbe)
├── If unhealthy → ArgoCD auto-reverts to previous known-good manifest
└── Dashboard: https://argocd.internal — shows sync status, resource tree, diff view

Existing K8s Resources (GitOps-ready):
├── k8s/namespace.yaml               — plum namespace
├── k8s/backend/deployment.yaml      — Backend (image tag updated by CI)
├── k8s/backend/service.yaml         — ClusterIP service
├── k8s/backend/configmap.yaml       — Environment-specific config
├── k8s/postgres/                    — PostgreSQL (ConfigMap, Secret, PVC, Deployment, Service)
├── k8s/kafka/                       — Kafka KRaft (ConfigMap, Deployment, Service)
├── k8s/redis/                       — Redis (Deployment, Service)
├── k8s/elasticsearch/               — Elasticsearch (Deployment, PVC, Service)
├── k8s/prometheus/                  — Prometheus (ConfigMap, Deployment, Service)
├── k8s/grafana/                     — Grafana (ConfigMap, Deployment, Service)
├── k8s/jaeger/                      — Jaeger (Deployment, Service)
├── k8s/logstash/                    — Logstash (ConfigMap, Deployment, Service)
├── k8s/kibana/                      — Kibana (Deployment, Service)
└── k8s/seed-job.yaml                — Data seeding Job
```

**ArgoCD Application manifest:**

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: plum-endorsements
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/org/plum-endorsements.git
    targetRevision: main
    path: k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: plum-production
  syncPolicy:
    automated:
      prune: true        # Delete resources removed from git
      selfHeal: true      # Revert manual kubectl changes
    syncOptions:
      - CreateNamespace=true
    retry:
      limit: 3
      backoff:
        duration: 5s
        maxDuration: 3m
```

### Quality Gates

| Gate | Threshold | Blocks Merge? |
|---|---|---|
| Unit tests | 100% pass rate (420 tests) | Yes |
| API tests | 100% pass rate (124 tests) | Yes |
| BDD tests | 100% pass rate (92 tests) | Yes |
| Line coverage | >= 80% (JaCoCo) | Yes |
| Branch coverage | >= 70% (JaCoCo) | Yes |
| SonarQube | No new critical/blocker issues | Yes |
| OWASP Dependency-Check | No HIGH/CRITICAL CVEs | Yes |
| Trivy container scan | No CRITICAL vulnerabilities | Yes |
| E2E tests | 100% pass rate (158 tests) | Blocks staging → production |
| P95 response time | < 500ms (Gatling) | Alerts only |
| Error rate | < 1% (Gatling) | Blocks staging → production |

### Environment Promotion

| Environment | Trigger | Infrastructure | Tests Run |
|---|---|---|---|
| **Dev** | PR merge to feature branch | Docker Compose (local) or K8s namespace `plum-dev` | Unit + API + BDD |
| **Staging** | Push to `main` | K8s namespace `plum-staging` (RDS, MSK single-broker, ElastiCache) | + E2E + Performance + Smoke |
| **Canary** | After staging tests pass | 10% of production traffic for 10 min | Automated Prometheus metric verification |
| **Production** | Manual approval by ops/engineering lead | K8s namespace `plum-production` (RDS Multi-AZ, MSK 3-broker, ElastiCache cluster) | Post-deploy smoke |

### Implementation Checklist

| # | Task | Effort | Status |
|---|---|---|---|
| 1 | Add JaCoCo plugin to `build.gradle.kts` | 0.5 day | Not started |
| 2 | Add OWASP Dependency-Check plugin to `build.gradle.kts` | 0.5 day | Not started |
| 3 | Create SonarQube project + configure quality gates | 1 day | Not started |
| 4 | Write Jenkinsfile (declarative pipeline) | 1 day | Not started |
| 5 | Write Harness pipeline YAML | 1 day | Not started |
| 6 | Set up Docker registry (ECR or Harbor) | 0.5 day | Not started |
| 7 | Configure ArgoCD Application for `k8s/` manifests | 0.5 day | Not started |
| 8 | Create `application-staging.yml` + `application-production.yml` | 1 day | Not started |
| 9 | Set up Slack/Teams webhook for build notifications | 0.5 day | Not started |
| 10 | Test pipeline end-to-end (compile → deploy → verify) | 2 days | Not started |
| 11 | Write pipeline runbook + troubleshooting guide | 1 day | Not started |
| | **Total** | **~10 days** | |

---

## Next Items to Pick Up (Priority Order)

The following items are listed in recommended execution order based on impact, dependencies, and effort:

| Priority | Item | Effort | Rationale |
|---|---|---|---|
| **1 (Now)** | **CI/CD Pipeline (Harness or Jenkins) + ArgoCD** | 2 weeks | P0 blocker — every subsequent capability needs automated testing and deployment; currently all manual |
| **2** | **JaCoCo + SonarQube Quality Gates** | 1 day | Part of CI/CD — enforce 80% coverage and static analysis before any new feature code |
| **3** | **Complete Advanced Analytics (100%)** | 3 days | 70% → 100%, LOW friction — trend analysis, employer segmentation, alert thresholds; all data already exists in DB |
| **4** | **Multi-Currency EA Support** | 4 days | MEDIUM friction — schema migration with defaults, currency validation, DTO extension; zero test breakage |
| **5** | **Localized Insurer Integrations** | 3 days | LOW-MEDIUM — add `country` field, registry filter, seed regional variants; zero routing changes |
| **6** | **IRDAI Compliance Framework** | 5 days | MEDIUM — new `CompliancePort` + adapter; follows established Strategy pattern; config-driven rules |
| **7** | **Mobile App (PWA)** | 2-3 weeks | MEDIUM — manifest, service worker, push notifications, offline queue; 95% code reuse from React frontend |
| **8** | **Authentication & Authorization** | 3 weeks | HIGH — JWT + OAuth2 + RBAC; feature-flagged (`auth.enabled: false`) for zero-disruption migration |
| **9** | **UAE + Singapore Compliance Adapters** | 4 weeks | MEDIUM — follow IRDAI pattern; new adapters, no existing code changes |
| **10** | **Self-Service Insurer Onboarding** | 8-10 weeks | HIGH — API spec parser, field mapping engine/UI, test sandbox, go-live checklist |
| **11** | **Platform API Marketplace** | 16-20 weeks | VERY HIGH — HRIS adapters (Darwinbox, Keka, greytHR), partner API, rate limiting |
| **12** | **Multi-Region AWS Deployment** | 8-12 weeks | VERY HIGH — infrastructure only; app is already cloud-native; defer to last |

### Immediate Action Plan (Weeks 1-2)

```
Week 1:
├── Day 1:  Add JaCoCo + OWASP plugins to build.gradle.kts
│           Create SonarQube project, configure quality profile
├── Day 2:  Write Jenkinsfile OR Harness pipeline YAML
│           Configure parallel test stages (unit, API, BDD)
├── Day 3:  Set up Docker registry (ECR/Harbor)
│           Write Docker build + push stage
├── Day 4:  Configure ArgoCD Application for k8s/ directory
│           Set up staging K8s namespace
├── Day 5:  Create application-staging.yml + application-production.yml
│           Configure Slack/Teams build notifications

Week 2:
├── Day 6-7: End-to-end pipeline testing
│            Fix any Testcontainers Docker-in-Docker issues
│            Verify quality gate thresholds block bad merges
├── Day 8:  Configure canary deployment + Prometheus verification
│           Set up manual approval gate for production
├── Day 9:  Write pipeline runbook + troubleshooting guide
│           Document rollback procedures
├── Day 10: Final validation — merge a test PR through full pipeline
│           Verify Allure, JaCoCo, SonarQube, Gatling reports generate
```

After CI/CD is operational, proceed to Item #3 (Complete Advanced Analytics) which takes only 3 days and brings the analytics capability from 70% to 100% with zero existing test breakage.
