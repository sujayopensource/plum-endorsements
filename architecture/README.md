# Plum Endorsement System — Architecture Visualization

Interactive C4 architecture model built with [Structurizr](https://structurizr.com/) DSL. Covers all four C4 levels plus dynamic flows and deployment views.

## Quick Start

```bash
./start.sh
```

Then open **http://localhost:8200** in your browser.

> Requires Docker. The script pulls and runs [Structurizr Lite](https://structurizr.com/help/lite) in a container.

To stop: `docker stop plum-structurizr`

To use a different port: `STRUCTURIZR_PORT=9090 ./start.sh`

---

## View Catalogue

The model contains **12 views** organized by C4 level. Use the dropdown in the Structurizr UI to navigate between them.

### Level 1 — System Context (`L1_SystemContext`)

**What it shows:** The Plum Endorsement System as a single box, surrounded by its users and external systems.

| Element | Description |
|---------|-------------|
| HR Administrator | Creates and manages endorsements |
| Finance Team | Monitors EA balances and forecasts |
| Operations Team | Reviews anomalies, manages insurers, monitors health |
| ICICI Lombard API | REST/JSON insurer (real-time only) |
| Niva Bupa SFTP | CSV/SFTP insurer (batch only) |
| Bajaj Allianz API | SOAP/XML insurer (real-time + batch) |
| Ollama LLM | Local LLM for GenAI anomaly enrichment and error resolution |

**Key insight:** The system integrates with 3 insurers via 3 different protocols and augments its intelligence with a local LLM.

---

### Level 2 — Container Diagram (`L2_Containers`)

**What it shows:** The 12 deployable units and their communication protocols.

| Container | Technology | Purpose |
|-----------|-----------|---------|
| React Dashboard | React 19, TypeScript, Vite | 10-screen SPA |
| Endorsement Service | Spring Boot 3.4, Java 21 | Core backend (hexagonal architecture) |
| PostgreSQL | PostgreSQL 16 | 13 tables, 20 Flyway migrations |
| Redis | Redis 7 | Distributed cache (60s TTL) |
| Kafka | Apache Kafka 3.7 (KRaft) | 4 topics, 88 partitions |
| Prometheus | Prometheus 2.50 | Metrics scraping |
| Grafana | Grafana 10.3 | 7 dashboards |
| Jaeger | Jaeger 1.55 | Distributed tracing |
| Elasticsearch | Elasticsearch 8.12 | Log storage |
| Logstash | Logstash 8.12 | Log pipeline |
| Kibana | Kibana 8.12 | Log search UI |

**Key insight:** Full observability stack (metrics + tracing + logging) is baked in from day one, not bolted on.

---

### Level 3 — Component Diagram (`L3_Components`)

**What it shows:** The internal structure of the Endorsement Service, organized into 4 hexagonal architecture layers.

#### API Layer (Driving Side) — 7 components
- 6 REST controllers exposing 27 endpoints
- `GlobalExceptionHandler` for RFC 7807 error responses

#### Application Layer — 19 components
- 3 CQRS handlers (2 command, 1 query)
- 8 domain services (anomaly detection, forecasting, error resolution, process mining, health scoring, benchmarking, audit, reconciliation)
- 8 schedulers (batch assembly, anomaly scan, forecast, reconciliation, process mining, provisional coverage cleanup, stuck retry, data retention)

#### Domain Core — 17 components
- 4 domain models (`Endorsement`, `EAAccount`, `EndorsementEvent`, `EndorsementStatus`)
- 3 domain services (`EndorsementStateMachine`, `EABalanceCalculator`, `InsurerRegistry`)
- 10 port interfaces (repositories, external system ports, intelligence ports)
- **Zero infrastructure imports** — pure business logic

#### Infrastructure Layer (Driven Side) — 20+ components
- 3 JPA persistence adapters + `EndorsementMapper` (Anti-Corruption Layer)
- 5 insurer adapters (Mock, ICICI Lombard, Niva Bupa, Bajaj Allianz) + `InsurerRouter` (Factory)
- 7 intelligence adapters (5 rule-based + 2 Ollama GenAI)
- `KafkaEventPublisher` for event publishing
- Cross-cutting: Security, MDC filter, metrics, ShedLock

**Key insight:** Domain Core defines port interfaces; Infrastructure implements them. Dependencies always point inward.

---

### Level 3 Filtered Views

These views zoom into specific architectural concerns:

| View | Key | What It Shows |
|------|-----|---------------|
| **API Layer** | `L3_APILayer` | Controllers, handlers, and exception handling. How HTTP requests flow to CQRS handlers. |
| **Domain Core** | `L3_DomainCore` | Models, domain services, and port interfaces. The pure business logic layer with zero infrastructure. |
| **Intelligence** | `L3_Intelligence` | 5 rule-based adapters + 2 Ollama GenAI adapters behind port interfaces. Shows the 3-stage GenAI evolution pattern. |
| **Insurer Adapters** | `L3_InsurerAdapters` | Strategy pattern: `InsurerRouter` (Factory) resolves the correct adapter at runtime from DB config. Per-insurer circuit breakers. |

---

### Dynamic Views — Runtime Flows

These views show how components interact at runtime for key use cases.

#### Create Endorsement (`Dyn_CreateEndorsement`)

```
HR Admin → React SPA → POST /api/v1/endorsements
  → Idempotency check (SELECT by key)
  → Reserve EA balance + save endorsement
  → Publish EndorsementEvent.Created to Kafka
  → Return 201 + endorsement ID
```

**Patterns demonstrated:** Idempotency (CNP Ch. 10), Event notification (CNP Ch. 4), CQRS command (CNP Ch. 13)

#### Submit to Insurer (`Dyn_InsurerSubmission`)

```
React SPA → POST /api/v1/endorsements/{id}/submit
  → Lookup insurer config (Redis cache)
  → Submit via resolved adapter (@CircuitBreaker + @Retry)
  → Update status to CONFIRMED
  → Publish EndorsementEvent.Confirmed
```

**Patterns demonstrated:** Strategy (HFDP Ch. 1), Factory (HFDP Ch. 4), Circuit Breaker (CNP Ch. 11), Retry (CNP Ch. 10)

#### Batch Assembly (`Dyn_BatchAssembly`)

```
Scheduler (every 15 min)
  → Find QUEUED_FOR_BATCH endorsements
  → Group by insurer, optimize order (Knapsack DP)
  → Upload CSV to Niva Bupa (SFTP)
  → Update to BATCH_SUBMITTED
  → Publish EndorsementEvent.BatchSubmitted
```

**Patterns demonstrated:** Template Method (HFDP Ch. 8), Strategy (HFDP Ch. 1), Event notification (CNP Ch. 4)

#### Anomaly Detection (`Dyn_AnomalyDetection`)

```
Scheduler (every 5 min)
  → Load recent endorsements per employer
  → Run 5 rules, compute scores (0-1)
  → Send scores > 0.7 to Ollama LLM for enrichment
  → Save anomaly with score + LLM explanation
  → Publish EndorsementEvent.AnomalyDetected
```

**Patterns demonstrated:** Strategy (rule-based vs. LLM), Circuit Breaker (Ollama fallback to rules), Observer (event notification)

---

### Deployment Views

#### Docker Compose — Development (`Deploy_DockerCompose`)

Shows all 11 containers running on a single developer machine via `docker-compose.yml`. Each container has health checks and the full observability stack is included for development parity with production.

#### Kubernetes — Production (`Deploy_Kubernetes`)

Shows the production deployment topology:
- **Backend:** Deployment with HPA (2-8 pods at 70% CPU), PodDisruptionBudget (minAvailable: 1)
- **PostgreSQL:** StatefulSet with PVC-backed storage
- **Kafka:** StatefulSet in KRaft mode
- **Observability Stack:** Prometheus + Grafana + Jaeger
- **ELK Stack:** Elasticsearch + Logstash + Kibana
- **Ingress Controller:** NGINX for external traffic routing

---

## Color Legend

| Color | Meaning |
|-------|---------|
| Purple (#4338CA) | People / Users |
| Indigo (#6366F1) | Primary system / Application services |
| Blue (#3B82F6) | Controllers / Web browser |
| Violet (#8B5CF6) | Handlers (CQRS) |
| Cyan (#0891B2) | Insurers / Schedulers |
| Green (#059669) | Domain models / Monitoring |
| Teal (#14B8A6) | Ports (dashed border) |
| Amber (#F59E0B) | Infrastructure adapters |
| Purple (#A855F7) | Intelligence adapters |
| Red (#EF4444) | Anti-Corruption Layer (diamond) |
| Orange (#F97316) | Factory (hexagon) |
| Slate (#64748B) | External systems / Cross-cutting |

---

## Files

| File | Purpose |
|------|---------|
| `workspace.dsl` | Structurizr DSL model — the single source of truth |
| `start.sh` | One-click script to launch Structurizr Lite via Docker |
| `README.md` | This walkthrough |

---

## Editing the Model

The `workspace.dsl` file is plain text. Edit it with any text editor. Structurizr Lite auto-reloads on file save — no restart needed.

**DSL reference:** https://docs.structurizr.com/dsl/language

**Common operations:**
- Add a component: add inside the `backend = container ... { }` block
- Add a relationship: `source -> destination "description" "technology"`
- Add a view: add inside the `views { }` block
- Change styles: modify the `styles { }` block
