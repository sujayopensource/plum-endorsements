# Performance Scenarios

## Overview

This document describes all Gatling performance test scenarios for the Plum Endorsement Service. The test suite validates throughput, latency, concurrency, and system stability across the endorsement lifecycle.

**Technology**: Gatling 3.10.5 (Scala DSL)
**Module**: `performance-tests` (Gradle submodule)
**Base Package**: `com.plum.endorsements.perf`

---

## Table of Contents

1. [Configuration](#1-configuration)
2. [Data Feeders](#2-data-feeders)
3. [HTTP Request Definitions](#3-http-request-definitions)
4. [Scenarios](#4-scenarios)
5. [Simulations](#5-simulations)
6. [SLA Assertions Summary](#6-sla-assertions-summary)
7. [Running the Tests](#7-running-the-tests)

---

## 1. Configuration

**File**: `config/TestConfig.scala`

All parameters are configurable via system properties with sensible defaults:

| Parameter | Default | Description |
|-----------|---------|-------------|
| `baseUrl` | `http://localhost:8080` | Application base URL |
| `dbUrl` | `jdbc:postgresql://localhost:5432/endorsements` | PostgreSQL connection URL |
| `dbUser` | `plum` | Database username |
| `dbPassword` | `plum_dev` | Database password |
| `durationMinutes` | `15` | Test duration in minutes |
| `targetRps` | `20` | Target requests per second |
| `rampDurationSeconds` | `60` | Ramp-up period |
| `thinkTimeMs` | `1000` | Think time between actions |

**HTTP Protocol Settings**:
- Content-Type: `application/json`
- Accept: `application/json`
- `X-Request-Id` header with UUID per request (traceability)
- Max 10 connections per host, shared connections enabled

---

## 2. Data Feeders

**File**: `feeders/EndorsementFeeders.scala`

| Feeder | Strategy | Source | Description |
|--------|----------|--------|-------------|
| `employerFeeder` | Circular | `feeders/employers.csv` | 10 pre-generated employer UUIDs |
| `insurerFeeder` | Circular | `feeders/insurers.csv` | 10 pre-generated insurer UUIDs |
| `endorsementTypeFeeder` | Circular | In-memory | Weighted: 60% ADD, 25% UPDATE, 15% DELETE |
| `employeeDataFeeder` | Iterator | Generated | Random name, age (22-64), department, email, UUIDs for employeeId, policyId, idempotencyKey |
| `premiumFeeder` | Iterator | Generated | Random BigDecimal between 100.00 and 5000.00 |
| `coverageDateFeeder` | Iterator | Generated | Start date: today + 1-30 days, End date: start + 365 days |

---

## 3. HTTP Request Definitions

### 3.1 Endorsement Requests

**File**: `requests/EndorsementRequests.scala`

| Request | Method | Endpoint | Expected Status | Saved Session Values |
|---------|--------|----------|-----------------|---------------------|
| `createEndorsement` | POST | `/api/v1/endorsements` | 201 | `endorsementId`, `endorsementStatus` |
| `getEndorsement` | GET | `/api/v1/endorsements/{endorsementId}` | 200 | `endorsementStatus` |
| `listEndorsements` | GET | `/api/v1/endorsements?employerId={}&page=0&size=20` | 200 | - |
| `listFilteredEndorsements` | GET | `/api/v1/endorsements?employerId={}&statuses=CONFIRMED&page=0&size=20` | 200 | - |
| `submitEndorsement` | POST | `/api/v1/endorsements/{endorsementId}/submit` | 202 | - |
| `confirmEndorsement` | POST | `/api/v1/endorsements/{endorsementId}/confirm?insurerReference=INS-PERF-{uuid}` | 200 | - |
| `rejectEndorsement` | POST | `/api/v1/endorsements/{endorsementId}/reject?reason=perf-test-rejection` | 200 | - |
| `getCoverage` | GET | `/api/v1/endorsements/{endorsementId}/coverage` | 200 or 404 | - |

### 3.2 EA Account Requests

**File**: `requests/EAAccountRequests.scala`

| Request | Method | Endpoint | Expected Status | Saved Session Values |
|---------|--------|----------|-----------------|---------------------|
| `getBalance` | GET | `/api/v1/ea-accounts?employerId={}&insurerId={}` | 200 or 404 | `availableBalance` (conditionally, only on 200) |

---

## 4. Scenarios

### 4.1 Create Endorsement Scenario

**File**: `scenarios/CreateEndorsementScenario.scala`
**Purpose**: Validates endorsement creation and immediate retrieval.

**Flow**:
1. Feed: employer, insurer, endorsementType, employeeData, premium, coverageDate
2. `POST /api/v1/endorsements` (Create Endorsement)
3. Pause 300ms - 1s (simulated think time)
4. `GET /api/v1/endorsements/{id}` (Verify endorsement was created)

**Key Characteristics**:
- Uses weighted endorsement type distribution (60% ADD, 25% UPDATE, 15% DELETE)
- Each virtual user creates exactly one endorsement and verifies it

---

### 4.2 Retrieval Scenario

**File**: `scenarios/RetrievalScenario.scala`
**Purpose**: Tests all read operations (single, paginated list, filtered list).

**Flow**:
1. Feed: employer, insurer, endorsementType, employeeData, premium, coverageDate
2. `POST /api/v1/endorsements` (Create endorsement to get a valid ID)
3. Pause 200ms - 500ms
4. `GET /api/v1/endorsements/{id}` (Get by ID)
5. Pause 200ms - 500ms
6. `GET /api/v1/endorsements?employerId={}&page=0&size=20` (Paginated list)
7. Pause 200ms - 500ms
8. `GET /api/v1/endorsements?employerId={}&statuses=CONFIRMED&page=0&size=20` (Filtered list)

**Key Characteristics**:
- Creates a seed endorsement first to ensure valid data for retrieval
- Tests three different read patterns in sequence
- Short think times to maximize read throughput measurement

---

### 4.3 Full Lifecycle Scenario

**File**: `scenarios/FullLifecycleScenario.scala`
**Purpose**: Exercises the complete endorsement lifecycle from creation to confirmation, including EA balance verification.

**Flow**:
1. Feed: employer, insurer, employeeData, premium, coverageDate
2. Force endorsement type to `ADD` (required for provisional coverage and EA reservation)
3. `POST /api/v1/endorsements` (Create endorsement)
4. Pause 500ms
5. `GET /api/v1/endorsements/{id}` (Verify status is `PROVISIONALLY_COVERED`)
6. Pause 200ms
7. `POST /api/v1/endorsements/{id}/submit` (Submit to insurer)
8. Pause 500ms
9. `GET /api/v1/endorsements/{id}` (Verify status is `CONFIRMED` — mock insurer auto-confirms)
10. `GET /api/v1/endorsements/{id}/coverage` (Check provisional coverage record)
11. `GET /api/v1/ea-accounts?employerId={}&insurerId={}` (Verify EA balance)

**Key Characteristics**:
- Forces ADD type because only ADD endorsements trigger provisional coverage and EA reservation
- Validates the full state machine: CREATED -> VALIDATED -> PROVISIONALLY_COVERED -> SUBMITTED_REALTIME -> CONFIRMED
- Includes EA balance check to verify financial accounting

---

### 4.4 EA Contention Scenario

**File**: `scenarios/EAContentionScenario.scala`
**Purpose**: Tests concurrent writes to the same EA account to validate optimistic locking and data integrity.

**Flow**:
1. Set fixed employer ID: `a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d`
2. Set fixed insurer ID: `11111111-1111-4111-8111-111111111111`
3. Force endorsement type to `ADD`
4. Feed: employeeData, premium, coverageDate
5. `POST /api/v1/endorsements` (Create endorsement — all users target same EA account)
6. `GET /api/v1/ea-accounts` (Check balance for same employer+insurer)

**Key Characteristics**:
- All virtual users target the **same** employer+insurer combination (first entry from feeders)
- Validates that concurrent EA balance reservations don't corrupt data
- Tests optimistic locking / row-level locking under contention
- Critical for proving financial integrity under load

---

### 4.5 Mixed Workload Scenario

**File**: `scenarios/MixedWorkloadScenario.scala`
**Purpose**: Simulates realistic production traffic with weighted distribution of all operations.

**Traffic Distribution** (via `randomSwitch`):

| Weight | Operation | Description |
|--------|-----------|-------------|
| 30% | Create ADD | Create endorsement with type ADD |
| 12% | Create UPDATE | Create endorsement with type UPDATE |
| 8% | Create DELETE | Create endorsement with type DELETE |
| 20% | Read Endorsement | Create then immediately read (GET by ID) |
| 15% | List Endorsements | Paginated list by employer |
| 10% | Submit and Confirm | Create -> Submit (full write path) |
| 5% | Check Balance | EA account balance lookup |

**Key Characteristics**:
- 50% write-heavy (create/submit), 35% read (get/list), 15% other (balance)
- Represents realistic production traffic patterns
- Each sub-flow is self-contained with its own feeder data

---

## 5. Simulations

### 5.1 Baseline Simulation

**File**: `simulations/BaselineSimulation.scala`
**Purpose**: Smoke test — verifies the system handles a single user without errors.
**Phase**: Pre-test validation

| Property | Value |
|----------|-------|
| Scenario | Full Lifecycle |
| Load Model | Closed: `atOnceUsers(1)` |
| Max Duration | 2 minutes |
| **Assertions** | |
| p95 Response Time | < 2000ms |
| Success Rate | > 99% |

---

### 5.2 Load Simulation

**File**: `simulations/LoadSimulation.scala`
**Purpose**: Validates system performance under expected production load with multiple concurrent scenario types.
**Phase**: Normal load testing

| Scenario | Injection Profile | Duration |
|----------|-------------------|----------|
| Create Endorsement | `constantUsersPerSec(20)` | 15 minutes |
| Retrieval | `constantUsersPerSec(50)` | 15 minutes |
| Full Lifecycle | `constantUsersPerSec(10)` | 15 minutes |

**Total Load**: 80 virtual users/second sustained

| Assertion | Threshold |
|-----------|-----------|
| p95 Response Time | < 500ms |
| Success Rate | > 99.9% |
| Max Response Time (any request) | < 5000ms |

---

### 5.3 Stress Simulation

**File**: `simulations/StressSimulation.scala`
**Purpose**: Finds system breaking point by progressively increasing load beyond normal capacity.
**Phase**: Stress testing

| Phase | Profile | Duration |
|-------|---------|----------|
| Ramp Up | `rampUsersPerSec(10).to(500)` | 10 minutes |
| Hold Peak | `constantUsersPerSec(500)` | 5 minutes |
| Ramp Down | `rampUsersPerSec(500).to(10)` | 5 minutes |

**Total Duration**: 20 minutes

| Assertion | Threshold |
|-----------|-----------|
| p99 Response Time | < 3000ms |
| Failure Rate | < 5% |

---

### 5.4 Soak Simulation

**File**: `simulations/SoakSimulation.scala`
**Purpose**: Detects memory leaks, connection pool exhaustion, and other long-running stability issues.
**Phase**: Endurance testing

| Phase | Profile | Duration |
|-------|---------|----------|
| Ramp Up | `rampUsersPerSec(1).to(100)` | 2 minutes |
| Sustained | `constantUsersPerSec(100)` | 4 hours |

**Max Duration**: 4 hours 5 minutes

| Assertion | Threshold |
|-----------|-----------|
| p95 Response Time | < 1000ms |
| Success Rate | > 99% |

---

### 5.5 Spike Simulation

**File**: `simulations/SpikeSimulation.scala`
**Purpose**: Tests system behavior under sudden, dramatic traffic spikes and recovery.
**Phase**: Spike/burst testing

| Phase | Profile | Duration |
|-------|---------|----------|
| Warm-up | `rampUsersPerSec(10).to(50)` | 1 minute |
| Steady | `constantUsersPerSec(100)` | 2 minutes |
| Spike Ramp | `rampUsersPerSec(100).to(500)` | 1 minute |
| Spike Hold | `constantUsersPerSec(500)` | 2 minutes |
| Recovery Ramp | `rampUsersPerSec(500).to(100)` | 2 minutes |
| Post-Recovery | `constantUsersPerSec(100)` | 6 minutes |

**Total Duration**: ~14 minutes

| Assertion | Threshold |
|-----------|-----------|
| Failure Rate | < 5% |
| Max Response Time | < 10000ms |

---

### 5.6 EA Contention Simulation

**File**: `simulations/EAContentionSimulation.scala`
**Purpose**: Validates data integrity when 200 concurrent users write to the same EA account simultaneously.
**Phase**: Concurrency/data integrity testing

| Property | Value |
|----------|-------|
| Scenario | EA Contention |
| Load Model | Closed: `atOnceUsers(200)` |
| All users share | Same employer ID + insurer ID |

| Assertion | Threshold |
|-----------|-----------|
| Success Rate | 100% (zero tolerance) |

---

### 5.7 Full Lifecycle Simulation

**File**: `simulations/FullLifecycleSimulation.scala`
**Purpose**: Tests the complete endorsement lifecycle under sustained load.
**Phase**: Integration performance testing

| Phase | Profile | Duration |
|-------|---------|----------|
| Ramp Up | `rampUsersPerSec(1).to(50)` | 5 minutes |
| Sustained | `constantUsersPerSec(50)` | 10 minutes |

**Total Duration**: 15 minutes

| Assertion | Threshold |
|-----------|-----------|
| p95 Response Time | < 1000ms |
| Success Rate | > 99% |

---

### 5.8 Mixed Workload Simulation

**File**: `simulations/MixedWorkloadSimulation.scala`
**Purpose**: Simulates realistic production traffic at configurable rates with per-endpoint SLA assertions.
**Phase**: Production readiness testing

| Property | Value |
|----------|-------|
| Scenario | Mixed Workload |
| Load Model | Open: `constantUsersPerSec(targetRps)` |
| Duration | Configurable (`durationMinutes`, default 15 min) |
| RPS | Configurable (`targetRps`, default 20) |

**Per-Endpoint SLA Assertions**:

| Assertion | Threshold |
|-----------|-----------|
| p50 Response Time (global) | < 200ms |
| p95 Response Time (global) | < 500ms |
| p99 Response Time (global) | < 1500ms |
| Success Rate (global) | > 99% |
| p95 Create Endorsement | < 500ms |
| p95 Get Endorsement | < 100ms |
| p95 Submit Endorsement | < 800ms |

---

## 6. SLA Assertions Summary

| Simulation | p50 | p95 | p99 | Max | Success Rate | Duration |
|------------|-----|-----|-----|-----|-------------|----------|
| Baseline | - | < 2s | - | - | > 99% | 2 min |
| Load | - | < 500ms | - | < 5s | > 99.9% | 15 min |
| Stress | - | - | < 3s | - | > 95% | 20 min |
| Soak | - | < 1s | - | - | > 99% | ~4 hrs |
| Spike | - | - | - | < 10s | > 95% | ~14 min |
| EA Contention | - | - | - | - | 100% | Burst |
| Full Lifecycle | - | < 1s | - | - | > 99% | 15 min |
| Mixed Workload | < 200ms | < 500ms | < 1.5s | - | > 99% | 15 min |

---

## 7. Running the Tests

### Quick Start

```bash
# Run baseline smoke test (default)
./run-perf-tests.sh

# Run a specific simulation
./run-perf-tests.sh LoadSimulation

# List all available simulations
./run-perf-tests.sh --list
```

### Custom Parameters

```bash
# Run stress test with custom RPS and duration
./run-perf-tests.sh StressSimulation --rps 50 --duration 30

# Run against a different environment
./run-perf-tests.sh LoadSimulation --url http://staging:8080
```

### Environment Variables

```bash
BASE_URL=http://staging:8080 \
DB_URL=jdbc:postgresql://staging-db:5432/endorsements \
TARGET_RPS=100 \
DURATION_MINUTES=30 \
./run-perf-tests.sh MixedWorkloadSimulation
```

### Direct Gradle Execution

```bash
./gradlew :performance-tests:gatlingRunSimulation \
  -Dgatling.simulation=com.plum.endorsements.perf.simulations.LoadSimulation \
  -DbaseUrl=http://localhost:8080 \
  -DtargetRps=50 \
  -DdurationMinutes=20
```

### Reports

- **Gatling HTML Report**: `performance-tests/build/reports/gatling/*/index.html`
- **Allure Integration**: Performance results are included in the combined Allure report when running `./run-all-tests.sh`

### Available Simulations

| Simulation | Use Case | Recommended For |
|------------|----------|-----------------|
| `BaselineSimulation` | Smoke test | Pre-deployment validation |
| `LoadSimulation` | Normal production load | Sprint release gate |
| `StressSimulation` | Find breaking point | Capacity planning |
| `SoakSimulation` | Long-running stability | Pre-production sign-off |
| `SpikeSimulation` | Traffic burst resilience | Black Friday / open enrollment prep |
| `EAContentionSimulation` | Concurrent write integrity | After schema or locking changes |
| `FullLifecycleSimulation` | End-to-end flow under load | Integration validation |
| `MixedWorkloadSimulation` | Realistic production traffic | Continuous performance monitoring |
