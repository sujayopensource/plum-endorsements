# CALM Architectural Governance

> Architecture as Code for the Plum Endorsement Service using [FINOS CALM](https://github.com/finos/architecture-as-code) (Common Architecture Language Model)

## What is CALM?

CALM is the **"architecture as code"** specification developed by FINOS (Fintech Open Source Foundation), used at Morgan Stanley for 1,400+ deployments. It enables:

- **Machine-readable architecture definitions** — JSON schemas describing nodes, relationships, and flows
- **Reusable patterns** — Templates that define required architectural structures
- **Automated validation** — CI/CD-enforced checks that architecture matches patterns
- **Controls** — Security, resilience, and data integrity rules mapped to implementation

## Why Architecture Governance?

The existing `architecture/` folder contains C4 diagrams (Structurizr DSL) for **visual documentation**. This `architectural-governance/` folder adds **enforceable rules**:

| Concern | C4 (Structurizr) | CALM (This Folder) |
|---------|-------------------|---------------------|
| Purpose | Visual diagrams for humans | Machine-readable definitions for CI/CD |
| Validation | Manual review | Automated (`calm validate` + Spectral) |
| Enforcement | Convention | CI pipeline blocks non-compliant PRs |
| Controls | Not modeled | Security, resilience, data integrity |
| Drift detection | Not supported | Pattern validation catches drift |

## Folder Structure

```
architectural-governance/
├── README.md                                    # This file
├── package.json                                 # @finos/calm-cli + spectral deps
├── patterns/                                    # 5 reusable architectural templates
│   ├── hexagonal-service.pattern.json           # Ports-and-adapters layer structure
│   ├── event-driven-service.pattern.json        # Kafka event architecture
│   ├── multi-insurer-integration.pattern.json   # Strategy + Factory pattern
│   ├── observability-stack.pattern.json         # Metrics + tracing + logging
│   └── resilient-external-integration.pattern.json  # CB + retry on external calls
├── architecture/                                # Concrete system instantiation
│   └── plum-endorsement-system.json             # 22 nodes, 21 relationships, 4 flows
├── controls/                                    # Security, resilience, data integrity
│   ├── security-controls.json                   # Stateless sessions, CORS, rate limiting
│   ├── resilience-controls.json                 # 6 circuit breakers, retry policies, idempotency
│   └── data-integrity-controls.json             # ACID, optimistic locking, Flyway, ACL
├── governance/                                  # Enforcement tooling
│   ├── enforcement-strategy.md                  # Three-tier governance model
│   ├── spectral-rules/.spectral.yml             # Custom linting rules
│   └── ci-pipeline.yml                          # GitHub Actions workflow
└── scripts/
    ├── setup.sh                                 # Install CALM CLI + Spectral
    └── validate.sh                              # One-click validation
```

## Quick Start

### Setup

```bash
cd architectural-governance
./scripts/setup.sh
```

This installs `@finos/calm-cli` and `@stoplight/spectral-cli` via npm.

### Validate

```bash
./scripts/validate.sh
```

Runs all validation steps:
1. JSON syntax validation on all `.json` files
2. CALM pattern validation (architecture vs 5 patterns)
3. Spectral custom rules (naming, completeness, resilience controls)
4. Architecture completeness report (node/relationship/flow counts)

## Patterns → Codebase Mapping

Each pattern maps directly to design patterns from the CLAUDE.md design guide:

| Pattern | Book Source | Codebase Implementation |
|---------|------------|------------------------|
| `hexagonal-service` | HFDP Ch. 7 (Adapter), CNP Ch. 6 (Stateless) | `domain/port/`, `infrastructure/persistence/adapter/` |
| `event-driven-service` | CNP Ch. 4–5 (Events), HFDP Ch. 2 (Observer) | `domain/model/EndorsementEvent.java`, `KafkaEventPublisher` |
| `multi-insurer-integration` | HFDP Ch. 1 (Strategy), Ch. 4 (Factory) | `InsurerPort`, `InsurerRouter`, 4 adapter classes |
| `observability-stack` | CNP Ch. 12 (Tracing & Logging) | `MetricsConfig`, `MdcRequestFilter`, `logback-spring.xml` |
| `resilient-external-integration` | CNP Ch. 10–11 (Retry & CB) | `@CircuitBreaker`, `@Retry` in adapters, `application.yml` |

## Controls

### Security Controls (6)
- Stateless sessions (`SecurityConfig.java`)
- CORS policy (allowed origins)
- CSRF disabled (justified for stateless JWT)
- Rate limiting (configurable, disabled in dev)
- Input validation (Jakarta Bean Validation)
- Structured error responses (no stack trace leaks)

### Resilience Controls (14)
- 6 circuit breaker instances (insurerSubmission, iciciLombard, bajajAllianz, webhookNotification, ollamaAnomalyDetection, ollamaErrorResolution)
- 6 retry policies with exponential backoff
- Idempotency keys (database unique constraint)
- Domain-level retry (max 3 retries for REJECTED endorsements)

### Data Integrity Controls (10)
- ACID transactions on command handlers
- Optimistic locking (@Version on entities)
- 20 Flyway migrations (V1–V20)
- Open-in-view disabled
- DDL validation mode (no auto-DDL)
- Batch operations (25 batch size)
- HikariCP connection pool (30 max)
- Kafka acks=all
- State machine transitions (11-state lifecycle)
- Anti-corruption layer (EndorsementMapper)

## Architecture at a Glance

**22 nodes** across 5 categories:

| Category | Nodes | Examples |
|----------|-------|---------|
| Actors | 3 | HR Admin, Finance Team, Ops Team |
| Application | 5 | Dashboard, Service, 4 hexagonal layers |
| Data Stores | 3 | PostgreSQL, Redis, Elasticsearch |
| Infrastructure | 5 | Kafka, Prometheus, Jaeger, Logstash, Kibana, Grafana |
| External Systems | 4 | ICICI Lombard, Niva Bupa, Bajaj Allianz, Ollama |

**4 flows** covering the core business processes:

1. **Endorsement Creation** — HR Admin → Dashboard → idempotency check → EA debit → Kafka event
2. **Insurer Submission** — InsurerRouter → adapter → @CircuitBreaker → confirm/reject
3. **Batch Assembly** — Scheduler → Knapsack DP → SFTP upload → Kafka event
4. **Anomaly Detection** — 5 rules → score > 0.7 → Ollama LLM → Kafka event

## Extending the Architecture

### Adding a New Insurer

1. Add `"node-type": "system"` node to `plum-endorsement-system.json`
2. Add relationship with `controls` referencing circuit breaker + retry
3. Add circuit breaker config to `resilience-controls.json`
4. Run `./scripts/validate.sh`

### Adding a New Infrastructure Component

1. Add node to architecture JSON
2. Add relationships (source/destination)
3. Verify the relevant pattern still validates
4. Run `./scripts/validate.sh`

## Enforcement Strategy

Three tiers of governance:

| Tier | Tool | Trigger | Blocking |
|------|------|---------|----------|
| Schema | `calm validate` | PR to governance/ or src/ | Yes |
| Rules | Spectral | Same trigger | Yes |
| Review | Manual ADR | New pattern or control | Yes |

See `governance/enforcement-strategy.md` for full details.
