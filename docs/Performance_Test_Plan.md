# Performance, Load & Stress Testing Plan

## Plum Endorsement Management System

**Version:** 1.0
**Date:** 2026-03-07
**Module:** `performance-tests`
**Framework:** Gatling (Scala DSL)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Objectives](#2-objectives)
3. [Scope](#3-scope)
4. [Test Environment](#4-test-environment)
5. [Architecture Under Test](#5-architecture-under-test)
6. [Test Strategy](#6-test-strategy)
7. [Test Scenarios](#7-test-scenarios)
8. [Workload Models](#8-workload-models)
9. [Performance SLAs & Acceptance Criteria](#9-performance-slas--acceptance-criteria)
10. [Data Strategy](#10-data-strategy)
11. [Gatling Implementation Plan](#11-gatling-implementation-plan)
12. [Monitoring & Observability During Tests](#12-monitoring--observability-during-tests)
13. [Risk Analysis & Bottleneck Hypothesis](#13-risk-analysis--bottleneck-hypothesis)
14. [Test Execution Plan](#14-test-execution-plan)
15. [Reporting](#15-reporting)
16. [Appendix](#16-appendix)

---

## 1. Executive Summary

This document defines the performance testing strategy for the Plum Endorsement Management System. The system handles insurance endorsement lifecycle management including creation, insurer submission, batch processing, EA (Endorsement Account) balance management, and provisional coverage tracking.

The test suite will validate system behavior under expected production load, peak load, and failure conditions using **Gatling** as the load generation framework. Tests reside in the `performance-tests` Gradle submodule and target the Spring Boot REST API backed by PostgreSQL, Redis, Kafka, and a mock insurer adapter.

### Key Business Flows Under Test

| Flow | Criticality | Why It Matters |
|------|-------------|----------------|
| Endorsement Creation | Critical | Core operation; involves DB writes, Kafka events, EA reservation |
| Insurer Submission | Critical | External dependency with circuit breaker; latency-sensitive |
| Batch Assembly | High | Scheduled bulk operation; database-intensive |
| Endorsement Listing | High | Most frequent read operation; pagination performance |
| Full Lifecycle | Critical | End-to-end latency from creation to confirmation |
| EA Balance Operations | High | Financial accuracy under concurrency |

---

## 2. Objectives

### Primary Objectives

1. **Baseline Performance** -- Establish response time, throughput, and resource utilization baselines for all API endpoints under normal load.
2. **Scalability Validation** -- Determine the maximum throughput the system can sustain while meeting SLA requirements.
3. **Bottleneck Identification** -- Identify database, Kafka, cache, and thread pool bottlenecks before production deployment.
4. **Concurrency Safety** -- Validate data consistency under concurrent operations, particularly EA balance reservations and idempotency enforcement.
5. **Resilience Verification** -- Confirm circuit breaker, retry, and fallback behavior under degraded conditions.

### Secondary Objectives

6. **Soak Testing** -- Verify no memory leaks, connection pool exhaustion, or thread accumulation over extended periods.
7. **Spike Testing** -- Validate system recovery after sudden traffic bursts (e.g., open enrollment periods).
8. **Scheduler Impact** -- Measure the performance impact of batch assembly, status polling, and coverage cleanup running concurrently with API traffic.

---

## 3. Scope

### In Scope

| Component | Endpoints / Operations |
|-----------|----------------------|
| Endorsement API | `POST /api/v1/endorsements` (create) |
| | `GET /api/v1/endorsements/{id}` (get by ID) |
| | `GET /api/v1/endorsements?employerId=...` (list/filter) |
| | `POST /api/v1/endorsements/{id}/submit` (submit to insurer) |
| | `POST /api/v1/endorsements/{id}/confirm` (confirm) |
| | `POST /api/v1/endorsements/{id}/reject` (reject) |
| | `GET /api/v1/endorsements/{id}/coverage` (provisional coverage) |
| EA Account API | `GET /api/v1/ea-accounts?employerId=...&insurerId=...` |
| Background Jobs | Batch assembly (every 15 min) |
| | Batch status polling (every 60s) |
| | Provisional coverage cleanup (daily 2 AM) |
| Infrastructure | PostgreSQL connection pool |
| | Kafka producer throughput |
| | Redis/Caffeine cache behavior |
| | Circuit breaker / retry under load |

### Out of Scope

- Frontend (React) performance testing
- Kubernetes cluster-level testing (covered separately)
- Network latency simulation between K8s pods
- Third-party insurer API load testing (mock adapter used)
- Browser-based performance testing

---

## 4. Test Environment

### Infrastructure Configuration

| Component | Spec | Notes |
|-----------|------|-------|
| Application | Spring Boot 3.4.3, Java 21, Virtual Threads | Single instance |
| PostgreSQL | 16-alpine | Docker container, default tuning |
| Redis | 7-alpine | Docker container |
| Kafka | Apache Kafka 3.7.0 | Single broker, 3 partitions |
| Jaeger | 1.55 | Tracing (100% sampling) |
| Elasticsearch | 8.12.2 | 512MB heap, log aggregation |
| Prometheus | v2.50.1 | Metrics scraping every 15s |
| Grafana | 10.3.3 | Dashboard visualization |

### Application Configuration for Performance Tests

```yaml
# performance-tests/src/test/resources/application-perftest.yml
server:
  tomcat:
    threads:
      max: 200
    max-connections: 8192
    accept-count: 100

spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

management:
  tracing:
    sampling:
      probability: 0.01  # Reduce to 1% during perf tests
```

### Gatling Load Generator

| Setting | Value |
|---------|-------|
| Gatling Version | 3.10.x |
| JVM Heap | `-Xms512m -Xmx2g` |
| Deployment | Same Docker network as application |
| Report Output | `performance-tests/build/gatling-reports/` |

---

## 5. Architecture Under Test

### Request Flow

```
Client (Gatling)
    │
    ▼
Spring Boot (port 8080)
    │
    ├── SecurityFilter (permitAll for API)
    ├── MdcRequestFilter (requestId, traceId)
    ├── RequestLoggingFilter (audit log)
    │
    ├── EndorsementController
    │       ├── CreateEndorsementHandler
    │       │     ├── IdempotencyCheck (SELECT endorsements)
    │       │     ├── Save endorsement (INSERT endorsements)
    │       │     ├── Kafka publish x3 (CREATED, VALIDATED, PROV_COVERED)
    │       │     ├── ProvisionalCoverage (INSERT, ADD type only)
    │       │     └── EA Reservation (SELECT + UPDATE ea_accounts, ADD type only)
    │       │
    │       ├── ProcessEndorsementHandler
    │       │     ├── Fetch endorsement (SELECT)
    │       │     ├── InsurerPort.submitRealTime (100ms simulated)
    │       │     │     └── @CircuitBreaker + @Retry
    │       │     ├── State transitions + saves
    │       │     ├── Kafka publish x2-3
    │       │     └── NotificationPort
    │       │
    │       └── EndorsementQueryHandler
    │             └── Repository queries (SELECT with pagination)
    │
    ├── EAAccountController
    │       └── Repository query (SELECT ea_accounts)
    │
    ├── PostgreSQL (5432) ──── Connection Pool (HikariCP)
    ├── Kafka (9092) ──────── Producer (acks=all)
    ├── Redis (6379) ─────── Cache (Caffeine primary)
    └── Logstash (5000) ──── Structured JSON logs
```

### Database Tables & Indexes

| Table | Expected Row Count (Steady State) | Hot Indexes |
|-------|----------------------------------|-------------|
| endorsements | 100K - 1M | idx_endorsements_employer, idx_endorsements_status |
| ea_accounts | 1K - 10K (employers x insurers) | PK (employer_id, insurer_id) |
| ea_transactions | 200K - 2M | idx_ea_tx_endorsement |
| endorsement_batches | 10K - 100K | idx_batches_status |
| provisional_coverages | 50K - 500K | idx_prov_cov_endorsement |
| endorsement_events | 500K - 5M | idx_events_endorsement |

### Kafka Topics

| Topic | Partitions | Write Rate (Expected) |
|-------|-----------|----------------------|
| endorsement-events | 3 | 3-5 events per endorsement lifecycle |
| endorsement-commands | 3 | 1 per submission |
| endorsement-notifications | 1 | 1 per confirmation/rejection |

---

## 6. Test Strategy

### Test Types

```
                          ┌────────────────────────────────────────────┐
                          │           Test Type Pyramid                │
                          │                                            │
                          │              ╱╲  Chaos/                    │
                          │             ╱  ╲ Resilience               │
                          │            ╱────╲                          │
                          │           ╱ Spike ╲                        │
                          │          ╱  Tests   ╲                      │
                          │         ╱────────────╲                     │
                          │        ╱  Stress Tests ╲                   │
                          │       ╱  (Beyond Limits) ╲                 │
                          │      ╱────────────────────╲                │
                          │     ╱    Soak / Endurance   ╲              │
                          │    ╱   (Extended Duration)    ╲             │
                          │   ╱────────────────────────────╲           │
                          │  ╱       Load Tests              ╲         │
                          │ ╱    (Expected Production)        ╲        │
                          │╱────────────────────────────────────╲      │
                          │         Baseline / Smoke Tests       │      │
                          │      (Single User, Functional)       │      │
                          └────────────────────────────────────────┘
```

| Test Type | Purpose | Duration | Concurrency |
|-----------|---------|----------|-------------|
| **Baseline** | Single-user response times, functional correctness | 2-5 min | 1 user |
| **Load** | Validate SLAs under expected production traffic | 15-30 min | 50-200 users |
| **Stress** | Find breaking point, identify first failure mode | 20-30 min | Ramp to 500+ users |
| **Soak** | Memory leaks, connection exhaustion, GC pressure | 2-4 hours | 100 users steady |
| **Spike** | Sudden burst recovery (open enrollment simulation) | 15 min | 0 → 500 → 0 in 60s |
| **Resilience** | Circuit breaker, retry behavior under failures | 15 min | 100 users + failure injection |

### Test Execution Order

```
Phase 1: Baseline (validate test harness)
    │
Phase 2: Load Tests (establish SLA compliance)
    │
Phase 3: Stress Tests (find breaking points)
    │
Phase 4: Soak Tests (long-running stability)
    │
Phase 5: Spike Tests (burst handling)
    │
Phase 6: Resilience Tests (failure modes)
```

---

## 7. Test Scenarios

### Scenario 1: Endorsement Creation Throughput

**Objective:** Measure maximum endorsement creation rate while maintaining p95 < 500ms.

**Flow:**
1. Generate unique CreateEndorsementRequest (randomized employeeId, idempotencyKey)
2. `POST /api/v1/endorsements`
3. Assert HTTP 200, status = PROVISIONALLY_COVERED
4. Extract endorsementId for downstream scenarios

**Variations:**

| Sub-Scenario | Description | Data |
|-------------|-------------|------|
| 1a: ADD type | Full flow with provisional coverage + EA reservation | type=ADD, premiumAmount=random(100-5000) |
| 1b: DELETE type | Lightweight flow, no coverage/EA | type=DELETE |
| 1c: UPDATE type | Lightweight flow, no coverage/EA | type=UPDATE |
| 1d: Mixed workload | Realistic ratio: 60% ADD, 25% UPDATE, 15% DELETE | Random type selection |
| 1e: Idempotency | Duplicate idempotencyKey submissions | Fixed keys, expect dedup |
| 1f: Large payload | Oversized employeeData JSON (~50KB) | Nested JSON structure |

**Assertions:**
- No HTTP 5xx errors
- Idempotency returns identical response for duplicate keys
- EA reservation count matches successful ADD count

---

### Scenario 2: Endorsement Retrieval Performance

**Objective:** Validate read path scalability with growing dataset.

**Flow:**
1. `GET /api/v1/endorsements/{id}` (single lookup)
2. `GET /api/v1/endorsements?employerId={id}&page=0&size=20` (paginated list)
3. `GET /api/v1/endorsements?employerId={id}&statuses=CREATED,VALIDATED&page=0&size=50` (filtered list)

**Variations:**

| Sub-Scenario | Description | Data |
|-------------|-------------|------|
| 2a: Single GET | Fetch by known ID | Pre-seeded endorsement IDs |
| 2b: List (small) | Employer with 10 endorsements | page=0, size=20 |
| 2c: List (large) | Employer with 10,000 endorsements | page=0-499, size=20 |
| 2d: Filtered list | Status filter with index usage | statuses=CONFIRMED |
| 2e: Deep pagination | Page 100+ of large result set | page=100, size=20 |
| 2f: 404 handling | Non-existent endorsement ID | Random UUID |

**Assertions:**
- p95 < 200ms for single GET
- p95 < 500ms for paginated list (first 10 pages)
- Deep pagination degrades gracefully (p95 < 2s)

---

### Scenario 3: Full Endorsement Lifecycle

**Objective:** Measure end-to-end latency for the complete endorsement workflow.

**Flow:**
1. `POST /api/v1/endorsements` -- Create (ADD type)
2. `GET /api/v1/endorsements/{id}` -- Verify status = PROVISIONALLY_COVERED
3. `POST /api/v1/endorsements/{id}/submit` -- Submit to insurer
4. `GET /api/v1/endorsements/{id}` -- Verify status = CONFIRMED (mock auto-confirms)
5. `GET /api/v1/endorsements/{id}/coverage` -- Verify coverage confirmed
6. `GET /api/v1/ea-accounts?employerId=...&insurerId=...` -- Verify balance

**Timing Breakdown:**
- Step 1 (Create): Expected 50-200ms
- Step 3 (Submit): Expected 150-500ms (includes 100ms mock insurer latency)
- Total lifecycle: Expected 300-1000ms

**Assertions:**
- All state transitions occur in correct order
- Provisional coverage transitions from PROVISIONAL to CONFIRMED
- EA balance reflects reservation

---

### Scenario 4: EA Account Contention

**Objective:** Validate financial accuracy under concurrent reservations against the same EA account.

**Flow:**
1. Seed single EA account with balance = 100,000
2. Concurrently create 200 ADD endorsements (premium = 1,000 each) for the same employer + insurer
3. Verify: total reserved <= 100,000 (never negative available balance)
4. Verify: sum of successful reservations = reserved amount on account

**Variations:**

| Sub-Scenario | Description |
|-------------|-------------|
| 4a: Same account | 200 concurrent requests, single employer+insurer |
| 4b: Account exhaustion | Requests exceeding available balance |
| 4c: Multi-account | 200 concurrent requests across 10 different employer+insurer pairs |

**Assertions:**
- No negative available balance at any point
- Transaction log matches final reserved amount
- No lost updates (optimistic locking via version column)

---

### Scenario 5: Batch Assembly Under Load

**Objective:** Measure batch assembly scheduler performance with large queues.

**Setup:**
1. Seed N endorsements in QUEUED_FOR_BATCH status (N = 100, 1K, 10K, 50K)
2. Distribute across M insurers (M = 1, 5, 20)
3. Trigger batch assembly

**Measurements:**

| Metric | Description |
|--------|-------------|
| Assembly time | Wall-clock time to process all queued endorsements |
| Batch count | Number of batches created (N / maxBatchSize per insurer) |
| DB operations | Total SELECT + UPDATE + INSERT queries |
| Kafka events | Total events published |
| Memory | JVM heap usage during assembly |

**Scaling Matrix:**

| Queued Endorsements | Insurers | Expected Batches | Target Assembly Time |
|-------------------|----------|------------------|---------------------|
| 100 | 1 | 1 | < 2s |
| 1,000 | 5 | 10 | < 10s |
| 10,000 | 20 | 100+ | < 60s |
| 50,000 | 20 | 500+ | < 300s |

---

### Scenario 6: Concurrent Insurer Submissions

**Objective:** Validate insurer submission path under concurrent load with circuit breaker behavior.

**Flow:**
1. Create endorsements in PROVISIONALLY_COVERED status
2. Concurrently submit N endorsements to insurer
3. Monitor circuit breaker state transitions

**Variations:**

| Sub-Scenario | Failure Rate | Expected Behavior |
|-------------|-------------|-------------------|
| 6a: Happy path | 0% | All confirmed, circuit stays CLOSED |
| 6b: Intermittent | 20% | Retries succeed, some rejections, circuit stays CLOSED |
| 6c: High failure | 60% | Circuit OPENS after ~10 calls, fallback invoked |
| 6d: Recovery | 60% → 0% | Circuit OPENS → HALF_OPEN → CLOSED recovery |

**Assertions:**
- Circuit breaker opens when failure rate > 50% (within 10-call window)
- Fallback returns graceful error (not HTTP 500)
- Recovery happens within 30 seconds of failures stopping
- Retry backoff follows exponential pattern (2s, 4s, 8s)

---

### Scenario 7: Mixed Workload Simulation

**Objective:** Simulate realistic production traffic patterns.

**Traffic Mix:**

| Operation | Weight | Description |
|-----------|--------|-------------|
| Create Endorsement (ADD) | 30% | New employee additions |
| Create Endorsement (UPDATE) | 12% | Policy updates |
| Create Endorsement (DELETE) | 8% | Employee terminations |
| Get Endorsement | 20% | Status checks |
| List Endorsements | 15% | Dashboard views |
| Submit to Insurer | 10% | Submission triggers |
| EA Account Balance | 5% | Balance inquiries |

**User Profiles:**

| Profile | Behavior | Think Time |
|---------|----------|------------|
| HR Admin | Create endorsements, check status | 5-15s between actions |
| System Integration | Bulk creation, no think time | 0-1s (API-to-API) |
| Dashboard Viewer | List + Get operations | 10-30s (browsing) |
| Batch Processor | Submit + Confirm/Reject flows | 2-5s |

---

### Scenario 8: Soak Test

**Objective:** Detect memory leaks, connection exhaustion, and degradation over time.

**Configuration:**
- Duration: 4 hours
- Constant load: 100 concurrent users
- Mixed workload (Scenario 7 ratios)
- Scheduler running concurrently (batch assembly every 15 min, status polling every 60s)

**Monitoring Focus:**

| Metric | Alert Threshold |
|--------|----------------|
| JVM heap used | Monotonic increase > 10% over 1 hour |
| HikariCP active connections | Sustained at maximum-pool-size |
| HikariCP pending threads | > 0 for > 30 seconds |
| Thread count | Monotonic increase > 20% |
| GC pause time (p99) | > 500ms |
| Response time (p95) | > 2x baseline after 2 hours |
| Error rate | > 0.1% sustained |
| Kafka producer queue | Growing backlog |

---

### Scenario 9: Spike Test

**Objective:** Simulate open enrollment period traffic surge.

**Load Profile:**

```
Users
500 ┤                    ╭────────╮
    │                   ╱          ╲
400 ┤                  ╱            ╲
    │                 ╱              ╲
300 ┤                ╱                ╲
    │               ╱                  ╲
200 ┤              ╱                    ╲
    │             ╱                      ╲
100 ┤   ╭────────╯                        ╲
    │  ╱                                    ╲──────
 50 ┤─╯
    │
  0 ┤─────┼─────┼─────┼─────┼─────┼─────┼─────┼──
    0    1m    2m    3m    5m    7m    9m   12m  15m
```

**Phases:**
1. **Warm-up** (0-1 min): Ramp to 50 users
2. **Steady** (1-3 min): Hold at 100 users, establish baseline
3. **Spike** (3-5 min): Ramp to 500 users in 60 seconds
4. **Peak** (5-7 min): Hold at 500 users
5. **Recovery** (7-9 min): Drop to 100 users
6. **Stabilize** (9-15 min): Verify metrics return to baseline

**Assertions:**
- No HTTP 503 during spike
- Error rate < 5% during peak
- Response times return to baseline within 2 minutes of recovery
- No data corruption or lost endorsements

---

### Scenario 10: Database Stress

**Objective:** Identify database bottlenecks under write-heavy load.

**Sub-Scenarios:**

| Test | Setup | Load | Focus |
|------|-------|------|-------|
| 10a: Write amplification | Empty DB | 1000 creates/sec | Measures writes per endorsement (endorsements + events + coverage + EA tx) |
| 10b: Index pressure | 1M pre-seeded rows | 100 creates/sec + 500 reads/sec | Query plan changes at scale |
| 10c: JSONB impact | Varied employeeData sizes (1KB - 100KB) | 100 creates/sec | Storage I/O and query performance |
| 10d: Connection pool saturation | HikariCP max=10 | 200 concurrent requests | Queue depth, timeout behavior |
| 10e: Lock contention | Same employer_id | 100 concurrent creates | Row-level lock wait times |

---

## 8. Workload Models

### Closed Model (Scenario-Based)

Used for lifecycle and contention tests where each virtual user completes a defined journey.

```
Each User:
  loop(iterations) {
    exec(createEndorsement)
    pause(thinkTime)
    exec(getEndorsement)
    pause(thinkTime)
    exec(submitEndorsement)
    pause(thinkTime)
    exec(verifyConfirmed)
  }
```

### Open Model (Throughput-Based)

Used for throughput and stress tests where arrival rate is controlled independently of response time.

```
constantUsersPerSec(rate).during(duration)
rampUsersPerSec(startRate).to(endRate).during(rampDuration)
```

### Load Profiles

| Profile | Model | Users/Rate | Duration | Use |
|---------|-------|-----------|----------|-----|
| Baseline | Closed | 1 user | 2 min | Functional validation |
| Normal Load | Open | 20 req/s | 15 min | SLA validation |
| Peak Load | Open | 100 req/s | 10 min | Capacity validation |
| Stress | Open | Ramp 10 → 500 req/s | 20 min | Breaking point |
| Soak | Closed | 100 users | 4 hours | Stability |
| Spike | Open | 20 → 500 → 20 req/s | 15 min | Burst recovery |

---

## 9. Performance SLAs & Acceptance Criteria

### Response Time SLAs

| Endpoint | p50 | p95 | p99 | Max |
|----------|-----|-----|-----|-----|
| POST /endorsements (create) | < 100ms | < 500ms | < 1s | < 3s |
| GET /endorsements/{id} | < 30ms | < 100ms | < 200ms | < 1s |
| GET /endorsements (list) | < 50ms | < 200ms | < 500ms | < 2s |
| POST /endorsements/{id}/submit | < 200ms | < 800ms | < 1.5s | < 5s |
| POST /endorsements/{id}/confirm | < 50ms | < 200ms | < 500ms | < 2s |
| POST /endorsements/{id}/reject | < 50ms | < 200ms | < 500ms | < 2s |
| GET /endorsements/{id}/coverage | < 30ms | < 100ms | < 200ms | < 1s |
| GET /ea-accounts | < 30ms | < 100ms | < 200ms | < 1s |

### Throughput SLAs

| Metric | Target |
|--------|--------|
| Endorsement creation rate | > 100 endorsements/second |
| Read throughput | > 500 reads/second |
| Full lifecycle throughput | > 50 lifecycles/second |
| Error rate (under normal load) | < 0.1% |
| Error rate (under peak load) | < 1% |
| Error rate (during spike) | < 5% |

### Resource Utilization Limits

| Resource | Warning | Critical |
|----------|---------|----------|
| JVM Heap Usage | > 70% | > 85% |
| CPU Usage | > 70% | > 90% |
| HikariCP Active Connections | > 80% of max | 100% of max |
| HikariCP Pending Threads | > 0 sustained | > 5 sustained |
| Kafka Producer Queue | > 100 pending | > 1000 pending |
| GC Pause (p99) | > 100ms | > 500ms |

### Data Integrity SLAs

| Invariant | Validation |
|-----------|------------|
| EA balance never negative | Query `SELECT * FROM ea_accounts WHERE (balance - reserved) < 0` returns 0 rows |
| Idempotency enforced | Duplicate key requests return identical endorsement ID |
| State machine valid | No endorsement in invalid status transition (verified by state machine) |
| Event count matches | Each endorsement has exactly the expected number of events for its state |
| Transaction audit trail | Sum of EA transactions matches current balance + reserved |

---

## 10. Data Strategy

### Pre-Seeded Data

| Entity | Count | Purpose |
|--------|-------|---------|
| EA Accounts | 100 (10 employers x 10 insurers) | Distributed load across accounts |
| EA Account Balance | 1,000,000 per account | Sufficient for sustained testing |
| Endorsements (QUEUED_FOR_BATCH) | 10,000 | Batch assembly scenario |
| Endorsements (various statuses) | 50,000 | Read/list/pagination scenarios |

### Dynamic Data Generation

| Field | Strategy |
|-------|----------|
| employerId | Round-robin from pre-seeded pool (10 employers) |
| employeeId | UUID.randomUUID() per request |
| insurerId | Round-robin from pre-seeded pool (10 insurers) |
| policyId | UUID.randomUUID() per request |
| idempotencyKey | `"perf-" + UUID.randomUUID()` |
| type | Weighted random: 60% ADD, 25% UPDATE, 15% DELETE |
| premiumAmount | Random BigDecimal(100.00 - 5000.00), 2 decimal places |
| coverageStartDate | Today + random(1-30) days |
| coverageEndDate | coverageStartDate + 365 days |
| employeeData | Template JSON with randomized name, age, department |

### Data Cleanup

- Truncate all tables before each test suite run
- Re-seed base data (EA accounts, pre-seeded endorsements)
- Gatling `before` hook executes SQL seed script via JDBC

### Feeder Strategy (Gatling)

| Feeder | Type | Source |
|--------|------|--------|
| employerFeeder | Circular | CSV with 10 employer UUIDs |
| insurerFeeder | Circular | CSV with 10 insurer UUIDs |
| endorsementTypeFeeder | Random weighted | In-memory: 60/25/15 split |
| createdEndorsementFeeder | Queue | Session-populated from create responses |
| employeeDataFeeder | Random | JSON template with Faker-style generation |

---

## 11. Gatling Implementation Plan

### Module Structure

```
performance-tests/
├── build.gradle.kts                    # Gatling plugin, dependencies
├── src/
│   └── gatling/
│       ├── scala/
│       │   └── com/plum/endorsements/perf/
│       │       ├── config/
│       │       │   └── TestConfig.scala           # Environment config, URLs, thresholds
│       │       ├── feeders/
│       │       │   ├── EndorsementFeeders.scala    # Data generators for all scenarios
│       │       │   └── AccountFeeders.scala        # EA account data generators
│       │       ├── requests/
│       │       │   ├── EndorsementRequests.scala   # HTTP request definitions
│       │       │   ├── EAAccountRequests.scala     # EA account request definitions
│       │       │   └── HealthRequests.scala        # Actuator health/metrics checks
│       │       ├── scenarios/
│       │       │   ├── CreateEndorsementScenario.scala
│       │       │   ├── RetrievalScenario.scala
│       │       │   ├── FullLifecycleScenario.scala
│       │       │   ├── EAContentionScenario.scala
│       │       │   ├── BatchAssemblyScenario.scala
│       │       │   ├── InsurerSubmissionScenario.scala
│       │       │   └── MixedWorkloadScenario.scala
│       │       ├── simulations/
│       │       │   ├── BaselineSimulation.scala      # Phase 1: Single user
│       │       │   ├── LoadSimulation.scala           # Phase 2: Normal load
│       │       │   ├── StressSimulation.scala         # Phase 3: Breaking point
│       │       │   ├── SoakSimulation.scala           # Phase 4: 4-hour stability
│       │       │   ├── SpikeSimulation.scala          # Phase 5: Burst recovery
│       │       │   ├── ResilienceSimulation.scala     # Phase 6: Failure modes
│       │       │   ├── EAContentionSimulation.scala   # EA concurrency test
│       │       │   └── DatabaseStressSimulation.scala # DB-focused stress
│       │       └── assertions/
│       │           └── SLAAssertions.scala           # Reusable SLA checks
│       └── resources/
│           ├── gatling.conf                          # Gatling configuration
│           ├── logback-test.xml                      # Logging config
│           ├── feeders/
│           │   ├── employers.csv                     # Pre-seeded employer IDs
│           │   ├── insurers.csv                      # Pre-seeded insurer IDs
│           │   └── employee-data-templates.json      # JSONB payload templates
│           └── sql/
│               ├── seed-perf-data.sql                # Pre-test data seeding
│               └── cleanup-perf-data.sql             # Post-test cleanup
│               └── verify-invariants.sql             # Data integrity checks
```

### build.gradle.kts Structure

```
plugins:
  - io.gatling.gradle (version 3.10.x)

dependencies:
  - gatling: io.gatling.highcharts:gatling-charts-highcharts (3.10.x)
  - gatling: com.github.javafaker:javafaker (for data generation)
  - gatling: org.postgresql:postgresql (for JDBC seed/verify)

gatling configuration:
  - jvmArgs: ["-Xms512m", "-Xmx2g"]
  - systemProperties: baseUrl, dbUrl, etc.
  - logLevel: WARN
```

### Request Definitions

**EndorsementRequests.scala** -- HTTP request builders:

| Request Name | Method | Path | Body | Checks |
|-------------|--------|------|------|--------|
| `createEndorsement` | POST | `/api/v1/endorsements` | JSON from feeder | status 200, jsonPath $.id saveAs endorsementId |
| `getEndorsement` | GET | `/api/v1/endorsements/${endorsementId}` | -- | status 200, jsonPath $.status |
| `listEndorsements` | GET | `/api/v1/endorsements?employerId=${employerId}` | -- | status 200, jsonPath $.totalElements |
| `submitEndorsement` | POST | `/api/v1/endorsements/${endorsementId}/submit` | -- | status 202 |
| `confirmEndorsement` | POST | `/api/v1/endorsements/${endorsementId}/confirm?insurerReference=${ref}` | -- | status 200 |
| `rejectEndorsement` | POST | `/api/v1/endorsements/${endorsementId}/reject?reason=test` | -- | status 200 |
| `getCoverage` | GET | `/api/v1/endorsements/${endorsementId}/coverage` | -- | status 200 or 404 |

**EAAccountRequests.scala:**

| Request Name | Method | Path | Checks |
|-------------|--------|------|--------|
| `getBalance` | GET | `/api/v1/ea-accounts?employerId=${employerId}&insurerId=${insurerId}` | status 200, jsonPath $.availableBalance |

### Scenario Definitions

**CreateEndorsementScenario.scala:**
```
Scenario: "Create Endorsement"
  feed(endorsementTypeFeeder)
  feed(employerFeeder)
  feed(insurerFeeder)
  exec(createEndorsement)         # POST create
  pause(300ms, 1s)                # Think time
  exec(getEndorsement)            # GET verify
```

**FullLifecycleScenario.scala:**
```
Scenario: "Full Lifecycle"
  feed(employerFeeder)
  feed(insurerFeeder)
  exec(createEndorsement)         # Step 1: Create
  pause(500ms)
  exec(getEndorsement)            # Step 2: Verify created
  pause(200ms)
  exec(submitEndorsement)         # Step 3: Submit
  pause(500ms)
  exec(getEndorsement)            # Step 4: Verify confirmed
  exec(getCoverage)               # Step 5: Check coverage
  exec(getBalance)                # Step 6: Check EA balance
```

**MixedWorkloadScenario.scala:**
```
Scenario: "Mixed Workload"
  randomSwitch(
    30.0 -> exec(createADD),
    12.0 -> exec(createUPDATE),
     8.0 -> exec(createDELETE),
    20.0 -> exec(getEndorsement),
    15.0 -> exec(listEndorsements),
    10.0 -> exec(submitAndConfirm),
     5.0 -> exec(getBalance)
  )
```

### Simulation Definitions

**LoadSimulation.scala:**
```
setUp(
  createScenario.inject(
    constantUsersPerSec(20).during(15.minutes)
  ),
  readScenario.inject(
    constantUsersPerSec(50).during(15.minutes)
  ),
  lifecycleScenario.inject(
    constantUsersPerSec(10).during(15.minutes)
  )
).protocols(httpProtocol)
 .assertions(
   global.responseTime.percentile(95).lt(500),
   global.successfulRequests.percent.gt(99.9),
   forAll.responseTime.max.lt(5000)
 )
```

**StressSimulation.scala:**
```
setUp(
  mixedWorkload.inject(
    rampUsersPerSec(10).to(500).during(10.minutes),
    constantUsersPerSec(500).during(5.minutes),
    rampUsersPerSec(500).to(10).during(5.minutes)
  )
).protocols(httpProtocol)
 .assertions(
   global.responseTime.percentile(99).lt(3000),
   global.failedRequests.percent.lt(5)
 )
```

**SpikeSimulation.scala:**
```
setUp(
  mixedWorkload.inject(
    rampUsersPerSec(10).to(50).during(1.minute),
    constantUsersPerSec(100).during(2.minutes),
    rampUsersPerSec(100).to(500).during(1.minute),
    constantUsersPerSec(500).during(2.minutes),
    rampUsersPerSec(500).to(100).during(2.minutes),
    constantUsersPerSec(100).during(6.minutes)
  )
).protocols(httpProtocol)
```

### Assertions (SLAAssertions.scala)

Reusable assertions to be mixed into simulations:

| Assertion | Threshold |
|-----------|-----------|
| `global.responseTime.percentile(50).lt(200)` | p50 < 200ms |
| `global.responseTime.percentile(95).lt(500)` | p95 < 500ms |
| `global.responseTime.percentile(99).lt(1500)` | p99 < 1.5s |
| `global.successfulRequests.percent.gt(99.0)` | > 99% success |
| `details("Create Endorsement").responseTime.percentile(95).lt(500)` | Create p95 < 500ms |
| `details("Get Endorsement").responseTime.percentile(95).lt(100)` | Get p95 < 100ms |
| `details("Submit Endorsement").responseTime.percentile(95).lt(800)` | Submit p95 < 800ms |

---

## 12. Monitoring & Observability During Tests

### Pre-Configured Dashboards

During performance tests, all 4 Grafana dashboards should be monitored in real-time:

| Dashboard | Key Panels to Watch |
|-----------|-------------------|
| **Application Overview** | Request rate, p95 latency, error rate (5xx), active threads |
| **Business Metrics** | Creation rate by type, state transition rate, insurer latency, EA reservations, error distribution |
| **Infrastructure Health** | JVM heap, GC pauses, HikariCP pool, Caffeine cache hits, CPU |
| **Scheduler Monitoring** | Scheduler duration, success/failure rate, batch size, circuit breaker state |

### Prometheus Queries for Alerting During Tests

```promql
# Request rate spike detection
rate(http_server_requests_seconds_count{application="endorsement-service"}[1m]) > 200

# Error rate threshold
rate(http_server_requests_seconds_count{application="endorsement-service", status=~"5.."}[1m])
  / rate(http_server_requests_seconds_count{application="endorsement-service"}[1m]) > 0.05

# HikariCP pool exhaustion
hikaricp_connections_active{application="endorsement-service"}
  / hikaricp_connections_max{application="endorsement-service"} > 0.9

# JVM heap pressure
jvm_memory_used_bytes{application="endorsement-service", area="heap"}
  / jvm_memory_max_bytes{application="endorsement-service", area="heap"} > 0.85

# Circuit breaker opened
resilience4j_circuitbreaker_state{application="endorsement-service", name="insurerSubmission"} == 1

# Kafka publish failures
rate(endorsement_kafka_publish_total{result="failure"}[1m]) > 0
```

### Log Analysis (Kibana)

During tests, monitor the `endorsement-logs-*` index in Kibana:

| Query | Purpose |
|-------|---------|
| `level:ERROR` | Any application errors |
| `message:"connection pool"` | HikariCP warnings |
| `message:"circuit breaker"` | Resilience4j state changes |
| `message:"Expired stale"` | Provisional coverage cleanup |
| `message:"Failed to publish"` | Kafka publish failures |
| `@timestamp` sorted, filter by `requestId` | Trace individual slow requests |

### Metrics Collection Script

Before and after each test run, capture system state:

```
Pre-test snapshot:
  - /actuator/health (all component health)
  - /actuator/metrics/jvm.memory.used
  - /actuator/metrics/hikaricp.connections.active
  - /actuator/prometheus (full metrics dump)
  - SELECT count(*) FROM endorsements GROUP BY status
  - SELECT count(*) FROM ea_accounts
  - SELECT sum(balance), sum(reserved) FROM ea_accounts

Post-test snapshot:
  - Same as above (compare deltas)
  - SELECT * FROM ea_accounts WHERE (balance - reserved) < 0 (invariant check)
  - SELECT count(*) FROM endorsement_events (total events generated)
```

---

## 13. Risk Analysis & Bottleneck Hypothesis

### Hypothesized Bottlenecks

| # | Bottleneck | Hypothesis | How to Validate | Mitigation |
|---|-----------|-----------|-----------------|------------|
| 1 | **HikariCP Pool** | Default pool size (10) will saturate under 100+ concurrent users | Monitor `hikaricp_connections_active` = max | Tune `maximum-pool-size` to 20-50 |
| 2 | **Kafka Producer** | Synchronous `kafkaTemplate.send()` within @Transactional blocks delays response | Measure time between first and last Kafka send in create flow | Consider async publish after transaction commit |
| 3 | **EA Account Row Locks** | Concurrent reservations on same employer+insurer cause lock waits | Scenario 4 (EA Contention) -- measure retry rate | Optimistic locking (version column exists) |
| 4 | **JSONB Storage** | Large `employeeData` payloads increase I/O and memory | Scenario 10c -- compare throughput with 1KB vs 100KB payloads | Consider size limits on API |
| 5 | **Endorsement Events Table** | Write amplification (3-5 events per endorsement) creates I/O pressure | Monitor `endorsement_events` table size growth and insert latency | Batch event inserts, async event logging |
| 6 | **Batch Assembly Query** | `SELECT * FROM endorsements WHERE status = 'QUEUED_FOR_BATCH'` full scan at 50K+ rows | Scenario 5 -- measure assembly time at scale | Verify index on status column is used |
| 7 | **GC Pressure** | Virtual threads + object allocation under high load | Monitor `jvm.gc.pause` during stress test | Tune GC parameters, reduce object churn |
| 8 | **Thread Pool** | Virtual threads + blocking JDBC may still exhaust carrier threads | Monitor `jvm_threads_live_threads` under load | Platform thread pool sizing for carrier threads |
| 9 | **Pagination Deep Scans** | `OFFSET 2000 LIMIT 20` performs sequential scan | Scenario 2e -- measure p95 at page 100+ | Cursor-based pagination for deep pages |
| 10 | **Logstash TCP** | Structured JSON logging via TCP to Logstash adds I/O overhead | Compare throughput with `!json` vs `json` profile | Async appender with ring buffer (already configured: 8192) |

### Risk Matrix

```
Impact
  High   │  [5] Events Write   [3] EA Locks     [1] HikariCP Pool
         │      Amplification                        Saturation
         │
  Medium │  [6] Batch Query    [2] Kafka Sync   [7] GC Pressure
         │      at Scale           in TX
         │
  Low    │  [9] Deep           [4] JSONB Size   [10] Logstash
         │      Pagination                            Overhead
         │
         └──────────────────────────────────────────────────────
              Low              Medium              High
                            Likelihood
```

---

## 14. Test Execution Plan

### Execution Schedule

| Phase | Test | Duration | Pre-Requisite | Go/No-Go |
|-------|------|----------|---------------|----------|
| 1 | Baseline Simulation | 5 min | Environment verified, seed data loaded | All endpoints respond correctly |
| 2a | Load - Create Endorsement | 15 min | Phase 1 passed | p95 < 500ms, 0 errors |
| 2b | Load - Mixed Workload | 15 min | Phase 2a passed | SLAs met |
| 2c | Load - Full Lifecycle | 15 min | Phase 2b passed | E2E p95 < 1s |
| 3a | Stress - Ramp to Break | 20 min | Phase 2 passed | Breaking point identified |
| 3b | EA Contention | 10 min | Phase 2 passed | No negative balance |
| 3c | Database Stress | 15 min | Phase 2 passed | Connection pool behaves |
| 4 | Soak Test | 4 hours | Phase 3 passed | No degradation trend |
| 5 | Spike Test | 15 min | Phase 4 passed | Recovery within 2 min |
| 6 | Resilience Test | 15 min | Phase 2 passed | Circuit breaker works correctly |

### Pre-Test Checklist

- [ ] Docker containers healthy (all 9 services)
- [ ] Application started with performance profile
- [ ] Flyway migrations applied successfully
- [ ] Seed data loaded (EA accounts, pre-seeded endorsements)
- [ ] Grafana dashboards accessible and showing data
- [ ] Prometheus scraping application metrics
- [ ] Kibana connected to Elasticsearch
- [ ] Gatling test compile succeeds
- [ ] Baseline simulation passes (single user)

### Post-Test Checklist

- [ ] Gatling HTML reports generated and archived
- [ ] Grafana dashboard screenshots saved for test window
- [ ] Data integrity invariants verified (SQL checks)
- [ ] JVM heap dump analyzed (if OOM suspected)
- [ ] Kibana error logs reviewed
- [ ] Prometheus metrics exported for trend analysis
- [ ] Results compared against SLA table
- [ ] Bottleneck findings documented

---

## 15. Reporting

### Gatling Report Output

Each simulation generates an HTML report in `performance-tests/build/gatling-reports/` containing:

- **Global statistics**: Request count, success/failure, mean/p50/p75/p95/p99/max response times
- **Per-request breakdown**: Individual endpoint statistics
- **Active users over time**: Concurrency graph
- **Response time distribution**: Histogram
- **Response time percentiles over time**: Trend graph
- **Requests per second**: Throughput graph
- **Responses per second**: By status code

### Performance Test Summary Template

```
# Performance Test Report — [Test Name]
Date: YYYY-MM-DD
Duration: X minutes
Concurrency: X users / X req/s

## Results Summary
| Metric              | Target    | Actual    | Status |
|---------------------|-----------|-----------|--------|
| p50 Response Time   | < 200ms   | XXms      | PASS   |
| p95 Response Time   | < 500ms   | XXms      | PASS   |
| p99 Response Time   | < 1500ms  | XXms      | PASS   |
| Throughput          | > 100/s   | XX/s      | PASS   |
| Error Rate          | < 0.1%    | X.X%      | PASS   |
| Max Response Time   | < 5000ms  | XXms      | PASS   |

## Resource Utilization
| Resource            | Avg       | Peak      | Status |
|---------------------|-----------|-----------|--------|
| JVM Heap            | XX%       | XX%       | OK     |
| CPU                 | XX%       | XX%       | OK     |
| DB Connections      | XX/20     | XX/20     | OK     |
| GC Pause (p99)      | XXms      | XXms      | OK     |

## Bottlenecks Identified
1. ...
2. ...

## Recommendations
1. ...
2. ...
```

### Allure Integration

Performance test results can be added to the combined Allure report alongside API, BDD, and E2E tests:

- Gatling results exported as Allure-compatible JSON
- Post-processed with `parentSuite = "Performance Tests"` and `epic = "Performance"`
- Combined via `./run-all-tests.sh` for unified reporting

---

## 16. Appendix

### A. Gradle Commands

```bash
# Run specific simulation
./gradlew :performance-tests:gatlingRun-com.plum.endorsements.perf.simulations.LoadSimulation

# Run all simulations
./gradlew :performance-tests:gatlingRun

# Run with custom parameters
./gradlew :performance-tests:gatlingRun \
  -DbaseUrl=http://localhost:8080 \
  -DdurationMinutes=30 \
  -DtargetRps=100

# Open latest report
open performance-tests/build/reports/gatling/*/index.html
```

### B. Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `BASE_URL` | `http://localhost:8080` | Application base URL |
| `DB_URL` | `jdbc:postgresql://localhost:5432/endorsements` | Database URL for seeding |
| `DB_USER` | `plum` | Database user |
| `DB_PASSWORD` | `plum_dev` | Database password |
| `DURATION_MINUTES` | `15` | Test duration |
| `TARGET_RPS` | `100` | Target requests per second |
| `RAMP_DURATION_SECONDS` | `60` | Ramp-up period |
| `THINK_TIME_MS` | `1000` | Pause between requests (closed model) |

### C. Key Database Indexes (Performance Relevant)

```sql
-- Hot indexes during performance tests
idx_endorsements_employer   ON endorsements(employer_id)        -- List by employer
idx_endorsements_status     ON endorsements(status)             -- Batch assembly, status polling
idx_endorsements_insurer    ON endorsements(insurer_id)         -- Batch grouping
idx_endorsements_created    ON endorsements(created_at DESC)    -- Recent endorsements
endorsements_idempotency_key_key ON endorsements(idempotency_key)  -- Unique constraint

-- Composite queries that may need additional indexes
-- (employer_id, status) -- for filtered list endpoint
-- (status, insurer_id) -- for batch assembly query
```

### D. Estimated Storage Growth During Tests

| Test | Duration | Est. Endorsements | Est. Events | Est. DB Size |
|------|----------|------------------|-------------|-------------|
| Load (15 min) | 15 min | ~18,000 | ~72,000 | ~50 MB |
| Stress (20 min) | 20 min | ~60,000 | ~240,000 | ~200 MB |
| Soak (4 hours) | 4 hours | ~360,000 | ~1,440,000 | ~1.2 GB |

### E. Reference: Endorsement Status State Machine

```
                    CREATED
                      │
                   VALIDATED
                      │
              PROVISIONALLY_COVERED
                 ╱              ╲
    SUBMITTED_REALTIME     QUEUED_FOR_BATCH
           │                     │
    INSURER_PROCESSING     BATCH_SUBMITTED
         ╱    ╲                  │
   CONFIRMED  REJECTED    INSURER_PROCESSING
               │              ╱    ╲
         RETRY_PENDING   CONFIRMED  REJECTED
               │                     │
        (re-enters flow)      RETRY_PENDING
                                     │
                              FAILED_PERMANENT
```

### F. Port Reference

| Service | Port | Usage During Tests |
|---------|------|--------------------|
| Spring Boot | 8080 | Target under test |
| PostgreSQL | 5432 | Data seeding/verification |
| Redis | 6379 | Cache backend |
| Kafka | 9092 | Event bus |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Real-time monitoring |
| Kibana | 5601 | Log analysis |
| Elasticsearch | 9200 | Log storage |
| Jaeger | 16686 | Trace analysis |
