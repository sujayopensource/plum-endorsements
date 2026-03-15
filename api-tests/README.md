# API Integration Tests

End-to-end integration test suite for the Plum Endorsement Service REST API. Tests run against a real Spring Boot application backed by Testcontainers (PostgreSQL, Redis, Kafka) — no mocks for infrastructure.

## Quick Start

```bash
# From the project root
./run-api-tests.sh              # Run all tests + Allure report in browser
./run-api-tests.sh --report     # Regenerate report from last run
./run-api-tests.sh --stop       # Stop the Allure server
```

Or run directly via Gradle:

```bash
./gradlew :api-tests:test
```

## Prerequisites

| Dependency | Version | Purpose |
|------------|---------|---------|
| Java | 21+ | Compilation and runtime |
| Docker | Running | Testcontainers spins up PostgreSQL, Redis, Kafka |
| Gradle | 8.10.2 (wrapper included) | Build and test execution |

No other setup needed — Testcontainers handles all infrastructure automatically.

## Architecture

```
api-tests/
├── build.gradle.kts                          # Dependencies and Allure config
└── src/test/
    ├── java/com/plum/endorsements/apitest/
    │   ├── base/
    │   │   └── BaseApiTest.java              # Testcontainers, helpers, cleanup
    │   └── tests/
    │       ├── CreateEndorsementApiTest.java  # 9 tests
    │       ├── GetEndorsementApiTest.java     # 6 tests
    │       ├── SubmitEndorsementApiTest.java  # 3 tests
    │       ├── ConfirmRejectEndorsementApiTest.java  # 5 tests
    │       ├── ProvisionalCoverageApiTest.java       # 3 tests
    │       ├── EndorsementLifecycleApiTest.java      # 2 tests
    │       └── EAAccountApiTest.java                 # 4 tests
    └── resources/
        └── application-apitest.yml           # Test profile config
```

### How It Works

1. **Testcontainers** starts PostgreSQL 16, Redis 7, and Kafka (Confluent 7.6.0) as Docker containers before any tests run
2. **Spring Boot** launches the full application on a random port with the `apitest` profile
3. **Flyway** runs all migrations against the Testcontainers PostgreSQL instance
4. **RestAssured** sends real HTTP requests to the running application
5. **Allure** captures every request/response for the test report
6. **BaseApiTest** cleans all tables between tests for full isolation

Background schedulers (batch assembly, batch polling, coverage cleanup) are mocked via `@MockitoBean` to prevent interference.

## Test Coverage — 32 Tests

### POST /api/v1/endorsements — Create (9 tests)

| Test | What It Validates |
|------|-------------------|
| Should create ADD endorsement with PROVISIONALLY_COVERED status | Happy path: ADD type transitions to PROVISIONALLY_COVERED |
| Should auto-generate idempotency key when not provided | Key format: `{employerId}-{employeeId}-{type}-{coverageStartDate}` |
| Should return existing endorsement when duplicate idempotency key is used | Idempotency guarantee: same key returns same endorsement |
| Should create DELETE endorsement without provisional coverage | DELETE type skips coverage creation |
| Should create UPDATE endorsement without EA reservation | UPDATE type skips balance reservation |
| Should reserve funds when EA account has sufficient balance | Verifies reserved amount and available balance |
| Should not reserve funds when balance is insufficient | Endorsement still created, balance unchanged |
| Should return 400 when required fields are missing | ProblemDetail with field-level validation errors |
| Should return 400 when endorsement type is invalid | Rejects unknown endorsement types |

### GET /api/v1/endorsements — Read (6 tests)

| Test | What It Validates |
|------|-------------------|
| Should return endorsement when it exists | Full response fields for GET by ID |
| Should return 404 when endorsement not found | ProblemDetail with "Endorsement Not Found" title |
| Should list endorsements by employer ID | Paginated response filtered by employerId |
| Should filter endorsements by status | Status query parameter filtering |
| Should paginate endorsements correctly | page/size params, totalElements, totalPages |
| Should return empty page when no endorsements for employer | Empty content array, zero totals |

### POST /api/v1/endorsements/{id}/submit — Submit (3 tests)

| Test | What It Validates |
|------|-------------------|
| Should submit and auto-confirm via real-time path | MockInsurerAdapter auto-confirms, status becomes CONFIRMED |
| Should return 404 when submitting non-existent endorsement | 404 with ProblemDetail |
| Should return 400 when endorsement is in invalid state for submission | Cannot submit an already CONFIRMED endorsement |

### POST /api/v1/endorsements/{id}/confirm & reject — Confirm/Reject (5 tests)

| Test | What It Validates |
|------|-------------------|
| Should confirm endorsement and set insurer reference | INSURER_PROCESSING -> CONFIRMED with reference |
| Should return 404 when confirming non-existent endorsement | 404 with ProblemDetail |
| Should set retry pending when retry is available | retryCount < 3: status -> RETRY_PENDING, retryCount incremented |
| Should set failed permanent when max retries exhausted | retryCount >= 3: status -> FAILED_PERMANENT |
| Should return 404 when rejecting non-existent endorsement | 404 with ProblemDetail |

### GET /api/v1/endorsements/{id}/coverage — Provisional Coverage (3 tests)

| Test | What It Validates |
|------|-------------------|
| Should return provisional coverage for ADD endorsement | Coverage record with endorsementId, employeeId, employerId |
| Should return 404 for DELETE endorsement coverage | DELETE endorsements have no coverage record |
| Should return confirmed coverage after submission | After submit, coverageType=CONFIRMED with confirmedAt timestamp |

### End-to-End Lifecycle (2 tests)

| Test | What It Validates |
|------|-------------------|
| Should complete full lifecycle: create -> submit -> confirm | Full flow: seed EA -> create -> verify coverage -> verify reservation -> submit -> verify CONFIRMED -> verify coverage CONFIRMED |
| Should verify all response fields match request on creation | Every field in the response matches the original request values |

### GET /api/v1/ea-accounts — EA Accounts (4 tests)

| Test | What It Validates |
|------|-------------------|
| Should return EA account when it exists | balance, reserved, availableBalance fields |
| Should return 404 when EA account not found | 404 for unknown employer/insurer pair |
| Should reflect reservation after ADD endorsement creation | reserved increases, availableBalance decreases |
| Should not change balance for DELETE endorsement | DELETE type does not affect EA balance |

## API Endpoints Covered

| Method | Endpoint | Tests |
|--------|----------|-------|
| POST | `/api/v1/endorsements` | 9 |
| GET | `/api/v1/endorsements?employerId=` | 4 |
| GET | `/api/v1/endorsements/{id}` | 2 |
| POST | `/api/v1/endorsements/{id}/submit` | 3 |
| POST | `/api/v1/endorsements/{id}/confirm?insurerReference=` | 2 |
| POST | `/api/v1/endorsements/{id}/reject?reason=` | 3 |
| GET | `/api/v1/endorsements/{id}/coverage` | 3 |
| GET | `/api/v1/ea-accounts?employerId=&insurerId=` | 4 |

All 8 REST endpoints are covered.

## Allure Report

Tests are instrumented with [Allure](https://docs.qameta.io/allure/) annotations and RestAssured filters for rich reporting.

### Annotations Used

- `@Epic("Endorsement API")` — top-level grouping
- `@Feature(...)` — per-endpoint grouping (Create, Get, Submit, Confirm & Reject, Coverage, Lifecycle, EA Accounts)
- `@DisplayName(...)` — human-readable test names
- `@Description(...)` — detailed test purpose

### Viewing the Report

The `run-api-tests.sh` script launches an [Allure Docker Service](https://github.com/fescobar/allure-docker-service) container:

| URL | Purpose |
|-----|---------|
| http://localhost:5050/allure-docker-service/latest-report | Interactive test report |
| http://localhost:5050/allure-docker-service/swagger | Allure API (Swagger UI) |

The server auto-detects new results every 5 seconds. Re-run tests and refresh the browser to see updated results.

### Report Features

- Request/response payloads for every API call (via AllureRestAssured filter)
- Test grouping by Epic -> Feature -> Test
- Pass/fail history across runs (KEEP_HISTORY=1)
- Timeline view showing test execution order
- Detailed failure stack traces with request context

## Test Data

### Fixed UUIDs

Tests use deterministic UUIDs for reproducibility:

```
Employer ID:  11111111-1111-1111-1111-111111111111
Employee ID:  aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa
Insurer ID:   22222222-2222-2222-2222-222222222222
Policy ID:    33333333-3333-3333-3333-333333333333
```

### Database Cleanup

Every test starts with a clean database. Tables are truncated in FK-safe order:

```
endorsement_events -> ea_transactions -> provisional_coverages
    -> endorsements -> endorsement_batches -> ea_accounts
```

### Helper Methods (BaseApiTest)

| Method | Purpose |
|--------|---------|
| `seedEAAccount(employerId, insurerId, balance)` | Insert EA account via JDBC |
| `createEndorsementRequest(type, premiumAmount)` | Build request map with defaults |
| `createEndorsementViaApi(request)` | POST to `/api/v1/endorsements` |
| `createEndorsementAndGetId(type, premiumAmount)` | Create + extract ID |
| `seedEndorsementAtStatus(status, retryCount)` | Insert endorsement at specific state via JDBC |
| `cleanDatabase()` | Truncate all tables |

## Tech Stack

| Library | Version | Role |
|---------|---------|------|
| Spring Boot Test | 3.4.3 | Full application context with random port |
| RestAssured | 5.4.0 | HTTP API testing DSL |
| Allure JUnit5 | 2.25.0 | Test reporting annotations |
| Allure RestAssured | 2.25.0 | Request/response capture |
| Testcontainers | 1.19.8 | PostgreSQL 16, Redis 7, Kafka containers |
| JUnit 5 | (via Spring Boot) | Test framework |
| AssertJ | (via Spring Boot) | Fluent assertions |
| Hamcrest | (via RestAssured) | Response body matchers |

## Configuration

The `application-apitest.yml` profile configures:

- **Server port:** 0 (random, for parallel-safe execution)
- **Flyway:** enabled, runs all production migrations
- **Hibernate:** validate mode (schema from Flyway)
- **Cache:** Caffeine, 100 max entries, 10s TTL
- **Batch scheduling:** disabled (`cron: "-"`)
- **Retry:** 3 max attempts, 100ms backoff
- **Virtual threads:** enabled
- **Logging:** WARN for most, DEBUG for `com.plum`

Database, Redis, and Kafka connection details are injected at runtime via `@DynamicPropertySource` from Testcontainers.

## Troubleshooting

### Tests fail with container startup errors

Ensure Docker Desktop is running and has sufficient resources (4GB+ RAM recommended).

```bash
docker info  # Should not error
```

### Tests are slow on first run

First run pulls Docker images (~1-2 min). Subsequent runs reuse cached images (~45s total).

### Allure report is empty

Ensure tests ran before generating the report:

```bash
./run-api-tests.sh           # runs tests first
# NOT
./run-api-tests.sh --report  # skips tests, uses last results
```

### Port conflicts

Tests use random ports, so conflicts are unlikely. If the Allure server port (5050) is busy:

```bash
./run-api-tests.sh --stop    # stop existing server
./run-api-tests.sh --report  # restart it
```
