# Plum Endorsement Service — Getting Started Guide

> **Audience**: Engineers, Engineering Managers, C-Level Executives
> **Version**: 1.0 | March 14, 2026

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Prerequisites](#2-prerequisites)
3. [One-Click Start — Up and Running in 2 Minutes](#3-one-click-start--up-and-running-in-2-minutes)
4. [What Gets Started](#4-what-gets-started)
5. [Navigating the System](#5-navigating-the-system)
   - [5.1 Application — Where Business Users Work](#51-application--where-business-users-work)
   - [5.2 API — Where Integrations Connect](#52-api--where-integrations-connect)
   - [5.3 Observability — Where Ops Teams Monitor](#53-observability--where-ops-teams-monitor)
6. [Running Tests](#6-running-tests)
   - [6.1 Run Everything (Recommended)](#61-run-everything-recommended)
   - [6.2 Run Individual Test Suites](#62-run-individual-test-suites)
   - [6.3 Reading Test Reports](#63-reading-test-reports)
7. [Shell Script Reference](#7-shell-script-reference)
8. [Demo Data — Pre-Seeded IDs](#8-demo-data--pre-seeded-ids)
9. [Stopping the System](#9-stopping-the-system)
10. [Troubleshooting](#10-troubleshooting)
11. [For Managers & Executives — What to Look At](#11-for-managers--executives--what-to-look-at)

---

## 1. Executive Summary

The Plum Endorsement Service manages insurance policy changes (add/remove/update employees) for employer-sponsored group health insurance. This guide walks you through starting the system, navigating the UI and infrastructure, and running the comprehensive test suite.

**What you get after running the installer**:
- A fully functional application with frontend, backend, and 9 infrastructure services
- Pre-seeded demo data for 4 insurers (ICICI Lombard, Niva Bupa, Bajaj Allianz, Mock)
- 7 pre-built Grafana dashboards for real-time monitoring
- Distributed tracing, centralized logging, and metrics collection — all wired up

**Test coverage**: 790+ automated tests across 5 categories (unit, API, BDD, E2E, performance), all passing.

---

## 2. Prerequisites

| Prerequisite | Minimum Version | Check Command | Install |
|-------------|----------------|---------------|---------|
| **Java (JDK)** | 21+ | `java -version` | `brew install openjdk@21` |
| **Node.js** | 18+ | `node -v` | `brew install node` |
| **npm** | 8+ | `npm -v` | Included with Node.js |
| **Docker Desktop** | 4.0+ | `docker info` | [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop) |
| **Docker Compose** | v2 | `docker compose version` | Included with Docker Desktop |

> **Note**: Docker Desktop must be **running** before you start. The installer checks all prerequisites and will report exactly what's missing.

---

## 3. One-Click Start — Up and Running in 2 Minutes

```bash
# Clone and enter the project
cd plum-endorsements

# Start everything
./start.sh
```

That's it. The installer will:

1. Check all prerequisites (Java, Node, Docker)
2. Start 9 infrastructure containers (PostgreSQL, Redis, Kafka, Jaeger, ELK stack, Prometheus, Grafana)
3. Wait for all containers to be healthy
4. Build the Spring Boot backend
5. Install frontend dependencies
6. Start the backend on port 8080
7. Seed demo data (4 insurer EA accounts, intelligence demo data)
8. Start the frontend on port 5173

When you see the green banner **"All services are running!"**, open your browser to **http://localhost:5173**.

---

## 4. What Gets Started

The installer brings up 11 services across 3 layers:

### Application Layer

| Service | URL | What It Does |
|---------|-----|-------------|
| **Frontend** | [localhost:5173](http://localhost:5173) | React UI — dashboards, endorsement management, intelligence |
| **Backend API** | [localhost:8080/api/v1](http://localhost:8080/api/v1) | Spring Boot REST API — business logic, event processing |
| **Swagger UI** | [localhost:8080/swagger-ui](http://localhost:8080/swagger-ui) | Interactive API documentation and testing |

### Data & Messaging Layer

| Service | Port | What It Does |
|---------|------|-------------|
| **PostgreSQL** | 5432 | Primary database — endorsements, EA accounts, configs |
| **Redis** | 6379 | Caching — insurer configurations, rate limiting |
| **Kafka** | 9092 | Event streaming — 4 topics, 88 partitions |

### Observability Layer

| Service | URL | Credentials | What It Does |
|---------|-----|-------------|-------------|
| **Grafana** | [localhost:3000](http://localhost:3000) | admin / plum | 7 pre-built dashboards for metrics visualization |
| **Prometheus** | [localhost:9090](http://localhost:9090) | — | Metrics collection and querying |
| **Jaeger** | [localhost:16686](http://localhost:16686) | — | Distributed tracing — follow requests across services |
| **Kibana** | [localhost:5601](http://localhost:5601) | — | Log search and visualization |
| **Elasticsearch** | 9200 | — | Log storage and indexing |

---

## 5. Navigating the System

### 5.1 Application — Where Business Users Work

Open **http://localhost:5173** in your browser. The sidebar navigation provides access to every feature:

| Page | URL Path | What You See |
|------|----------|-------------|
| **Dashboard** | `/` | KPI cards (total, pending, confirmed), EA balance, recent endorsements, active batches |
| **Endorsements** | `/endorsements` | Filterable table of all endorsements — sort, search by employer, filter by status |
| **Create Endorsement** | `/endorsements/new` | Form to create ADD/DELETE/UPDATE endorsements with validation |
| **Endorsement Detail** | `/endorsements/{id}` | Full lifecycle view — status timeline, coverage card, submit/confirm/reject actions |
| **Batches** | `/batches` | Batch assembly progress by employer — see which endorsements are grouped |
| **EA Accounts** | `/ea-accounts` | Employer Advance balance lookup — total balance, reserved, available |
| **Insurers** | `/insurers` | Self-service insurer onboarding — view, create, edit, deactivate insurer configs |
| **Reconciliation** | `/reconciliation` | Trigger and view reconciliation runs by insurer |
| **Intelligence** | `/intelligence` | AI/ML dashboard with 4 tabs: Anomalies, Forecasts, Error Resolutions, Process Mining |
| **Notifications** | Bell icon (top-right) | Real-time WebSocket notifications — endorsement status changes |

**Quick walkthrough for first-timers:**

1. Start on the **Dashboard** — see the system overview
2. Click **Create Endorsement** — fill in the form, submit
3. View the new endorsement in **Endorsement Detail** — see the status timeline
4. Check **EA Accounts** — look up balance for employer `11111111-1111-1111-1111-111111111111`
5. Visit **Intelligence** — explore anomaly detection, forecasts, error resolutions, process mining

### 5.2 API — Where Integrations Connect

Open **http://localhost:8080/swagger-ui** for interactive API docs. Key endpoints:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/endorsements` | POST | Create a new endorsement |
| `/api/v1/endorsements/{id}` | GET | Get endorsement details |
| `/api/v1/endorsements/{id}/submit` | POST | Submit to insurer |
| `/api/v1/endorsements/{id}/confirm` | POST | Confirm (insurer accepted) |
| `/api/v1/endorsements/{id}/reject` | POST | Reject (insurer rejected) |
| `/api/v1/ea-accounts` | GET | Look up EA account balance |
| `/api/v1/intelligence/anomalies` | GET | List detected anomalies |
| `/api/v1/intelligence/forecasts/generate` | POST | Generate balance forecast |
| `/api/v1/intelligence/error-resolutions/resolve` | POST | Auto-resolve insurer errors |
| `/api/v1/intelligence/process-mining/metrics` | GET | Get process mining metrics |
| `/api/v1/audit-logs` | GET | Query audit trail |
| `/api/v1/reconciliation/trigger` | POST | Trigger reconciliation run |
| `/api/v1/insurer-configurations` | GET/POST | Manage insurer configs |

**Quick test from terminal:**

```bash
# Create an endorsement
curl -s -X POST http://localhost:8080/api/v1/endorsements \
  -H 'Content-Type: application/json' \
  -d '{
    "employerId": "11111111-1111-1111-1111-111111111111",
    "employeeId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "insurerId": "22222222-2222-2222-2222-222222222222",
    "policyId": "33333333-3333-3333-3333-333333333333",
    "type": "ADD",
    "coverageStartDate": "2026-03-14",
    "premiumAmount": 1500.00,
    "employeeData": {"name": "Test Employee"}
  }' | python3 -m json.tool

# Check EA account balance
curl -s "http://localhost:8080/api/v1/ea-accounts?employerId=11111111-1111-1111-1111-111111111111&insurerId=22222222-2222-2222-2222-222222222222" | python3 -m json.tool

# Check health
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

### 5.3 Observability — Where Ops Teams Monitor

#### Grafana — Metrics Dashboards

Open **http://localhost:3000** (login: `admin` / `plum`). Navigate to **Dashboards** in the left sidebar. 7 pre-built dashboards are available:

| Dashboard | What It Shows | Who Should Look |
|-----------|--------------|----------------|
| **Application Overview** | Request rates, response times, error rates, JVM metrics | Engineers, SREs |
| **Endorsement Business** | Endorsements created/confirmed/rejected, lifecycle timing | Product Managers, Execs |
| **Infrastructure Health** | CPU, memory, database connections, Kafka lag | SREs, Platform Engineers |
| **Intelligence Monitoring** | Anomaly detection rates, forecast accuracy, error resolution stats | Data/ML Engineers |
| **Multi-Insurer Monitoring** | Per-insurer submission rates, circuit breaker states, STP rates | Operations, Insurer Relations |
| **Reconciliation Monitoring** | Reconciliation run outcomes, mismatch rates | Finance, Operations |
| **Scheduler Monitoring** | Batch assembly, anomaly detection, forecast scheduler health | Engineers, SREs |

#### Jaeger — Distributed Tracing

Open **http://localhost:16686**. Select service `endorsement-service` from the dropdown and click **Find Traces**. Click any trace to see the full request flow — from HTTP request through database queries, Kafka publishing, and insurer submission.

**When to use it**: Investigating slow requests, understanding request flow, debugging errors.

#### Kibana — Log Search

Open **http://localhost:5601**. Navigate to **Discover** to search structured logs. Every log entry includes `traceId`, `spanId`, `requestId`, `endorsementId`, and `employerId` for correlation.

**When to use it**: Searching for errors, auditing operations, correlating events across requests.

#### Prometheus — Raw Metrics

Open **http://localhost:9090**. Use the expression browser to query 20+ custom metrics:

```promql
# Endorsements created per minute
rate(endorsement_created_total[5m])

# Insurer submission latency (p95)
histogram_quantile(0.95, rate(endorsement_insurer_submission_duration_seconds_bucket[5m]))

# Active endorsements by status
endorsement_active_count
```

---

## 6. Running Tests

### 6.1 Run Everything (Recommended)

```bash
./run-all-tests.sh
```

This runs **all 4 test suites** sequentially and produces a **combined Allure report**:

1. **API Tests** (RestAssured) — 106 tests against the running backend with Testcontainers
2. **BDD Tests** (Cucumber) — 89 scenario tests covering business rules
3. **E2E Tests** (Playwright) — 156 tests against the real UI and backend
4. **Performance Tests** (Gatling) — Baseline simulation validating p95 < 2s

**When it finishes**, the combined Allure report opens automatically at:
**http://localhost:5050/allure-docker-service/latest-report**

**Options:**

```bash
./run-all-tests.sh                # Run everything
./run-all-tests.sh --skip-perf    # Skip performance tests (faster)
./run-all-tests.sh --report       # Regenerate report from last run (no re-execution)
./run-all-tests.sh --stop         # Stop the Allure report server
```

### 6.2 Run Individual Test Suites

Each test suite has its own runner script:

#### API Tests

```bash
./run-api-tests.sh                # Run API tests, generate Allure report on :5050
./run-api-tests.sh --report       # Regenerate report only
./run-api-tests.sh --stop         # Stop the Allure server
```

Tests endpoint contracts, error handling, pagination, authentication, and data integrity using RestAssured. Testcontainers spins up isolated PostgreSQL, Redis, and Kafka instances — no dependency on the running app.

#### BDD Tests

```bash
./run-behaviour-tests.sh          # Run BDD tests, generate Allure report on :5051
./run-behaviour-tests.sh --report # Regenerate report only
./run-behaviour-tests.sh --stop   # Stop the Allure server
```

Cucumber feature files test business scenarios in plain English: endorsement creation, lifecycle transitions, anomaly detection, reconciliation, and more. Also produces a Cucumber HTML report at `behaviour-tests/build/reports/cucumber/cucumber-report.html`.

#### E2E Tests

```bash
./run-e2e-tests.sh                # Run all E2E + Storybook tests
./run-e2e-tests.sh --e2e          # Run only E2E flow tests
./run-e2e-tests.sh --storybook    # Run only Storybook component tests
./run-e2e-tests.sh --report       # Regenerate report only
./run-e2e-tests.sh --stop         # Stop the Allure server
```

Playwright tests exercise the full UI against the live frontend and backend. Storybook tests validate individual component rendering in isolation. **Requires the application to be running** (`./start.sh`).

#### Performance Tests

```bash
./run-perf-tests.sh                              # Run baseline (default)
./run-perf-tests.sh LoadSimulation                # Run load test
./run-perf-tests.sh StressSimulation --rps 50     # Stress test with custom RPS
./run-perf-tests.sh --list                        # List all available simulations
```

Available simulations:

| Simulation | What It Tests |
|-----------|--------------|
| `BaselineSimulation` | Single-user happy path — validates correctness under no load |
| `LoadSimulation` | Sustained load — ramp to target RPS, hold for duration |
| `StressSimulation` | Beyond expected capacity — find breaking points |
| `SoakSimulation` | Extended duration — detect memory leaks, connection exhaustion |
| `SpikeSimulation` | Sudden traffic burst — test auto-scaling and recovery |
| `MixedWorkloadSimulation` | Realistic mix of create, read, submit, confirm operations |
| `FullLifecycleSimulation` | Complete endorsement lifecycle at load |
| `EAContentionSimulation` | Concurrent EA balance reservations — tests optimistic locking |
| `MultiInsurerLoadSimulation` | Load distributed across all 4 insurer adapters |
| `IntelligenceApiSimulation` | Intelligence endpoints under load |
| `AnomalyDetectionUnderLoadSimulation` | Anomaly detection with concurrent endorsements |
| `BatchOptimizationUnderLoadSimulation` | Batch assembly under concurrent writes |
| `IntelligenceSoakSimulation` | Intelligence endpoints — extended duration |
| `IntelligenceSpikeSimulation` | Intelligence endpoints — burst traffic |

### 6.3 Reading Test Reports

All test reports use **Allure** — a visual report framework. The report opens in your browser automatically.

**Navigating the Allure report:**

- **Suites tab** (left sidebar) — Tests grouped by type:
  - `API Tests` — API integration tests
  - `BDD Tests` — Behaviour/Cucumber tests
  - `E2E Tests` — Playwright end-to-end tests
  - `Performance Tests` — Gatling results
- **Behaviors tab** — Tests grouped by business epic
- **Graphs tab** — Pass/fail distribution, duration charts
- **Timeline tab** — Execution timeline showing parallelism

Each test shows: status, duration, steps, and on failure: screenshot + trace (for E2E tests).

---

## 7. Shell Script Reference

All scripts are in the project root. Every script is self-contained — no manual setup required.

| Script | Purpose | Key Options |
|--------|---------|-------------|
| `start.sh` | Start the entire system (infra + backend + frontend + seed data) | `stop` — shut everything down |
| `run-all-tests.sh` | Run all test suites with combined Allure report | `--skip-perf`, `--report`, `--stop` |
| `run-api-tests.sh` | Run API tests only (Testcontainers) | `--report`, `--stop` |
| `run-behaviour-tests.sh` | Run BDD/Cucumber tests only | `--report`, `--stop` |
| `run-e2e-tests.sh` | Run E2E/Storybook tests (requires app running) | `--e2e`, `--storybook`, `--report`, `--stop` |
| `run-perf-tests.sh` | Run Gatling performance tests | `SimulationName`, `--rps N`, `--list` |
| `k8s-start.sh` | Start on local Kubernetes (Docker Desktop or minikube) | `stop` — tear down K8s resources |

**Common workflow:**

```bash
# 1. Start the system
./start.sh

# 2. Run all tests
./run-all-tests.sh

# 3. When done, shut everything down
./start.sh stop
./run-all-tests.sh --stop    # Stop the Allure report server
```

---

## 8. Demo Data — Pre-Seeded IDs

The installer seeds demo data automatically. Use these IDs when exploring:

| Entity | ID | Notes |
|--------|----|-------|
| **Employer** | `11111111-1111-1111-1111-111111111111` | Pre-configured with EA accounts for all insurers |
| **Mock Insurer** | `22222222-2222-2222-2222-222222222222` | JSON/REST, supports real-time + batch |
| **ICICI Lombard** | `33333333-3333-3333-3333-333333333333` | REST/JSON, real-time only |
| **Niva Bupa** | `44444444-4444-4444-4444-444444444444` | CSV/SFTP, batch only |
| **Bajaj Allianz** | `55555555-5555-5555-5555-555555555555` | SOAP/XML, real-time + batch |

Pre-seeded intelligence data includes 5 anomaly detections, 3 balance forecasts, 4 error resolutions, and 8 process mining metrics across all insurers.

---

## 9. Stopping the System

```bash
# Stop everything: backend, frontend, and all Docker containers
./start.sh stop

# Stop test report servers (if running)
./run-all-tests.sh --stop
```

---

## 10. Troubleshooting

| Problem | Solution |
|---------|----------|
| `Docker is installed but not running` | Open Docker Desktop and wait for it to start, then re-run `./start.sh` |
| `Java 21+ required` | `brew install openjdk@21` and ensure it's on your PATH |
| `Port 8080 already in use` | The installer auto-kills stale processes. If it persists: `lsof -ti:8080 \| xargs kill` |
| `Frontend not loading` | Check `cat .logs/frontend.log` for errors. Ensure port 5173 is free. |
| `Backend health check timing out` | Check `cat .logs/backend.log`. Common cause: PostgreSQL container still starting. |
| `E2E tests failing` | E2E tests require the app to be running. Run `./start.sh` first, then `./run-e2e-tests.sh`. |
| `Allure report not loading` | Ensure Docker is running. The Allure server runs as a container. Try `./run-all-tests.sh --report`. |
| `Kafka connection refused` | Wait 30s after `./start.sh` for Kafka to fully initialize. Check with `docker compose ps`. |

**Logs location:** `.logs/backend.log` and `.logs/frontend.log`

---

## 11. For Managers & Executives — What to Look At

If you have limited time, here's what demonstrates the most value:

### 5-Minute Tour

1. **Start the system**: `./start.sh` (2 min wait)
2. **Dashboard** at [localhost:5173](http://localhost:5173) — see KPI cards, recent endorsements, EA balance
3. **Intelligence** at [localhost:5173/intelligence](http://localhost:5173/intelligence) — see anomaly detection, forecasts, auto-error resolution, process mining
4. **Grafana** at [localhost:3000](http://localhost:3000) (admin/plum) — open the **Endorsement Business** dashboard for business metrics

### What Each Dashboard Tells You

| Dashboard | Business Question It Answers |
|-----------|------------------------------|
| **Application Frontend** | How does the product look and feel? Is the UX intuitive? |
| **Grafana: Endorsement Business** | How many endorsements are processing? What's the confirmation rate? |
| **Grafana: Multi-Insurer Monitoring** | Which insurer is fastest? Which has the most rejections? |
| **Grafana: Intelligence Monitoring** | How effective is the AI? What percentage of errors are auto-resolved? |
| **Allure Test Report** | What's the test coverage? Are all tests passing? |

### Key Numbers to Reference

| Metric | Value |
|--------|-------|
| Total automated tests | 790+ |
| Test categories | Unit, API, BDD, E2E, Performance |
| Insurer integrations | 4 (Mock, ICICI Lombard, Niva Bupa, Bajaj Allianz) |
| Endorsement lifecycle states | 11 |
| Kafka topics / partitions | 4 / 88 |
| Custom Prometheus metrics | 20+ |
| Grafana dashboards | 7 pre-built |
| Infrastructure services | 9 (containerized) |
| One-click startup | `./start.sh` |

### For Engineering Managers

- **Architecture**: See `docs/deliverables/High_Level_Architecture.md` for the hexagonal (ports & adapters) architecture
- **Test strategy**: Run `./run-all-tests.sh` — the combined Allure report shows coverage across all layers
- **Code quality**: CLAUDE.md documents all design patterns (Strategy, State, Observer, Adapter, Factory) with file references
- **Operational readiness**: Grafana dashboards, distributed tracing (Jaeger), centralized logging (ELK), and structured metrics (Prometheus) are all pre-configured

### For C-Level Executives

- **Demo**: See `docs/deliverables/Demo_Script_15min.md` for a structured 15-minute walkthrough
- **Product guide**: See `docs/deliverables/Product_Usage_Guide.md` for screen-by-screen feature documentation
- **AI/Automation strategy**: See `docs/deliverables/AI_Automation_Approach.md` for the intelligence layer vision
- **No coverage gap**: See `docs/deliverables/No_Loss_of_Coverage_Approach.md` — employees get provisional coverage immediately
- **EA balance optimization**: See `docs/deliverables/EA_Balance_Minimization_Algorithm.md` — reduces employer float requirements
