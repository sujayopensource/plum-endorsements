# High-Level Architecture: Plum Endorsement Management System

**Deliverable #1** -- High-level architecture and system components (C4 Model)
**Version**: 2.0
**Date**: 2026-03-15

---

## About This Document

This architecture document follows the **C4 Model** by Simon Brown -- a hierarchical approach to software architecture documentation that describes a system at four levels of abstraction:

| Level | Name | Audience | Shows |
|-------|------|----------|-------|
| **1** | System Context | Everyone | The system as a box, surrounded by users and external systems |
| **2** | Container | Technical stakeholders | Major deployable units and their communication protocols |
| **3** | Component | Developers & architects | Internal structure of a container (hexagonal architecture layers) |
| **4** | Code | Developers | Implementation-level patterns and data structures |

Each level zooms deeper into the previous one. Read top-down for a full picture, or jump to a specific level for the detail you need.

---

## Table of Contents

1. [Level 1: System Context](#level-1-system-context)
2. [Level 2: Container Diagram](#level-2-container-diagram)
3. [Level 3: Component Diagram -- Hexagonal Architecture](#level-3-component-diagram----hexagonal-architecture)
   - 3.1 [Hexagonal Architecture Overview](#31-hexagonal-architecture-overview)
   - 3.2 [Driving Side (Inbound Adapters)](#32-driving-side-inbound-adapters)
   - 3.3 [Application Layer](#33-application-layer)
   - 3.4 [Domain Core](#34-domain-core)
   - 3.5 [Driven Side (Outbound Adapters)](#35-driven-side-outbound-adapters)
   - 3.6 [Multi-Insurer Adapter Framework](#36-multi-insurer-adapter-framework)
4. [Level 4: Code -- Key Design Patterns](#level-4-code----key-design-patterns)
   - 4.1 [Endorsement State Machine](#41-endorsement-state-machine)
   - 4.2 [Event-Driven Architecture](#42-event-driven-architecture)
   - 4.3 [EA Account -- Rich Domain Model](#43-ea-account----rich-domain-model)
   - 4.4 [Data Model (13 Tables)](#44-data-model-13-tables)
5. [Scalability Design](#5-scalability-design)
6. [Observability Stack](#6-observability-stack)
7. [Architecture Decision Records](#7-architecture-decision-records)

---

## Level 1: System Context

> *What is this system, who uses it, and what does it connect to?*

The Plum Endorsement Service is a backend platform for managing insurance policy endorsements in an employer-sponsored group health insurance context. An **endorsement** represents a change to an insurance policy -- adding an employee, removing an employee, or updating employee details. The service manages the full lifecycle of these endorsements, from creation through insurer processing to final confirmation or rejection.

### System Context Diagram

```
                                    +------------------+
                                    |  Employer HR     |
                                    |  Users           |
                                    |  [Person]        |
                                    +--------+---------+
                                             |
                                     Creates & tracks
                                     endorsements via
                                     React SPA
                                             |
                                             v
+------------------+            +---------------------------+            +------------------+
|  Plum Operations |  monitors  |                           |  submits   |  ICICI Lombard   |
|  Team            +----------->|   Plum Endorsement        +----------->|  API             |
|  [Person]        |            |   Management System       |  REST/JSON |  [External]      |
+------------------+            |                           |            +------------------+
                                |   Manages insurance       |
                                |   endorsement lifecycle:  |            +------------------+
                                |   creation, validation,   |  uploads   |  Niva Bupa       |
                                |   insurer submission,     +----------->|  SFTP            |
                                |   reconciliation, and     |  CSV/SFTP  |  [External]      |
                                |   intelligence analytics  |            +------------------+
                                |                           |
                                |                           |            +------------------+
                                |                           |  submits   |  Bajaj Allianz   |
                                |                           +----------->|  API             |
                                +-------------+-------------+  SOAP/XML  |  [External]      |
                                              |                          +------------------+
                                              |
                                      queries for            +------------------+
                                      AI-powered             |  Ollama LLM      |
                                      intelligence           |  Service         |
                                              +------------->|  [External]      |
                                                             +------------------+
```

### Design Targets

| Dimension               | Target                          |
|--------------------------|---------------------------------|
| Employers                | 100,000+                        |
| Endorsements per day     | 1,000,000+                      |
| Insurer integrations     | 10+ (4 implemented)             |
| Availability             | 99.9% uptime                    |
| API latency (p99)        | < 200ms (read), < 500ms (write) |
| Batch processing cadence | Every 15 minutes                |

### Technology Stack

| Layer          | Technology                                           |
|----------------|------------------------------------------------------|
| Language       | Java 21 (Virtual Threads enabled)                    |
| Framework      | Spring Boot 3.x                                      |
| Database       | PostgreSQL 16                                        |
| Messaging      | Apache Kafka 3.7 (KRaft mode, no ZooKeeper)          |
| Cache          | Redis 7 + Caffeine (local)                           |
| Observability  | Micrometer, Prometheus, Grafana, Jaeger, ELK          |
| Resilience     | Resilience4j (Circuit Breaker + Retry)                |
| Schema Mgmt    | Flyway (13 versioned migrations)                     |
| API Docs       | SpringDoc / OpenAPI                                  |
| Frontend       | React 18 + TypeScript + Vite + shadcn/ui             |
| AI/ML          | Ollama (local LLM inference)                         |
| Deployment     | Docker Compose, Kubernetes                           |

---

## Level 2: Container Diagram

> *What are the major deployable units, and how do they communicate?*

### Container Diagram

```
+=========================================================================+
|                      PLUM ENDORSEMENT SYSTEM                            |
|                                                                         |
|  +-------------------------------------------------------------------+ |
|  |                     React SPA (Frontend)                          | |
|  |                     Port: 5173                                     | |
|  |                     Vite + React 18 + TypeScript + shadcn/ui       | |
|  +-------------------------------+-----------------------------------+ |
|                                  |                                      |
|                                  | HTTP/REST (JSON)                     |
|                                  v                                      |
|  +-------------------------------------------------------------------+ |
|  |                  Endorsement Service (Backend)                     | |
|  |                  Port: 8080                                        | |
|  |                  Spring Boot 3.x + Java 21 Virtual Threads         | |
|  |                  36 REST endpoints across 6 controllers            | |
|  +---+--------+--------+--------+--------+--------+---------+-------+ |
|      |        |        |        |        |        |         |         |
|      | JDBC   | TCP    | TCP    | HTTP   | HTTP   | TCP     | HTTP    |
|      v        v        v        v        v        v         v         |
|  +------+ +------+ +------+ +------+ +------+ +------+ +--------+   |
|  |Postgr| |Redis | |Kafka | |Jaeger| |Prome-| |Elast-| |Ollama  |   |
|  |eSQL  | |      | |      | |      | |theus | |search| |        |   |
|  |:5432 | |:6379 | |:9092 | |:16686| |:9090 | |:9200 | |:11434  |   |
|  +------+ +------+ +------+ +------+ +--+---+ +--+---+ +--------+   |
|                                              |       |                |
|                                              v       v                |
|                                          +------+ +------+ +------+  |
|                                          |Grafa-| |Logst-| |Kiban-|  |
|                                          |na    | |ash   | |a     |  |
|                                          |:3000 | |:5044 | |:5601 |  |
|                                          +------+ +------+ +------+  |
+=========================================================================+
```

### Container Responsibilities

| Container | Technology | Purpose | Port |
|-----------|-----------|---------|------|
| **React SPA** | Vite + React 18 + TypeScript | Employer HR portal: create endorsements, track status, view intelligence | 5173 |
| **Endorsement Service** | Spring Boot 3.x, Java 21 | Core business logic: endorsement lifecycle, insurer routing, intelligence | 8080 |
| **PostgreSQL 16** | PostgreSQL 16-alpine | Persistent storage: 13 tables, Flyway V1-V13 migrations | 5432 |
| **Apache Kafka** | Kafka 3.7 (KRaft) | Event streaming: 4 topics, 88 partitions, employerId partition key | 9092 |
| **Redis 7** | Redis 7-alpine | Distributed cache + session store, Caffeine L1 cache in front | 6379 |
| **Ollama** | Ollama (local LLM) | AI inference: anomaly scoring, error resolution, pattern detection | 11434 |
| **Prometheus** | Prometheus 2.50 | Metrics collection: scrapes /actuator/prometheus every 15s | 9090 |
| **Grafana** | Grafana 10.3 | Metrics visualization: 7 provisioned dashboards | 3000 |
| **Jaeger** | Jaeger all-in-one | Distributed tracing: receives OTLP traces over gRPC/HTTP | 16686 |
| **Elasticsearch** | Elasticsearch 8.12 | Log storage: structured JSON logs from Logstash | 9200 |
| **Logstash** | Logstash 8.12 | Log pipeline: receives from app via TCP, forwards to Elasticsearch | 5044 |
| **Kibana** | Kibana 8.12 | Log visualization: search and dashboard over Elasticsearch | 5601 |

### Kafka Topics -- 4 Topics, 88 Partitions

```
+------------------------------+----------+-----------+------------------------+
|         Topic Name           | Partns   | Replicas  | Purpose                |
+------------------------------+----------+-----------+------------------------+
| endorsement-events           |    32    |     1     | All endorsement state  |
|                              |          |           | change events (28      |
|                              |          |           | event types). Key:     |
|                              |          |           | employerId             |
+------------------------------+----------+-----------+------------------------+
| endorsement-commands         |    32    |     1     | Command messages for   |
|                              |          |           | async processing       |
+------------------------------+----------+-----------+------------------------+
| endorsement-notifications    |     8    |     1     | Notification dispatch  |
|                              |          |           | (email, webhook, etc.) |
+------------------------------+----------+-----------+------------------------+
| endorsement-reconciliation   |    16    |     1     | Reconciliation results |
|                              |          |           | and discrepancy alerts |
+------------------------------+----------+-----------+------------------------+
                               Total: 88 partitions
```

Partition key strategy: `employerId` ensures all endorsements for a single employer are processed in order within the same partition, preventing race conditions on EA account balance operations.

### Docker Compose Services

```
docker-compose.yml orchestrates 10 services:

  +---------------------+         +---------------------+
  | postgres:16-alpine  |         | redis:7-alpine      |
  | Port: 5432          |         | Port: 6379          |
  | Vol: pgdata         |         | Healthcheck: ping   |
  | Healthcheck: ready  |         +---------------------+
  +---------------------+
                                  +---------------------+
  +---------------------+        | jaeger (all-in-one)  |
  | kafka (KRaft 3.7)   |        | Port: 16686 (UI)    |
  | Port: 9092           |        | Port: 4317 (OTLP)   |
  | No ZooKeeper needed  |        | Port: 4318 (HTTP)   |
  +---------------------+        +---------------------+

  +---------------------+        +---------------------+
  | elasticsearch 8.12  |------->| logstash 8.12       |
  | Port: 9200          |        | Port: 5044, 5000    |
  | Vol: esdata         |        | Pipeline: custom    |
  | Security: disabled  |        +---------------------+
  +---------------------+              |
         |                             v
         +--------------------> +---------------------+
                                | kibana 8.12         |
                                | Port: 5601          |
                                +---------------------+

  +---------------------+        +---------------------+
  | prometheus 2.50     |------->| grafana 10.3        |
  | Port: 9090          |        | Port: 3000          |
  | Retention: 7d       |        | 7 provisioned       |
  +---------------------+        | dashboards          |
                                  +---------------------+

  +---------------------+
  | ollama              |
  | Port: 11434         |
  | Model: configurable |
  +---------------------+
```

### Kubernetes Deployment (k8s/)

The system includes full Kubernetes manifests for production deployment:

```
k8s/
  namespace.yaml                    # plum-endorsements namespace
  backend/
    deployment.yaml                 # Spring Boot app (replicas, probes, HPA)
    service.yaml                    # ClusterIP service
    configmap.yaml                  # Application configuration
    hpa.yaml                        # HorizontalPodAutoscaler
    pdb.yaml                        # PodDisruptionBudget
  postgres/
    deployment.yaml                 # PostgreSQL StatefulSet
    service.yaml                    # Headless service
    pvc.yaml                        # PersistentVolumeClaim
    configmap.yaml                  # pg_hba.conf, postgresql.conf
    secret.yaml                     # Credentials (base64-encoded)
  kafka/
    deployment.yaml                 # KRaft-mode Kafka
    service.yaml
    configmap.yaml
  redis/
    deployment.yaml
    service.yaml
  elasticsearch/
    deployment.yaml
    service.yaml
    pvc.yaml
  logstash/
    deployment.yaml
    service.yaml
    configmap.yaml                  # Pipeline configuration
  kibana/
    deployment.yaml
    service.yaml
  jaeger/
    deployment.yaml
    service.yaml
  prometheus/
    deployment.yaml
    service.yaml
    configmap.yaml                  # Scrape configuration
  grafana/
    deployment.yaml
    service.yaml
    configmap.yaml                  # Dashboard provisioning
  seed-job.yaml                     # One-shot Job: seed demo data
```

---

## Level 3: Component Diagram -- Hexagonal Architecture

> *What is the internal structure of the Endorsement Service container?*

This level zooms into the Endorsement Service -- the core backend container -- and shows its internal components organized by hexagonal architecture layers.

### 3.1 Hexagonal Architecture Overview

The system implements hexagonal (ports-and-adapters) architecture, enforcing strict dependency inversion. The domain core has zero dependencies on infrastructure. All external concerns connect through port interfaces that the domain defines and infrastructure implements.

```
+=========================================================================+
|                        API LAYER (Driving Side)                         |
|                                                                         |
|  EndorsementController    EAAccountController    InsurerConfigController |
|  ReconciliationController IntelligenceController AuditLogController      |
|  GlobalExceptionHandler (RFC 7807 ProblemDetail)                        |
|                                                                         |
|  36 REST endpoints across 6 controllers                                 |
+=========================================================================+
         |              |              |             |            |
         v              v              v             v            v
+=========================================================================+
|                      APPLICATION LAYER                                  |
|                                                                         |
|  Handlers (CQRS):                                                       |
|    CreateEndorsementHandler   (validate, dedup, provision, debit)       |
|    ProcessEndorsementHandler  (submit RT/batch, confirm, reject)        |
|    EndorsementQueryHandler    (find, list, paginate, filter)            |
|                                                                         |
|  Services (8):                                                          |
|    AnomalyDetectionService    BalanceForecastService                    |
|    ErrorResolutionService     ProcessMiningService                      |
|    EmployerHealthScoreService InsurerBenchmarkService                   |
|    AuditLogService            ReconciliationEngine                      |
|                                                                         |
|  Schedulers (9):                                                        |
|    BatchAssemblyScheduler         (every 15 min)                        |
|    BatchStatusPollerScheduler     (poll insurer batch results)          |
|    ProvisionalCoverageCleanup     (expire stale coverage)              |
|    ReconciliationScheduler        (nightly reconciliation)             |
|    AnomalyDetectionScheduler      (every 5 min)                        |
|    BalanceForecastScheduler       (daily at 06:00)                     |
|    ProcessMiningScheduler         (daily at 03:00)                     |
|    StuckEndorsementRetryScheduler (retry stuck endorsements)           |
|    DataRetentionScheduler         (archive old data)                   |
+=========================================================================+
         |              |              |             |            |
         v              v              v             v            v
+=========================================================================+
|                        DOMAIN CORE                                      |
|                   [ZERO infrastructure imports]                         |
|                                                                         |
|  Models (16+):                                                          |
|    Endorsement          (11-state lifecycle, optimistic locking)        |
|    EAAccount            (balance + reserved, debit/credit/reserve)      |
|    EndorsementBatch     (batch assembly for batch-mode insurers)        |
|    ProvisionalCoverage  (immediate coverage before insurer confirms)    |
|    InsurerConfiguration (per-insurer settings, adapter type, SLAs)      |
|    ReconciliationRun    (run-level reconciliation metadata)             |
|    ReconciliationItem   (per-endorsement match/mismatch result)        |
|    AnomalyDetection     (flagged anomalies with scores)                |
|    BalanceForecastRecord(forecasted balance projections)                |
|    ErrorResolution      (suggested/auto-applied fixes)                 |
|    ProcessMiningMetric  (STP rates, bottleneck analysis)               |
|    StpRateSnapshot      (STP rate point-in-time snapshots)             |
|    EndorsementEvent     (sealed interface, 28 event types)             |
|    EndorsementPriority  (P0 Deletion > P1 CostNeutral > P2 Add > P3)  |
|    AuditLog             (immutable audit trail entries)                |
|                                                                         |
|  Domain Services (3):                                                   |
|    EndorsementStateMachine  (enforces valid state transitions)         |
|    EABalanceCalculator      (batch sequencing, balance forecasting)     |
|    InsurerRegistry          (@Cacheable insurer config lookup)         |
|                                                                         |
|  Ports (19 interfaces -- domain defines, infrastructure implements):    |
|    EndorsementRepository     EAAccountRepository                        |
|    BatchRepository           ProvisionalCoverageRepository              |
|    InsurerConfigurationRepository  ReconciliationRepository             |
|    AnomalyDetectionRepository      BalanceForecastRepository            |
|    ErrorResolutionRepository       ProcessMiningRepository              |
|    StpRateSnapshotRepository                                            |
|    EventPublisher            NotificationPort                           |
|    InsurerPort               BatchOptimizerPort                         |
|    AnomalyDetectionPort      BalanceForecastPort                        |
|    ErrorResolutionPort       ProcessMiningPort                          |
+=========================================================================+
         |              |              |             |            |
         v              v              v             v            v
+=========================================================================+
|                   INFRASTRUCTURE LAYER (Driven Side)                    |
|                                                                         |
|  Persistence (10 JPA Adapters, 13 Spring Data Repos, 14 JPA Entities): |
|    JpaEndorsementRepositoryAdapter     JpaEAAccountRepositoryAdapter    |
|    JpaBatchRepositoryAdapter           JpaProvisionalCoverageAdapter    |
|    JpaInsurerConfigurationAdapter      JpaReconciliationAdapter         |
|    JpaAnomalyDetectionAdapter          JpaBalanceForecastAdapter        |
|    JpaErrorResolutionAdapter           JpaProcessMiningAdapter          |
|                                                                         |
|  Messaging:                                                             |
|    KafkaEventPublisher     (endorsement-events topic, employerId key)   |
|    KafkaConfig             (4 topics, 88 partitions)                    |
|                                                                         |
|  Insurer Adapters (4):                                                  |
|    MockInsurerAdapter      (JSON, RT+Batch)                            |
|    IciciLombardAdapter     (JSON, RT only)                             |
|    NivaBupaAdapter         (CSV, Batch only)                           |
|    BajajAllianzAdapter     (XML, RT+Batch)                             |
|    InsurerRouter           (factory pattern for runtime resolution)     |
|                                                                         |
|  Intelligence Adapters (5):                                             |
|    RuleBasedAnomalyDetector      StatisticalForecastEngine             |
|    SimulatedErrorResolver        EventStreamAnalyzer                    |
|    ConstraintBatchOptimizer                                             |
|                                                                         |
|  Cross-Cutting:                                                         |
|    LoggingNotificationAdapter    SecurityConfig                         |
|    MdcRequestFilter              RequestLoggingFilter                   |
|    MetricsConfig                 EndorsementGaugeRegistrar              |
|    EndorsementMapper             (Entity <-> Domain model ACL)          |
+=========================================================================+
```

### Why Hexagonal?

1. **Testability** -- Domain logic is tested in isolation with mock ports. No database, no Kafka, no HTTP in unit tests.
2. **Insurer Independence** -- Adding a new insurer means implementing a single `InsurerPort` interface. Zero changes to domain or application layers.
3. **Infrastructure Swappability** -- Kafka could be swapped for RabbitMQ by replacing `KafkaEventPublisher`. PostgreSQL could be swapped for another RDBMS by re-implementing the JPA adapters. The domain never knows.
4. **Intelligence Extensibility** -- Each intelligence capability (anomaly detection, forecasting, etc.) is behind a port interface. The current rule-based/statistical implementations can be swapped for ML models without touching application code.

### 3.2 Driving Side (Inbound Adapters)

The driving side handles incoming requests from external actors. All inbound traffic enters through REST controllers that translate HTTP requests into application-layer calls.

#### 6 Controllers, 36 REST Endpoints

```
+--------+-----------------------------------------------------+--------------------+---------+
| Method | Path                                                | Controller         | Purpose |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         ENDORSEMENT MANAGEMENT (9)                  |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| POST   | /api/v1/endorsements                                | Endorsement        | Create  |
| GET    | /api/v1/endorsements/{id}                           | Endorsement        | Get     |
| GET    | /api/v1/endorsements                                | Endorsement        | List    |
| POST   | /api/v1/endorsements/{id}/submit                    | Endorsement        | Submit  |
| POST   | /api/v1/endorsements/{id}/confirm                   | Endorsement        | Confirm |
| POST   | /api/v1/endorsements/{id}/reject                    | Endorsement        | Reject  |
| GET    | /api/v1/endorsements/{id}/coverage                  | Endorsement        | Prov.Cv |
| GET    | /api/v1/endorsements/employers/{eid}/batches        | Endorsement        | Batches |
| GET    | /api/v1/endorsements/employers/{eid}/outstanding    | Endorsement        | Outstdg |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         EA ACCOUNT (1)                              |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| GET    | /api/v1/ea-accounts                                 | EAAccount          | Balance |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         AUDIT LOG (1)                               |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| GET    | /api/v1/audit-logs                                  | AuditLog           | List    |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         INSURER CONFIGURATION (5)                   |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| GET    | /api/v1/insurers                                    | InsurerConfig      | List    |
| GET    | /api/v1/insurers/{id}                               | InsurerConfig      | Get     |
| GET    | /api/v1/insurers/{id}/capabilities                  | InsurerConfig      | Caps    |
| POST   | /api/v1/insurers                                    | InsurerConfig      | Create  |
| PUT    | /api/v1/insurers/{id}                               | InsurerConfig      | Update  |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         RECONCILIATION (3)                          |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| GET    | /api/v1/reconciliation/runs                         | Reconciliation     | Runs    |
| GET    | /api/v1/reconciliation/runs/{id}/items              | Reconciliation     | Items   |
| POST   | /api/v1/reconciliation/trigger                      | Reconciliation     | Trigger |
+--------+-----------------------------------------------------+--------------------+---------+
|        |         INTELLIGENCE (17)                           |                    |         |
+--------+-----------------------------------------------------+--------------------+---------+
| GET    | /api/v1/intelligence/anomalies                      | Intelligence       | List    |
| GET    | /api/v1/intelligence/anomalies/{id}                 | Intelligence       | Get     |
| PUT    | /api/v1/intelligence/anomalies/{id}/review          | Intelligence       | Review  |
| GET    | /api/v1/intelligence/forecasts                       | Intelligence       | Latest  |
| GET    | /api/v1/intelligence/forecasts/history               | Intelligence       | History |
| POST   | /api/v1/intelligence/forecasts/generate              | Intelligence       | Gen     |
| GET    | /api/v1/intelligence/error-resolutions               | Intelligence       | List    |
| GET    | /api/v1/intelligence/error-resolutions/stats         | Intelligence       | Stats   |
| POST   | /api/v1/intelligence/error-resolutions/resolve       | Intelligence       | Resolve |
| POST   | /api/v1/intelligence/error-resolutions/{id}/approve  | Intelligence       | Approve |
| GET    | /api/v1/intelligence/process-mining/metrics          | Intelligence       | Metrics |
| GET    | /api/v1/intelligence/process-mining/insights         | Intelligence       | Insight |
| GET    | /api/v1/intelligence/process-mining/stp-rate         | Intelligence       | STP     |
| GET    | /api/v1/intelligence/process-mining/stp-rate/trend   | Intelligence       | Trend   |
| POST   | /api/v1/intelligence/process-mining/analyze          | Intelligence       | Trigger |
| GET    | /api/v1/intelligence/employers/{eid}/health-score    | Intelligence       | Health  |
| GET    | /api/v1/intelligence/benchmarks                      | Intelligence       | Bench   |
+--------+-----------------------------------------------------+--------------------+---------+

Total: 36 endpoints
```

#### Error Handling -- RFC 7807 Problem Details

All errors are returned as RFC 7807 `ProblemDetail` responses via the `GlobalExceptionHandler`:

```json
{
  "type": "about:blank",
  "title": "Endorsement Not Found",
  "status": 404,
  "detail": "Endorsement with id 123e4567-... not found",
  "instance": "/api/v1/endorsements/123e4567-..."
}
```

The handler covers 9 exception types with dedicated HTTP status codes:

```
EndorsementNotFoundException  --> 404 Not Found
InsurerNotFoundException      --> 404 Not Found
DuplicateEndorsementException --> 409 Conflict
InsufficientBalanceException  --> 422 Unprocessable Entity
IllegalStateException         --> 400 Bad Request
MethodArgumentNotValid        --> 400 Bad Request (with field-level errors)
MissingServletRequestParam    --> 400 Bad Request
MethodArgumentTypeMismatch    --> 400 Bad Request
Exception (catch-all)         --> 500 Internal Server Error
```

### 3.3 Application Layer

The application layer orchestrates domain operations. It is stateless -- all fields are `private final` injected dependencies. No mutable instance state.

#### Handlers (CQRS Separation)

| Handler | Role | Transaction | Key Operations |
|---------|------|-------------|----------------|
| `CreateEndorsementHandler` | **Command** | `@Transactional` | Validate, idempotency check, debit EA account, grant provisional coverage, publish `Created` event |
| `ProcessEndorsementHandler` | **Command** | `@Transactional` | Route to insurer (RT/batch), confirm, reject, retry, state transitions, publish lifecycle events |
| `EndorsementQueryHandler` | **Query** | `@Transactional(readOnly = true)` | Find by ID, list by employer, paginate, filter by status/type/date |

Commands and queries are in separate handler classes. Query handlers use `readOnly = true` to enable PostgreSQL read-replica routing. Commands publish events; queries never do.

#### Application Services (8)

| Service | Purpose |
|---------|---------|
| `AnomalyDetectionService` | Detect unusual endorsement patterns (volume spikes, dormancy breaks, STP drops) |
| `BalanceForecastService` | Project future EA balance needs and identify shortfalls |
| `ErrorResolutionService` | Suggest or auto-apply fixes for rejected endorsements |
| `ProcessMiningService` | Analyze STP rates, bottlenecks, and processing efficiency |
| `EmployerHealthScoreService` | Compute composite health scores per employer |
| `InsurerBenchmarkService` | Compare insurer performance metrics |
| `AuditLogService` | Record and query immutable audit trail entries |
| `ReconciliationEngine` | Match system records against insurer records, flag discrepancies |

#### Schedulers (9)

| Scheduler | Schedule | Purpose |
|-----------|----------|---------|
| `BatchAssemblyScheduler` | Every 15 min | Assemble queued endorsements into batches per insurer |
| `BatchStatusPollerScheduler` | Configurable | Poll insurers for batch processing results |
| `ProvisionalCoverageCleanupScheduler` | Configurable | Expire provisional coverages past SLA window |
| `ReconciliationScheduler` | Nightly | Run reconciliation against insurer records |
| `AnomalyDetectionScheduler` | Every 5 min | Scan for anomalous endorsement patterns |
| `BalanceForecastScheduler` | Daily 06:00 | Generate balance forecasts for all employers |
| `ProcessMiningScheduler` | Daily 03:00 | Compute STP rates and process efficiency metrics |
| `StuckEndorsementRetryScheduler` | Configurable | Retry endorsements stuck in intermediate states |
| `DataRetentionScheduler` | Configurable | Archive or purge old data per retention policy |

### 3.4 Domain Core

The domain core has **zero infrastructure imports** -- no JPA annotations, no Kafka classes, no Spring HTTP types. It defines the business rules, models, and port interfaces that the infrastructure layer implements.

#### Domain Models (16+)

| Model | Key Characteristics |
|-------|-------------------|
| `Endorsement` | 11-state lifecycle, optimistic locking (`version`), idempotency key, retry count |
| `EAAccount` | Composite key `(employerId, insurerId)`, balance/reserved/available, debit/credit/reserve operations |
| `EndorsementBatch` | Groups endorsements for batch-mode insurers, tracks batch status |
| `ProvisionalCoverage` | Immediate coverage before insurer confirms, can be confirmed/expired |
| `InsurerConfiguration` | Per-insurer settings: adapter type, capabilities, SLAs, rate limits |
| `ReconciliationRun` | Run-level metadata: counts of matched, discrepant, missing items |
| `ReconciliationItem` | Per-endorsement reconciliation result with outcome |
| `AnomalyDetection` | Flagged anomalies with type, score, explanation, review status |
| `BalanceForecastRecord` | Projected balance, forecasted need, shortfall, top-up flag |
| `ErrorResolution` | Suggested fix with confidence score, auto-apply flag, status |
| `ProcessMiningMetric` | STP rates, bottleneck metrics, period-based analysis |
| `StpRateSnapshot` | Point-in-time STP rate snapshots for trending |
| `AuditLog` | Immutable audit trail: entity, action, actor, timestamp |
| `EndorsementEvent` | Sealed interface with 28 event record types |
| `EndorsementPriority` | Priority ordering: P0 Deletion > P1 CostNeutral > P2 Add > P3 Other |
| `EndorsementStatus` | 11-state enum with `EnumSet`-based transition validation |

#### Domain Services (3)

| Service | Pattern | Purpose |
|---------|---------|---------|
| `EndorsementStateMachine` | State Pattern | Enforces valid state transitions via `EnumSet` O(1) validation |
| `EABalanceCalculator` | Strategy | Sequences batches for optimal balance (deletions first), forecasts shortfalls |
| `InsurerRegistry` | Factory + Cache | `@Cacheable` insurer configuration lookup with `@CacheEvict` on updates |

#### Port Interfaces (19)

The domain defines 19 port interfaces organized into three categories:

**Repository Ports (11)** -- data persistence:
```
EndorsementRepository          EAAccountRepository
BatchRepository                ProvisionalCoverageRepository
InsurerConfigurationRepository ReconciliationRepository
AnomalyDetectionRepository     BalanceForecastRepository
ErrorResolutionRepository      ProcessMiningRepository
StpRateSnapshotRepository
```

**Integration Ports (3)** -- external system communication:
```
InsurerPort                    EventPublisher
NotificationPort
```

**Intelligence Ports (5)** -- pluggable analytics engines:
```
AnomalyDetectionPort           BalanceForecastPort
ErrorResolutionPort            ProcessMiningPort
BatchOptimizerPort
```

### 3.5 Driven Side (Outbound Adapters)

The driven side implements the port interfaces defined by the domain. Each adapter translates between the domain's language and an infrastructure technology's language.

#### Persistence Adapters (10 JPA Adapters)

Each persistence adapter follows the same pattern:

```
Domain Port (interface)
     ^
     |  implements
     |
JPA Repository Adapter (@Component)
     |
     |  uses
     v
EndorsementMapper (Anti-Corruption Layer)
     |
     |  translates
     v
Spring Data JPA Repository + JPA Entity
```

The `EndorsementMapper` is the anti-corruption layer (ACL) -- the **only place** that knows both domain models and JPA entities. Domain models never import JPA annotations; JPA entities never leak into handlers.

| Adapter | Domain Port | Entity |
|---------|------------|--------|
| `JpaEndorsementRepositoryAdapter` | `EndorsementRepository` | `EndorsementEntity` |
| `JpaEAAccountRepositoryAdapter` | `EAAccountRepository` | `EAAccountEntity` |
| `JpaBatchRepositoryAdapter` | `BatchRepository` | `EndorsementBatchEntity` |
| `JpaProvisionalCoverageAdapter` | `ProvisionalCoverageRepository` | `ProvisionalCoverageEntity` |
| `JpaInsurerConfigurationAdapter` | `InsurerConfigurationRepository` | `InsurerConfigurationEntity` |
| `JpaReconciliationAdapter` | `ReconciliationRepository` | `ReconciliationRunEntity` |
| `JpaAnomalyDetectionAdapter` | `AnomalyDetectionRepository` | `AnomalyDetectionEntity` |
| `JpaBalanceForecastAdapter` | `BalanceForecastRepository` | `BalanceForecastEntity` |
| `JpaErrorResolutionAdapter` | `ErrorResolutionRepository` | `ErrorResolutionEntity` |
| `JpaProcessMiningAdapter` | `ProcessMiningRepository` | `ProcessMiningMetricEntity` |

#### Intelligence Adapters (5)

| Adapter | Port | Algorithm |
|---------|------|-----------|
| `RuleBasedAnomalyDetector` | `AnomalyDetectionPort` | Rule engine: volume spikes, dormancy breaks, STP drops |
| `StatisticalForecastEngine` | `BalanceForecastPort` | Statistical projection from historical trends |
| `SimulatedErrorResolver` | `ErrorResolutionPort` | Pattern matching + confidence scoring for error fixes |
| `EventStreamAnalyzer` | `ProcessMiningPort` | STP rate computation, bottleneck identification |
| `ConstraintBatchOptimizer` | `BatchOptimizerPort` | Priority sequencing, balance-aware batch assembly |

All intelligence adapters implement domain-defined ports. The current rule-based/statistical implementations can be swapped for ML models (via Ollama LLM integration) without touching application code.

#### Messaging Adapter

`KafkaEventPublisher` implements `EventPublisher`:
- Serializes `EndorsementEvent` records to JSON
- Sends to `endorsement-events` topic with `employerId` as partition key
- Records success/failure metrics via Micrometer
- Producer config: `acks: all` (all in-sync replicas must acknowledge)

#### Cross-Cutting Infrastructure

| Component | Purpose |
|-----------|---------|
| `SecurityConfig` | Stateless sessions (`SessionCreationPolicy.STATELESS`), CORS, CSRF disabled |
| `MdcRequestFilter` | Injects `requestId`, `traceId`, `spanId` into MDC for structured logging |
| `RequestLoggingFilter` | Logs HTTP method, URI, status, duration, body size for every request |
| `MetricsConfig` | Registers 20+ custom Micrometer metrics (counters, timers, gauges) |
| `EndorsementGaugeRegistrar` | Maintains 11 real-time gauges for endorsement status distribution |
| `EndorsementMapper` | Anti-corruption layer: translates between domain models and JPA entities |
| `LoggingNotificationAdapter` | Implements `NotificationPort` with structured logging (pluggable for email/SMS) |

### 3.6 Multi-Insurer Adapter Framework

Indian health insurers expose radically different APIs. Some offer REST/JSON, others require SOAP/XML, and some only accept CSV batch uploads via SFTP. The adapter framework normalizes this heterogeneity behind a single `InsurerPort` interface.

#### InsurerPort Interface

```java
public interface InsurerPort {
    SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> data);
    String submitBatch(UUID batchId, List<Map<String, Object>> endorsements);
    BatchStatusResult checkBatchStatus(String insurerBatchRef);
    InsurerCapabilities getCapabilities();

    // Template method defaults (HFDP Ch. 8)
    default String getAdapterType() { return "MOCK"; }
    default Map<String, Object> mapToInsurerFormat(Map<String, Object> data) { return data; }
    default Map<String, Object> mapFromInsurerFormat(Map<String, Object> data) { return data; }

    // Nested value objects
    record SubmissionResult(boolean success, String insurerReference, String errorMessage) {}
    record BatchStatusResult(String status, List<EndorsementResult> results) {}
    record EndorsementResult(UUID endorsementId, boolean confirmed,
                             String insurerReference, String rejectionReason) {}
    record InsurerCapabilities(boolean supportsRealTime, boolean supportsBatch,
                               int maxBatchSize, long batchSlaHours, int rateLimitPerMinute) {}
}
```

#### Adapter Comparison Matrix

```
+-------------------+----------+---------+---------+----------+------------------+
|     Adapter       |  Format  |  Real-  |  Batch  | Latency  |   Resilience     |
|                   |          |  Time   |         |  (sim.)  |                  |
+-------------------+----------+---------+---------+----------+------------------+
| MockInsurer       |  JSON    |   Yes   |   Yes   |  100ms   | CB + Retry       |
| IciciLombard      |  JSON    |   Yes   |   No    |  150ms   | CB + Retry       |
| NivaBupa          |  CSV     |   No    |   Yes   |  200ms   | --               |
| BajajAllianz      |  XML     |   Yes   |   Yes   |  250ms   | CB + Retry       |
+-------------------+----------+---------+---------+----------+------------------+

CB = Resilience4j Circuit Breaker
```

#### InsurerRouter -- Runtime Adapter Resolution (Factory Pattern)

```
                       +------------------+
                       |  InsurerRouter   |
                       |  (Factory)       |
                       +--------+---------+
                                |
               resolve(insurerId)
                                |
                  +-------------+-------------+
                  |                           |
                  v                           v
        +-----------------+         +------------------+
        | InsurerRegistry |         | Map<String,      |
        | (@Cacheable)    |         |   InsurerPort>   |
        |                 |         | (auto-wired)     |
        +--------+--------+         +--------+---------+
                 |                           |
                 v                           v
        InsurerConfiguration         Matching Adapter
        { adapterType: "ICICI_LOMBARD" }
                                             |
                                             v
                                    IciciLombardAdapter
```

The `InsurerRouter` uses a two-step resolution process:
1. Look up the `InsurerConfiguration` via the `InsurerRegistry` (cached with `@Cacheable`)
2. Match the configuration's `adapterType` string to the registered adapter map (populated at startup via Spring's `List<InsurerPort>` injection)

Adding a 5th insurer requires:
- One new `InsurerPort` implementation class
- One new Flyway migration to seed the `insurer_configurations` row
- Zero changes to routing, domain, or application logic

#### Resilience Configuration (Per-Insurer)

Each insurer adapter has independently tuned circuit breaker and retry policies:

```
Resilience4j Circuit Breaker Configuration:
+-------------------+--------+-----------+-----------+---------+
|     Instance      | Window | Min Calls | Fail Rate | Wait    |
+-------------------+--------+-----------+-----------+---------+
| insurerSubmission |   10   |     5     |    50%    |  30s    |
| iciciLombard      |   20   |    10     |    50%    |  30s    |
| bajajAllianz      |   15   |     5     |    40%    |  45s    |
+-------------------+--------+-----------+-----------+---------+

Resilience4j Retry Configuration:
+-------------------+------+------+----------+
|     Instance      | Max  | Wait | Backoff  |
+-------------------+------+------+----------+
| insurerSubmission |  3   |  2s  | 2x exp.  |
| iciciLombard      |  3   |  1s  | 2x exp.  |
| bajajAllianz      |  5   |  3s  | 2x exp.  |
+-------------------+------+------+----------+
```

Bajaj Allianz has a higher retry count (5) and longer base wait (3s) because SOAP/XML endpoints tend to have longer recovery cycles. ICICI Lombard uses a shorter wait (1s) because their REST API recovers faster.

---

## Level 4: Code -- Key Design Patterns

> *How are specific components implemented at the code level?*

This level zooms into critical components to show implementation-level patterns, data structures, and design decisions.

### 4.1 Endorsement State Machine

The endorsement lifecycle is governed by an 11-state finite state machine. Every transition is validated by the `EndorsementStateMachine` domain service. Invalid transitions throw `IllegalStateException`.

#### State Machine Diagram

```
                                  +-------------------+
                                  |     CREATED       |
                                  +--------+----------+
                                           |
                                      validate
                                           |
                                           v
                                  +-------------------+
                                  |    VALIDATED      |
                                  +--------+----------+
                                           |
                                   grant provisional
                                      coverage
                                           |
                                           v
                                  +-------------------+
                                  | PROVISIONALLY     |
                                  | _COVERED          |
                                  +--------+----------+
                                           |
                          +----------------+----------------+
                          |                                 |
                  insurer supports             insurer is batch-only
                    real-time                   (e.g., Niva Bupa)
                          |                                 |
                          v                                 v
                +-----------------+               +-------------------+
                | SUBMITTED       |               | QUEUED_FOR_BATCH  |
                | _REALTIME       |               +--------+----------+
                +--------+--------+                        |
                         |                          batch assembly
                         |                          (every 15 min)
                         |                                 |
                         |                                 v
                         |                        +-------------------+
                         |                        | BATCH_SUBMITTED   |
                         |                        +--------+----------+
                         |                                 |
                         +----------+    +-----------------+
                                    |    |
                                    v    v
                              +-------------------+
                              | INSURER           |
                              | _PROCESSING       |
                              +--------+----------+
                                       |
                        +--------------+--------------+
                        |                             |
                    confirmed                     rejected
                        |                             |
                        v                             v
              +-------------------+         +-------------------+
              |    CONFIRMED      |         |    REJECTED       |
              | (terminal)        |         +--------+----------+
              +-------------------+                  |
                                        +------------+------------+
                                        |                         |
                                   canRetry()                can't retry
                                   (retries < 3)             (retries >= 3)
                                        |                         |
                                        v                         v
                              +-------------------+     +-------------------+
                              |  RETRY_PENDING    |     | FAILED_PERMANENT  |
                              +--------+----------+     | (terminal)        |
                                       |                +-------------------+
                              resubmit |
                              (RT or   |
                               Batch)  |
                                       v
                              Back to SUBMITTED_REALTIME
                                  or QUEUED_FOR_BATCH
```

#### State Transition Table

```
+-------------------------+--------------------------------------------------+
|    Current State        |              Allowed Transitions                 |
+-------------------------+--------------------------------------------------+
| CREATED                 | VALIDATED                                        |
| VALIDATED               | PROVISIONALLY_COVERED                            |
| PROVISIONALLY_COVERED   | SUBMITTED_REALTIME, QUEUED_FOR_BATCH             |
| SUBMITTED_REALTIME      | INSURER_PROCESSING, REJECTED                     |
| QUEUED_FOR_BATCH        | BATCH_SUBMITTED                                  |
| BATCH_SUBMITTED         | INSURER_PROCESSING, REJECTED                     |
| INSURER_PROCESSING      | CONFIRMED, REJECTED                              |
| CONFIRMED               | (terminal -- no transitions)                     |
| REJECTED                | RETRY_PENDING, FAILED_PERMANENT                  |
| RETRY_PENDING           | SUBMITTED_REALTIME, QUEUED_FOR_BATCH,            |
|                         | FAILED_PERMANENT                                 |
| FAILED_PERMANENT        | (terminal -- no transitions)                     |
+-------------------------+--------------------------------------------------+
```

#### Implementation: EnumSet-Based O(1) Validation

The state machine is implemented using Java's `EnumSet` for O(1) transition validation. Each `EndorsementStatus` enum value carries its set of allowed target states:

```java
public enum EndorsementStatus {
    CREATED(EnumSet.of(LazyRef.VALIDATED)),
    VALIDATED(EnumSet.of(LazyRef.PROVISIONALLY_COVERED)),
    PROVISIONALLY_COVERED(EnumSet.of(LazyRef.SUBMITTED_REALTIME, LazyRef.QUEUED_FOR_BATCH)),
    // ... each state declares its valid targets

    public boolean canTransitionTo(EndorsementStatus next) {
        return allowedTransitions.contains(LazyRef.valueOf(next.name()));
    }

    public boolean isTerminal() { return allowedTransitions.isEmpty(); }
}
```

This eliminates the need for external state machine libraries while remaining exhaustive and type-safe.

### 4.2 Event-Driven Architecture

#### EndorsementEvent -- Sealed Interface with 28 Event Types

The event model uses Java's `sealed interface` feature for exhaustive pattern matching. Every domain event implements the `EndorsementEvent` interface with four common fields: `endorsementId`, `occurredAt`, `employerId`, and `eventType`.

```
EndorsementEvent (sealed interface)
  |
  +-- Lifecycle Events (11):
  |     Created, Validated, ProvisionalCoverageGranted,
  |     SubmittedRealtime, QueuedForBatch, BatchSubmitted,
  |     InsurerProcessing, Confirmed, Rejected,
  |     RetryScheduled, FailedPermanent
  |
  +-- Financial Events (2):
  |     EADebited, EACredited
  |
  +-- Reconciliation Events (3):
  |     ReconciliationMatched, ReconciliationDiscrepancy,
  |     ReconciliationMissing
  |
  +-- Intelligence Events (8):
  |     AnomalyDetected, ForecastGenerated, BatchOptimized,
  |     ErrorAutoResolved, ErrorResolutionSuggested,
  |     ProcessMiningInsight, BalanceForecastAlert
  |
  +-- Coverage Events (2):
        ProvisionalCoverageExpired, ProvisionalCoverageConfirmed
```

Lifecycle (11) + Financial (2) + Reconciliation (3) + Intelligence (8) + Coverage (2) = **28 event types**

#### Event Flow

```
  Domain Action
       |
       v
+------------------+      +-------------------+      +------------------+
| EventPublisher   |----->| KafkaEvent        |----->| endorsement-     |
| (Port)           |      | Publisher          |      | events topic     |
+------------------+      | (Infrastructure)  |      | (32 partitions)  |
                          +-------------------+      +--------+---------+
                                                              |
                                Key: employerId               |
                                (partition ordering)          |
                                                              v
                                              +-------------------------------+
                                              | Downstream Consumers:         |
                                              |  - Notification Service       |
                                              |  - Analytics Pipeline         |
                                              |  - Audit Log                  |
                                              |  - Intelligence Services      |
                                              +-------------------------------+
```

#### Why Sealed Interface?

1. **Exhaustive `switch` expressions** -- The compiler verifies every event type is handled.
2. **No deserialization surprises** -- The set of event types is closed at compile time.
3. **Record immutability** -- Each event is a `record`, ensuring thread safety and structural equality.
4. **Self-documenting** -- The sealed hierarchy IS the event catalog.

#### Partition Key Strategy

Using `employerId` as the Kafka partition key ensures:
- All events for a single employer land on the same partition
- Consumers see events in causal order per employer
- EA account balance operations are never processed out of order
- No cross-employer contention for consumer threads

### 4.3 EA Account -- Rich Domain Model

The EA (Endorsement Account) is a financial account per employer-insurer pair. It demonstrates rich domain model design with encapsulated business logic.

#### Balance Model

```
+------------------------------------------------------------------+
|  EAAccount (employer_id, insurer_id)                             |
|                                                                    |
|  balance: 500,000.00      Total deposited funds                  |
|  reserved: 120,000.00     Funds reserved for pending endorsements |
|                                                                    |
|  availableBalance() = balance - reserved = 380,000.00             |
+------------------------------------------------------------------+

Operations:
  reserve(amount)   --> reserved += amount  (when endorsement created)
  debit(amount)     --> balance -= amount, reserved -= amount (when confirmed)
  credit(amount)    --> balance += amount   (when rejected / refund)
  releaseReserve(n) --> reserved -= amount  (when provisional coverage expires)
```

#### Key Design Decisions

- **Composite primary key** `(employer_id, insurer_id)` -- one balance per employer-insurer pair, natural key
- **Optimistic locking** via `version` column -- concurrent modifications detected without pessimistic DB locks
- **All mutations through domain methods** -- no direct field access; business rules enforced by the model

### 4.4 Data Model (13 Tables)

#### Entity-Relationship Diagram

```
                        +-------------------+
                        | insurer_          |
                        | configurations    |
                        | (V6/V7)           |
                        +--------+----------+
                                 |
                    1            | 1
                    |            |
          +---------+------------|------------------+
          |                      |                  |
          v                      v                  v
+-------------------+  +-------------------+  +-------------------+
| endorsements      |  | ea_accounts       |  | endorsement_      |
| (V1)              |  | (V2)              |  | batches (V3)      |
| 1M+ rows/day      |  | composite PK      |  |                   |
+--------+----------+  +--------+----------+  +-------------------+
         |                      |
    +----+----+                 |
    |    |    |                 v
    |    |    |         +-------------------+
    |    |    |         | ea_transactions   |
    |    |    |         | (V2)              |
    |    |    |         +-------------------+
    |    |    |
    |    |    +-------> +-------------------+
    |    |              | provisional_      |
    |    |              | coverages (V4)    |
    |    |              +-------------------+
    |    |
    |    +------------> +-------------------+
    |                   | endorsement_      |
    |                   | events (V5)       |
    |                   +-------------------+
    |
    +------+-------+-------+-------+
           |       |       |       |
           v       v       v       v
  +----------+ +----------+ +----------+ +----------+
  | recon_   | | anomaly_ | | balance_ | | error_   |
  | runs(V8) | | detect.  | | forecast | | resolut. |
  |          | | (V9)     | | (V10)    | | (V11)    |
  +----+-----+ +----------+ +----------+ +----------+
       |
       v
  +----------+                          +----------+
  | recon_   |                          | process_ |
  | items    |                          | mining_  |
  | (V8)     |                          | metrics  |
  +----------+                          | (V12)    |
                                        +----------+

V13: seed_intelligence_demo_data.sql
```

#### Table Schemas

```
+---------------------------------+    +----------------------------------+
| endorsements                    |    | ea_accounts                      |
| (V1)                            |    | (V2) PK: employer_id, insurer_id |
|---------------------------------|    |----------------------------------|
| id UUID PK                      |    | employer_id UUID                 |
| employer_id UUID [idx]          |    | insurer_id UUID                  |
| employee_id UUID [idx]          |    | balance DECIMAL(12,2)            |
| insurer_id UUID [idx]           |    | reserved DECIMAL(12,2)           |
| policy_id UUID                  |    | updated_at TIMESTAMPTZ           |
| type VARCHAR(20)                |    +----------------------------------+
| status VARCHAR(30) [idx]        |
| coverage_start_date DATE        |    +----------------------------------+
| coverage_end_date DATE          |    | ea_transactions                  |
| employee_data JSONB             |    | (V2)                             |
| premium_amount DECIMAL(12,2)    |    |----------------------------------|
| batch_id UUID [idx]             |    | id BIGSERIAL PK                  |
| insurer_reference VARCHAR(100)  |    | employer_id UUID [idx]           |
| retry_count INT                 |    | insurer_id UUID                  |
| failure_reason TEXT              |    | endorsement_id UUID FK           |
| idempotency_key VARCHAR UNIQUE  |    | type VARCHAR(20)                 |
| created_at TIMESTAMPTZ [idx]    |    | amount DECIMAL(12,2)             |
| updated_at TIMESTAMPTZ          |    | balance_after DECIMAL(12,2)      |
| version INT (optimistic lock)   |    | description TEXT                 |
+---------------------------------+    | created_at TIMESTAMPTZ           |
                                       +----------------------------------+

+----------------------------------+    +----------------------------------+
| endorsement_batches              |    | provisional_coverages            |
| (V3)                             |    | (V4)                             |
|----------------------------------|    |----------------------------------|
| id UUID PK                       |    | id UUID PK                       |
| insurer_id UUID                  |    | endorsement_id UUID FK           |
| status VARCHAR(20)               |    | employee_id UUID                 |
| endorsement_count INT            |    | coverage_start DATE              |
| insurer_batch_ref VARCHAR        |    | coverage_end DATE                |
| submitted_at TIMESTAMPTZ         |    | status VARCHAR(20)               |
| completed_at TIMESTAMPTZ         |    | created_at TIMESTAMPTZ           |
| created_at TIMESTAMPTZ           |    +----------------------------------+
+----------------------------------+

+----------------------------------+    +----------------------------------+
| endorsement_events               |    | insurer_configurations           |
| (V5)                             |    | (V6, seeded in V7)               |
|----------------------------------|    |----------------------------------|
| id BIGSERIAL PK                  |    | id UUID PK                       |
| endorsement_id UUID FK           |    | insurer_code VARCHAR UNIQUE      |
| event_type VARCHAR(50)           |    | insurer_name VARCHAR             |
| event_data JSONB                 |    | adapter_type VARCHAR(30)         |
| employer_id UUID                 |    | supports_real_time BOOLEAN       |
| created_at TIMESTAMPTZ           |    | supports_batch BOOLEAN           |
+----------------------------------+    | max_batch_size INT               |
                                        | batch_sla_hours INT              |
                                        | rate_limit_per_minute INT        |
                                        | active BOOLEAN                   |
                                        +----------------------------------+

+----------------------------------+    +----------------------------------+
| reconciliation_runs              |    | reconciliation_items             |
| (V8)                             |    | (V8)                             |
|----------------------------------|    |----------------------------------|
| id UUID PK                       |    | id UUID PK                       |
| insurer_id UUID                  |    | run_id UUID FK                   |
| started_at TIMESTAMPTZ           |    | endorsement_id UUID FK           |
| completed_at TIMESTAMPTZ         |    | outcome VARCHAR(20)              |
| total_count INT                  |    | insurer_reference VARCHAR        |
| matched_count INT                |    | details TEXT                     |
| discrepancy_count INT            |    | created_at TIMESTAMPTZ           |
| missing_count INT                |    +----------------------------------+
+----------------------------------+

+----------------------------------+    +----------------------------------+
| anomaly_detections               |    | balance_forecasts                |
| (V9)                             |    | (V10)                            |
|----------------------------------|    |----------------------------------|
| id UUID PK                       |    | id UUID PK                       |
| employer_id UUID                 |    | employer_id UUID                 |
| anomaly_type VARCHAR(30)         |    | insurer_id UUID                  |
| anomaly_score DOUBLE             |    | forecast_date DATE               |
| status VARCHAR(20)               |    | current_balance DECIMAL          |
| explanation TEXT                  |    | forecasted_need DECIMAL          |
| detected_at TIMESTAMPTZ          |    | shortfall DECIMAL                |
| reviewed_at TIMESTAMPTZ          |    | top_up_required BOOLEAN          |
| review_notes TEXT                 |    | narrative TEXT                   |
+----------------------------------+    | created_at TIMESTAMPTZ           |
                                        +----------------------------------+

+----------------------------------+    +----------------------------------+
| error_resolutions                |    | process_mining_metrics           |
| (V11)                            |    | (V12)                            |
|----------------------------------|    |----------------------------------|
| id UUID PK                       |    | id UUID PK                       |
| endorsement_id UUID FK           |    | insurer_id UUID                  |
| error_message TEXT               |    | metric_type VARCHAR(30)          |
| resolution_type VARCHAR(30)      |    | metric_value DOUBLE              |
| suggested_fix TEXT               |    | period_start TIMESTAMPTZ         |
| confidence DOUBLE                |    | period_end TIMESTAMPTZ           |
| auto_applied BOOLEAN             |    | details JSONB                    |
| status VARCHAR(20)               |    | created_at TIMESTAMPTZ           |
| created_at TIMESTAMPTZ           |    +----------------------------------+
+----------------------------------+

V13: seed_intelligence_demo_data.sql (demo anomalies, forecasts, resolutions)
```

#### Key Design Decisions

| Decision                         | Rationale                                                 |
|----------------------------------|-----------------------------------------------------------|
| **Composite PK on ea_accounts**  | Natural key `(employer_id, insurer_id)` -- one balance per employer-insurer pair |
| **JSONB for employee_data**      | Flexible schema for insurer-specific fields without ALTER TABLE |
| **Optimistic locking (version)** | Detects concurrent modifications without pessimistic DB locks |
| **Idempotency key (UNIQUE)**     | Prevents duplicate endorsement creation on retry           |
| **Separate events table**        | Immutable append-only audit trail, independent of endorsement mutations |
| **Flyway migrations**            | Versioned, repeatable, auditable schema evolution          |

---

## 5. Scalability Design

### Horizontal Scaling Model

```
                          Load Balancer
                               |
             +-----------------+-----------------+
             |                 |                 |
             v                 v                 v
   +-------------------+  +-------------------+  +-------------------+
   | Endorsement       |  | Endorsement       |  | Endorsement       |
   | Service           |  | Service           |  | Service           |
   | Instance 1        |  | Instance 2        |  | Instance N        |
   | (Stateless)       |  | (Stateless)       |  | (Stateless)       |
   +-------------------+  +-------------------+  +-------------------+
             |                 |                 |
             +-----------------+-----------------+
                               |
             +-----------------+-----------------+
             |                 |                 |
             v                 v                 v
   +-------------------+ +---------+ +--------------------+
   | PostgreSQL        | | Redis   | | Kafka Cluster      |
   | (Connection Pool) | | Cluster | | (32 partitions per |
   |                   | |         | |  event topic)      |
   +-------------------+ +---------+ +--------------------+
```

### Key Scaling Strategies

| Strategy                     | Implementation                                         |
|------------------------------|--------------------------------------------------------|
| **Stateless Services**       | No in-process state. Any instance handles any request.  |
| **Virtual Threads**          | Java 21 virtual threads (`spring.threads.virtual.enabled=true`). Eliminates thread-pool sizing as bottleneck. |
| **Kafka Partitioning**       | 32 partitions on the events topic. Up to 32 consumer instances can process events in parallel. |
| **Connection Pooling**       | HikariCP default pool. Configurable per deployment tier.|
| **Caching**                  | `@Cacheable` on `InsurerRegistry` with Caffeine (1000 entries, 60s TTL). Avoids DB round-trip on every insurer lookup. |
| **Batch Processing**         | Endorsements queued and assembled into batches every 15 minutes. Reduces per-endorsement API call overhead for batch-mode insurers. |
| **Idempotency Keys**         | `UNIQUE` constraint on `idempotency_key`. Safe to retry any create operation. |
| **Optimistic Locking**       | `version` column on endorsements. Concurrent updates detected and retried without pessimistic locks. |
| **Priority Sequencing**      | `EABalanceCalculator.sequenceForOptimalBalance()` processes deletions (P0) first to free balance before additions (P2). |

### Capacity Estimation

```
Target: 1M endorsements/day

Throughput:
  1,000,000 / 86,400s = ~12 endorsements/second (steady state)
  Peak (10x): ~120 endorsements/second

Kafka:
  32 partitions x 120 msgs/s = ~4 msgs/s per partition (well within limits)

Database:
  ~12 writes/s steady, ~120 writes/s peak
  PostgreSQL can handle 10,000+ TPS on moderate hardware
  Indexes on employer_id, status, batch_id, insurer_id, created_at

Batch Processing:
  Every 15 minutes = 96 batches/day
  ~10,400 endorsements per batch cycle at steady state
  With 4 insurers, ~2,600 endorsements per insurer per cycle
```

---

## 6. Observability Stack

### Four Pillars of Observability

```
+=========================================================================+
|                        OBSERVABILITY STACK                              |
+=========================================================================+
|                                                                         |
|  1. METRICS           2. TRACING          3. LOGGING        4. HEALTH  |
|                                                                         |
|  Micrometer           OpenTelemetry       Logback           Spring      |
|      |                    |                   |              Actuator    |
|      v                    v                   v                 |       |
|  Prometheus           Jaeger              Logstash              |       |
|  (:9090)              (:16686)            (:5044)               |       |
|      |                                        |                 |       |
|      v                                        v                 |       |
|  Grafana                                 Elasticsearch          |       |
|  (:3000)                                 (:9200)                |       |
|  7 dashboards                                 |                 |       |
|                                               v                 v       |
|                                          Kibana           /actuator/    |
|                                          (:5601)          health        |
+=========================================================================+
```

### Grafana Dashboards (7)

```
+----------------------------------+----------------------------------------+
|          Dashboard               |           Key Panels                   |
+----------------------------------+----------------------------------------+
| application-overview.json        | Request rates, latency p50/p95/p99,    |
|                                  | error rates, JVM metrics               |
+----------------------------------+----------------------------------------+
| endorsement-business.json        | Endorsement creation rate, status      |
|                                  | distribution, type breakdown,          |
|                                  | time-to-confirmation                   |
+----------------------------------+----------------------------------------+
| infrastructure-health.json       | DB connections, Kafka lag, Redis       |
|                                  | hit rate, JVM heap, GC pauses          |
+----------------------------------+----------------------------------------+
| multi-insurer-monitoring.json    | Per-insurer submission latency,        |
|                                  | circuit breaker state, success rates   |
+----------------------------------+----------------------------------------+
| reconciliation-monitoring.json   | Reconciliation run outcomes, match     |
|                                  | rates, discrepancy trends              |
+----------------------------------+----------------------------------------+
| intelligence-monitoring.json     | Anomaly detection rates, forecast      |
|                                  | accuracy, error resolution stats,      |
|                                  | STP rate trends                        |
+----------------------------------+----------------------------------------+
| scheduler-monitoring.json        | Scheduler execution times, batch       |
|                                  | sizes, failure counts per scheduler    |
+----------------------------------+----------------------------------------+
```

### Custom Metrics Catalog

The application exports domain-specific metrics via Micrometer:

```
Counters:
  endorsement.created          {type, insurer}
  endorsement.confirmed        {insurer}
  endorsement.rejected         {insurer, reason}
  endorsement.kafka.publish    {result, eventType}
  endorsement.error            {type}

Timers:
  endorsement.insurer.mock.duration      {method}
  endorsement.insurer.icici.duration     {method}
  endorsement.insurer.nivabupa.duration  {method}
  endorsement.insurer.bajaj.duration     {method}

Gauges:
  endorsement.pending.count    {insurer}
  ea.balance.available         {employer, insurer}

Distribution Summaries:
  http.server.requests                        (p50, p95, p99 + histogram)
  endorsement.insurer.submission.duration     (p50, p95, p99 + histogram)
```

### Distributed Tracing

OpenTelemetry integration with 100% sampling in development:

```
management.tracing.sampling.probability: 1.0

Baggage propagation:
  - endorsementId   (propagated across service boundaries)
  - employerId      (propagated across service boundaries)

Trace flow for a single endorsement:
  [API Request] --> [CreateHandler] --> [StateMachine] --> [EAAccount.reserve]
       |                                                         |
       +-- [EventPublisher.publish] --> [KafkaTemplate.send]     |
                                                                 |
       +-- [JpaEndorsementAdapter.save] --> [PostgreSQL]  <------+
```

### Structured Logging

```
MDC fields injected by MdcRequestFilter:
  - requestId       (UUID per request)
  - endorsementId   (when available)
  - employerId      (when available)
  - kafkaEventType  (during event publishing)

Log pipeline:
  Application (Logback JSON) --> Logstash (:5044) --> Elasticsearch --> Kibana

Log format (structured JSON):
{
  "timestamp": "2026-03-15T10:15:30.123Z",
  "level": "INFO",
  "logger": "c.p.e.a.h.CreateEndorsementHandler",
  "message": "Endorsement created",
  "requestId": "abc-123",
  "endorsementId": "def-456",
  "employerId": "ghi-789",
  "thread": "virtual-42"
}
```

### Health Checks

Spring Actuator exposes health and operational endpoints:

```
/actuator/health             -- Composite health (DB, Kafka, Redis, disk)
/actuator/info               -- Application version and build info
/actuator/metrics            -- Micrometer metric names
/actuator/prometheus         -- Prometheus-format metrics scrape endpoint
/actuator/circuitbreakers    -- Resilience4j circuit breaker states
/actuator/retries            -- Resilience4j retry statistics
```

---

## 7. Architecture Decision Records

| Decision                         | Alternative Considered         | Why This Choice                                        |
|----------------------------------|--------------------------------|--------------------------------------------------------|
| Hexagonal Architecture           | Layered (N-tier)               | Insurer adapters and intelligence engines must be independently swappable. Hexagonal enforces this at the type level. |
| Sealed Interface for Events      | Abstract class hierarchy       | Compiler-enforced exhaustiveness. Records give immutability and structural equality for free. |
| Kafka for Event Bus              | RabbitMQ, Redis Streams        | Partition-based ordering by employerId. Replay capability. Proven at 1M+/day scale. |
| PostgreSQL + JSONB               | MongoDB, DynamoDB              | ACID for financial operations (EA account debits). JSONB for flexible employee_data without schema explosion. |
| Flyway over Liquibase            | Liquibase, manual DDL          | SQL-first migrations. Simpler mental model. Works natively with PostgreSQL. |
| Resilience4j                     | Hystrix, Sentinel              | Spring Boot 3 native. Per-insurer circuit breaker tuning. Decorator-based (annotations). |
| Java 21 Virtual Threads          | Reactive (WebFlux)             | Imperative code style with reactive scalability. No callback hell. Familiar to all Java developers. |
| Caffeine + Redis Caching         | Redis-only, Hazelcast          | Two-tier: Caffeine for hot path (InsurerRegistry), Redis for distributed invalidation across instances. |
| Optimistic Locking               | Pessimistic (SELECT FOR UPDATE)| Higher throughput under normal conditions. Retries on conflict are rare at expected scale. |
| RFC 7807 Problem Details         | Custom error JSON              | Industry standard. Machine-readable. Extensible with custom properties. |
| C4 Model for Documentation       | Arc42, custom format           | Industry standard. Hierarchical zoom levels. Accessible to both technical and non-technical stakeholders. |

---

*This document reflects the implemented system as of 2026-03-15. All components described are backed by working code, tests, and infrastructure configuration in the repository. Structured using the C4 Model (Simon Brown) to provide consistent, zoomable architecture views.*
