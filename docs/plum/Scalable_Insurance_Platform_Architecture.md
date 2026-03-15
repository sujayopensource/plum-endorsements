# Scalable Insurance Processing Platform Architecture

## Scaling from 500K to 10M Lives (20x Growth)

---

## Table of Contents

1. [Executive Summary & Scale Analysis](#1-executive-summary--scale-analysis)
2. [Claims Processing Pipeline](#2-claims-processing-pipeline)
3. [Real-Time Pricing Engine](#3-real-time-pricing-engine)
4. [Telehealth Platform](#4-telehealth-platform)
5. [Notification System](#5-notification-system)
6. [Document Processing](#6-document-processing)
7. [Cross-Cutting Concerns](#7-cross-cutting-concerns)
8. [Migration Strategy](#8-migration-strategy)

---

## 1. Executive Summary & Scale Analysis

### Current State (500K Lives)

| Metric | Current (500K) | Target (10M) | Growth Factor |
|--------|---------------|---------------|---------------|
| Claims/day | ~5,000 | ~100,000 | 20x |
| Pricing API calls/sec | ~50 | ~1,000 | 20x |
| Concurrent telehealth sessions | ~500 | ~10,000 | 20x |
| Notifications/day | ~50,000 | ~1,000,000 | 20x |
| Documents processed/day | ~10,000 | ~200,000 | 20x |
| Data storage growth/year | ~5 TB | ~100 TB | 20x |

### What Breaks at 20x

| System | Failure Mode | Root Cause |
|--------|-------------|------------|
| Claims Pipeline | Backpressure collapse, SLA misses | Synchronous processing, single DB bottleneck |
| Pricing Engine | Latency spikes > 2s, timeouts | In-memory model can't fit, cache thrashing |
| Telehealth | Session drops, media quality degradation | Single-region media servers, no admission control |
| Notifications | Delivery delays (hours), duplicate sends | Monolithic queue, no priority lanes |
| Document Processing | Queue backlog grows unbounded | Sequential OCR, no parallelism, CPU-bound |

### Core Architecture Principles for 20x Scale

1. **Event-Driven First** -- Decouple producers from consumers via async messaging
2. **Partition Everything** -- Data, compute, and queues partitioned by tenant/region/LOB
3. **Back-Pressure Aware** -- Every pipeline stage must signal capacity limits upstream
4. **Idempotent Operations** -- Every write operation must be safely retryable
5. **Observability as a Feature** -- Distributed tracing, metrics, and alerting are not afterthoughts

---

## 2. Claims Processing Pipeline

### 2.1 Current Pain Points at Scale

- Synchronous request-response for claim adjudication blocks threads
- Single relational DB becomes write bottleneck at ~20K claims/day
- Rules engine loaded in-process; memory pressure at scale
- No separation between simple auto-adjudicated claims and complex manual review

### 2.2 Target Architecture

```
                        ┌──────────────────────────────────────────────────────┐
                        │                   API Gateway                        │
                        │         (Rate Limiting, Auth, Request Routing)        │
                        └──────────────┬──────────────┬────────────────────────┘
                                       │              │
                              ┌────────▼──────┐ ┌────▼──────────────┐
                              │ Claims Intake  │ │ Claims Status API │
                              │   Service      │ │   (Read Path)     │
                              └────────┬───────┘ └────▲──────────────┘
                                       │              │
                                       │         ┌────┴──────────┐
                              ┌────────▼──────┐  │   Read Replica │
                              │  Kafka Topic  │  │   (PostgreSQL  │
                              │ claims.intake │  │   or DynamoDB)  │
                              └──┬─────┬──┬───┘  └────▲──────────┘
                                 │     │  │           │
                    ┌────────────▼┐ ┌──▼──▼────┐  ┌───┴──────────┐
                    │  Validation  │ │ Fraud     │  │  Projection  │
                    │  & Enrichment│ │ Detection │  │  Service     │
                    │  Service     │ │ Service   │  │  (CQRS)      │
                    └──────┬──────┘ └─────┬─────┘  └──────────────┘
                           │              │
                    ┌──────▼──────────────▼──────┐
                    │     Kafka Topic             │
                    │   claims.validated           │
                    └──────┬──────────────────────┘
                           │
              ┌────────────▼────────────┐
              │   Adjudication Router   │
              │  (Complexity Scoring)   │
              └──┬───────────────────┬──┘
                 │                   │
        ┌────────▼────────┐  ┌──────▼──────────┐
        │ Auto-Adjudicate │  │ Manual Review   │
        │ (Rules Engine)  │  │ Queue (Human)   │
        │ ~70% of claims  │  │ ~30% of claims  │
        └────────┬────────┘  └──────┬──────────┘
                 │                   │
        ┌────────▼───────────────────▼──────┐
        │       Kafka Topic                  │
        │     claims.adjudicated             │
        └────────┬──────────────────────────┘
                 │
        ┌────────▼────────┐     ┌────────────────┐
        │  Payment        │────►│ Payment Gateway │
        │  Service        │     └────────────────┘
        └────────┬────────┘
                 │
        ┌────────▼────────┐
        │  Notification   │
        │  Trigger        │
        └─────────────────┘
```

### 2.3 Key Design Decisions

#### Event Sourcing for Claims State

```
ClaimEvent {
    claim_id: UUID,
    event_type: SUBMITTED | VALIDATED | FLAGGED | ADJUDICATED | PAID | DENIED | APPEALED,
    timestamp: Instant,
    payload: JSON,
    version: long,          // optimistic concurrency
    partition_key: string   // tenant_id or LOB for Kafka partitioning
}
```

- **Why event sourcing**: Full audit trail is a regulatory requirement in insurance. Every state transition is an immutable event. Enables replay, debugging, and compliance reporting without additional instrumentation.
- **Compaction**: Use Kafka log compaction on `claims.state` topic to maintain latest state per claim_id for bootstrapping new consumers.

#### Partitioning Strategy

```
Kafka partition key = hash(tenant_id + line_of_business)
```

- Ensures claims from the same tenant land on the same partition (ordering guarantee per member)
- 64 partitions at 500K lives, scale to 256 partitions at 10M
- Consumer group per processing stage, allows independent scaling

#### Auto-Adjudication Engine

```
┌─────────────────────────────────────────────────┐
│           Adjudication Router                    │
│                                                  │
│  Complexity Score = f(claim_type, amount,         │
│                       provider_history,           │
│                       member_risk_score,          │
│                       code_combination_rarity)    │
│                                                  │
│  Score < 0.3  ──► Auto-Adjudicate (STP)          │
│  Score 0.3-0.7 ──► Auto + Human Spot Check       │
│  Score > 0.7  ──► Manual Review Queue            │
└─────────────────────────────────────────────────┘
```

- **Straight-Through Processing (STP)** target: 70-80% of claims at 10M lives
- Rules engine (Drools or custom) runs in stateless containers, horizontally scalable
- ML-based fraud scoring runs asynchronously; flags injected back into the event stream
- STP reduces human reviewer load from 100K/day to ~20-30K/day

#### Database Strategy

| Data | Store | Reason |
|------|-------|--------|
| Claim events | Kafka + S3 (long-term) | Append-only, high throughput |
| Claim current state | PostgreSQL (partitioned by tenant) | Strong consistency for writes |
| Claim read models | DynamoDB or Elasticsearch | Low-latency reads, flexible queries |
| Claim documents | S3 + metadata in PostgreSQL | Binary storage at scale |
| Adjudication rules | PostgreSQL + in-memory cache | Versioned, auditable |

#### Back-Pressure Handling

```
Producer ──► Kafka ──► Consumer (with max.poll.records + pause/resume)
                          │
                          ├── If processing latency > threshold:
                          │     1. Consumer pauses partition
                          │     2. Emits backpressure metric
                          │     3. Autoscaler adds consumer instances
                          │     4. Resume when lag < threshold
                          │
                          └── Dead Letter Queue for poison messages
                                (max 3 retries with exponential backoff)
```

### 2.4 Capacity Planning

| Component | 500K Lives | 10M Lives | Scaling Mechanism |
|-----------|-----------|-----------|-------------------|
| Kafka brokers | 3 | 9-12 | Broker addition + partition rebalance |
| Claims intake pods | 3 | 15-20 | HPA on CPU/request rate |
| Adjudication workers | 5 | 30-50 | HPA on Kafka consumer lag |
| PostgreSQL | 1 primary + 2 read replicas | Citus/partitioned cluster (8 shards) | Horizontal sharding by tenant |
| Fraud detection | 2 GPU instances | 8-12 GPU instances | Batch inference queue depth |

---

## 3. Real-Time Pricing Engine

### 3.1 What Breaks at 20x

- Actuarial models loaded fully in-memory (~4GB at 500K, ~80GB at 10M): won't fit in a single node
- Cache hit rates drop as product variety grows (more plan combinations)
- Rate table lookups become hot-path bottleneck
- Regulatory rate filing changes must propagate instantly (no stale prices)

### 3.2 Target Architecture

```
                  ┌───────────────────────────┐
                  │       Client Apps          │
                  │  (Broker Portal, Member    │
                  │   Portal, Partner APIs)    │
                  └────────────┬───────────────┘
                               │
                  ┌────────────▼───────────────┐
                  │        API Gateway          │
                  │  (Rate Limit: 1000 req/s)   │
                  └────────────┬───────────────┘
                               │
                  ┌────────────▼───────────────┐
                  │    Pricing Orchestrator     │
                  │    (Stateless, K8s)         │
                  │                             │
                  │  1. Parse quote request     │
                  │  2. Check L1 cache (local)  │
                  │  3. Check L2 cache (Redis)  │
                  │  4. Compute if cache miss   │
                  └──┬───────┬──────────┬──────┘
                     │       │          │
          ┌──────────▼┐  ┌───▼────┐  ┌──▼──────────┐
          │ Rate Table │  │ Risk   │  │ Regulatory  │
          │ Service    │  │ Scoring│  │ Compliance  │
          │            │  │ Engine │  │ Service     │
          └──────┬─────┘  └───┬────┘  └──┬──────────┘
                 │            │          │
          ┌──────▼────────────▼──────────▼──────────┐
          │              Redis Cluster               │
          │   (Rate tables, risk scores, filing      │
          │    effective dates, computed quotes)      │
          │   Partitioned by state + product line     │
          └──────────────────────────────────────────┘
                               │
          ┌────────────────────▼─────────────────────┐
          │         PostgreSQL (Source of Truth)       │
          │   Rate filings, actuarial tables,          │
          │   historical quotes, audit log             │
          └───────────────────────────────────────────┘
```

### 3.3 Key Design Decisions

#### Multi-Tier Caching

```
L1 Cache (In-Process, Caffeine)
├── TTL: 60 seconds
├── Size: 2GB per pod
├── Content: Hot rate tables for top 10 states
├── Hit rate target: 40-50%
│
L2 Cache (Redis Cluster, 6 shards)
├── TTL: 15 minutes (or invalidated on rate filing change)
├── Size: 64GB total cluster
├── Content: All computed quotes, rate tables, risk adjustments
├── Hit rate target: 85-90%
│
L3 (Compute on cache miss)
├── Pulls from PostgreSQL
├── Writes back to L2
└── Timeout: 500ms budget for full computation
```

#### Rate Table Sharding

Rate tables sharded by `(state, product_line, effective_date)`:

```
Shard Key = hash(state_code + product_line)

Examples:
  CA-Individual  → Shard 0
  CA-SmallGroup  → Shard 1
  NY-Individual  → Shard 2
  ...
```

- 50 states x 5 product lines = 250 logical shards, mapped to 6-12 Redis shards
- Each shard fits comfortably in memory (~500MB-1GB)
- New rate filings pushed via CDC (Change Data Capture) from PostgreSQL → Kafka → Redis invalidation

#### Pricing Computation Model

```
QuoteRequest {
    member_census: List<MemberDemographic>,
    zip_code: String,
    product_line: Enum,
    effective_date: LocalDate,
    riders: List<Rider>
}

// Computation pipeline (each step is independently cacheable)
Step 1: Base Rate Lookup        → f(age, zip, tobacco, product)
Step 2: Area Factor             → f(rating_area)
Step 3: Risk Adjustment         → f(census_demographics)
Step 4: Rider Adjustments       → f(selected_riders)
Step 5: Regulatory Compliance   → f(state, effective_date, filing_version)
Step 6: Final Assembly          → aggregate all factors

// Each intermediate result is cached independently
// Cache key: hash(step_number + input_parameters)
// Enables partial cache hits: if only riders change, steps 1-3 are cache hits
```

#### Handling Rate Filing Updates (Zero-Downtime)

```
Rate Filing Change Event
        │
        ▼
┌──────────────────┐
│  Filing Service   │
│  (validates new   │
│   rate tables)    │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐     ┌──────────────────┐
│  Kafka Topic:     │────►│  Cache Invalidation│
│  rate.filing      │     │  Consumer          │
│  .changes         │     │  (targeted purge)  │
└──────────────────┘     └──────────────────┘
         │
         ▼
┌──────────────────┐
│  Version-stamped  │  ← New quotes use version N+1
│  rate tables in   │  ← In-flight quotes complete with version N
│  Redis            │  ← Dual-version window: 5 minutes
└──────────────────┘
```

- Version-stamped rate tables ensure no partial updates
- Dual-version window allows in-flight requests to complete
- Filing audit log captures exact version used for every quote

### 3.4 Latency Budget

```
Total SLA: < 200ms P99

Breakdown:
  API Gateway + Auth:        10ms
  L1 Cache Check:             1ms
  L2 Cache Check:            5ms  (cache hit path: total ~16ms)
  Rate Table Lookup:         20ms  (cache miss)
  Risk Score Computation:    50ms  (cache miss)
  Regulatory Validation:    15ms  (cache miss)
  Assembly + Response:        5ms
  ─────────────────────────────
  Cache miss worst case:   ~106ms  (within 200ms budget)
```

### 3.5 Capacity Planning

| Component | 500K Lives | 10M Lives |
|-----------|-----------|-----------|
| Pricing pods | 4 | 20-30 |
| Redis cluster | 3 nodes (24GB) | 6-12 nodes (64-128GB) |
| PostgreSQL (rate tables) | 1 primary + 1 replica | 1 primary + 3 replicas |
| Peak QPS | 50 | 1,000-2,000 |
| Cache hit ratio required | 70% | 90%+ (critical for latency) |

---

## 4. Telehealth Platform

### 4.1 What Breaks at 20x

- WebRTC media servers are CPU-intensive; single-region deployment causes latency for distributed users
- Signaling server becomes bottleneck at ~2,000 concurrent sessions
- Session state stored in-memory; server crash = dropped sessions
- No admission control; overloaded servers degrade quality for all sessions

### 4.2 Target Architecture

```
                    ┌─────────────────────────┐
                    │   Client (Browser/App)   │
                    └────────────┬─────────────┘
                                 │
                    ┌────────────▼─────────────┐
                    │   Global Load Balancer    │
                    │   (Latency-based routing) │
                    └──┬──────────────────┬────┘
                       │                  │
            ┌──────────▼──────┐  ┌────────▼────────┐
            │  Region: US-East │  │ Region: US-West  │
            │                  │  │                   │
            │ ┌──────────────┐ │  │ ┌──────────────┐  │
            │ │  Signaling   │ │  │ │  Signaling   │  │
            │ │  Server      │ │  │ │  Server      │  │
            │ │  (WebSocket) │ │  │ │  (WebSocket) │  │
            │ └──────┬───────┘ │  │ └──────┬───────┘  │
            │        │         │  │        │          │
            │ ┌──────▼───────┐ │  │ ┌──────▼───────┐  │
            │ │  SFU Cluster │ │  │ │  SFU Cluster │  │
            │ │  (Selective  │ │  │ │  (Selective  │  │
            │ │   Forwarding │ │  │ │   Forwarding │  │
            │ │   Units)     │ │  │ │   Units)     │  │
            │ │  Capacity:   │ │  │ │  Capacity:   │  │
            │ │  5000 sessions│ │  │ │  5000 sessions│ │
            │ └──────────────┘ │  │ └──────────────┘  │
            │                  │  │                   │
            │ ┌──────────────┐ │  │ ┌──────────────┐  │
            │ │  TURN/STUN   │ │  │ │  TURN/STUN   │  │
            │ │  Servers     │ │  │ │  Servers     │  │
            │ └──────────────┘ │  │ └──────────────┘  │
            └──────────────────┘  └───────────────────┘
                       │                  │
            ┌──────────▼──────────────────▼────────────┐
            │            Shared Services                │
            │                                           │
            │  ┌─────────────┐  ┌──────────────────┐   │
            │  │  Session     │  │  Recording        │  │
            │  │  State Store │  │  Service (S3)     │  │
            │  │  (Redis)     │  │                   │  │
            │  └─────────────┘  └──────────────────┘   │
            │                                           │
            │  ┌─────────────┐  ┌──────────────────┐   │
            │  │  Scheduling  │  │  EHR Integration  │  │
            │  │  Service     │  │  (HL7 FHIR)      │  │
            │  └─────────────┘  └──────────────────┘   │
            └───────────────────────────────────────────┘
```

### 4.3 Key Design Decisions

#### SFU over MCU

```
MCU (Multipoint Control Unit):
  - Decodes all streams, mixes, re-encodes
  - CPU: O(N^2) per session
  - At 10K sessions: IMPOSSIBLE to scale cost-effectively

SFU (Selective Forwarding Unit):
  - Forwards packets without decoding
  - CPU: O(N) per session
  - At 10K sessions: 20-40 SFU instances (feasible)
```

- Use SFU architecture (e.g., Janus, mediasoup, or LiveKit)
- Each SFU handles ~250-500 concurrent 1:1 sessions
- For group sessions (provider + patient + specialist): SFU simulcast with 3 quality layers

#### Admission Control

```
Session Request
      │
      ▼
┌─────────────────────────┐
│   Admission Controller   │
│                          │
│   1. Check region        │
│      capacity            │
│   2. If capacity > 85%:  │
│      → Route to          │
│        alternate region  │
│   3. If all regions      │
│      > 95%:              │
│      → Queue with ETA    │
│      → Offer callback    │
│   4. Reserve SFU slot    │
│      (TTL: 60s)          │
└─────────────────────────┘
```

- Prevents overload-induced quality degradation for all users
- Capacity tracked in Redis with atomic INCR/DECR per SFU instance
- Health checks every 5s; unhealthy SFUs drained gracefully (existing sessions continue, no new sessions)

#### Session State Externalization

```
SessionState (Redis, TTL = session_duration + 30min) {
    session_id: UUID,
    provider_id: String,
    patient_id: String,
    sfu_node_id: String,
    region: String,
    start_time: Instant,
    recording_enabled: boolean,
    ice_candidates: List<ICECandidate>,
    quality_metrics: {
        packet_loss: float,
        jitter: float,
        rtt: float
    }
}
```

- If an SFU node crashes, reconnecting client gets routed to a new SFU in the same region
- Signaling server recovers session from Redis and re-establishes media path
- Mean Time to Recovery: < 5 seconds (vs. full session loss without externalization)

#### HIPAA Compliance for Telehealth

```
┌──────────────────────────────────────────────────┐
│              HIPAA Requirements                    │
│                                                    │
│  ✓ End-to-end encryption (SRTP for media,         │
│    DTLS for data channels)                         │
│  ✓ Recording encryption at rest (AES-256)          │
│  ✓ Access logging for all session events           │
│  ✓ BAA with cloud provider                         │
│  ✓ Session recordings stored in HIPAA-compliant    │
│    S3 bucket with versioning + MFA delete          │
│  ✓ Automatic session recording purge after          │
│    retention period (configurable per state)        │
└──────────────────────────────────────────────────┘
```

### 4.4 Capacity Planning

| Component | 500K Lives | 10M Lives |
|-----------|-----------|-----------|
| Regions | 1 | 2-3 (US-East, US-West, US-Central) |
| SFU instances per region | 2-4 | 15-25 |
| Signaling servers | 2 | 6-10 (2-3 per region) |
| TURN servers | 2 | 6-9 (2-3 per region) |
| Concurrent sessions | 500 | 10,000 |
| Redis (session state) | 1 cluster | 1 cluster per region |
| Recording storage/month | 5 TB | 100 TB |

---

## 5. Notification System

### 5.1 What Breaks at 20x

- Single notification queue: all notification types compete for the same consumers
- Claim status update (time-sensitive) stuck behind batch renewal reminders
- No deduplication: member gets 3 identical emails from retry logic
- Template rendering is synchronous and CPU-bound; blocks sending pipeline
- No channel preference management: members can't opt out of specific channels

### 5.2 Target Architecture

```
                    ┌──────────────────────────────────┐
                    │        Event Sources              │
                    │  (Claims, Policies, Billing,      │
                    │   Telehealth, Enrollment)         │
                    └─────────────┬────────────────────┘
                                  │
                    ┌─────────────▼────────────────────┐
                    │     Notification Router            │
                    │     (Event → Notification Map)     │
                    │                                    │
                    │  claim.adjudicated → CLAIM_STATUS  │
                    │  policy.renewed    → RENEWAL_CONF  │
                    │  payment.failed    → PAYMENT_ALERT │
                    └──┬──────────┬──────────┬─────────┘
                       │          │          │
              ┌────────▼──┐  ┌───▼────┐  ┌──▼──────────┐
              │ Priority   │  │Standard│  │ Bulk        │
              │ Queue (P0) │  │Queue   │  │ Queue (P2)  │
              │            │  │(P1)    │  │             │
              │ Claim deny,│  │ Status │  │ Renewal     │
              │ fraud alert│  │ updates│  │ reminders,  │
              │ auth codes │  │        │  │ newsletters │
              └────────┬───┘  └───┬────┘  └──┬──────────┘
                       │          │          │
              ┌────────▼──────────▼──────────▼──────────┐
              │        Notification Processor            │
              │                                          │
              │  1. Deduplication (Redis, 24h window)    │
              │  2. Member preference lookup             │
              │  3. Channel selection (email/SMS/push)   │
              │  4. Template rendering                   │
              │  5. Regulatory check (quiet hours, etc.) │
              └──┬────────────┬──────────────┬──────────┘
                 │            │              │
          ┌──────▼─────┐ ┌───▼──────┐ ┌─────▼──────┐
          │  Email      │ │  SMS     │ │  Push      │
          │  Provider   │ │  Provider│ │  Provider  │
          │  (SES/      │ │  (Twilio)│ │  (FCM/APNs)│
          │   SendGrid) │ │          │ │            │
          └──────┬──────┘ └───┬──────┘ └─────┬──────┘
                 │            │              │
          ┌──────▼────────────▼──────────────▼──────────┐
          │           Delivery Tracking                  │
          │   (Webhook receivers for delivery status)    │
          │   → delivered / bounced / opened / failed    │
          │   → Retry logic for transient failures       │
          └──────────────────────────────────────────────┘
```

### 5.3 Key Design Decisions

#### Priority Lanes

```
P0 (Critical) - SLA: < 30 seconds
├── Claim denials, fraud alerts, auth codes, payment failures
├── Dedicated consumer group: 10 consumers
├── Rate limit: none (always send immediately)
│
P1 (Standard) - SLA: < 5 minutes
├── Claim status updates, appointment reminders, EOB availability
├── Consumer group: 20 consumers
├── Rate limit: 500/sec per channel
│
P2 (Bulk) - SLA: < 4 hours
├── Renewal reminders, newsletters, benefit summaries, surveys
├── Consumer group: 10 consumers
├── Rate limit: 100/sec per channel (avoid provider throttling)
├── Scheduled window: off-peak hours preferred
```

#### Deduplication

```
Dedup Key = hash(member_id + notification_type + content_hash)
TTL = 24 hours

Redis SET with NX (set-if-not-exists):
  If key exists → skip (duplicate)
  If key doesn't exist → set key with TTL, proceed to send
```

- Prevents duplicate sends from Kafka consumer retries or upstream event replays
- Content hash ensures legitimately different notifications of the same type are sent

#### Template Engine (Pre-rendered)

```
Template Rendering Pipeline:
  1. Templates stored in S3 (versioned)
  2. Template cache in Redis (TTL: 1 hour)
  3. Rendering: Handlebars/Mustache (stateless, fast)
  4. Pre-rendered bulk templates during off-peak:
     - Nightly job pre-renders renewal notices for next 7 days
     - Stored as rendered HTML/text in S3
     - Send job just fetches and dispatches

At 10M lives:
  - 1M notifications/day
  - Pre-rendering eliminates runtime template bottleneck for 60%+ of volume
```

#### Channel Failover

```
Primary Channel → Attempt Send
                      │
              ┌───────▼───────┐
              │  Success?      │
              │                │
         Yes──┤           No───┤
              │                │
              ▼                ▼
         Record           Retry (3x exponential)
         Delivery              │
                          ┌────▼────┐
                          │ Success? │
                     Yes──┤     No───┤
                          │         │
                          ▼         ▼
                     Record    Failover to
                     Delivery  Secondary Channel
                                    │
                               (Email fails → SMS)
                               (Push fails → Email)
```

### 5.4 Capacity Planning

| Component | 500K Lives | 10M Lives |
|-----------|-----------|-----------|
| Kafka partitions (per priority) | 8 | 32-64 |
| P0 consumers | 3 | 10 |
| P1 consumers | 5 | 20 |
| P2 consumers | 3 | 10 |
| Redis (dedup + templates) | 1 node | 3-node cluster |
| Email throughput | 500/min | 10,000/min |
| SMS throughput | 100/min | 2,000/min |

---

## 6. Document Processing

### 6.1 What Breaks at 20x

- Sequential OCR processing: single-threaded, GPU-bound
- Large claim packages (50+ page PDFs) block the pipeline
- No classification step: all documents go through the same expensive processing
- Storage costs grow linearly with no tiering strategy
- No extraction validation: OCR errors propagate to claims adjudication

### 6.2 Target Architecture

```
                    ┌──────────────────────────────────┐
                    │        Document Ingestion         │
                    │  (API Upload, Email, Fax, EDI)    │
                    └─────────────┬────────────────────┘
                                  │
                    ┌─────────────▼────────────────────┐
                    │     S3 Landing Zone               │
                    │  (Raw documents, virus scanned)   │
                    └─────────────┬────────────────────┘
                                  │
                    ┌─────────────▼────────────────────┐
                    │  Document Classification          │
                    │  (ML Model - lightweight)         │
                    │                                   │
                    │  Types: CMS-1500, UB-04, EOB,     │
                    │         ID Card, Lab Report,      │
                    │         Prior Auth, Appeal Letter  │
                    └──┬───────┬───────┬───────────────┘
                       │       │       │
          ┌────────────▼┐  ┌──▼────┐  ┌▼──────────────┐
          │  Fast Path   │  │Standard│ │ Complex Path  │
          │  (Structured │  │ Path  │  │ (Handwritten, │
          │   forms)     │  │       │  │  multi-doc    │
          │              │  │       │  │  packages)    │
          └──────┬───────┘  └──┬────┘  └──┬────────────┘
                 │             │          │
          ┌──────▼─────────────▼──────────▼────────────┐
          │              OCR / Extraction                │
          │                                              │
          │  Fast: Template-based extraction (no GPU)    │
          │  Standard: Tesseract + post-processing       │
          │  Complex: Cloud Vision AI / Textract (GPU)   │
          └──────────────────┬─────────────────────────┘
                             │
          ┌──────────────────▼─────────────────────────┐
          │         Extraction Validation               │
          │                                             │
          │  Confidence score per field                  │
          │  Score > 0.95 → Auto-accept                 │
          │  Score 0.70-0.95 → Flag for human review    │
          │  Score < 0.70 → Route to manual data entry  │
          └──────────────────┬─────────────────────────┘
                             │
          ┌──────────────────▼─────────────────────────┐
          │         Structured Data Output               │
          │                                              │
          │  → Claims pipeline (claim data)              │
          │  → Member records (ID cards, enrollment)     │
          │  → Provider records (credentialing docs)     │
          │  → Compliance archive (audit documents)      │
          └──────────────────┬─────────────────────────┘
                             │
          ┌──────────────────▼─────────────────────────┐
          │         Document Storage Tiers               │
          │                                              │
          │  Hot (0-90 days):    S3 Standard              │
          │  Warm (90d-2yr):    S3 Infrequent Access      │
          │  Cold (2yr-7yr):    S3 Glacier                │
          │  Archive (7yr+):    S3 Glacier Deep Archive   │
          └────────────────────────────────────────────┘
```

### 6.3 Key Design Decisions

#### Fan-Out Processing for Large Documents

```
Large PDF (50+ pages)
        │
        ▼
┌──────────────────┐
│  Splitter Service │
│                   │
│  1. Split PDF     │
│     into 10-page  │
│     chunks        │
│  2. Submit each   │
│     chunk as      │
│     independent   │
│     work item     │
│  3. Parallel OCR  │
│     across chunks │
└────────┬──────────┘
         │
    ┌────▼────┬────────┬────────┐
    │ Chunk 1 │ Chunk 2│ Chunk N│
    │  OCR    │  OCR   │  OCR   │
    └────┬────┘────┬───┘────┬───┘
         │         │        │
    ┌────▼─────────▼────────▼───┐
    │     Aggregator Service     │
    │  (Reassemble + Validate)   │
    └───────────────────────────┘

Result: 50-page document processed in ~30s instead of ~5 minutes
```

#### Processing Queue Design

```
SQS (or Kafka) with visibility timeout:

Document Queue:
├── Fast Path Queue
│   ├── Consumers: 10 pods (CPU-only)
│   ├── Avg processing time: 2-5 seconds
│   └── Volume: 60% of documents
│
├── Standard Queue
│   ├── Consumers: 15 pods (CPU + light GPU)
│   ├── Avg processing time: 15-30 seconds
│   └── Volume: 30% of documents
│
└── Complex Queue
    ├── Consumers: 5 pods (GPU instances)
    ├── Avg processing time: 1-3 minutes
    └── Volume: 10% of documents

Autoscaling:
  - Scale on queue depth (ApproximateNumberOfMessages)
  - Fast path: scale when depth > 100
  - Standard: scale when depth > 50
  - Complex: scale when depth > 20
```

#### Extraction Schema (Standardized Output)

```json
{
    "document_id": "uuid",
    "document_type": "CMS_1500",
    "classification_confidence": 0.98,
    "processing_path": "FAST",
    "extracted_fields": {
        "patient_name": {
            "value": "John Smith",
            "confidence": 0.97,
            "bounding_box": {"page": 1, "x": 100, "y": 200, "w": 150, "h": 20}
        },
        "diagnosis_codes": {
            "value": ["Z00.00", "E11.9"],
            "confidence": 0.94,
            "bounding_box": {"page": 1, "x": 300, "y": 400, "w": 200, "h": 40}
        },
        "total_charges": {
            "value": 1250.00,
            "confidence": 0.99,
            "bounding_box": {"page": 1, "x": 500, "y": 600, "w": 100, "h": 20}
        }
    },
    "validation_status": "AUTO_ACCEPTED",
    "processing_time_ms": 3200,
    "source_document_s3_uri": "s3://docs-hot/2026/03/uuid.pdf"
}
```

### 6.4 Capacity Planning

| Component | 500K Lives | 10M Lives |
|-----------|-----------|-----------|
| Documents/day | 10,000 | 200,000 |
| Fast path workers | 3 | 10-15 |
| Standard workers | 3 | 15-20 |
| Complex workers (GPU) | 1 | 5-8 |
| S3 storage (annual) | 5 TB | 100 TB |
| Storage cost optimization | None | Lifecycle policies save ~60% |

---

## 7. Cross-Cutting Concerns

### 7.1 Data Platform & Analytics

```
┌──────────────────────────────────────────────────────┐
│                  Operational Systems                  │
│  (Claims, Pricing, Telehealth, Notifications, Docs)  │
└──────────────────────┬───────────────────────────────┘
                       │  CDC (Debezium)
                       ▼
              ┌────────────────┐
              │  Kafka Connect  │
              └───────┬────────┘
                      │
         ┌────────────▼─────────────┐
         │    Data Lake (S3/Iceberg) │
         │                          │
         │  Raw Zone → Curated Zone │
         │                          │
         │  Partitioned by:         │
         │    date / tenant / LOB   │
         └─────────┬────────────────┘
                   │
         ┌─────────▼────────────────┐
         │   Analytics Engine        │
         │   (Spark / Trino)         │
         │                           │
         │   - Claims analytics      │
         │   - Fraud detection ML    │
         │   - Actuarial reporting   │
         │   - SLA dashboards        │
         └───────────────────────────┘
```

### 7.2 Observability Stack

```
┌─────────────────────────────────────────────────────────┐
│                    Observability                          │
│                                                          │
│  Metrics (Prometheus + Grafana)                          │
│  ├── Claims: processing latency, STP rate, backlog       │
│  ├── Pricing: cache hit rate, P99 latency, error rate    │
│  ├── Telehealth: concurrent sessions, quality scores     │
│  ├── Notifications: delivery rate, latency by priority   │
│  └── Documents: queue depth, processing time, accuracy   │
│                                                          │
│  Tracing (OpenTelemetry → Jaeger/Tempo)                  │
│  ├── Claim lifecycle: intake → adjudication → payment    │
│  ├── Quote request: API → cache → compute → response     │
│  └── Correlation IDs across all services                 │
│                                                          │
│  Logging (Structured JSON → ELK/Loki)                    │
│  ├── Claim ID in every log line                          │
│  ├── Request ID propagated across services               │
│  └── PII redacted at ingestion (member SSN, DOB)         │
│                                                          │
│  Alerting                                                │
│  ├── Claims backlog > 10,000: Page on-call               │
│  ├── Pricing P99 > 500ms: Alert engineering              │
│  ├── Telehealth capacity > 85%: Auto-scale + warn        │
│  ├── Notification delivery rate < 95%: Investigate        │
│  └── Document queue age > 1 hour: Escalate               │
└─────────────────────────────────────────────────────────┘
```

### 7.3 Security & Compliance

```
┌─────────────────────────────────────────────────────┐
│              Security Architecture                    │
│                                                      │
│  Identity & Access                                   │
│  ├── OAuth 2.0 + OIDC for member/provider portals    │
│  ├── mTLS for service-to-service communication       │
│  ├── RBAC with least-privilege per service            │
│  └── API keys + rate limiting for partner APIs       │
│                                                      │
│  Data Protection                                     │
│  ├── Encryption at rest: AES-256 (all stores)        │
│  ├── Encryption in transit: TLS 1.3                  │
│  ├── PII tokenization (member SSN, DOB)              │
│  ├── Field-level encryption for PHI in databases     │
│  └── Key rotation: 90-day cycle (AWS KMS)            │
│                                                      │
│  Compliance                                          │
│  ├── HIPAA: BAA, audit logs, access controls, PHI    │
│  ├── SOC 2 Type II: annual audit                     │
│  ├── State insurance regulations: rate filing audit   │
│  └── Data retention: configurable per state/type     │
│                                                      │
│  Audit Trail                                         │
│  ├── Every data access logged (who, what, when)      │
│  ├── Immutable audit log (append-only, S3 + Athena)  │
│  └── Retention: 7 years minimum (regulatory)         │
└─────────────────────────────────────────────────────┘
```

### 7.4 Multi-Tenancy Strategy

```
Tenant Isolation Model: Silo for data, pool for compute

┌──────────────────────────────────────────┐
│          Compute Layer (Pooled)           │
│  Shared K8s clusters with namespace      │
│  isolation per service                   │
│  Resource quotas per tenant (CPU/memory) │
└──────────────────────┬───────────────────┘
                       │
┌──────────────────────▼───────────────────┐
│          Data Layer (Siloed)              │
│                                          │
│  Option A: Schema-per-tenant (< 50)      │
│    PostgreSQL with separate schemas      │
│    Simpler ops, tenant migration easy    │
│                                          │
│  Option B: Tenant-ID partitioning        │
│    (50+ tenants)                         │
│    Shared tables, partitioned by         │
│    tenant_id, row-level security (RLS)   │
│                                          │
│  Kafka: Topic-per-tenant for isolation   │
│    claims.{tenant_id}.intake             │
│  S3: Prefix-per-tenant                   │
│    s3://bucket/{tenant_id}/docs/         │
└──────────────────────────────────────────┘
```

---

## 8. Migration Strategy

### 8.1 Phased Rollout (Strangler Fig Pattern)

```
Phase 1 (Months 1-3): Foundation
├── Deploy Kafka cluster
├── Implement event sourcing for new claims
├── Set up observability stack
├── Migrate document storage to tiered S3
└── Gate: Process 10% of claims through new pipeline

Phase 2 (Months 4-6): Core Migration
├── Claims pipeline: dual-write (old + new), compare results
├── Pricing engine: deploy Redis cluster, cache warming
├── Notification priority queues live
├── Document classification model trained and deployed
└── Gate: New pipeline handles 50% of claims with < 1% discrepancy

Phase 3 (Months 7-9): Full Cutover
├── Claims: 100% through new pipeline, old system read-only
├── Pricing: full cache coverage, old engine standby
├── Telehealth: multi-region deployment
├── All notification channels through new system
└── Gate: All SLAs met at current load

Phase 4 (Months 10-12): Scale Testing & Hardening
├── Load test at 2x current (1M lives equivalent)
├── Chaos engineering (kill nodes, network partitions)
├── Performance tuning based on production metrics
├── Capacity planning validation
└── Gate: System handles 20x load in staging

Phase 5 (Ongoing): Growth
├── Scale infrastructure as lives grow
├── Autoscaling policies tuned from production data
├── Quarterly capacity reviews
└── Continuous optimization of STP rates, cache hit rates
```

### 8.2 Risk Mitigation

| Risk | Mitigation |
|------|------------|
| Data loss during migration | Dual-write with reconciliation jobs comparing old and new |
| Regulatory non-compliance during transition | Compliance team reviews each phase; audit logging from day 1 |
| Performance regression | Shadow traffic testing; feature flags for instant rollback |
| Team skill gaps (event-driven architecture) | Training sprints in Phase 1; pair programming with platform team |
| Vendor lock-in | Abstract cloud services behind interfaces; use open standards (Kafka, S3 API, FHIR) |

---

## Appendix: Technology Stack Summary

| Layer | Technology | Rationale |
|-------|-----------|-----------|
| Compute | Kubernetes (EKS) | Container orchestration, autoscaling |
| Messaging | Apache Kafka (MSK) | Event sourcing, high throughput, ordering |
| Primary DB | PostgreSQL (RDS/Aurora) | ACID compliance, regulatory requirements |
| Cache | Redis Cluster (ElastiCache) | Low-latency reads, session state, dedup |
| Read Store | DynamoDB or Elasticsearch | Flexible queries, search |
| Object Storage | S3 | Documents, recordings, data lake |
| Search | OpenSearch/Elasticsearch | Claims search, document search |
| ML/AI | SageMaker | Fraud detection, document classification |
| Media (Telehealth) | LiveKit or Janus | WebRTC SFU, open source |
| Observability | Prometheus, Grafana, OTel, Loki | Full-stack observability |
| CI/CD | GitHub Actions + ArgoCD | GitOps deployment |
| IaC | Terraform | Infrastructure as code |

---

*This architecture is designed to scale incrementally. Not every component needs to be at 10M-scale on day one -- the event-driven foundation allows each subsystem to scale independently as load demands.*
