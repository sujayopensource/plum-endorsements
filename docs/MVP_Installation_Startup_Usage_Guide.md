# Endorsement Management Service — MVP Installation, Startup & Usage Guide

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Project Structure](#2-project-structure)
3. [Installation](#3-installation)
4. [Infrastructure Setup](#4-infrastructure-setup)
5. [Build & Run](#5-build--run)
6. [Verify the Service](#6-verify-the-service)
7. [API Reference](#7-api-reference)
8. [End-to-End Usage Walkthrough](#8-end-to-end-usage-walkthrough)
9. [Monitoring & Observability](#9-monitoring--observability)
10. [Running Tests](#10-running-tests)
11. [Docker Production Build](#11-docker-production-build)
12. [Deploy to Railway](#12-deploy-to-railway)
13. [Configuration Reference](#13-configuration-reference)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Prerequisites

| Tool          | Version  | Check Command              |
|---------------|----------|----------------------------|
| Java (JDK)    | 21+      | `java -version`            |
| Docker        | 24+      | `docker --version`         |
| Docker Compose| v2+      | `docker compose version`   |
| Git           | 2.x      | `git --version`            |
| curl / httpie | any      | `curl --version`           |

> **Note:** Gradle wrapper is bundled — no separate Gradle install needed.

---

## 2. Project Structure

```
plum-endorsements/
├── build.gradle.kts              # Build config (Spring Boot 3.4.3, Java 21)
├── docker-compose.yml            # PostgreSQL, Redis, Kafka, Jaeger
├── Dockerfile                    # Multi-stage production image
├── gradlew / gradlew.bat         # Gradle wrapper
├── src/main/java/com/plum/endorsements/
│   ├── EndorsementApplication.java
│   ├── api/
│   │   ├── controller/           # REST controllers
│   │   ├── dto/                  # Request/Response records
│   │   └── exception/            # GlobalExceptionHandler
│   ├── application/
│   │   ├── handler/              # CreateEndorsement, ProcessEndorsement, Query
│   │   ├── scheduler/            # BatchAssembly, BatchPoller, CoverageCleanup
│   │   └── exception/            # Domain exceptions
│   ├── domain/
│   │   ├── model/                # Endorsement, EAAccount, ProvisionalCoverage, etc.
│   │   ├── port/                 # Repository & service port interfaces
│   │   └── service/              # StateMachine, BalanceCalculator
│   └── infrastructure/
│       ├── persistence/          # JPA entities, repos, adapters, mapper
│       ├── messaging/            # KafkaEventPublisher, KafkaConfig
│       └── config/               # SecurityConfig, MockInsurerAdapter, etc.
├── src/main/resources/
│   ├── application.yml           # Main config
│   ├── application-test.yml      # Test config (H2, embedded Kafka)
│   └── db/migration/             # Flyway V1–V5 SQL migrations
└── src/test/java/                # Unit & integration tests (101 tests)
```

---

## 3. Installation

```bash
# Clone the repository
git clone <repository-url> plum-endorsements
cd plum-endorsements

# Verify Java 21
java -version
# Expected: openjdk version "21.x.x"

# Make Gradle wrapper executable (macOS/Linux)
chmod +x gradlew

# Download dependencies (first run takes 1–2 minutes)
./gradlew dependencies --no-daemon
```

---

## 4. Infrastructure Setup

Start all backing services using Docker Compose:

```bash
docker compose up -d
```

This starts:

| Service    | Port(s)           | Purpose                        |
|------------|-------------------|--------------------------------|
| PostgreSQL | `5432`            | Primary database               |
| Redis      | `6379`            | Cache layer                    |
| Kafka      | `9092`            | Event streaming (KRaft mode)   |
| Jaeger     | `16686`, `4317`   | Distributed tracing UI & OTLP  |

**Verify all services are healthy:**

```bash
docker compose ps
```

All services should show `healthy` or `running` status.

**Wait for readiness (important for first run):**

```bash
# PostgreSQL
docker compose exec postgres pg_isready -U plum -d endorsements

# Redis
docker compose exec redis redis-cli ping
# Expected: PONG

# Kafka
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:29092 --list
```

---

## 5. Build & Run

### Option A: Run with Gradle (Development)

```bash
# Build and run
./gradlew bootRun --no-daemon
```

The service starts on `http://localhost:8080`. You'll see Flyway migrate the database schema automatically on first boot.

**Expected startup log output:**

```
Flyway ... Successfully applied 5 migrations
Started EndorsementApplication in X.XXs
```

### Option B: Build JAR and run directly

```bash
# Build the fat JAR
./gradlew bootJar --no-daemon

# Run it
java -jar build/libs/endorsement-service.jar
```

---

## 6. Verify the Service

```bash
# Health check
curl -s http://localhost:8080/actuator/health | python3 -m json.tool

# Expected:
# {
#   "status": "UP",
#   "components": {
#     "db": { "status": "UP" },
#     "redis": { "status": "UP" },
#     ...
#   }
# }

# OpenAPI / Swagger UI
open http://localhost:8080/swagger-ui
# or
curl -s http://localhost:8080/api-docs | python3 -m json.tool
```

---

## 7. API Reference

Base URL: `http://localhost:8080`

### 7.1 Endorsements

| Method | Endpoint                              | Description                    |
|--------|---------------------------------------|--------------------------------|
| POST   | `/api/v1/endorsements`                | Create a new endorsement       |
| GET    | `/api/v1/endorsements/{id}`           | Get endorsement by ID          |
| GET    | `/api/v1/endorsements`                | List endorsements (paginated)  |
| POST   | `/api/v1/endorsements/{id}/submit`    | Submit to insurer              |
| POST   | `/api/v1/endorsements/{id}/confirm`   | Confirm (insurer accepted)     |
| POST   | `/api/v1/endorsements/{id}/reject`    | Reject (insurer declined)      |
| GET    | `/api/v1/endorsements/{id}/coverage`  | Get provisional coverage       |

### 7.2 EA Accounts

| Method | Endpoint                | Description            |
|--------|-------------------------|------------------------|
| GET    | `/api/v1/ea-accounts`   | Get EA account balance |

### 7.3 Actuator

| Method | Endpoint                      | Description               |
|--------|-------------------------------|---------------------------|
| GET    | `/actuator/health`            | Health status              |
| GET    | `/actuator/info`              | Application info           |
| GET    | `/actuator/metrics`           | Micrometer metrics         |
| GET    | `/actuator/prometheus`        | Prometheus scrape endpoint |

---

## 8. End-to-End Usage Walkthrough

Below is a complete walkthrough demonstrating the endorsement lifecycle: creation, provisional coverage, insurer submission, and confirmation.

### Step 0: Seed an EA Account

The MVP does not include an admin API for creating EA accounts. Insert one directly into the database:

```bash
docker compose exec postgres psql -U plum -d endorsements -c "
INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  '22222222-2222-2222-2222-222222222222',
  50000.00,
  0.00,
  now()
);
"
```

### Step 1: Create an Endorsement (ADD employee)

```bash
curl -s -X POST http://localhost:8080/api/v1/endorsements \
  -H "Content-Type: application/json" \
  -d '{
    "employerId": "11111111-1111-1111-1111-111111111111",
    "employeeId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "insurerId": "22222222-2222-2222-2222-222222222222",
    "policyId": "33333333-3333-3333-3333-333333333333",
    "type": "ADD",
    "coverageStartDate": "2026-04-01",
    "coverageEndDate": "2027-03-31",
    "employeeData": {
      "name": "Ravi Kumar",
      "dob": "1990-05-15",
      "gender": "M",
      "relation": "SELF",
      "sumInsured": 500000
    },
    "premiumAmount": 1200.00
  }' | python3 -m json.tool
```

**Expected response (HTTP 201):**

```json
{
  "id": "<generated-uuid>",
  "employerId": "11111111-1111-1111-1111-111111111111",
  "employeeId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "insurerId": "22222222-2222-2222-2222-222222222222",
  "policyId": "33333333-3333-3333-3333-333333333333",
  "type": "ADD",
  "status": "PROVISIONALLY_COVERED",
  "coverageStartDate": "2026-04-01",
  "coverageEndDate": "2027-03-31",
  "premiumAmount": 1200.00,
  "retryCount": 0,
  "idempotencyKey": "11111111-...-ADD-2026-04-01",
  "createdAt": "2026-03-07T...",
  "updatedAt": "2026-03-07T..."
}
```

> **What happened behind the scenes:**
> - Endorsement created with status `CREATED`
> - Validated and transitioned to `VALIDATED`
> - Provisional coverage record created (employee is covered immediately)
> - Status set to `PROVISIONALLY_COVERED`
> - EA account balance reserved: 1200.00 held against the employer's account
> - Events published to Kafka: `Created`, `Validated`, `ProvisionalCoverageGranted`

Save the returned `id` for subsequent steps:

```bash
export ENDORSEMENT_ID="<paste-the-id-from-response>"
```

### Step 2: Verify Provisional Coverage

```bash
curl -s http://localhost:8080/api/v1/endorsements/$ENDORSEMENT_ID/coverage \
  | python3 -m json.tool
```

This returns the provisional coverage record — the employee is covered from `coverageStartDate` even before the insurer confirms.

### Step 3: Check EA Account Balance

```bash
curl -s "http://localhost:8080/api/v1/ea-accounts?employerId=11111111-1111-1111-1111-111111111111&insurerId=22222222-2222-2222-2222-222222222222" \
  | python3 -m json.tool
```

**Expected:** `balance: 50000.00`, `reserved: 1200.00`, `availableBalance: 48800.00`

### Step 4: Submit to Insurer

```bash
curl -s -X POST http://localhost:8080/api/v1/endorsements/$ENDORSEMENT_ID/submit
# HTTP 202 Accepted
```

The endorsement is now submitted to the insurer (via mock adapter in MVP). Status transitions to either `SUBMITTED_REALTIME` or `QUEUED_FOR_BATCH` depending on the mock insurer's capability response.

### Step 5: Verify Current Status

```bash
curl -s http://localhost:8080/api/v1/endorsements/$ENDORSEMENT_ID \
  | python3 -m json.tool
```

### Step 6: Simulate Insurer Confirmation

```bash
curl -s -X POST "http://localhost:8080/api/v1/endorsements/$ENDORSEMENT_ID/confirm?insurerReference=INS-REF-2026-001"
# HTTP 200 OK
```

> **What happened:**
> - Status transitions to `CONFIRMED`
> - Provisional coverage upgraded to permanent
> - EA reserved funds debited

### Step 7: Verify Final State

```bash
# Check endorsement is CONFIRMED
curl -s http://localhost:8080/api/v1/endorsements/$ENDORSEMENT_ID \
  | python3 -m json.tool

# Check EA balance (reserved released, balance debited)
curl -s "http://localhost:8080/api/v1/ea-accounts?employerId=11111111-1111-1111-1111-111111111111&insurerId=22222222-2222-2222-2222-222222222222" \
  | python3 -m json.tool
```

### Step 8: Simulate Rejection (Alternative Flow)

Create another endorsement (repeating Step 1 with a different employee), then reject it:

```bash
curl -s -X POST "http://localhost:8080/api/v1/endorsements/$NEW_ENDORSEMENT_ID/reject?reason=Employee%20not%20eligible"
# HTTP 200 OK
```

The endorsement moves to `REJECTED`. If `retryCount < 3`, it can be resubmitted.

### Step 9: List Endorsements with Filters

```bash
# All endorsements for an employer
curl -s "http://localhost:8080/api/v1/endorsements?employerId=11111111-1111-1111-1111-111111111111" \
  | python3 -m json.tool

# Filter by status
curl -s "http://localhost:8080/api/v1/endorsements?employerId=11111111-1111-1111-1111-111111111111&statuses=CONFIRMED&statuses=REJECTED&page=0&size=10" \
  | python3 -m json.tool
```

### Step 10: Idempotency Check

Repeat the exact same `POST /api/v1/endorsements` from Step 1. The system detects the duplicate `idempotencyKey` and returns the existing endorsement instead of creating a new one.

---

## 9. Monitoring & Observability

### Jaeger (Distributed Tracing)

Open [http://localhost:16686](http://localhost:16686) to view traces for all API calls, database queries, and Kafka interactions.

### Prometheus Metrics

```bash
curl -s http://localhost:8080/actuator/prometheus
```

Key metrics include:
- `http_server_requests_seconds_*` — API latency
- `jvm_memory_*` — Memory usage
- `hikaricp_connections_*` — Connection pool stats
- `spring_kafka_*` — Kafka producer/consumer metrics

### Health Endpoint

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

Shows status for: database, Redis, Kafka, and disk space.

---

## 10. Running Tests

```bash
# Run all 101 tests
./gradlew test --no-daemon

# Run with verbose output
./gradlew test --no-daemon --info

# Run a specific test class
./gradlew test --no-daemon --tests "com.plum.endorsements.domain.model.EndorsementStatusTest"
```

Tests use H2 in-memory database (no Docker required for tests).

---

## 11. Docker Production Build

```bash
# Build the Docker image
docker build -t plum/endorsement-service:0.1.0 .

# Run with Docker (connect to compose network)
docker run --rm \
  --network plum-endorsements_default \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/endorsements \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092 \
  -p 8080:8080 \
  plum/endorsement-service:0.1.0
```

The Dockerfile uses:
- **Multi-stage build** — JDK 21 for build, JRE 21 for runtime
- **ZGC** garbage collector optimized for low-latency
- **Virtual threads** enabled by default
- **Non-root user** (`plum`) for security
- **Built-in health check** via actuator

---

## 12. Deploy to Railway

Railway is a cloud platform that deploys directly from your Git repository using the project's Dockerfile. This section covers deploying the full endorsement service stack (app + PostgreSQL + Redis + Kafka) to Railway.

### 12.1 Prerequisites

- A [Railway account](https://railway.com) (Hobby or Pro plan)
- [Railway CLI](https://docs.railway.com/develop/cli) installed (optional, for CLI deployment)
- Your code pushed to a GitHub repository

```bash
# Install Railway CLI (macOS)
brew install railway

# Or via npm
npm install -g @railway/cli

# Login
railway login
```

### 12.2 Project Files for Railway

The following files have been added to support Railway deployment:

**`railway.toml`** — Config-as-code for Railway build and deploy settings:

```toml
[build]
builder = "DOCKERFILE"
dockerfilePath = "Dockerfile"

[deploy]
healthcheckPath = "/actuator/health"
healthcheckTimeout = 300
restartPolicyType = "ON_FAILURE"
restartPolicyMaxRetries = 5
```

**`src/main/resources/application-railway.yml`** — Spring profile activated on Railway:

- Reads `DATABASE_URL`, `REDIS_URL`, `KAFKA_URL` from Railway service variables
- Reads `PORT` injected by Railway
- Production-safe log levels

**`Dockerfile`** — Updated to respect Railway's `PORT` injection:

- Uses `ENV PORT=8080` with fallback
- Health check uses `${PORT}` dynamically

### 12.3 Step-by-Step Deployment

#### Step 1: Create a Railway Project

```bash
# Via CLI
railway init
# Select "Empty Project"
```

Or go to [railway.com/new](https://railway.com/new) and create an empty project.

#### Step 2: Provision PostgreSQL

```bash
# Via CLI
railway add --plugin postgresql
```

Or on the Railway dashboard:
1. Open your project
2. Click **+ New** on the project canvas
3. Select **Database** > **PostgreSQL**

Railway automatically provisions PostgreSQL 16 and exposes these variables:
- `DATABASE_URL` — full JDBC-compatible connection string
- `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`

#### Step 3: Provision Redis

```bash
# Via CLI
railway add --plugin redis
```

Or on the dashboard: **+ New** > **Database** > **Redis**

Railway exposes:
- `REDIS_URL` — full connection string (e.g., `redis://default:pass@host:6379`)
- `REDISHOST`, `REDISPORT`, `REDISPASSWORD`

#### Step 4: Provision Kafka

Railway provides a Kafka template using Confluent's KRaft-based broker (no ZooKeeper):

1. On the dashboard, click **+ New** > **Template**
2. Search for **"Kafka"** and deploy the [Kafka (w/Kafka UI)](https://railway.com/deploy/kafka-wkafka-ui) template
3. This deploys a single Kafka broker with Kafka UI for topic inspection

The Kafka service exposes:
- `KAFKA_URL` — internal address via private networking (e.g., `kafka.railway.internal:9092`)
- `KAFKA_PUBLIC_URL` — external address via TCP proxy

> **Note:** Kafka runs on Railway's private networking. Your endorsement service communicates with Kafka internally — no public exposure needed.

#### Step 5: Deploy the Endorsement Service

**Option A: Deploy from GitHub (Recommended)**

1. On the dashboard, click **+ New** > **GitHub Repo**
2. Select your `plum-endorsements` repository
3. Railway detects the `Dockerfile` and `railway.toml` automatically

**Option B: Deploy via CLI**

```bash
cd plum-endorsements
railway up
```

#### Step 6: Configure Service Variables

In the Railway dashboard, select the endorsement service and go to the **Variables** tab. Add these variables using Railway reference variables to link to the provisioned services:

| Variable | Value | Description |
|----------|-------|-------------|
| `SPRING_PROFILES_ACTIVE` | `railway` | Activates the `application-railway.yml` profile |
| `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` | Reference to PostgreSQL service |
| `REDIS_URL` | `${{Redis.REDIS_URL}}` | Reference to Redis service |
| `KAFKA_URL` | `${{Kafka.KAFKA_URL}}` | Reference to Kafka service (private network) |
| `PORT` | _(auto-injected by Railway)_ | Railway injects this automatically |

> **Reference variables** (`${{ServiceName.VARIABLE}}`) dynamically resolve to the actual values from the linked service. This ensures credentials rotate correctly and private networking addresses are used.

**To set variables via CLI:**

```bash
railway variables set SPRING_PROFILES_ACTIVE=railway
railway variables set DATABASE_URL='${{Postgres.DATABASE_URL}}'
railway variables set REDIS_URL='${{Redis.REDIS_URL}}'
railway variables set KAFKA_URL='${{Kafka.KAFKA_URL}}'
```

#### Step 7: Generate a Public Domain

1. In the Railway dashboard, select the endorsement service
2. Go to **Settings** > **Networking**
3. Click **Generate Domain**

Railway assigns a public URL like `endorsement-service-production.up.railway.app`.

#### Step 8: Verify Deployment

```bash
# Replace with your Railway-assigned domain
export RAILWAY_URL="https://endorsement-service-production.up.railway.app"

# Health check
curl -s $RAILWAY_URL/actuator/health | python3 -m json.tool

# Swagger UI
open $RAILWAY_URL/swagger-ui

# Create a test endorsement
curl -s -X POST $RAILWAY_URL/api/v1/endorsements \
  -H "Content-Type: application/json" \
  -d '{
    "employerId": "11111111-1111-1111-1111-111111111111",
    "employeeId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "insurerId": "22222222-2222-2222-2222-222222222222",
    "policyId": "33333333-3333-3333-3333-333333333333",
    "type": "ADD",
    "coverageStartDate": "2026-04-01",
    "employeeData": {"name": "Ravi Kumar", "dob": "1990-05-15"},
    "premiumAmount": 1200.00
  }' | python3 -m json.tool
```

### 12.4 Seed Data on Railway

To seed an EA account on Railway's PostgreSQL, use the Railway CLI to open a database shell:

```bash
# Connect to the Railway PostgreSQL instance
railway connect postgres

# Then run:
INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  '22222222-2222-2222-2222-222222222222',
  50000.00, 0.00, now()
);
```

Or use `railway run` to execute a one-off command:

```bash
railway run psql $DATABASE_URL -c "
INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111','22222222-2222-2222-2222-222222222222', 50000.00, 0.00, now());
"
```

### 12.5 Railway Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│                   Railway Project                       │
│                                                         │
│  ┌─────────────────────┐    Private Networking          │
│  │  Endorsement Service │◄─────────────────────┐        │
│  │  (Dockerfile)        │                      │        │
│  │                      │──┐                   │        │
│  │  PORT (auto)         │  │                   │        │
│  │  SPRING_PROFILES_    │  │                   │        │
│  │    ACTIVE=railway    │  │                   │        │
│  └──────────┬───────────┘  │                   │        │
│             │              │                   │        │
│     ┌───────▼───────┐  ┌──▼─────────┐  ┌─────▼──────┐  │
│     │  PostgreSQL   │  │   Redis    │  │   Kafka    │  │
│     │  (DATABASE_   │  │  (REDIS_   │  │  (KAFKA_   │  │
│     │    URL)       │  │    URL)    │  │    URL)    │  │
│     └───────────────┘  └────────────┘  └──────┬─────┘  │
│                                               │        │
│                                        ┌──────▼─────┐  │
│                                        │  Kafka UI  │  │
│                                        │  (public)  │  │
│                                        └────────────┘  │
│                                                         │
└─────────────────────────────────────────────────────────┘
         │
         ▼ Public Domain
   https://endorsement-service-production.up.railway.app
```

### 12.6 Railway Deployment Checklist

- [ ] Code pushed to GitHub
- [ ] Railway project created
- [ ] PostgreSQL provisioned
- [ ] Redis provisioned
- [ ] Kafka provisioned (via template)
- [ ] Service variables configured (`SPRING_PROFILES_ACTIVE`, `DATABASE_URL`, `REDIS_URL`, `KAFKA_URL`)
- [ ] GitHub repo connected to Railway service
- [ ] Public domain generated
- [ ] Health check passing (`/actuator/health` returns 200)
- [ ] Swagger UI accessible (`/swagger-ui`)
- [ ] EA account seeded for testing
- [ ] Test endorsement created successfully

### 12.7 Railway Costs (Hobby Plan Reference)

| Resource | Estimated Monthly Cost |
|----------|----------------------|
| Endorsement Service | ~$5 (512 MB RAM) |
| PostgreSQL | ~$5 (included plugin) |
| Redis | ~$5 (included plugin) |
| Kafka (Docker) | ~$7 (1 GB RAM) |
| **Total (estimated)** | **~$22/month** |

> Railway Hobby plan includes $5/month free credit. Pro plan ($20/month) includes higher resource limits and team features.

### 12.8 Railway Troubleshooting

**Build fails with "out of memory":**
Upgrade to Railway Pro plan for higher build limits, or add to `railway.toml`:
```toml
[build]
builder = "DOCKERFILE"
```
Multi-stage builds reduce final image size — the current Dockerfile already uses this pattern.

**Health check fails (deploy rolls back):**
- Check logs: Railway dashboard > Service > **Deployments** > click on the failed deploy > **View Logs**
- Ensure `DATABASE_URL` and `REDIS_URL` variables are correctly set
- Increase timeout in `railway.toml` if the app needs more startup time:
  ```toml
  [deploy]
  healthcheckTimeout = 600
  ```

**Cannot connect to Kafka:**
- Kafka must be in the same Railway project for private networking to work
- Verify `KAFKA_URL` resolves to `kafka.railway.internal:9092` (private domain)
- Check Kafka service logs in the Railway dashboard

**Flyway migration conflict after redeployment:**
- Railway PostgreSQL persists data across deploys (volume-backed)
- If schema is out of sync, connect via `railway connect postgres` and inspect `flyway_schema_history`

**Application starts but API returns 502:**
- Railway may not have finished DNS propagation. Wait 30–60 seconds after domain generation
- Ensure the app listens on `${PORT}` (not hardcoded 8080). The `application-railway.yml` profile handles this

---

## 13. Configuration Reference

Key properties in `application.yml` that can be overridden via environment variables:

| Property | Env Variable | Default | Description |
|----------|-------------|---------|-------------|
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/endorsements` | Database URL |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | `plum` | DB user |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | `plum_dev` | DB password |
| `spring.data.redis.host` | `SPRING_DATA_REDIS_HOST` | `localhost` | Redis host |
| `spring.data.redis.port` | `SPRING_DATA_REDIS_PORT` | `6379` | Redis port |
| `spring.kafka.bootstrap-servers` | `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `server.port` | `SERVER_PORT` | `8080` | HTTP port |
| `endorsement.batch.schedule-cron` | `ENDORSEMENT_BATCH_SCHEDULE_CRON` | `0 */15 * * * *` | Batch assembly schedule (every 15 min) |
| `endorsement.retry.max-attempts` | `ENDORSEMENT_RETRY_MAX_ATTEMPTS` | `3` | Max retry attempts per endorsement |
| `endorsement.provisional-coverage.max-days` | `ENDORSEMENT_PROVISIONAL_COVERAGE_MAX_DAYS` | `30` | Days before stale coverage is expired |

---

## 14. Troubleshooting

### Service won't start: "Connection refused" to PostgreSQL

```bash
# Check if postgres container is running and healthy
docker compose ps postgres
docker compose logs postgres

# Restart if needed
docker compose restart postgres
```

### Flyway migration fails

```bash
# Check migration status
docker compose exec postgres psql -U plum -d endorsements \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank;"

# To reset (CAUTION: destroys all data)
docker compose down -v
docker compose up -d
```

### Kafka connection errors at startup

Kafka can take 10–15 seconds to initialize in KRaft mode. The application retries automatically. If persistent:

```bash
docker compose logs kafka
docker compose restart kafka
```

### Port already in use

```bash
# Find and kill process on port 8080
lsof -i :8080
kill -9 <PID>
```

### Out of memory / slow startup

Ensure Docker has at least **4 GB** RAM allocated (Docker Desktop > Settings > Resources).

### Rebuild from scratch

```bash
# Stop everything, remove volumes, rebuild
docker compose down -v
./gradlew clean bootJar --no-daemon
docker compose up -d
./gradlew bootRun --no-daemon
```

---

## Endorsement State Machine (Quick Reference)

```
CREATED
  └─> VALIDATED
        └─> PROVISIONALLY_COVERED
              ├─> SUBMITTED_REALTIME ─┐
              └─> QUEUED_FOR_BATCH    │
                    └─> BATCH_SUBMITTED
                          └─> INSURER_PROCESSING
                                ├─> CONFIRMED ✓ (terminal)
                                └─> REJECTED
                                      ├─> RETRY_PENDING
                                      │     ├─> SUBMITTED_REALTIME (loop)
                                      │     ├─> QUEUED_FOR_BATCH (loop)
                                      │     └─> FAILED_PERMANENT ✗ (terminal)
                                      └─> FAILED_PERMANENT ✗ (terminal)
```
