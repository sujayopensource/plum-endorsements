# Plum Endorsement Service — 15-20 Minute Demo Script

> **Purpose**: Walk through the complete system — features, architecture, design patterns, tech choices, and observability — against the 6 design challenge requirements.
>
> **Pre-requisites**: `docker-compose up -d` (all 9 services running), `cd frontend && npm run dev` (UI at :5173), backend at :8080.

---

## Table of Contents

- [Demo Structure & Timing](#demo-structure--timing)
- [Section 1: Problem Statement & Solution Overview](#section-1-problem-statement--solution-overview-130)
- [Section 2: Architecture Deep-Dive](#section-2-architecture-deep-dive-300)
- [Section 3: Live — Endorsement Lifecycle](#section-3-live--endorsement-lifecycle-300)
- [Section 4: EA Balance Optimization](#section-4-ea-balance-optimization-200)
- [Section 5: Live — Real-Time Visibility Screens](#section-5-live--real-time-visibility-screens-230)
- [Section 6: Live — Intelligence & AI/Automation](#section-6-live--intelligence--aiautomation-300)
- [Section 7: Observability Stack](#section-7-observability-stack-200)
- [Section 8: Scalability & Resilience](#section-8-scalability--resilience-130)
- [Section 9: Test Coverage & Quality](#section-9-test-coverage--quality-030)
- [Closing: Mapping to Deliverables](#closing-mapping-to-deliverables-30-seconds)
- [Appendix A: Backup Demo Commands](#appendix-a-backup-demo-commands-if-ui-isnt-available)
- [Appendix B: Quick Reference — Port Mappings](#appendix-b-quick-reference--port-mappings)
- [Appendix C: Design Challenge Requirements Traceability](#appendix-c-design-challenge-requirements-traceability)

---

## Demo Structure & Timing

| # | Section | Time | Deliverable Addressed |
|---|---------|------|----------------------|
| 1 | Problem Statement & Solution Overview | 1:30 | — |
| 2 | Architecture Deep-Dive | 3:00 | High-level architecture |
| 3 | Live: Endorsement Lifecycle (RT + Batch) | 3:00 | Real-time/batch execution, No coverage gap |
| 4 | EA Balance Optimization | 2:00 | EA balance minimization algorithm |
| 5 | Live: Real-Time Visibility Screens | 2:30 | User flows & dashboards |
| 6 | Live: Intelligence & AI/Automation | 3:00 | AI/automation |
| 7 | Observability Stack | 2:00 | — |
| 8 | Scalability & Resilience | 1:30 | Scalability architecture |
| 9 | Test Coverage & Quality | 0:30 | Code/prototype |
| | **Total** | **~19 min** | |

---

## Section 1: Problem Statement & Solution Overview (1:30)

### Talking Points

> *"The problem: when an employer adds a new employee mid-year, they need an endorsement — a change to the group insurance policy. This sounds simple, but at scale it creates four hard problems."*

**The Four Hard Problems** (show on screen or whiteboard):

```
Problem 1: COVERAGE GAP
  Employee joins → endorsement created → insurer confirms (days later)
  Employee has NO coverage during processing. Unacceptable.

Problem 2: FINANCIAL DRAIN
  Each ADD endorsement locks premium in EA account.
  Without optimization, employers need to maintain massive float.

Problem 3: MULTI-INSURER CHAOS
  4+ insurers, each with different APIs (REST, SOAP, CSV/SFTP),
  different SLAs (hours vs days), different batch constraints.

Problem 4: INVISIBLE FAILURES
  Submissions fail silently. Batches get stuck.
  Nobody knows until month-end reconciliation surfaces discrepancies.
```

> *"Our solution addresses all four. Let me show you the architecture, then we'll see it live."*

**Quick System Stats** (flash on screen):

| Dimension | Value |
|-----------|-------|
| Endpoints | 27 REST APIs |
| Insurer Integrations | 4 (ICICI Lombard, Niva Bupa, Bajaj Allianz + Mock) |
| Database Tables | 13 (Flyway-managed) |
| Kafka Topics | 4 (88 partitions) |
| Intelligence Pillars | 5 AI/automation modules (2 with Ollama/GenAI augmentation) |
| Grafana Dashboards | 7 |
| Custom Metrics | 40+ |
| Tests | 800+ (all passing) |
| Scale Target | 100K employers, 1M endorsements/day |

---

## Section 2: Architecture Deep-Dive (3:00)

### Show: Hexagonal Architecture Diagram

> *"We use Hexagonal Architecture — also called Ports and Adapters. The domain core has zero infrastructure imports. This isn't academic — it's how we support 4 different insurer protocols without touching business logic."*

```
                    ┌────────────────────────────────────────────────────┐
                    │                   API Layer                        │
                    │  5 Controllers (27 endpoints)                      │
                    │  RFC 7807 ProblemDetail error handling             │
                    └──────────────────┬─────────────────────────────────┘
                                       │
                    ┌──────────────────▼─────────────────────────────────┐
                    │              Application Layer                     │
                    │  3 Handlers (CQRS) · 5 Services · 8 Schedulers    │
                    │  Stateless · @Transactional · MDC · Metrics        │
                    └──────────────────┬─────────────────────────────────┘
                                       │
         ┌─────────────────────────────▼──────────────────────────────────┐
         │                      DOMAIN CORE                               │
         │  Models: Endorsement (11-state), EAAccount, Batch              │
         │  Ports:  18 interfaces (InsurerPort, EndorsementRepository...) │
         │  Events: EndorsementEvent sealed interface (24 event types)    │
         │  Services: StateMachine, BalanceCalculator, InsurerRegistry    │
         │                                                                │
         │         >>> ZERO infrastructure imports <<<                    │
         └─────────────────────────────┬──────────────────────────────────┘
                                       │
    ┌──────────────────────────────────▼───────────────────────────────────┐
    │                       Infrastructure Layer                           │
    │                                                                      │
    │  ┌─ JPA Adapters ──────────┐  ┌─ Insurer Adapters ────────────────┐ │
    │  │ 10 adapters + mappers   │  │ Mock (JSON, RT+Batch)             │ │
    │  │ Anti-corruption layer   │  │ ICICI Lombard (JSON, RT only)     │ │
    │  └─────────────────────────┘  │ Niva Bupa (CSV, Batch only)       │ │
    │                                │ Bajaj Allianz (XML/SOAP, RT+Batch)│ │
    │  ┌─ Messaging ─────────────┐  │ InsurerRouter (Strategy + Factory)│ │
    │  │ KafkaEventPublisher     │  └───────────────────────────────────┘ │
    │  │ 4 topics · 88 partitions│                                        │
    │  └─────────────────────────┘  ┌─ Intelligence ─────────────────────┐│
    │                                │ RuleBasedAnomalyDetector           ││
    │  ┌─ Resilience ────────────┐  │ StatisticalForecastEngine          ││
    │  │ Circuit Breakers (per   │  │ SimulatedErrorResolver             ││
    │  │   insurer, tuned)       │  │ EventStreamAnalyzer                ││
    │  │ Retries + Exp. Backoff  │  │ ConstraintBatchOptimizer           ││
    │  └─────────────────────────┘  │ + Ollama/GenAI Adapters:           ││
    │                                │   OllamaAugmentedAnomalyDetector  ││
    │                                │   OllamaErrorResolver             ││
    │                                └────────────────────────────────────┘│
    └─────────────────────────────────────────────────────────────────────┘
```

### Design Pattern Callouts (30 seconds)

> *"This isn't just layered architecture. Every layer uses specific design patterns from the GOF book."*

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `InsurerPort` — 4 adapters (JSON, CSV, SOAP) | Add insurer = add class + DB row. Zero handler changes. |
| **State** | `EndorsementStatus` enum — 11 states with `canTransitionTo()` | Compile-time safety. Invalid transitions are impossible. |
| **Observer** | `EventPublisher` → Kafka → consumers | Handlers publish events, don't know who listens. |
| **Factory** | `InsurerRouter.resolve(insurerId)` | Runtime adapter selection from DB config. No if/else. |
| **Adapter** | All JPA/Kafka/insurer infrastructure classes | Domain never imports `jakarta.persistence.*` or `org.apache.kafka.*`. |
| **CQRS** | Command handlers vs `EndorsementQueryHandler` | Write-optimized commands, read-optimized queries with `readOnly=true`. |

### Technology Choices (30 seconds)

> *"Every tech choice has a reason."*

| Choice | Rationale |
|--------|-----------|
| **Java 21 + Virtual Threads** | 1M+ threads without thread-pool tuning. Each endorsement gets its own virtual thread. |
| **Spring Boot 3.4** | Mature ecosystem. Native virtual thread support. Actuator for observability. |
| **PostgreSQL 16** | ACID for financial operations (EA balance). Optimistic locking via `version` column. |
| **Kafka (KRaft, no ZooKeeper)** | 32 partitions on `endorsement-events` topic. `employerId` as partition key = per-employer ordering = no EA race conditions. |
| **Redis 7 + Caffeine** | Redis for distributed cache. Caffeine (60s TTL) for hot config data (insurer configs). |
| **Resilience4j** | Per-insurer circuit breakers. Bajaj SOAP is flakier → 40% threshold, 45s wait. ICICI REST → 50%, 30s. |
| **Flyway** | 15 versioned migrations. Schema changes are code-reviewed and reversible. |
| **ZGC** | Low-latency garbage collector. Sub-millisecond pause times at 75% heap utilization. |

---

## Section 3: Live — Endorsement Lifecycle (3:00)

### Demo: Create Endorsement (Real-Time Path)

> *"Let me create an endorsement and show you what happens step by step."*

**Step 1: Open the UI** → `http://localhost:5173`

> *"Dashboard shows our command center — KPI cards, EA balance, recent endorsements."*

Point out: Total, Pending, Confirmed, Failed KPI cards. EA Account card showing Balance / Reserved / Available.

**Step 2: Navigate to Create Endorsement** → Click "Endorsements" in sidebar → "Create Endorsement"

> *"Notice the form adapts based on type. Let me select DELETE —"*

Select DELETE type → Premium and Employee Data sections disappear.

> *"Progressive disclosure. Only show what's relevant. Back to ADD."*

Switch back to ADD.

**Step 3: Fill the form**

- Employee ID: paste a UUID
- Employee Name: `Priya Sharma`
- Coverage Start: tomorrow's date
- Premium: `1500`

> *"Note the pre-filled Employer ID and Insurer ID. Also, the EA balance is checked in real-time — if it's below 10K, you get a warning banner."*

Click **"Create Endorsement"**.

**Step 4: Endorsement Detail page loads**

> *"Three things happened atomically in one database transaction:"*

Point at the Status Timeline:
1. **Created** — Record saved, idempotency key checked (dedup)
2. **Validated** — Business rules: sufficient EA balance, valid insurer config
3. **Provisionally Covered** — *"This is the key innovation. The employee is covered RIGHT NOW. Not when the insurer confirms days later. Right now."*

Point at the Coverage Card:
> *"Type: PROVISIONAL. Active: Yes. Priya Sharma has medical coverage as of this moment."*

**Step 5: Submit to insurer**

Click **"Submit to Insurer"** → Confirm dialog.

> *"This uses the Mock adapter (JSON/REST). In production, the `InsurerRouter` would resolve to ICICI Lombard's adapter, Bajaj's SOAP adapter, or Niva Bupa's CSV batch adapter — based on the insurer configuration in the database. The handler never knows which one it's talking to. Strategy pattern."*

Reload the page.

> *"Status is now CONFIRMED. Coverage card updated from PROVISIONAL to CONFIRMED. The insurer reference number is populated. That's the complete real-time path."*

### Show: Batch Path (talk through, don't re-create)

> *"For batch-only insurers like Niva Bupa (CSV/SFTP), the endorsement would go:
> Provisionally Covered → Queued for Batch → Batch Submitted (every 15 min) → Insurer Processing → Confirmed.
> Coverage is active the entire time. The employee never has a gap."*

### Show: Rejection & Retry

> *"If the insurer rejects — say, a data format error — the status becomes REJECTED. The timeline shows the rejection reason and retry count. Up to 3 retries with 5-second exponential backoff. After 3 failures → FAILED_PERMANENT. But even then, provisional coverage stays active for 30 days as a safety net."*

---

## Section 4: EA Balance Optimization (2:00)

### Talking Points

> *"Requirement 3: minimize EA balance. Without optimization, an employer processing 60 ADDs at Rs.1,000 needs Rs.60,000 in their EA account. But they also have 40 DELETEs at Rs.800 generating Rs.32,000 in credits. The question is: what order do you process them?"*

**Show the math** (narrate):

```
NAIVE approach (random order):
  Peak requirement = Rs.60,000 (worst case: all ADDs first)

OPTIMIZED approach (DELETEs first):
  Step 1: Process 40 DELETEs → +Rs.32,000 credited
  Step 2: Process 60 ADDs    → -Rs.60,000
  Net requirement = Rs.60,000 - Rs.32,000 = Rs.28,000
  Savings: 53% less working capital
```

### Priority Ordering

> *"Our batch optimizer uses a priority system."*

| Priority | Type | Rationale |
|----------|------|-----------|
| P0 | DELETE | Frees balance. Always process first. |
| P1 | UPDATE (no cost) | Cost-neutral changes. |
| P2 | ADD | Consumes balance. Sorted by coverage start date (urgent first). |
| P3 | UPDATE (cost change) | Lowest priority — premium adjustments. |

### Composite Scoring (for advanced audience)

> *"Within each priority tier, we use a composite score."*

```
compositeScore = (urgencyScore × 0.60) + (eaImpactScore × 0.40)

urgencyScore = (priorityRank + timePressure) / 2
  timePressure = max(0, 1.0 - (daysUntilCoverage / 30))
  → Employee starting tomorrow = 0.97 urgency
  → Employee starting in 25 days = 0.17 urgency

eaImpactScore:
  DELETE = 1.0 (always maximum — frees money)
  ADD = max(0, 1.0 - (premium / availableBalance))
  → Rs.500 premium with Rs.100K balance = 0.995 (cheap, do it)
  → Rs.50K premium with Rs.60K balance = 0.167 (expensive, defer)
```

> *"This is implemented as a 0-1 knapsack with DP optimization — the `ConstraintBatchOptimizer` class. Every 15 minutes, the `BatchAssemblyScheduler` assembles the optimal batch per insurer."*

---

## Section 5: Live — Real-Time Visibility Screens (2:30)

### Dashboard Tour

> *"Let me quickly walk through what each screen gives you."*

**Dashboard** (`/`) — Already shown. Call out:
- KPI cards are **clickable** — Total → all endorsements, Failed → filtered to failures
- Active Batches card → links to batch progress
- Refresh button shows "Dashboard updated 5s ago" — live timestamp

**Endorsements List** (`/endorsements`) — Navigate here:
- **Sortable columns** — Click "Premium" header → sorts ascending/descending. TanStack Table under the hood.
- **Status filter** — Click filter icon → check "REJECTED" → instant filter. Show filter count badge.
- **URL persistence** — *"Notice the URL now has `?statuses=REJECTED&page=0`. This is bookmarkable. Share it with a colleague."*
- **Bulk actions** — Check 2-3 rows → fixed bar appears at bottom: "3 selected · Submit Selected"
- **CSV Export** — Click Export CSV → file downloads instantly
- **Pagination** — Show page size selector (10/25/50/100), "Showing 1-10 of 47"

**Endorsement Detail** (`/endorsements/:id`) — Click into one:
- **Status Timeline** — horizontal visual journey. Two paths rendered (RT vs batch).
- **Coverage Card** — PROVISIONAL badge. Live status.
- **Action Buttons** — context-aware: Submit / Confirm / Reject based on current state
- **Copy ID** — click copy button on endorsement ID

**Batch Progress** (`/endorsements/batches`):
- Table showing batches with Status badge (SUBMITTED / COMPLETED / FAILED)
- Endorsement count per batch, insurer ref

**EA Accounts** (`/ea-accounts`):
- Enter employer + insurer IDs → Look Up
- Three KPI cards: Total / Reserved / Available
- Progress bar visualization

**Insurers** (`/insurers`):
- All 4 configured insurers in a table
- Capabilities: RT badge, Batch badge, rate limits
- Click into one → Insurer Detail with config + reconciliation runs

**Reconciliation** (`/reconciliation`):
- Select insurer → Summary cards (Matched/Partial/Rejected/Missing)
- Expandable rows — click a run to see individual reconciliation items
- Trigger manual reconciliation + export CSV

---

## Section 6: Live — Intelligence & AI/Automation (3:00)

> *"Requirement 5: AI and automation. We have 5 intelligence pillars, each implemented as a pluggable adapter behind a domain port. The base layer is rule-based. Two pillars already have Ollama/GenAI-augmented adapters deployed — anomaly detection and error resolution. The remaining three have planned Ollama adapters. And the full ML vision is architecturally ready. The key: swap the adapter, domain stays untouched."*

### Navigate to Intelligence Dashboard (`/intelligence`)

### Tab 1: Anomaly Detection

> *"Five detection rules run every 5 minutes."*

| Rule | Trigger | Score |
|------|---------|-------|
| VOLUME_SPIKE | 24h count > 30-day avg × 5 | ~0.95 |
| ADD_DELETE_CYCLING | Same employee ADD+DELETE within 30 days | 0.85 |
| SUSPICIOUS_TIMING | Coverage starts within 7 days of creation | 0.75 |
| UNUSUAL_PREMIUM | Premium > mean ± 3 standard deviations | 0.70 |
| DORMANCY_BREAK | Employee with no activity for 90+ days | up to 0.85 |

Show the anomalies table. Point out score badges (red ≥ 90%, amber ≥ 70%).

> *"Click Review → enter notes → status changes to UNDER_REVIEW. Click Dismiss → false positive. This feeds back into the system — future ML models will learn from dismissed vs confirmed-fraud labels."*

**Ollama/GenAI augmentation (already deployed)**: *"When the `ollama` Spring profile is active, the `OllamaAugmentedAnomalyDetector` takes over. It runs the same 5 rules, then sends the flagged anomaly to a local LLM (llama3.2) for enrichment — generating a natural-language explanation of WHY the anomaly was flagged and what action to take. If Ollama is unavailable, the circuit breaker falls back to rule-based output. Zero domain changes — same `AnomalyDetectionPort` interface."*

**Full ML upgrade path**: *"Beyond Ollama, we can deploy an `IsolationForestAnomalyDetector` as `@Primary`, run both in shadow mode, compare results, then canary rollout. The port interface is the same — swap adapters, domain stays untouched."*

### Tab 2: Forecasts

Enter Employer ID + Insurer ID → Click **"Generate Forecast"**.

> *"This projects EA balance 30 days ahead using dual seasonality — day-of-week factors (Monday 1.2x, Sunday 0.2x — reflecting Indian business patterns) and monthly factors (April 1.4x for fiscal year start, October 1.3x for appraisal cycles)."*

Point out: Forecasted Amount, Narrative text explaining methodology.

> *"If the forecast shows a shortfall, the system automatically notifies the employer. Finance teams can proactively top up before the account runs dry."*

### Tab 3: Error Resolution

> *"When an insurer rejects an endorsement — say, wrong date format — the error resolver kicks in."*

Show the stats cards: Total Resolutions, Auto-Applied, Suggested, Auto-Apply Rate, Success Count, Failure Count, Success Rate.

> *"Five error patterns, each with a confidence score. Confidence ≥ 95% → auto-applied, no human needed. Below that → suggested for approval. And now we track outcomes — when an auto-applied fix leads to a confirmed endorsement, that's a SUCCESS. If it gets rejected again, that's a FAILURE. The success rate is shown in the stats. If it drops below 80%, the system can auto-disable auto-apply for that error type."*

| Pattern | Example | Confidence | Action |
|---------|---------|-----------|--------|
| DATE_FORMAT | `07/03/1990` → `1990-03-07` | 0.98 | Auto-apply |
| MEMBER_ID | `emp123` → `PLM-emp12345` | 0.96 | Auto-apply |
| MISSING_FIELD | Empty email → default@employer.com | 0.90 | Suggest |
| PREMIUM_MISMATCH | Rs.1,200 → Rs.1,260 (5% adj) | 0.85 | Suggest |
| UNKNOWN | — | 0.30 | Manual review |

Point out the Approve button on suggested (non-auto-applied) rows.

**Ollama/GenAI augmentation (already deployed)**: *"With the `ollama` profile active, the `OllamaErrorResolver` enhances error resolution. For patterns below 95% confidence — the ones that currently require human review — the LLM analyzes the insurer rejection message and endorsement data to suggest a specific fix with reasoning. High-confidence rule-based fixes still auto-apply instantly; the LLM handles the ambiguous edge cases. Circuit breaker falls back to rule-based if Ollama is down."*

### Tab 4: Process Mining

> *"This analyzes how efficiently endorsements flow through the system."*

Show:
- **Overall STP Rate** — e.g., 87.3%. *"87% of endorsements go straight through without human touch."*
- **Per-Insurer STP Cards** — *"ICICI Lombard at 93%, Bajaj at 82%. Tells us where to focus optimization."*
- **STP Rate Trend** — *"And now we have daily snapshots — we can see the trend over 30 days. Is STP improving or degrading? This is a new feature: `GET /api/v1/intelligence/process-mining/stp-rate/trend` returns daily data points for trend visualization."*
- **Transition Metrics Table** — *"Every state transition measured. Avg duration, P95, P99. The highlighted row is the bottleneck — the longest transition."*

Click **"Run Analysis"** to trigger fresh bottleneck detection.

> *"A bottleneck is flagged when P95 exceeds 2x the average OR the average exceeds 4 hours, with a minimum of 5 samples. This prevents false positives from outliers."*

**Ollama/GenAI upgrade path (planned)**: *"Today this is `EventStreamAnalyzer`. The next step is an `OllamaAugmentedProcessMiner` that sends bottleneck data to the LLM for natural-language root-cause analysis and recommended actions — same pattern as the deployed anomaly and error adapters. Beyond that, the full ML version would use PM4Py for conformance checking, variant analysis, and social network mining across the process graph."*

### Bonus: Live Toggle — Rule-Based vs Ollama/GenAI (if time permits)

> *"Let me show you the adapter switching live. Right now we're running the default profile — rule-based adapters. Watch what happens when I activate Ollama."*

**Step 1**: Show current anomaly response (rule-based):
```bash
curl -s "http://localhost:8080/api/v1/intelligence/anomalies?status=FLAGGED" | python3 -m json.tool
```
> *"Notice: scores and types — pure rule-based output, no natural language explanation."*

**Step 2**: Restart with Ollama profile:
```bash
SPRING_PROFILES_ACTIVE=ollama docker-compose up -d backend
```
> *"One environment variable. The `@ConditionalOnProperty` annotation activates `OllamaAugmentedAnomalyDetector` and `OllamaErrorResolver` instead of their rule-based counterparts. Same port interface, different adapter."*

**Step 3**: Show enriched anomaly response:
```bash
curl -s "http://localhost:8080/api/v1/intelligence/anomalies?status=FLAGGED" | python3 -m json.tool
```
> *"Same API, same JSON structure — but now the anomaly includes an LLM-generated explanation and recommended action. The frontend doesn't change. The domain doesn't change. Only the infrastructure adapter changed."*

> *"This is the hexagonal architecture payoff: rule-based → Ollama/GenAI → full ML. Each stage is an adapter swap behind the same port. The pattern is established with 2 deployed adapters, 3 more planned, and the full ML vision documented."*

---

## Section 7: Observability Stack (2:00)

### Show: Docker Compose Services

> *"9 services, all with health checks, all running."*

```
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

| Service | Port | Purpose |
|---------|------|---------|
| PostgreSQL | 5432 | Primary database (13 tables) |
| Redis | 6379 | Distributed cache |
| Kafka (KRaft) | 9092 | 4 topics, 88 partitions, no ZooKeeper |
| Prometheus | 9090 | Metrics scraping (15s interval) |
| Grafana | 3000 | 7 pre-provisioned dashboards |
| Jaeger | 16686 | Distributed tracing (100% sampling) |
| Elasticsearch | 9200 | Log aggregation |
| Logstash | 5000 | Log pipeline (TCP → ES) |
| Kibana | 5601 | Log visualization |

### Show: Grafana Dashboards (open `http://localhost:3000`)

Quick fly-through of 3 key dashboards:

**Dashboard 1: Application Overview** — *"Request rate, P95 latency, error rate, JVM heap, active threads, DB connection pool. Standard SRE dashboard."*

**Dashboard 2: Endorsement Business** — *"This is the business dashboard. Creation rate by type (ADD/DELETE/UPDATE), active endorsements by status (pie chart), state transition rate, insurer submission latency. Notice per-insurer latency — Bajaj at 250ms vs ICICI at 150ms."*

**Dashboard 4: Intelligence Monitoring** — *"Anomaly detection rate, forecast shortfall detections, error auto-resolution rate (gauge), STP rate trend, batch optimization savings (cumulative INR). This is the AI ops dashboard."*

### Show: Custom Metrics (narrate, don't deep-dive)

> *"40+ custom Micrometer metrics. Every endorsement creation, state transition, insurer submission, EA reservation, Kafka publish, scheduler execution, anomaly detection, forecast generation — all instrumented. Tagged by type, insurer, status, result. These power all 7 Grafana dashboards."*

### Show: Distributed Tracing (open Jaeger at `http://localhost:16686`)

> *"100% sampling in development. Every request gets a traceId and spanId propagated through MDC into structured JSON logs. Baggage fields: endorsementId, employerId — so you can search Jaeger by endorsement and see the full trace from controller → handler → repository → Kafka → insurer adapter."*

### Show: Structured Logging (mention, skip deep-dive)

> *"JSON structured logging in production via Logstash encoder. App → Logstash (TCP:5000) → Elasticsearch → Kibana. Each log line carries traceId, spanId, requestId, endorsementId, employerId. You can correlate a single endorsement's journey across all components."*

---

## Section 8: Scalability & Resilience (1:30)

### Scalability

> *"Requirement 6: 100K employers, 10 changes per employer per day = 1M endorsements/day."*

| Design Decision | How It Enables Scale |
|----------------|---------------------|
| **Stateless services** | Every `@Service` has only `private final` fields. Any instance handles any request. Scale horizontally by adding pods. |
| **Java 21 Virtual Threads** | 1M+ virtual threads. No thread-pool bottleneck. Each endorsement gets its own lightweight thread. |
| **Kafka partitioning** | 32 partitions on events topic. `employerId` as key → per-employer ordering → no EA balance race conditions even at 1M/day. |
| **Optimistic locking** | `version` column on `endorsements` and `ea_accounts`. Concurrent updates detected, not blocked. |
| **Idempotency keys** | `UNIQUE` constraint on `idempotency_key`. Safe retries — creating the same endorsement twice returns the existing one. |
| **K8s HPA** | Min 2, Max 8 pods. Scale up at 70% CPU. Scale up: +2 pods/60s. Scale down: -1 pod/120s. PDB ensures min 1 pod during disruptions. |

### Resilience

> *"Each insurer gets its own circuit breaker, tuned to its reliability profile."*

| Insurer | Failure Threshold | Window | Wait | Max Retries | Backoff |
|---------|------------------|--------|------|-------------|---------|
| Mock (default) | 50% | 10 calls | 30s | 3 | 2s × 2x |
| ICICI Lombard | 50% | 20 calls | 30s | 3 | 1s × 2x |
| Bajaj Allianz | 40% | 15 calls | 45s | 5 | 3s × 2x |

> *"Bajaj has a lower threshold (40%) and longer wait (45s) because SOAP services are historically more fragile. ICICI has a wider window (20 calls) because it handles more volume. Every breaker has a fallback method that returns a graceful degradation — never throws to the caller."*

> *"You can see circuit breaker state live in Grafana Dashboard 5 (Multi-Insurer Monitoring) or via the Actuator endpoint: `/actuator/circuitbreakers`."*

### Error Handling

> *"RFC 7807 ProblemDetail for every error response. 9 exception types mapped to specific HTTP status codes. The `GlobalExceptionHandler` catches all exceptions, increments the `endorsement.error` counter with a `type` tag, and returns structured JSON with a `detail` field that the frontend shows in toast notifications — no generic 'Something went wrong'."*

---

## Section 9: Test Coverage & Quality (0:30)

> *"We don't just demo features — we prove they work."*

| Suite | Framework | Count | Status |
|-------|-----------|-------|--------|
| Unit | JUnit 5 + Mockito + AssertJ | 381 | All passing |
| API Integration | REST Assured + Testcontainers (Postgres, Redis, Kafka) | 105 | All passing |
| BDD (Behaviour) | Cucumber + Gherkin (75 scenarios across 16 feature files) | 75 | All passing |
| E2E | Playwright (Chromium) — 14 spec files covering all 10 screens | 138 | All passing |
| Performance | Gatling (Baseline, Load, Soak, Spike, Stress simulations) | 6 | All passing |
| **Total** | | **800+** | **0 failures** |

> *"Combined Allure report at localhost:5050 — segregated into 4 sections: API Tests, BDD Tests, E2E Tests, Performance Tests. You can drill into any test to see assertions, request/response payloads, and execution timeline."*

If time permits, open `http://localhost:5050/allure-docker-service/latest-report` and show the Allure report briefly.

---

## Closing: Mapping to Deliverables (30 seconds)

> *"Let me map what we've seen to the 6 deliverables in the design challenge."*

| # | Deliverable | Where We Covered It |
|---|-------------|-------------------|
| 1 | High-level architecture (illustrated) | Section 2: Hexagonal architecture diagram, tech stack, design patterns |
| 2 | No loss of coverage | Section 3: Provisional coverage granted at creation, 5 edge cases identified and fixed, 30-day safety net |
| 3 | EA balance minimization | Section 4: Priority ordering (DELETE → ADD), composite scoring, 53% savings example, 0-1 knapsack DP |
| 4 | Real-time visibility dashboards | Section 5: 10 UI screens (Dashboard, List, Detail, Batches, EA, Insurers, Reconciliation, Intelligence) |
| 5 | AI/automation | Section 6: 5 intelligence pillars — anomaly detection, forecasting, error resolution, process mining, batch optimization. 2 Ollama/GenAI adapters deployed (anomaly + error resolution), 3 planned. |
| 6 | Code/prototype | Sections 3-6 (live demo), Section 9 (800+ tests, Allure report) |

> *"All the code is production-grade. Hexagonal architecture, design patterns, cloud-native resilience, comprehensive observability, Ollama/GenAI augmentation already deployed on 2 intelligence pillars, and 800+ tests. Questions?"*

---

## Appendix A: Backup Demo Commands (if UI isn't available)

### Create Endorsement via API

```bash
curl -s -X POST http://localhost:8080/api/v1/endorsements \
  -H "Content-Type: application/json" \
  -d '{
    "employerId": "11111111-1111-1111-1111-111111111111",
    "employeeId": "'$(uuidgen)'",
    "insurerId": "22222222-2222-2222-2222-222222222222",
    "policyId": "33333333-3333-3333-3333-333333333333",
    "type": "ADD",
    "premiumAmount": 1500,
    "coverageStartDate": "2026-04-01",
    "employeeData": {"name": "Priya Sharma", "dob": "1990-05-15", "gender": "F"}
  }' | python3 -m json.tool
```

### Check Coverage

```bash
curl -s http://localhost:8080/api/v1/endorsements/{id}/coverage | python3 -m json.tool
```

### Submit to Insurer

```bash
curl -s -X POST http://localhost:8080/api/v1/endorsements/{id}/submit | python3 -m json.tool
```

### Check EA Balance

```bash
curl -s "http://localhost:8080/api/v1/ea-accounts?employerId=11111111-1111-1111-1111-111111111111&insurerId=22222222-2222-2222-2222-222222222222" | python3 -m json.tool
```

### Check STP Rate

```bash
curl -s http://localhost:8080/api/v1/intelligence/process-mining/stp-rate | python3 -m json.tool
```

### Check Anomalies

```bash
curl -s "http://localhost:8080/api/v1/intelligence/anomalies?status=FLAGGED" | python3 -m json.tool
```

### Generate Forecast

```bash
curl -s -X POST "http://localhost:8080/api/v1/intelligence/forecasts/generate?employerId=11111111-1111-1111-1111-111111111111&insurerId=22222222-2222-2222-2222-222222222222" | python3 -m json.tool
```

### Toggle Ollama/GenAI Profile

```bash
# Start with Ollama augmentation enabled (requires Ollama running on :11434)
SPRING_PROFILES_ACTIVE=ollama ./gradlew bootRun

# Or via Docker Compose
SPRING_PROFILES_ACTIVE=ollama docker-compose up -d backend

# Verify Ollama adapters are active
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
# Look for: "ollamaAnomalyDetector": "UP", "ollamaErrorResolver": "UP"
```

### Check Health + Metrics

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
curl -s http://localhost:8080/actuator/prometheus | head -50
curl -s http://localhost:8080/actuator/circuitbreakers | python3 -m json.tool
```

---

## Appendix B: Quick Reference — Port Mappings

| Service | URL |
|---------|-----|
| Frontend (React) | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Actuator Health | http://localhost:8080/actuator/health |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 (admin/admin) |
| Jaeger UI | http://localhost:16686 |
| Kibana | http://localhost:5601 |
| Allure Report | http://localhost:5050/allure-docker-service/latest-report |

---

## Appendix C: Design Challenge Requirements Traceability

| Requirement | Implementation | Evidence |
|-------------|---------------|----------|
| **1. RT or Batch execution** | `InsurerPort` Strategy pattern. 4 adapters: Mock (RT+Batch), ICICI (RT), Niva Bupa (Batch), Bajaj (RT+Batch). `InsurerRouter` resolves at runtime from DB config. `BatchAssemblyScheduler` runs every 15 min. | `infrastructure/insurer/` — 4 adapter classes. `InsurerRouter.java`. `BatchAssemblyScheduler.java`. |
| **2. No coverage gap** | `ProvisionalCoverage` created atomically with endorsement. Stays active through all processing states. Upgraded on confirmation. 30-day safety net. 5 edge-case gaps identified and fixed. | `domain/model/ProvisionalCoverage.java`. `CreateEndorsementHandler.java` (lines 60-70). `ProvisionalCoverageCleanupScheduler.java`. |
| **3. Minimize EA balance** | Priority ordering: DELETE first (free balance), then ADD. `ConstraintBatchOptimizer` with composite scoring. 53% savings demonstrated. `StatisticalForecastEngine` for 30-day projections. | `domain/service/EABalanceCalculator.java`. `infrastructure/intelligence/ConstraintBatchOptimizer.java`. |
| **4. Real-time visibility** | 10 UI screens (React 19 + TanStack Table + shadcn/ui). WebSocket for live updates. 7 Grafana dashboards. 40+ custom metrics. URL-persisted filters. CSV export. Bulk actions. | `frontend/src/pages/` — 10 page components. `observability/grafana/dashboards/` — 7 JSON files. |
| **5. AI/Automation** | 5 intelligence pillars: Anomaly Detection (4 rules + Ollama enrichment), Balance Forecasting (dual seasonality), Error Resolution (5 patterns + Ollama augmentation, auto-apply at 95%), Process Mining (STP, bottlenecks), Batch Optimization (composite scoring). 2 Ollama/GenAI adapters deployed, 3 planned. All behind ports — GenAI/ML-upgradeable via `@ConditionalOnProperty`. | `infrastructure/intelligence/` — 5 rule-based + 2 Ollama adapter classes. `application/service/` — 5 service classes. `application-ollama.yml` — LLM profile config. |
| **6. Scalability** | Stateless services. Java 21 virtual threads. Kafka 32-partition per-employer ordering. Optimistic locking. K8s HPA (2-8 pods). PDB. ZGC. Per-insurer circuit breakers + retries. | `k8s/backend/` — deployment, HPA, PDB. `application.yml` — Resilience4j config. `Dockerfile` — JVM flags. |
