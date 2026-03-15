# Plum Endorsement Management System

A production-grade endorsement management platform for group health insurance, built with **Hexagonal Architecture**, **Cloud-Native Patterns**, and **GenAI augmentation via Ollama**.

When an employer adds or removes employees mid-policy, the insurer needs an "endorsement" — a change to the group policy. At scale, this creates four hard problems: coverage gaps during insurer processing, financial drain from unoptimized EA (Endorsement Account) floats, multi-insurer protocol chaos, and invisible failures. This system solves all four.

---

## Key Numbers

| Metric | Value |
|--------|-------|
| REST Endpoints | 27 across 6 controllers |
| Insurer Integrations | 4 (ICICI Lombard, Niva Bupa, Bajaj Allianz, Mock) |
| Database Tables | 13 (PostgreSQL 16, Flyway-managed) |
| Kafka Topics | 4 topics, 88 partitions |
| AI/Intelligence Modules | 5 pillars, 2 with Ollama GenAI |
| Grafana Dashboards | 7 auto-provisioned |
| Custom Metrics | 40+ (Prometheus/Micrometer) |
| Test Count | 800+ (Unit + API + BDD + E2E + Performance) |
| Docker Services | 9 (+ optional Ollama) |

---

## Table of Contents

- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Endorsement Lifecycle](#endorsement-lifecycle)
- [Key Features](#key-features)
- [Intelligence & AI](#intelligence--ai)
- [Multi-Insurer Integration](#multi-insurer-integration)
- [Observability](#observability)
- [Testing](#testing)
- [API Documentation](#api-documentation)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Configuration](#configuration)
- [Documentation](#documentation)

---

## Architecture

The system follows **Hexagonal Architecture** (Ports & Adapters) with design principles from two foundational texts:
- **Cloud Native Patterns** (Cornelia Davis, Manning) — event-driven, stateless, resilient, observable
- **Head First Design Patterns** (Freeman & Robson, O'Reilly) — Strategy, State, Observer, Adapter, Factory, CQRS

```
                     ┌────────────────────────────────────────────────┐
                     │              API Layer                         │
                     │   6 Controllers  ·  27 Endpoints  ·  RFC 7807 │
                     └──────────────────┬─────────────────────────────┘
                                        │
                     ┌──────────────────▼─────────────────────────────┐
                     │           Application Layer                    │
                     │   3 CQRS Handlers  ·  8 Services  ·  9 Schedulers  │
                     │   Stateless  ·  @Transactional  ·  MDC  ·  Metrics │
                     └──────────────────┬─────────────────────────────┘
                                        │
        ┌───────────────────────────────▼──────────────────────────────┐
        │                       DOMAIN CORE                            │
        │   Endorsement (11-state)  ·  EAAccount  ·  18 Port interfaces │
        │   EndorsementEvent sealed (24 types)  ·  StateMachine         │
        │              >>> ZERO infrastructure imports <<<              │
        └───────────────────────────────┬──────────────────────────────┘
                                        │
  ┌─────────────────────────────────────▼─────────────────────────────────┐
  │                       Infrastructure Layer                            │
  │   JPA: 10 adapters + mappers       Insurer: Mock · ICICI · Niva · Bajaj │
  │   Kafka: 4 topics, 88 partitions   Intelligence: 5 rule-based          │
  │   Resilience: CB + Retry           + OllamaAnomalyDetector             │
  │                                    + OllamaErrorResolver               │
  └───────────────────────────────────────────────────────────────────────┘
```

### Design Patterns in Use

| Pattern | Application |
|---------|-------------|
| **Strategy** | `InsurerPort` — add a new insurer with one class + one DB row |
| **State** | 11-state endorsement lifecycle with `canTransitionTo()` compile-time safety |
| **Observer** | `EventPublisher` → Kafka → downstream consumers |
| **Factory** | `InsurerRouter.resolve()` discovers adapters from DB config |
| **Adapter** | Every infrastructure integration behind a domain port interface |
| **CQRS** | Command handlers (write) separated from query handlers (read-only) |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 21 (Virtual Threads) |
| **Framework** | Spring Boot 3.4 |
| **Database** | PostgreSQL 16 (ACID, optimistic locking, Flyway migrations) |
| **Cache** | Redis 7 (distributed, 60s TTL via `@Cacheable`) |
| **Messaging** | Apache Kafka (KRaft mode, employer-ID partitioning, `acks=all`) |
| **Resilience** | Resilience4j (per-insurer circuit breakers + retry with exponential backoff) |
| **AI/LLM** | Ollama (llama3.2, Spring AI starter) |
| **Frontend** | React 19, TanStack Table, shadcn/ui, Vite |
| **Tracing** | OpenTelemetry + Jaeger (100% sampling) |
| **Metrics** | Micrometer + Prometheus + Grafana (7 dashboards) |
| **Logging** | Logstash JSON encoder + ELK stack |
| **API Docs** | SpringDoc OpenAPI (Swagger UI) |
| **Testing** | JUnit 5, Mockito, REST Assured, Testcontainers, Cucumber, Playwright, Gatling |
| **GC** | ZGC (sub-millisecond pauses) |
| **Container** | Docker multi-stage build, non-root user |

---

## Getting Started

### Prerequisites

- **Java 21** (Eclipse Temurin recommended)
- **Docker** and **Docker Compose**
- **Node.js 18+** (for frontend and E2E tests)

### Quick Start

**1. Start infrastructure services:**

```bash
docker compose up -d
```

This starts PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger, and the ELK stack (9 services with health checks).

**2. Build and run the backend:**

```bash
./gradlew bootRun
```

The backend starts on `http://localhost:8080` with virtual threads enabled. Flyway runs 20 migrations automatically and seeds demo data (4 insurers, EA accounts).

**3. Start the frontend:**

```bash
cd frontend
npm install
npm run dev
```

The React dashboard is available at `http://localhost:5173`.

**4. (Optional) Enable Ollama GenAI:**

```bash
docker compose --profile ollama up -d
# Then start backend with the ollama profile:
SPRING_PROFILES_ACTIVE=ollama ./gradlew bootRun
```

### Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| React Dashboard | http://localhost:5173 | — |
| Spring Boot API | http://localhost:8080 | — |
| Swagger UI | http://localhost:8080/swagger-ui | — |
| Grafana | http://localhost:3000 | admin / plum |
| Jaeger | http://localhost:16686 | — |
| Prometheus | http://localhost:9090 | — |
| Kibana | http://localhost:5601 | — |
| Actuator Health | http://localhost:8080/actuator/health | — |

---

## Project Structure

```
plum-endorsements/
├── src/main/java/com/plum/endorsements/
│   ├── api/                          # REST controllers, DTOs, exception handler
│   │   ├── controller/               # 6 controllers (27 endpoints)
│   │   ├── dto/                      # 30+ request/response records
│   │   └── exception/                # GlobalExceptionHandler (RFC 7807)
│   ├── application/                  # Use cases (CQRS)
│   │   ├── handler/                  # CreateEndorsement, ProcessEndorsement, EndorsementQuery
│   │   ├── service/                  # 8 services (anomaly, forecast, reconciliation, etc.)
│   │   ├── scheduler/               # 9 scheduled jobs (batch, anomaly, retention, etc.)
│   │   └── exception/               # Domain exceptions
│   ├── domain/                       # Pure business logic (ZERO infra imports)
│   │   ├── model/                    # 22 entities & value objects
│   │   ├── port/                     # 16 port interfaces (abstractions)
│   │   └── service/                  # StateMachine, EABalanceCalculator, InsurerRegistry
│   └── infrastructure/              # Technology adapters
│       ├── persistence/             # 10 JPA adapters + anti-corruption mappers
│       ├── insurer/                 # 4 insurer adapters (Mock, ICICI, Niva, Bajaj)
│       ├── intelligence/           # Rule-based + Ollama AI adapters
│       ├── messaging/              # Kafka publisher, WebSocket broadcaster
│       ├── config/                 # Security, metrics, MDC, rate limiting
│       └── notification/           # Logging + webhook notification adapters
│
├── src/main/resources/
│   ├── db/migration/               # 20 Flyway migrations (V1–V20)
│   ├── application.yml             # Default config (local dev)
│   ├── application-test.yml        # Test profile (H2)
│   ├── application-railway.yml     # Cloud deployment (env vars)
│   └── logback-spring.xml          # Structured JSON logging
│
├── src/test/java/                  # Unit tests (420+)
├── api-tests/                      # API integration tests (124, REST Assured + Testcontainers)
├── behaviour-tests/                # BDD tests (92, Cucumber + 22 feature files)
├── e2e-tests/                      # E2E tests (158, Playwright)
├── performance-tests/              # Load tests (6, Gatling/Scala)
│
├── frontend/                       # React 19 SPA (TanStack Table + shadcn/ui)
├── k8s/                            # Kubernetes manifests (deployment, HPA, PDB)
├── observability/                  # Grafana dashboards, Prometheus config, Logstash pipeline
├── docker-compose.yml              # 9 infrastructure services
├── Dockerfile                      # Multi-stage build (JDK 21 + ZGC)
└── docs/                           # Architecture docs, deliverables, vision documents
```

---

## Endorsement Lifecycle

The endorsement follows an **11-state lifecycle** with compile-time transition safety:

```
CREATED → VALIDATED → PROVISIONALLY COVERED → SUBMITTED TO INSURER → CONFIRMED
                                             ↘ QUEUED FOR BATCH → BATCH SUBMITTED ↗
                                                                → REJECTED
                                                                → CANCELLED
                                                                → EXPIRED
```

### Key Innovation: Provisional Coverage

The employee gets coverage **immediately at endorsement creation**, not when the insurer confirms days later. This eliminates the coverage gap problem entirely.

- Provisional coverage activates at the `PROVISIONALLY_COVERED` state
- Auto-expires after 30 days if insurer never confirms
- Seamlessly upgrades to confirmed coverage on insurer acknowledgment
- Works for both real-time and batch submission paths

### State Transition Rules

Every transition is validated by `EndorsementStatus.canTransitionTo()` — invalid transitions throw `IllegalStateException`. The `EndorsementStateMachine` service is the single entry point for all state changes.

---

## Key Features

### EA Balance Optimization

The batch optimizer reduces employer float requirements through priority ordering:

1. **DELETEs first** — release locked premium back to available balance
2. **Cost-neutral UPDATEs** — process without affecting balance
3. **ADDs by urgency** — composite score: 60% urgency + 40% EA impact

**Result:** 60 ADDs + 40 DELETEs optimized from Rs. 60,000 peak to Rs. 28,000 — **53% savings**.

Algorithm: 0/1 Knapsack with dynamic programming, running on a 15-minute scheduler.

### Real-Time Visibility

10 interactive UI screens built with React 19 + TanStack Table:

1. **Dashboard** — KPI cards (total, pending, confirmed, failed) + EA balance
2. **Create Endorsement** — progressive disclosure form adapting by type
3. **Endorsement Detail** — status timeline, insurer reference, full history
4. **Endorsement List** — sortable, filterable, CSV export, bookmarkable URLs
5. **Batch Progress** — batch status tracking
6. **Insurer Configuration** — capabilities, rate limits, reconciliation history
7. **EA Account Lookup** — balance, reserved, available with utilization bar
8. **Reconciliation** — matched, partial, rejected, missing counts
9. **Intelligence Hub** — 5 tabs (anomalies, forecasts, errors, process mining, health)
10. **Audit Log** — full event history with filters

### Idempotency & Safe Retries

- Every create endpoint accepts an idempotency key backed by a UNIQUE DB constraint
- Duplicate requests return the existing resource with no side effects
- Infrastructure retries (Resilience4j) handle transient failures
- Domain retries (`Endorsement.canRetry()`) handle business-level rejections

---

## Intelligence & AI

Five intelligence pillars, each behind a domain port interface, supporting a 3-stage evolution:

### Stage 1: Rule-Based (Current Default)

| Pillar | Implementation | Schedule |
|--------|---------------|----------|
| Anomaly Detection | 5 rules (Volume Spike, ADD/DELETE Cycling, Suspicious Timing, Unusual Premium, Dormancy Break) | Every 5 min |
| Balance Forecasting | 30-day projection with dual seasonality (day-of-week + monthly) | Daily 6 AM |
| Error Resolution | 5 patterns with confidence scoring; auto-apply above 95% | On error |
| Process Mining | STP rate tracking, per-insurer optimization, transition metrics | Daily 3 AM |
| Employer Health Score | Composite score from endorsement patterns, EA utilization, error rates | On demand |

### Stage 2: Ollama GenAI (Deployed)

Two adapters use a local llama3.2 model via Spring AI:

- **OllamaAugmentedAnomalyDetector** — anomalies scoring > 0.7 get LLM-generated explanations and action recommendations
- **OllamaErrorResolver** — ambiguous error patterns (< 95% confidence) analyzed by LLM for specific fix suggestions

Both have `@CircuitBreaker` + `@Retry` with graceful fallback to rule-based results. Activated via `@ConditionalOnProperty` with the `ollama` Spring profile.

Config: Temperature 0.3 (deterministic), 512 token max, zero cloud API costs.

### Stage 3: Full ML (Vision)

| Pillar | Planned Technology |
|--------|--------------------|
| Anomaly Detection | Isolation Forest → Autoencoder → Graph-based fraud detection |
| Forecasting | Facebook Prophet → LSTM neural networks → Ensemble methods |
| Error Resolution | RAG pipeline with vector database of past resolutions |
| Process Mining | PM4Py conformance checking via Python sidecar |
| Batch Optimization | Google OR-Tools linear programming |

Each stage is an **adapter swap behind the same port interface** — the hexagonal architecture payoff. The domain never changes.

---

## Multi-Insurer Integration

Four insurer adapters implement the `InsurerPort` strategy interface:

| Insurer | Protocol | Mode | Circuit Breaker |
|---------|----------|------|-----------------|
| Mock | JSON/REST | Real-time + Batch | `insurerSubmission`: 50% / 10 calls / 30s |
| ICICI Lombard | REST/JSON | Real-time only | `iciciLombard`: 50% / 20 calls / 30s |
| Niva Bupa | CSV/SFTP | Batch only | Default |
| Bajaj Allianz | SOAP/XML | Real-time + Batch | `bajajAllianz`: 40% / 15 calls / 45s |

**Adding a new insurer requires:**
1. One adapter class implementing `InsurerPort`
2. One Flyway migration inserting an `insurer_configurations` row
3. Zero changes to handlers, domain, or router

The `InsurerRouter` factory auto-discovers Spring beans and resolves the correct adapter at runtime via database configuration lookup.

---

## Observability

### Docker Compose Services

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL 16 | 5432 | Primary database (13 tables) |
| Redis 7 | 6379 | Distributed cache |
| Kafka (KRaft) | 9092 | Event streaming (4 topics, 88 partitions) |
| Prometheus | 9090 | Metrics scraping (15s interval) |
| Grafana | 3000 | 7 auto-provisioned dashboards |
| Jaeger | 16686 | Distributed tracing (100% sampling) |
| Elasticsearch | 9200 | Log aggregation |
| Logstash | 5000 | Log pipeline |
| Kibana | 5601 | Log visualization |

### Grafana Dashboards

1. **Application Overview** — request rate, P95 latency, error rate, JVM heap, threads, DB pool
2. **Business Metrics** — creation rate by type, endorsements by status, insurer submission latency
3. **Intelligence Monitoring** — anomaly detection rate, forecast shortfalls, error auto-resolution, STP trend
4. **Infrastructure Health** — Kafka consumer lag, PostgreSQL connections, Redis hit rate
5. **Multi-Insurer** — per-insurer submission rates, circuit breaker states, SLA compliance
6. **Reconciliation** — match rates, discrepancy trends, resolution time
7. **Scheduler** — batch assembly timing, anomaly scan duration, forecast generation

### Distributed Tracing

- OpenTelemetry with Micrometer bridge, OTLP exporter to Jaeger
- 100% sampling in development
- MDC propagation: `traceId`, `spanId`, `requestId`, `endorsementId`, `employerId`
- Search by endorsement ID to see the full request journey

### Structured Logging

- JSON format in production via Logstash encoder → Elasticsearch → Kibana
- Human-readable format in development
- Correlation IDs in every log line

---

## Testing

### Test Pyramid

| Layer | Count | Framework | Details |
|-------|-------|-----------|---------|
| **Unit** | 420+ | JUnit 5 + Mockito + AssertJ | Domain logic, handlers, services |
| **API** | 124 | REST Assured + Testcontainers | Full Spring context with real PostgreSQL, Kafka, Redis |
| **BDD** | 92 | Cucumber (22 feature files) | Business scenario validation |
| **E2E** | 158 | Playwright (30 spec files) | Browser-based UI testing |
| **Performance** | 6 | Gatling (Scala) | Load, stress, soak, spike simulations |
| **Total** | **800+** | | **Zero failures** |

### Running Tests

```bash
# Unit tests
./gradlew test

# API integration tests (requires Docker for Testcontainers)
./run-api-tests.sh

# BDD tests
./run-behaviour-tests.sh

# E2E tests (requires frontend + backend running)
./run-e2e-tests.sh

# Performance tests (requires backend running)
./run-perf-tests.sh

# All tests with combined Allure report (Docker)
./run-all-tests.sh
```

The `run-all-tests.sh` script builds all suites and publishes a combined Allure report at `http://localhost:5050`.

### Test Conventions

- Class: `{ClassUnderTest}Test`
- Method: `{method}_{scenario}_{expectedBehaviour}`
- Pattern: Arrange-Act-Assert
- Assertions: AssertJ only (`assertThat(...)`)
- Mocking: `@Mock` for ports, `@InjectMocks` on class under test

---

## API Documentation

Interactive API docs available via Swagger UI when the backend is running:

```
http://localhost:8080/swagger-ui
```

OpenAPI spec:

```
http://localhost:8080/api-docs
```

### Key Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/endorsements` | Create endorsement (with idempotency key) |
| `GET` | `/api/endorsements/{id}` | Get endorsement by ID |
| `GET` | `/api/endorsements` | List endorsements (paginated, filterable) |
| `POST` | `/api/endorsements/{id}/submit` | Submit to insurer |
| `POST` | `/api/endorsements/{id}/confirm` | Confirm endorsement |
| `POST` | `/api/endorsements/{id}/reject` | Reject endorsement |
| `GET` | `/api/ea-accounts/{employerId}/{insurerId}` | EA account lookup |
| `GET` | `/api/intelligence/anomalies` | List detected anomalies |
| `POST` | `/api/intelligence/forecasts/generate` | Generate balance forecast |
| `GET` | `/api/intelligence/error-resolutions` | List error resolutions |
| `GET` | `/api/intelligence/process-mining/stp-rate` | STP rate metrics |
| `GET` | `/api/intelligence/health-score/{employerId}` | Employer health score |
| `GET` | `/api/insurers` | List insurer configurations |
| `GET` | `/api/reconciliation/runs` | List reconciliation runs |
| `GET` | `/api/audit-logs` | Query audit logs |

---

## Kubernetes Deployment

Full Kubernetes manifests in `k8s/`:

```bash
./k8s-start.sh
```

Includes:
- **Deployment** with resource limits (768Mi memory, 500m CPU)
- **HPA** — auto-scale from 2 to 8 pods at 70% CPU
- **PDB** — minimum 1 pod available during disruptions
- **ConfigMap** — externalized connection strings
- **Secrets** — database credentials
- **Services** for all infrastructure components
- **PVCs** for PostgreSQL and Elasticsearch persistence

JVM: Virtual threads enabled, ZGC, `MaxRAMPercentage=75.0`.

---

## Configuration

### Spring Profiles

| Profile | Purpose | Config File |
|---------|---------|-------------|
| (default) | Local development | `application.yml` |
| `test` | Unit/integration tests | `application-test.yml` |
| `railway` | Cloud deployment | `application-railway.yml` |
| `ollama` | Enable GenAI features | Activates Ollama adapters |
| `json` | Structured logging | Enables Logstash JSON encoder |

### Key Configuration Properties

```yaml
endorsement:
  batch:
    schedule-cron: "0 */15 * * * *"        # Batch assembly every 15 min
  provisional-coverage:
    max-days: 30                            # Auto-expire after 30 days
  intelligence:
    anomaly-detection:
      min-anomaly-score: 0.7               # Threshold for flagging
    error-resolution:
      auto-apply-threshold: 0.95           # Auto-apply above 95% confidence
    balance-forecast:
      forecast-days-ahead: 30              # 30-day projection window
  ea:
    safety-margin-pct: 0.10                # 10% safety margin on EA balance
```

All properties support `${ENV_VAR:default}` syntax for environment-specific overrides.

---

## Documentation

Detailed documentation in `docs/`:

### Architecture & Design
- [High-Level Architecture](docs/deliverables/High_Level_Architecture.md)
- [Functional Specification](docs/Functional_Specification.md)
- [Hexagonal Ports & Adapters](docs/Architecture_Hexagonal_Ports_and_Adapters.md)

### Key Approaches
- [No Loss of Coverage](docs/deliverables/No_Loss_of_Coverage_Approach.md)
- [EA Balance Minimization Algorithm](docs/deliverables/EA_Balance_Minimization_Algorithm.md)
- [GenAI Augmentation Strategy](docs/deliverables/GenAI_Augmentation_Strategy.md)
- [AI Automation Approach](docs/deliverables/AI_Automation_Approach.md)
- [Real-Time Visibility User Flows](docs/deliverables/Real_Time_Visibility_User_Flows.md)

### Vision Documents
- [AI Automation Vision](docs/deliverables/vision/AI_Automation_Vision.md) — 15 ML capabilities across 5 pillars
- [Product Evolution Vision](docs/deliverables/vision/Product_Evolution_Vision.md) — Phase 4 global platform
- [Architectural Vision](docs/deliverables/vision/Architectural_Vision.md) — 5-year roadmap (2026-2030)
- [UX Improvement Strategy](docs/deliverables/vision/UX_Improvement_Strategy.md)

### Operations
- [Getting Started Guide](docs/deliverables/Getting_Started_Guide.md)
- [Product Usage Guide](docs/deliverables/Product_Usage_Guide.md)
- [Demo Script (15 min)](docs/deliverables/Demo_Script_15min.md)
- [Performance Test Plan](docs/Performance_Test_Plan.md)

---

## License

This project is a design challenge submission and is not licensed for production use.
