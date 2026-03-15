# Endorsement Management System — Phased Execution Strategy

## From MVP to World-Class Global Product

**Author:** Sujay Hegde | **Date:** March 7, 2026
**Domain:** Group Health Insurance — Endorsement Processing
**Target Scale:** 100K employers × 10 changes/employer/day × 10 insurers = **1M endorsements/day**

---

## Table of Contents

1. [Executive Summary & Scale Analysis](#1-executive-summary--scale-analysis)
2. [Phased Execution Strategy Overview](#2-phased-execution-strategy-overview)
3. [Phase 1 — MVP: Core Endorsement Engine](#3-phase-1--mvp-core-endorsement-engine)
4. [Phase 2 — Scale: Multi-Insurer & Optimization](#4-phase-2--scale-multi-insurer--optimization)
5. [Phase 3 — Intelligence: AI/Automation & Predictive](#5-phase-3--intelligence-aiautomation--predictive)
6. [Phase 4 — Global: World-Class Platform](#6-phase-4--global-world-class-platform)
7. [C4 Architecture Model](#7-c4-architecture-model)
8. [High-Level Design](#8-high-level-design)
9. [Low-Level Design](#9-low-level-design)
10. [Coding Plan](#10-coding-plan)
11. [Design Patterns](#11-design-patterns)
12. [Architectural Patterns](#12-architectural-patterns)
13. [Testing Strategy](#13-testing-strategy)
14. [Observability Strategy](#14-observability-strategy)
15. [Distributed Tracing Strategy](#15-distributed-tracing-strategy)
16. [Deployment Strategy](#16-deployment-strategy)
17. [Security Model](#17-security-model)
18. [Strategy Evolution — MVP to Global Product](#18-strategy-evolution--mvp-to-global-product)

---

## 1. Executive Summary & Scale Analysis

### Business Context

At the start of every year, an employer purchases group insurance for all employees and their dependents. Throughout the year, employee lifecycle events (joinings, exits, role changes, dependent additions) produce **endorsements** — change requests that must be executed by the insurer. Endorsements are the lifeblood of group insurance operations.

### Scale Numbers

| Metric | MVP (Phase 1) | Scale (Phase 2) | Global (Phase 4) |
|--------|--------------|-----------------|-------------------|
| Employers | 1K | 10K | 100K+ |
| Endorsements/day | 10K | 100K | 1M+ |
| Insurance providers | 2-3 | 5-7 | 10+ (multi-country) |
| Concurrent batch jobs | 2-3 | 20-30 | 100+ |
| Dashboard concurrent users | 50 | 500 | 5,000+ |
| EA balance computations/sec | 10 | 100 | 1,000+ |

### Core Challenges

1. **Zero Coverage Gap**: An employee must NEVER lose medical coverage between endorsement submission and insurer confirmation
2. **EA Balance Optimization**: Employers want minimum capital locked in the Endorsement Account — requires intelligent batching and sequencing
3. **Multi-Insurer Heterogeneity**: 10 insurers with different APIs (batch vs real-time), SLAs (hours vs days), and data formats
4. **Reconciliation Complexity**: Tracking what was sent vs what was confirmed vs what failed across async batch processing
5. **Real-Time Visibility**: Stakeholders need instant answers to "what's the status of my endorsement?"

---

## 2. Phased Execution Strategy Overview

```
Phase 1 — MVP (Weeks 1-4)
├── Core endorsement CRUD + state machine
├── Single insurer integration (real-time + batch)
├── Basic EA balance tracking
├── Simple dashboard (status visibility)
├── Provisional coverage mechanism
└── Gate: Process 10K endorsements/day for 1 insurer

Phase 2 — Scale (Weeks 5-10)
├── Multi-insurer adapter framework
├── EA balance optimization algorithm
├── Kafka consumer groups scaled (multi-insurer)
├── Automated reconciliation engine
├── Advanced dashboard + notifications (React + WebSocket)
├── Elasticsearch full-text search + analytics
└── Gate: Process 100K endorsements/day across 5 insurers

Phase 3 — Intelligence (Weeks 11-16)
├── AI-powered anomaly detection
├── Predictive EA balance forecasting
├── Smart batch optimization (ML-driven)
├── Automated error resolution
├── Process mining & optimization
└── Gate: 95% STP rate, <5min anomaly detection

Phase 4 — Global (Weeks 17-24)
├── Multi-region deployment
├── Multi-country/regulation support
├── Advanced analytics & BI
├── Self-service insurer onboarding
├── Platform API marketplace
└── Gate: 1M endorsements/day, 99.9% uptime
```

---

## 3. Phase 1 — MVP: Core Endorsement Engine

### 3.1 Scope

**In Scope:**
- Endorsement lifecycle management (create, submit, track, confirm/reject)
- Single insurer integration with both real-time and batch APIs
- Provisional coverage: employees are covered from eligibility date regardless of insurer confirmation
- Basic EA balance tracking (debit on addition, credit on deletion)
- Status dashboard showing pending/processing/confirmed/failed endorsements
- Retry mechanism for failed endorsements

**Out of Scope for MVP:**
- Multi-insurer support (adapter framework deferred)
- EA balance optimization algorithm (basic FIFO processing)
- AI/ML capabilities
- Advanced analytics

### 3.2 Endorsement State Machine (Core of MVP)

```
                    ┌──────────┐
                    │  CREATED │ ← HR submits employee change
                    └────┬─────┘
                         │
                    ┌────▼─────────────┐
                    │  VALIDATED       │ ← Data validation passed
                    └────┬─────────────┘
                         │
                    ┌────▼─────────────┐
                    │  PROVISIONALLY   │ ← Employee gets coverage immediately
                    │  COVERED         │   (internal record, no insurer yet)
                    └────┬─────────────┘
                         │
              ┌──────────▼──────────┐
              │  Is real-time or    │
              │  batch insurer?     │
              └──┬──────────────┬───┘
                 │              │
        ┌────────▼────┐  ┌─────▼──────────┐
        │ SUBMITTED   │  │ QUEUED_FOR     │
        │ _REALTIME   │  │ _BATCH         │
        └────────┬────┘  └─────┬──────────┘
                 │              │
                 │         ┌────▼──────────┐
                 │         │ BATCH         │
                 │         │ _SUBMITTED    │
                 │         └─────┬─────────┘
                 │               │
              ┌──▼───────────────▼──┐
              │ INSURER_PROCESSING  │ ← Insurer acknowledged receipt
              └──┬──────────────┬───┘
                 │              │
        ┌────────▼────┐  ┌─────▼──────────┐
        │ CONFIRMED   │  │ REJECTED       │
        │             │  │                │
        │ EA debited/ │  │ Retry or       │
        │ credited    │  │ escalate       │
        └─────────────┘  └─────┬──────────┘
                               │
                          ┌────▼──────────┐
                          │ RETRY         │ ← Max 3 retries
                          │ _PENDING      │   with exponential backoff
                          └────┬──────────┘
                               │
                          ┌────▼──────────┐
                          │ FAILED        │ ← Manual intervention needed
                          │ _PERMANENT    │   Alert sent to ops
                          └───────────────┘
```

### 3.3 Provisional Coverage — Zero Gap Guarantee

**The single most critical design decision in this system.**

```
Timeline: Employee Joins
──────────────────────────────────────────────────────►

Day 0          Day 0+1min      Day 1-3           Day 3+
HR submits     System marks    Insurer            Insurer
endorsement    PROVISIONALLY   processes          confirms
               COVERED         batch

               ◄────────────── COVERED ──────────────────►
               No gap. Ever.

Implementation:
┌──────────────────────────────────────────────────────┐
│  Provisional Coverage Record                         │
│                                                      │
│  employee_id: UUID                                   │
│  employer_id: UUID                                   │
│  coverage_start: eligibility_date (NOT submit date)  │
│  coverage_type: PROVISIONAL                          │
│  endorsement_id: UUID (links to pending endorsement) │
│  insurer_policy_id: NULL (filled on confirmation)    │
│                                                      │
│  Rule: If a claim comes in while PROVISIONAL,        │
│        Plum processes it internally and settles       │
│        with insurer post-confirmation.                │
│                                                      │
│  Risk Mitigation: Provisional period capped at 30    │
│  days. If no insurer confirmation in 30 days,        │
│  escalate to ops + insurer relationship manager.     │
└──────────────────────────────────────────────────────┘
```

### 3.4 MVP Technology Choices

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Language | Java 21 | Virtual threads (Project Loom) for high-throughput async I/O, pattern matching, sealed classes for domain modeling, record types for DTOs |
| Framework | Spring Boot 3.5.x | Latest stable, virtual threads support, native OTel integration, comprehensive ecosystem |
| Database | PostgreSQL 16 | ACID for financial data (EA), JSONB for flexible endorsement payloads |
| Message Queue | Apache Kafka | Event-driven backbone from Day 1 — endorsement lifecycle events, consumer groups for parallel processing |
| Distributed Cache | Redis 7 | Idempotency keys, rate limiting, CQRS read model materialized views, session state |
| In-Memory Cache | Caffeine | L1 cache for hot data (insurer configs, rate tables, EA balance snapshots) — sub-millisecond reads |
| Secondary Store | Elasticsearch 8 | Full-text search on endorsements, audit log search, analytics aggregations |
| Frontend | React 18 + TypeScript | Dashboard SPA with real-time WebSocket updates |
| AI/ML Framework | Spring AI + LangChain4j | Spring AI for LLM integration (anomaly explanation, error resolution), LangChain4j for RAG pipelines and agent orchestration |
| Distributed Tracing | OpenTelemetry (OTel) | Vendor-neutral tracing SDK, auto-instrumentation for Spring Boot, Kafka, JDBC, Redis |
| Containerization | Docker | Multi-stage builds for minimal JRE 21 images |
| Container Orchestration | Kubernetes | Pod autoscaling, health checks, rolling deployments |
| Cloud Deployment | Railway | Initial deployment platform — Docker-based, easy setup; architecture uses Docker + K8s abstractions for portability to AWS EKS/ECS |
| API Docs | OpenAPI 3.1 (springdoc) | Contract-first design, auto-generated from annotations |
| Build | Gradle (Kotlin DSL) | Already in project setup |
| DB Migration | Flyway | Version-controlled schema migrations |

**Cloud Portability Note:** All infrastructure dependencies are abstracted behind interfaces (Hexagonal Architecture). Railway is the initial deployment target for speed of iteration. The Docker + Kubernetes foundation ensures zero-friction migration to AWS (EKS + RDS + MSK + ElastiCache + Elasticsearch) when scale demands it. No Railway-proprietary APIs are used — only standard Docker, K8s manifests, and environment-variable-based configuration.

---

## 4. Phase 2 — Scale: Multi-Insurer & Optimization

### 4.1 Multi-Insurer Adapter Framework

```
┌──────────────────────────────────────────────────────────┐
│                 Endorsement Orchestrator                   │
│                                                           │
│  Receives validated endorsements, routes to correct        │
│  insurer adapter based on employer's policy configuration  │
└───────────┬──────────────┬──────────────┬────────────────┘
            │              │              │
   ┌────────▼────────┐ ┌──▼──────────┐ ┌─▼───────────────┐
   │ ICICI Lombard   │ │ Niva Bupa   │ │ Bajaj Allianz   │
   │ Adapter         │ │ Adapter     │ │ Adapter         │
   │                 │ │             │ │                 │
   │ ● Real-time API │ │ ● Batch     │ │ ● Real-time +   │
   │ ● REST + OAuth  │ │   only      │ │   Batch         │
   │ ● SLA: 30sec    │ │ ● SFTP CSV  │ │ ● SOAP + WS-Sec │
   │ ● Format: JSON  │ │ ● SLA: 24hr │ │ ● SLA: 4hr      │
   │                 │ │ ● Format:   │ │ ● Format: XML   │
   │                 │ │   CSV       │ │                 │
   └─────────────────┘ └─────────────┘ └─────────────────┘

Each Adapter Implements:
┌──────────────────────────────────────────┐
│  InsurerPort (Hexagonal Architecture)    │
│                                          │
│  + submitRealTime(endorsement): Result   │
│  + submitBatch(endorsements): BatchId    │
│  + checkBatchStatus(batchId): Status     │
│  + fetchConfirmation(refId): Confirm     │
│  + mapToInsurerFormat(e): InsurerDTO     │
│  + mapFromInsurerFormat(r): Endorsement  │
│  + getCapabilities(): InsurerCapability  │
│                                          │
│  InsurerCapability:                      │
│  ├── supportsRealTime: boolean           │
│  ├── supportsBatch: boolean              │
│  ├── maxBatchSize: int                   │
│  ├── batchSLA: Duration                  │
│  ├── rateLimitPerMinute: int             │
│  └── retryPolicy: RetryConfig            │
└──────────────────────────────────────────┘
```

### 4.2 EA Balance Optimization Algorithm

**Goal:** Minimize the capital an employer must keep in the Endorsement Account while ensuring all additions are funded.

```
Algorithm: Smart Endorsement Sequencing

Principle: Process deletions BEFORE additions within the same batch
           to maximize credit recovery before debits.

Input:
  pending_endorsements: List<Endorsement>  // additions + deletions + updates
  current_ea_balance: Money
  employer_monthly_budget: Money

Step 1 — Classify and Prioritize:
┌─────────────────────────────────────────────────────┐
│  Priority Queue (descending):                        │
│                                                      │
│  P0: Deletions (immediate credit recovery)           │
│      → Process first to free up EA balance           │
│                                                      │
│  P1: Cost-neutral updates (name change, address)     │
│      → No EA impact, process anytime                 │
│                                                      │
│  P2: Additions by coverage_start_date ASC            │
│      → Earliest eligibility first (zero gap promise) │
│                                                      │
│  P3: Premium-affecting updates (age band change)     │
│      → May need additional funds                     │
└─────────────────────────────────────────────────────┘

Step 2 — Batch Construction:
  FOR each insurer_batch_window:
    1. Take all P0 (deletions) → compute expected_credits
    2. available_balance = current_ea_balance + expected_credits
    3. Take P1 (neutral updates) → add to batch (no cost)
    4. Take P2 (additions) in eligibility_date order:
       WHILE available_balance >= next_addition_premium:
         add to batch
         available_balance -= premium
    5. Take P3 (premium updates):
       IF available_balance >= delta:
         add to batch
    6. Remaining items → hold for next batch window

Step 3 — Balance Prediction:
  required_minimum = SUM(pending_addition_premiums)
                   - SUM(pending_deletion_credits)
                   + safety_margin (10%)

  IF current_balance < required_minimum:
    → Alert employer: "Top up ₹X to process N pending additions"
    → Forecast: "At current change rate, you'll need ₹Y/month"

Result:
  Employer holds 15-25% LESS capital in EA compared to
  naive FIFO processing, because deletions are batched
  before additions to recover credits first.
```

### 4.3 Event-Driven Architecture (Kafka)

```
┌──────────────────────────────────────────────────────────────┐
│                    Event-Driven Endorsement Flow              │
│                                                               │
│  Topic: endorsement.lifecycle                                 │
│  ┌──────────────────────────────────────────────────────┐     │
│  │ Events:                                              │     │
│  │  ENDORSEMENT_CREATED                                 │     │
│  │  ENDORSEMENT_VALIDATED                               │     │
│  │  PROVISIONAL_COVERAGE_GRANTED                        │     │
│  │  ENDORSEMENT_QUEUED_FOR_BATCH                        │     │
│  │  BATCH_SUBMITTED                                     │     │
│  │  INSURER_ACKNOWLEDGED                                │     │
│  │  ENDORSEMENT_CONFIRMED                               │     │
│  │  ENDORSEMENT_REJECTED                                │     │
│  │  EA_DEBITED / EA_CREDITED                            │     │
│  │  ENDORSEMENT_RETRY_SCHEDULED                         │     │
│  │  ENDORSEMENT_FAILED_PERMANENT                        │     │
│  └──────────────────────────────────────────────────────┘     │
│                                                               │
│  Partition Key: employer_id (ordering per employer)            │
│  Partitions: 32 (MVP) → 128 (scale) → 256 (global)           │
│  Retention: 30 days hot, S3 archive indefinite (audit)        │
│                                                               │
│  Consumer Groups:                                             │
│  ├── endorsement-processor (core state machine)               │
│  ├── ea-balance-tracker (financial ledger)                    │
│  ├── notification-sender (status updates to employers)        │
│  ├── reconciliation-engine (match sent vs confirmed)          │
│  ├── analytics-projector (dashboard read models)              │
│  └── audit-logger (compliance, immutable log)                 │
└──────────────────────────────────────────────────────────────┘
```

### 4.4 Automated Reconciliation Engine

```
Reconciliation runs continuously (every 15 minutes):

┌────────────────────────────────────────────────────────┐
│              Reconciliation Engine                       │
│                                                         │
│  Input Sources:                                         │
│  ├── Internal: endorsement DB (what we sent)            │
│  ├── Insurer: batch response files / API callbacks      │
│  └── EA Ledger: balance movements                       │
│                                                         │
│  Match Logic:                                           │
│  FOR each submitted endorsement:                        │
│    1. Check insurer response received? (by ref_id)      │
│    2. Match: insurer_ref ↔ internal_endorsement_id      │
│    3. Compare: sent_data vs confirmed_data              │
│       ├── MATCH → mark CONFIRMED, update coverage       │
│       ├── PARTIAL_MATCH → flag discrepancy, alert ops   │
│       ├── REJECTED → extract reason, trigger retry      │
│       └── MISSING → batch SLA exceeded? escalate        │
│                                                         │
│  Output:                                                │
│  ├── Reconciliation Report (per insurer, per employer)  │
│  ├── Discrepancy alerts (Slack/email to ops)            │
│  ├── Auto-retry queue for retriable failures            │
│  └── EA balance adjustments                             │
│                                                         │
│  SLA Monitoring:                                        │
│  IF batch_submit_time + insurer_sla < now:              │
│    AND no response received:                            │
│    → P0 Alert: "Batch #{id} overdue by {duration}"      │
│    → Auto-escalate to insurer relationship team         │
└────────────────────────────────────────────────────────┘
```

---

## 5. Phase 3 — Intelligence: AI/Automation & Predictive

### 5.1 AI/Automation Integration Points

```
┌─────────────────────────────────────────────────────────┐
│            AI/Automation Layer                            │
│                                                          │
│  1. ANOMALY DETECTION                                    │
│  ├── Unusual endorsement patterns:                       │
│  │   - Employer adds 50% of workforce in one day         │
│  │   - Same employee added/deleted multiple times         │
│  │   - Addition just before a known expensive procedure   │
│  ├── Model: Isolation Forest + rule-based hybrid          │
│  ├── Framework: LangChain4j agent for pattern analysis    │
│  ├── Training data: historical endorsement patterns       │
│  └── Action: Flag for review, don't auto-block            │
│                                                          │
│  2. PREDICTIVE EA BALANCE FORECASTING                    │
│  ├── Input: historical change rates, seasonal patterns,   │
│  │   company growth rate, industry benchmarks             │
│  ├── Model: Time-series forecasting (Prophet/ARIMA)       │
│  ├── Integration: Spring AI ChatClient for natural-       │
│  │   language forecast explanations to employers          │
│  ├── Output: "Employer X will need ₹Y in EA by date Z"   │
│  └── Action: Proactive top-up alerts 7 days in advance    │
│                                                          │
│  3. SMART BATCH OPTIMIZATION                             │
│  ├── Input: endorsement queue, insurer SLAs, EA balance,  │
│  │   insurer batch processing patterns                    │
│  ├── Model: Constraint optimization (OR-Tools/LP)         │
│  ├── Output: Optimal batch composition & timing           │
│  └── Goal: Minimize EA lock-up while maximizing coverage  │
│            speed                                          │
│                                                          │
│  4. AUTOMATED ERROR RESOLUTION                           │
│  ├── Pattern: 80% of rejections are data quality issues   │
│  │   (wrong DOB format, missing fields, invalid member ID)│
│  ├── Framework: LangChain4j RAG pipeline                  │
│  │   → Indexes insurer API docs + historical error fixes  │
│  │   → LLM suggests fix for new error patterns            │
│  │   → Human-in-the-loop approval for novel fixes         │
│  ├── Model: Classification of error types + auto-fix rules│
│  ├── Example: DOB "03-07-1990" rejected by insurer that   │
│  │   expects "1990-03-07" → auto-transform and retry      │
│  └── Target: 60% of rejections auto-resolved              │
│                                                          │
│  5. PROCESS MINING                                       │
│  ├── Input: Event stream of all endorsement lifecycle     │
│  ├── Analysis: Bottleneck identification, happy path %,   │
│  │   average time-in-state per insurer                    │
│  └── Output: Actionable insights for ops team             │
│                                                          │
│  AI/ML FRAMEWORK DETAILS:                                │
│  ├── Spring AI:                                          │
│  │   ├── ChatClient for LLM interactions (OpenAI/Anthropic│
│  │   │   via spring.ai.* config — provider-agnostic)      │
│  │   ├── EmbeddingClient for vectorizing endorsement data │
│  │   ├── VectorStore abstraction (backed by Elasticsearch)│
│  │   └── Function calling for structured tool use         │
│  │                                                       │
│  └── LangChain4j:                                        │
│      ├── AiServices for type-safe AI service interfaces   │
│      ├── RAG pipeline: insurer docs → Elasticsearch       │
│      │   vector store → retrieval → LLM augmented response│
│      ├── Memory: ChatMemoryProvider for multi-turn ops    │
│      │   conversations                                    │
│      └── Tools: @Tool annotated Java methods for          │
│          endorsement lookups, EA balance checks            │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Dashboard & Real-Time Visibility

```
┌─────────────────────────────────────────────────────────────┐
│  EMPLOYER DASHBOARD                                          │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  EA BALANCE         │  ENDORSEMENT PIPELINE              │ │
│  │                     │                                    │ │
│  │  Current: ₹4,50,000 │  Pending:    12                   │ │
│  │  Reserved: ₹1,20,000│  Processing: 45                   │ │
│  │  Available: ₹3,30,000│ Confirmed:  1,234 (this month)   │ │
│  │                     │  Failed:     3 ⚠️                  │ │
│  │  Forecast:          │                                    │ │
│  │  Next 30d need:     │  Avg Processing Time:              │ │
│  │  ₹2,80,000          │  ICICI: 45min (RT) | 18hr (batch) │ │
│  │                     │  Niva:  24hr (batch only)          │ │
│  │  [Top Up EA] button │  Bajaj: 2hr (RT) | 8hr (batch)    │ │
│  └─────────────────────┴────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  RECENT ENDORSEMENTS                                    │ │
│  │                                                         │ │
│  │  Employee       │ Type    │ Status      │ Coverage      │ │
│  │  ─────────────────────────────────────────────────────  │ │
│  │  Rahul Kumar    │ ADD     │ ✅ Confirmed │ Active        │ │
│  │  Priya Sharma   │ ADD     │ 🔄 Processing│ Provisional  │ │
│  │  Amit Patel     │ DELETE  │ ✅ Confirmed │ Ended Mar 31 │ │
│  │  Neha Singh     │ UPDATE  │ ⚠️ Failed    │ Active (old) │ │
│  │                 │         │ [Retry]     │              │ │
│  └─────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐ │
│  │  NOTIFICATIONS                                          │ │
│  │                                                         │ │
│  │  🔴 3 endorsements failed - action needed               │ │
│  │  🟡 EA balance will be insufficient in 12 days          │ │
│  │  🟢 Batch #4521 confirmed by ICICI (45 endorsements)    │ │
│  └─────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  PLUM OPS DASHBOARD (Internal)                               │
│                                                              │
│  System Health:                                              │
│  ├── Endorsements processed today: 847,231 / 1,000,000      │
│  ├── STP rate: 94.7%                                        │
│  ├── Avg endorsement lifecycle: 4.2 hours                   │
│  ├── Reconciliation match rate: 99.2%                       │
│  └── EA balance alerts active: 23 employers                 │
│                                                              │
│  Per Insurer:                                                │
│  ├── ICICI:  SLA compliance 98.5% | Rejection rate 1.2%     │
│  ├── Niva:   SLA compliance 92.1% | Rejection rate 3.4%     │
│  ├── Bajaj:  SLA compliance 97.8% | Rejection rate 1.8%     │
│  └── Digit:  SLA compliance 99.1% | Rejection rate 0.9%     │
│                                                              │
│  Anomaly Alerts:                                             │
│  ├── 🔴 Employer #4521: 200 additions in 1 hour (normal: 5) │
│  └── 🟡 Niva Bupa batch SLA breach: 3 batches > 36 hours    │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Phase 4 — Global: World-Class Platform

### 6.1 Multi-Region & Multi-Country

```
Phase 4 Capabilities:
├── Multi-region deployment (India, SEA, Middle East)
├── Multi-currency EA support
├── Country-specific regulatory compliance
│   ├── India: IRDAI regulations
│   ├── UAE: DHIC/DHA requirements
│   └── Singapore: MAS guidelines
├── Localized insurer integrations
├── Self-service insurer onboarding portal
│   ├── API spec upload (OpenAPI/WSDL)
│   ├── Field mapping UI
│   ├── Test sandbox with mock responses
│   └── Go-live checklist automation
├── Platform API marketplace
│   ├── HRIS integrations (Darwinbox, Keka, greytHR)
│   ├── Payroll system connectors
│   └── Benefits administration APIs
└── Advanced analytics
    ├── Cross-insurer performance benchmarking
    ├── Industry endorsement pattern insights
    └── Employer health score (endorsement efficiency)
```

---

## 7. C4 Architecture Model

### Level 1 — System Context

```
┌──────────────────────────────────────────────────────────────────┐
│                      SYSTEM CONTEXT                               │
│                                                                   │
│                                                                   │
│   ┌──────────┐    submits        ┌─────────────────────┐          │
│   │ Employer │──endorsements───►│                     │          │
│   │ (HR)     │◄──status/alerts──│                     │          │
│   └──────────┘                  │   ENDORSEMENT       │          │
│                                 │   MANAGEMENT        │          │
│   ┌──────────┐    views status  │   SYSTEM            │          │
│   │ Employee │──────────────────►│                     │          │
│   └──────────┘                  │                     │          │
│                                 │                     │          │
│   ┌──────────┐    ops/monitor   │                     │          │
│   │ Plum Ops │──────────────────►│                     │          │
│   └──────────┘                  └──┬──────┬───────┬──┘          │
│                                    │      │       │              │
│                                    │      │       │              │
│                  ┌─────────────────▼┐  ┌──▼────┐ ┌▼────────────┐│
│                  │ Insurance        │  │ HRIS  │ │ Payment     ││
│                  │ Providers        │  │Systems│ │ Gateway     ││
│                  │ (ICICI, Niva,    │  │       │ │ (EA top-up) ││
│                  │  Bajaj, Digit)   │  │       │ │             ││
│                  └──────────────────┘  └───────┘ └─────────────┘│
└──────────────────────────────────────────────────────────────────┘
```

### Level 2 — Container Diagram

```
┌──────────────────────────────────────────────────────────────────────┐
│                       CONTAINER DIAGRAM                               │
│                                                                       │
│  ┌─────────────┐     ┌──────────────────────────────────────────┐     │
│  │ React SPA   │────►│              API Gateway                 │     │
│  │ (Dashboard) │     │  (Auth, Rate Limit, Request Routing)     │     │
│  └─────────────┘     └─────┬──────┬──────┬──────┬──────┬──────┘     │
│                            │      │      │      │      │             │
│                 ┌──────────▼┐ ┌───▼────┐ │  ┌───▼────┐ │             │
│                 │Endorsement│ │EA      │ │  │Notif.  │ │             │
│                 │Service    │ │Balance │ │  │Service │ │             │
│                 │(Core)     │ │Service │ │  │        │ │             │
│                 └─────┬─────┘ └───┬────┘ │  └───┬────┘ │             │
│                       │           │      │      │      │             │
│                 ┌─────▼───────────▼──────▼──────▼──────▼───────┐     │
│                 │              Apache Kafka                      │     │
│                 │   (endorsement.lifecycle, ea.balance,          │     │
│                 │    notifications, reconciliation)              │     │
│                 └─────┬──────────┬──────────┬──────────────────┘     │
│                       │          │          │                         │
│               ┌───────▼──┐  ┌───▼─────┐  ┌─▼────────────────┐       │
│               │Insurer   │  │Reconcil.│  │Analytics         │       │
│               │Gateway   │  │Engine   │  │Projector (CQRS)  │       │
│               │Service   │  │         │  │                  │       │
│               └─────┬────┘  └────┬────┘  └────┬─────────────┘       │
│                     │            │             │                      │
│               ┌─────▼────┐  ┌───▼──┐    ┌─────▼──────────┐          │
│               │Insurer   │  │      │    │                │          │
│               │Adapters  │  │  PostgreSQL (Write/SoT)     │          │
│               │(per      │  │  Redis (Distributed Cache)  │          │
│               │ insurer) │  │  Caffeine (In-Memory L1)    │          │
│               └──────────┘  │  Elasticsearch (Search/     │          │
│                             │    Analytics/Vector Store)   │          │
│                             └──────────────────────────── ┘          │
└──────────────────────────────────────────────────────────────────────┘
```

### Level 3 — Component Diagram (Endorsement Service)

```
┌──────────────────────────────────────────────────────────────┐
│                 ENDORSEMENT SERVICE — Components              │
│                                                               │
│  ┌─────────────────┐    ┌─────────────────┐                   │
│  │ REST Controller  │    │ Event Consumer   │                   │
│  │ (Endorsement API)│    │ (Kafka Listener) │                   │
│  └────────┬────────┘    └────────┬─────────┘                   │
│           │                      │                              │
│  ┌────────▼──────────────────────▼──────────┐                  │
│  │         Endorsement Application Service   │                  │
│  │                                           │                  │
│  │  ├── EndorsementCommandHandler            │                  │
│  │  │   (create, validate, submit, retry)    │                  │
│  │  │                                        │                  │
│  │  ├── EndorsementStateMachine              │                  │
│  │  │   (state transitions, guard clauses)   │                  │
│  │  │                                        │                  │
│  │  ├── ProvisionalCoverageManager           │                  │
│  │  │   (grant/revoke provisional coverage)  │                  │
│  │  │                                        │                  │
│  │  ├── BatchAssembler                       │                  │
│  │  │   (group endorsements into batches,    │                  │
│  │  │    optimize sequencing)                │                  │
│  │  │                                        │                  │
│  │  └── EndorsementQueryHandler              │                  │
│  │      (status lookups, dashboard queries)  │                  │
│  └───────────┬──────────────┬───────────────┘                  │
│              │              │                                   │
│  ┌───────────▼──────┐  ┌───▼──────────────────┐               │
│  │ Domain Model      │  │ Ports (Interfaces)    │               │
│  │                   │  │                       │               │
│  │ Endorsement       │  │ EndorsementRepository │               │
│  │ EndorsementBatch  │  │ InsurerPort           │               │
│  │ ProvisionalCover  │  │ EABalancePort         │               │
│  │ EATransaction     │  │ NotificationPort      │               │
│  │ InsurerResponse   │  │ EventPublisherPort    │               │
│  └──────────────────┘  └───────────────────────┘               │
└──────────────────────────────────────────────────────────────┘
```

### Level 4 — Code Diagram (Key Classes)

```
┌─────────────────────────────────────────────────────────────┐
│           CODE-LEVEL: Domain Model (Java 21)                  │
│                                                              │
│  // Sealed interface for type-safe events (Java 21)          │
│  sealed interface EndorsementEvent {                         │
│      record Created(EndorsementId id, Instant at)            │
│          implements EndorsementEvent {}                       │
│      record Validated(EndorsementId id, Instant at)          │
│          implements EndorsementEvent {}                       │
│      record ProvisionalCoverageGranted(EndorsementId id,     │
│          EmployeeId empId, LocalDate coverageStart)          │
│          implements EndorsementEvent {}                       │
│      record Confirmed(EndorsementId id, String insurerRef)   │
│          implements EndorsementEvent {}                       │
│      record Rejected(EndorsementId id, String reason)        │
│          implements EndorsementEvent {}                       │
│  }                                                           │
│                                                              │
│  class Endorsement {                                         │
│      EndorsementId id;                                       │
│      EmployerId employerId;                                  │
│      EmployeeId employeeId;                                  │
│      EndorsementType type;     // enum: ADD, DELETE, UPDATE  │
│      EndorsementStatus status; // state machine enum         │
│      LocalDate coverageStartDate;                            │
│      InsurerId insurerId;                                    │
│      Money premium;                                          │
│      ProvisionalCoverage provisionalCoverage; // nullable    │
│      BatchId batchId;              // nullable               │
│      String insurerReference;      // nullable               │
│      int retryCount;                                         │
│      List<EndorsementEvent> events; // event sourced         │
│      Instant createdAt;                                      │
│      Instant updatedAt;                                      │
│                                                              │
│      ValidationResult validate();                            │
│      Endorsement transitionTo(EndorsementStatus newStatus);  │
│      boolean canRetry();                                     │
│  }                                                           │
│                                                              │
│  // Commands as Java 21 records (immutable DTOs)             │
│  record CreateEndorsementCommand(                            │
│      EmployerId employerId,                                  │
│      EmployeeId employeeId,                                  │
│      EndorsementType type,                                   │
│      JsonNode employeeData,                                  │
│      String idempotencyKey                                   │
│  ) {}                                                        │
│                                                              │
│  class EndorsementBatch {                                     │
│      BatchId id;                                             │
│      InsurerId insurerId;                                    │
│      List<Endorsement> endorsements;                         │
│      BatchStatus status; // ASSEMBLING|SUBMITTED|PARTIAL|COMPLETE│
│      Instant submittedAt;    // nullable                     │
│      Instant slaDeadline;    // nullable                     │
│      String insurerBatchRef; // nullable                     │
│                                                              │
│      List<Endorsement> optimizeSequencing(); // dels first   │
│      Money totalPremiumImpact();                             │
│  }                                                           │
│                                                              │
│  class EAAccount {                                           │
│      EmployerId employerId;                                  │
│      InsurerId insurerId;                                    │
│      Money balance;                                          │
│      Money reservedAmount; // for pending additions          │
│      List<EATransaction> transactions;                       │
│                                                              │
│      Money availableBalance() { return balance.subtract(reservedAmount); }│
│      boolean canFundEndorsement(Money premium);              │
│      EATransaction reserve(Money amount);                    │
│      EATransaction debit(Money amount);                      │
│      EATransaction credit(Money amount);                     │
│  }                                                           │
└─────────────────────────────────────────────────────────────┘
```

---

## 8. High-Level Design

### 8.1 System Flow — End-to-End Endorsement Processing

```
┌─────────────────────────────────────────────────────────────────────┐
│                    END-TO-END ENDORSEMENT FLOW                       │
│                                                                      │
│  HR Action          System Processing          Insurer                │
│  ─────────          ──────────────────          ───────               │
│                                                                      │
│  1. HR submits  ──► 2. Validate data   ──► 3. Check EA balance      │
│     employee           (schema, biz rules)     (sufficient funds?)   │
│     change                                                           │
│                     4. Grant provisional ──► 5. Employee covered     │
│                        coverage                 IMMEDIATELY          │
│                                                                      │
│                     6. Route to insurer:                              │
│                        ├── Real-time? ────► 7a. Call insurer API     │
│                        │                        ├── 200 OK → confirm │
│                        │                        └── Error → retry    │
│                        │                                             │
│                        └── Batch? ────────► 7b. Queue for batch     │
│                                                  │                   │
│                                             8. Assemble batch       │
│                                                (optimize sequencing) │
│                                                  │                   │
│                                             9. Submit batch ──────► │
│                                                                      │
│                     10. Poll/webhook for  ◄─── 11. Insurer processes │
│                         confirmation             (hours to days)     │
│                                                                      │
│                     12. Reconcile:                                    │
│                         ├── Confirmed → update coverage, debit EA    │
│                         ├── Rejected → retry or escalate             │
│                         └── Missing → SLA alert                      │
│                                                                      │
│                     13. Notify employer                               │
│                         (status update via dashboard + email)         │
│                                                                      │
│                     14. Update analytics                              │
│                         (dashboard projections)                       │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Data Flow Architecture

```
                  ┌─────────────────────────────┐
                  │        WRITE PATH            │
                  │     (Command Side)           │
                  │                              │
  API Request ───►│  Endorsement Service         │
                  │     │                        │
                  │     ▼                        │
                  │  PostgreSQL                  │──── Endorsement Events ───►  Kafka
                  │  (Source of Truth)            │                              │
                  │     │                        │                              │
                  │     ▼                        │                              │
                  │  EA Balance Service          │                              │
                  │  (Financial Ledger)          │                              │
                  └─────────────────────────────┘                              │
                                                                               │
                  ┌─────────────────────────────┐                              │
                  │        READ PATH             │◄─────────────────────────────┘
                  │     (Query Side — CQRS)      │
                  │                              │
  Dashboard  ◄────│  Caffeine (L1 In-Memory)     │
  Queries         │  ├── Hot endorsement status   │
                  │  ├── Insurer config cache     │
                  │  └── TTL: 30-60 seconds       │
                  │                              │
                  │  Redis (L2 Distributed Cache) │
                  │  ├── Materialized Views       │
                  │  ├── EA balance summary      │
                  │  ├── Pipeline metrics        │
                  │  └── Per-employer aggregates  │
                  │                              │
                  │  Elasticsearch 8 (Search +    │
                  │   Analytics + Vector Store)   │
                  │  ├── Full-text endorsement    │
                  │  │   search                   │
                  │  ├── Audit log search         │
                  │  ├── Analytics aggregations   │
                  │  └── Vector store for AI/RAG  │
                  └─────────────────────────────┘
```

### 8.3 Database Schema (Core Tables)

```sql
-- Core endorsement record
CREATE TABLE endorsements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id         UUID NOT NULL,
    employee_id         UUID NOT NULL,
    insurer_id          UUID NOT NULL,
    policy_id           UUID NOT NULL,
    type                VARCHAR(20) NOT NULL,  -- ADD, DELETE, UPDATE
    status              VARCHAR(30) NOT NULL,  -- state machine values
    coverage_start_date DATE NOT NULL,
    coverage_end_date   DATE,
    employee_data       JSONB NOT NULL,        -- flexible: name, DOB, dependents, etc.
    premium_amount      DECIMAL(12,2),
    batch_id            UUID,
    insurer_reference   VARCHAR(100),
    retry_count         INT DEFAULT 0,
    failure_reason      TEXT,
    idempotency_key     VARCHAR(100) UNIQUE,
    created_at          TIMESTAMPTZ DEFAULT now(),
    updated_at          TIMESTAMPTZ DEFAULT now(),
    version             INT DEFAULT 1         -- optimistic locking
) PARTITION BY HASH (employer_id);

-- Endorsement event log (event sourcing)
CREATE TABLE endorsement_events (
    id              BIGSERIAL PRIMARY KEY,
    endorsement_id  UUID NOT NULL REFERENCES endorsements(id),
    event_type      VARCHAR(50) NOT NULL,
    event_data      JSONB NOT NULL,
    actor           VARCHAR(100),
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_events_endorsement ON endorsement_events(endorsement_id);

-- EA Balance Ledger (double-entry bookkeeping)
CREATE TABLE ea_transactions (
    id              BIGSERIAL PRIMARY KEY,
    employer_id     UUID NOT NULL,
    insurer_id      UUID NOT NULL,
    endorsement_id  UUID REFERENCES endorsements(id),
    type            VARCHAR(20) NOT NULL,  -- DEBIT, CREDIT, RESERVE, RELEASE, TOP_UP
    amount          DECIMAL(12,2) NOT NULL,
    balance_after   DECIMAL(12,2) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX idx_ea_employer ON ea_transactions(employer_id, insurer_id);

-- EA Account summary (materialized)
CREATE TABLE ea_accounts (
    employer_id     UUID NOT NULL,
    insurer_id      UUID NOT NULL,
    balance         DECIMAL(12,2) NOT NULL DEFAULT 0,
    reserved        DECIMAL(12,2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (employer_id, insurer_id)
);

-- Batch tracking
CREATE TABLE endorsement_batches (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id      UUID NOT NULL,
    status          VARCHAR(20) NOT NULL,
    endorsement_count INT NOT NULL,
    total_premium   DECIMAL(12,2),
    submitted_at    TIMESTAMPTZ,
    sla_deadline    TIMESTAMPTZ,
    insurer_batch_ref VARCHAR(100),
    response_data   JSONB,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- Provisional coverage records
CREATE TABLE provisional_coverages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endorsement_id  UUID NOT NULL REFERENCES endorsements(id),
    employee_id     UUID NOT NULL,
    employer_id     UUID NOT NULL,
    coverage_start  DATE NOT NULL,
    coverage_type   VARCHAR(20) DEFAULT 'PROVISIONAL',
    confirmed_at    TIMESTAMPTZ,
    expired_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT now()
);
```

---

## 9. Low-Level Design

### 9.1 Endorsement Service — API Design

```yaml
openapi: 3.1.0
info:
  title: Endorsement Management API
  version: 1.0.0

paths:
  /api/v1/endorsements:
    post:
      summary: Create a new endorsement
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateEndorsementRequest'
      responses:
        '201':
          description: Endorsement created with provisional coverage
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EndorsementResponse'
        '400':
          description: Validation error
        '409':
          description: Duplicate (idempotency key conflict)

    get:
      summary: List endorsements with filters
      parameters:
        - name: employer_id
          in: query
          required: true
        - name: status
          in: query
          schema:
            type: array
            items:
              type: string
        - name: type
          in: query
        - name: from_date
          in: query
        - name: to_date
          in: query
        - name: page
          in: query
        - name: size
          in: query

  /api/v1/endorsements/{id}:
    get:
      summary: Get endorsement details with full event history

  /api/v1/endorsements/{id}/retry:
    post:
      summary: Retry a failed endorsement

  /api/v1/endorsements/bulk:
    post:
      summary: Bulk create endorsements (CSV/JSON upload)
      description: >
        Accepts up to 1000 endorsements in a single request.
        Returns a bulk_operation_id for tracking.

  /api/v1/employers/{id}/ea-balance:
    get:
      summary: Get EA balance summary (current, reserved, available)

  /api/v1/employers/{id}/ea-balance/forecast:
    get:
      summary: Get EA balance forecast for next N days

  /api/v1/employers/{id}/ea-balance/top-up:
    post:
      summary: Initiate EA top-up via payment gateway

  /api/v1/batches:
    get:
      summary: List batches with status (per insurer)

  /api/v1/batches/{id}:
    get:
      summary: Get batch details with individual endorsement statuses

  /api/v1/dashboard/employer/{id}:
    get:
      summary: Aggregated dashboard data (pipeline, balance, alerts)

  /api/v1/dashboard/ops:
    get:
      summary: Internal ops dashboard (system-wide metrics)

  # Webhook endpoint for insurer callbacks
  /api/v1/webhooks/insurer/{insurer_id}:
    post:
      summary: Receive insurer confirmation/rejection callbacks
      security:
        - webhookHMAC: []
```

### 9.2 Batch Assembly & Submission Flow (Detailed)

```
┌───────────────────────────────────────────────────────────────┐
│           BATCH ASSEMBLY — Scheduled Job (every 15 min)       │
│                                                               │
│  Input: All endorsements in status QUEUED_FOR_BATCH           │
│         grouped by insurer_id                                 │
│                                                               │
│  FOR each insurer:                                            │
│    1. Check: Is insurer currently processing a batch?         │
│       ├── YES → Skip (insurer constraint: one batch at a time)│
│       └── NO  → Proceed                                       │
│                                                               │
│    2. Fetch pending endorsements for this insurer             │
│       ORDER BY:                                               │
│         type = 'DELETE' ASC,    -- deletions first (EA optim) │
│         type = 'UPDATE' ASC,    -- neutral updates next       │
│         coverage_start_date ASC -- earliest eligibility first │
│                                                               │
│    3. Check EA balance per employer:                          │
│       FOR each employer's additions in this batch:            │
│         IF ea_available < total_addition_premiums:            │
│           → Include only what can be funded                   │
│           → Alert employer about insufficient balance         │
│                                                               │
│    4. Respect insurer batch size limits:                      │
│       IF count > insurer.maxBatchSize:                        │
│         → Split into multiple batches                         │
│                                                               │
│    5. Transform to insurer format:                            │
│       adapter.mapToInsurerFormat(endorsements)                │
│                                                               │
│    6. Submit batch:                                           │
│       adapter.submitBatch(formattedBatch)                     │
│       ├── SUCCESS → Status = BATCH_SUBMITTED                  │
│       │             Set sla_deadline = now + insurer.batchSLA  │
│       │             Reserve EA funds for additions            │
│       └── FAILURE → Retry with backoff                        │
│                     Alert ops after 3 failures                │
└───────────────────────────────────────────────────────────────┘
```

### 9.3 Retry & Error Handling Strategy

```
┌───────────────────────────────────────────────────────────────┐
│                    ERROR CLASSIFICATION                        │
│                                                               │
│  TRANSIENT (auto-retry):                                      │
│  ├── HTTP 429 (rate limited) → backoff: 30s, 60s, 120s       │
│  ├── HTTP 500/502/503 (server error) → backoff: 5s, 15s, 45s │
│  ├── Timeout → backoff: 10s, 30s, 90s                        │
│  └── Network error → backoff: 5s, 15s, 45s                   │
│                                                               │
│  DATA QUALITY (auto-fix where possible):                      │
│  ├── Date format mismatch → transform and retry               │
│  ├── Missing optional field → populate default and retry      │
│  ├── Invalid member ID → lookup correct ID and retry          │
│  └── Duplicate submission → deduplicate and skip              │
│                                                               │
│  BUSINESS (requires human intervention):                      │
│  ├── Employee not found in insurer system → escalate to ops   │
│  ├── Policy expired/inactive → alert employer                 │
│  ├── Premium mismatch → reconcile manually                    │
│  └── Regulatory hold → compliance review                      │
│                                                               │
│  PERMANENT (fail, alert, manual resolution):                  │
│  ├── Max retries exhausted (3 attempts)                       │
│  ├── Insurer explicitly rejected with non-retriable reason    │
│  └── Data integrity violation                                 │
│                                                               │
│  Retry Policy:                                                │
│  ├── Max retries: 3 (configurable per insurer)                │
│  ├── Backoff: exponential with jitter                         │
│  ├── Dead Letter Queue after max retries                      │
│  └── DLQ review: ops team daily, auto-alert if DLQ depth > 50│
└───────────────────────────────────────────────────────────────┘
```

### 9.4 Idempotency Design

```
Every endorsement creation requires an idempotency key:

  Idempotency-Key: {employer_id}:{employee_id}:{type}:{date}:{hash}

Implementation:
  1. Client sends request with Idempotency-Key header
  2. Server checks Redis: EXISTS idempotency:{key}
     ├── EXISTS → Return cached response (HTTP 200, not 201)
     └── NOT EXISTS → Process request
         a. SET idempotency:{key} = "processing" EX 300 NX
         b. Process endorsement
         c. SET idempotency:{key} = {response_json} EX 86400
         d. Return HTTP 201

  For batch submissions:
    Idempotency at batch level: batch content hash
    Prevents duplicate batch submissions during retries
```

---

## 10. Coding Plan

### 10.1 Module Structure

```
plum-endorsements/
├── src/main/java/com/plum/endorsements/
│   │
│   ├── EndorsementApplication.java                # Spring Boot 3.5 entry point
│   │
│   ├── domain/                                    # Domain Model (no framework deps)
│   │   ├── model/
│   │   │   ├── Endorsement.java                   # Core aggregate (sealed interface for status)
│   │   │   ├── EndorsementBatch.java              # Batch aggregate
│   │   │   ├── EndorsementStatus.java             # State machine enum (Java 21 sealed)
│   │   │   ├── EndorsementType.java               # ADD, DELETE, UPDATE
│   │   │   ├── EndorsementEvent.java              # Event types (sealed interface + records)
│   │   │   ├── ProvisionalCoverage.java           # Coverage record
│   │   │   ├── EAAccount.java                     # EA balance aggregate
│   │   │   └── EATransaction.java                 # Ledger entry
│   │   ├── service/
│   │   │   ├── EndorsementStateMachine.java       # State transition logic (pattern matching)
│   │   │   ├── BatchSequencingOptimizer.java      # EA optimization algorithm
│   │   │   └── EABalanceCalculator.java           # Balance computation
│   │   └── port/
│   │       ├── EndorsementRepository.java         # Persistence port (interface)
│   │       ├── InsurerPort.java                   # Insurer integration port (interface)
│   │       ├── EABalancePort.java                 # Balance operations port (interface)
│   │       ├── EventPublisherPort.java            # Event publishing port (interface)
│   │       └── NotificationPort.java              # Notification port (interface)
│   │
│   ├── application/                               # Use Cases / Application Services
│   │   ├── command/
│   │   │   ├── CreateEndorsementCommand.java      # Java 21 record
│   │   │   ├── SubmitEndorsementCommand.java      # Java 21 record
│   │   │   ├── RetryEndorsementCommand.java       # Java 21 record
│   │   │   ├── ProcessBatchResponseCommand.java   # Java 21 record
│   │   │   └── TopUpEACommand.java                # Java 21 record
│   │   ├── query/
│   │   │   ├── GetEndorsementQuery.java           # Java 21 record
│   │   │   ├── ListEndorsementsQuery.java         # Java 21 record
│   │   │   ├── GetEABalanceQuery.java             # Java 21 record
│   │   │   ├── GetDashboardQuery.java             # Java 21 record
│   │   │   └── ForecastEABalanceQuery.java        # Java 21 record
│   │   ├── handler/
│   │   │   ├── EndorsementCommandHandler.java     # Orchestrates commands
│   │   │   └── EndorsementQueryHandler.java       # Orchestrates queries
│   │   └── scheduler/
│   │       ├── BatchAssemblyScheduler.java        # Periodic batch assembly
│   │       ├── ReconciliationScheduler.java       # Periodic reconciliation
│   │       └── SLAMonitorScheduler.java           # SLA breach detection
│   │
│   ├── infrastructure/                            # Adapters (Hexagonal)
│   │   ├── persistence/
│   │   │   ├── JpaEndorsementRepository.java
│   │   │   ├── JpaEAAccountRepository.java
│   │   │   ├── entity/                            # JPA entities
│   │   │   └── mapper/                            # Entity ↔ Domain mappers (MapStruct)
│   │   ├── messaging/
│   │   │   ├── KafkaEventPublisher.java
│   │   │   ├── KafkaEventConsumer.java
│   │   │   └── config/KafkaConfig.java
│   │   ├── insurer/
│   │   │   ├── InsurerAdapterFactory.java         # Factory for adapters
│   │   │   ├── ICICILombardAdapter.java
│   │   │   ├── NivaBupaAdapter.java
│   │   │   ├── BajajAllianzAdapter.java
│   │   │   ├── DigitAdapter.java
│   │   │   └── mock/MockInsurerAdapter.java       # For testing
│   │   ├── notification/
│   │   │   ├── EmailNotificationAdapter.java
│   │   │   ├── SlackNotificationAdapter.java
│   │   │   └── WebhookNotificationAdapter.java
│   │   ├── cache/
│   │   │   ├── RedisIdempotencyStore.java         # Redis (distributed)
│   │   │   ├── RedisDashboardCache.java           # Redis (CQRS read model)
│   │   │   └── CaffeineCacheConfig.java           # Caffeine (L1 in-memory)
│   │   ├── search/
│   │   │   ├── ElasticsearchEndorsementIndex.java # ES indexing
│   │   │   └── ElasticsearchQueryAdapter.java     # ES search queries
│   │   ├── ai/
│   │   │   ├── SpringAiAnomalyService.java        # Spring AI ChatClient integration
│   │   │   ├── LangChain4jErrorResolver.java      # LangChain4j RAG for error resolution
│   │   │   └── config/AiConfig.java               # AI provider configuration
│   │   ├── tracing/
│   │   │   └── OtelTracingConfig.java             # OpenTelemetry configuration
│   │   └── config/
│   │       ├── SecurityConfig.java
│   │       ├── RedisConfig.java
│   │       ├── ElasticsearchConfig.java
│   │       └── SchedulerConfig.java
│   │
│   └── api/                                       # REST Controllers
│       ├── EndorsementController.java
│       ├── EABalanceController.java
│       ├── BatchController.java
│       ├── DashboardController.java
│       ├── WebhookController.java                 # Insurer callbacks
│       ├── dto/                                   # Request/Response DTOs (Java records)
│       └── exception/                             # @ControllerAdvice exception handlers
│
├── src/test/java/com/plum/endorsements/
│   ├── domain/                                    # Unit tests (JUnit 5, pure logic)
│   ├── application/                               # Integration tests (Testcontainers)
│   ├── infrastructure/                            # Adapter tests
│   ├── api/                                       # API contract tests (@WebMvcTest)
│   └── e2e/                                       # End-to-end flows
│
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   ├── application-staging.yml
│   ├── application-prod.yml
│   └── db/migration/                              # Flyway migrations
│       ├── V1__create_endorsements.sql
│       ├── V2__create_ea_accounts.sql
│       ├── V3__create_batches.sql
│       └── V4__create_provisional_coverages.sql
│
├── frontend/                                      # React SPA (separate build)
│   ├── src/
│   │   ├── components/
│   │   │   ├── Dashboard/
│   │   │   ├── EndorsementList/
│   │   │   ├── EABalance/
│   │   │   └── common/
│   │   ├── hooks/                                 # Custom React hooks (useWebSocket, etc.)
│   │   ├── services/                              # API client (axios/fetch)
│   │   ├── store/                                 # State management (Zustand or Redux Toolkit)
│   │   └── App.tsx
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── Dockerfile                                 # Nginx serving built React assets
│
├── build.gradle.kts
├── settings.gradle.kts
├── docker-compose.yml                             # Local dev: PostgreSQL, Redis, Kafka,
│                                                  #   Elasticsearch, Jaeger (OTel backend)
├── Dockerfile                                     # Multi-stage: Gradle build → JRE 21 slim
├── railway.toml                                   # Railway deployment config
└── k8s/                                           # Kubernetes manifests (portable)
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    └── hpa.yaml
```

**Java 21 Features Leveraged:**
- **Virtual Threads**: `spring.threads.virtual.enabled=true` — eliminates thread-pool sizing for blocking I/O (insurer API calls, DB queries)
- **Record Types**: All DTOs, commands, queries, and events as records (immutable, compact)
- **Sealed Interfaces**: `EndorsementEvent` as sealed interface with record implementations for exhaustive pattern matching
- **Pattern Matching** (`switch` expressions): State machine transitions with compile-time exhaustiveness checking
- **Text Blocks**: Multi-line SQL, JSON templates for insurer payloads

### 10.2 Implementation Order (Sprint Plan)

```
Sprint 1 (Week 1-2): Foundation
├── [x] Project setup (Gradle, Spring Boot 3.5.x, Java 21)
├── [ ] Docker Compose: PostgreSQL 16, Redis 7, Kafka (KRaft),
│       Elasticsearch 8, Jaeger (OTel backend)
├── [ ] Domain model: Endorsement, EndorsementStatus (sealed),
│       EndorsementEvent (sealed interface + records)
├── [ ] State machine implementation (Java 21 pattern matching switch)
├── [ ] Database schema (Flyway migrations V1-V4)
├── [ ] EndorsementRepository (Spring Data JPA)
├── [ ] Create Endorsement API (POST /endorsements)
├── [ ] Get Endorsement API (GET /endorsements/{id})
├── [ ] List Endorsements API (GET /endorsements)
├── [ ] Idempotency middleware (Redis)
├── [ ] Caffeine L1 cache for insurer config lookups
├── [ ] OpenTelemetry auto-instrumentation (Spring Boot starter)
├── [ ] Unit tests for state machine, domain model (JUnit 5)
└── [ ] Enable virtual threads (spring.threads.virtual.enabled=true)

Sprint 2 (Week 3-4): Core Processing + Kafka Events
├── [ ] Kafka event publishing for all state transitions
│       (spring-kafka, endorsement.lifecycle topic)
├── [ ] ProvisionalCoverage creation on endorsement validation
├── [ ] EAAccount and EATransaction domain model
├── [ ] EA balance check before endorsement processing
├── [ ] Mock insurer adapter (simulates real-time + batch)
├── [ ] Real-time endorsement submission flow
├── [ ] Batch assembly scheduler (15-min intervals)
├── [ ] Batch submission flow with mock insurer
├── [ ] Webhook endpoint for insurer callbacks
├── [ ] Retry logic (exponential backoff, Resilience4j)
├── [ ] CQRS read model (Redis materialized views via Kafka consumers)
├── [ ] Elasticsearch indexing (endorsement search)
├── [ ] Integration tests (Testcontainers: PG, Redis, Kafka, ES)
├── [ ] React dashboard scaffold (Vite + TypeScript + React 18)
├── [ ] Basic dashboard: endorsement status list view
└── [ ] Deploy to Railway (Docker-based, staging environment)

Sprint 3 (Week 5-6): Multi-Insurer & Reconciliation
├── [ ] InsurerPort interface + adapter factory
├── [ ] First real insurer adapter (ICICI or Digit)
├── [ ] Second insurer adapter (Niva Bupa — batch/SFTP)
├── [ ] Batch sequencing optimizer (deletions first — EA optimization)
├── [ ] Reconciliation engine (scheduled, 15-min, Kafka-driven)
├── [ ] SLA monitoring scheduler with alerts
├── [ ] Notification service (email alerts via SES/SendGrid)
├── [ ] Dashboard: EA balance view, pipeline funnel, per-insurer metrics
├── [ ] Dashboard: WebSocket real-time updates (Spring WebSocket + SockJS)
├── [ ] OpenTelemetry custom spans for business operations
├── [ ] Contract tests for insurer adapters (Spring Cloud Contract)
└── [ ] Caffeine + Redis two-tier cache for dashboard queries

Sprint 4 (Week 7-8): AI/Intelligence & Production Readiness
├── [ ] Spring AI integration (ChatClient for anomaly explanations)
├── [ ] LangChain4j RAG pipeline (insurer docs → ES vector store →
│       error resolution suggestions)
├── [ ] EA balance forecast (time-series + Spring AI natural language)
├── [ ] Anomaly detection (rule-based + LangChain4j agent analysis)
├── [ ] Error auto-resolution (LangChain4j @Tool methods)
├── [ ] Third + fourth insurer adapters
├── [ ] Bulk endorsement upload (CSV)
├── [ ] Advanced ops dashboard (system health, anomaly alerts)
├── [ ] Performance testing (Gatling — 10K endorsements/day target)
├── [ ] Kubernetes manifests (deployment, service, HPA, configmap)
├── [ ] Production deployment to Railway (Docker + K8s ready)
└── [ ] Runbook + operational documentation
```

---

## 11. Design Patterns

### 11.1 Patterns Used and Why

```
┌──────────────────────────────────────────────────────────────────┐
│  PATTERN                  │ WHERE USED           │ WHY           │
├───────────────────────────┼──────────────────────┼───────────────┤
│                           │                      │               │
│  State Machine            │ Endorsement          │ Complex life- │
│                           │ lifecycle            │ cycle with    │
│                           │                      │ strict valid  │
│                           │                      │ transitions   │
│                           │                      │               │
│  Hexagonal Architecture   │ Entire service       │ Swap insurer  │
│  (Ports & Adapters)       │ structure            │ adapters,     │
│                           │                      │ databases,    │
│                           │                      │ message       │
│                           │                      │ brokers       │
│                           │                      │ without       │
│                           │                      │ domain change │
│                           │                      │               │
│  CQRS                     │ Write (PostgreSQL)   │ Dashboard     │
│                           │ vs Read (Redis)      │ reads are     │
│                           │                      │ different     │
│                           │                      │ shape than    │
│                           │                      │ write model   │
│                           │                      │               │
│  Event Sourcing           │ Endorsement event    │ Full audit    │
│                           │ log                  │ trail, replay │
│                           │                      │ capability,   │
│                           │                      │ insurance     │
│                           │                      │ compliance    │
│                           │                      │               │
│  Adapter Pattern          │ Insurer integrations │ Each insurer  │
│                           │                      │ has different  │
│                           │                      │ API, format,  │
│                           │                      │ protocol      │
│                           │                      │               │
│  Factory Pattern          │ InsurerAdapterFactory│ Runtime       │
│                           │                      │ selection of  │
│                           │                      │ correct       │
│                           │                      │ adapter       │
│                           │                      │               │
│  Strategy Pattern         │ BatchSequencing      │ Different     │
│                           │ Optimizer            │ optimization  │
│                           │                      │ strategies    │
│                           │                      │ per insurer   │
│                           │                      │               │
│  Observer (Event-Driven)  │ Kafka consumers      │ Decouple      │
│                           │                      │ endorsement   │
│                           │                      │ processing    │
│                           │                      │ from notif,   │
│                           │                      │ analytics,    │
│                           │                      │ reconciliation│
│                           │                      │               │
│  Circuit Breaker          │ Insurer API calls    │ Prevent       │
│                           │                      │ cascade       │
│                           │                      │ failure when  │
│                           │                      │ insurer is    │
│                           │                      │ down          │
│                           │                      │               │
│  Idempotency Key          │ Endorsement creation │ Prevent       │
│                           │ & batch submission   │ duplicate     │
│                           │                      │ processing    │
│                           │                      │ from retries  │
│                           │                      │               │
│  Saga Pattern             │ Endorsement + EA     │ Distributed   │
│  (Choreography-based)     │ balance + insurer    │ transaction   │
│                           │ submission           │ across        │
│                           │                      │ services      │
│                           │                      │               │
│  Domain Events            │ All state transitions│ Business      │
│                           │                      │ event         │
│                           │                      │ notification  │
│                           │                      │ without       │
│                           │                      │ coupling      │
│                           │                      │               │
│  Specification Pattern    │ Endorsement          │ Complex       │
│                           │ validation rules     │ business      │
│                           │                      │ rules as      │
│                           │                      │ composable    │
│                           │                      │ predicates    │
│                           │                      │               │
│  Anti-Corruption Layer    │ Insurer data mapping │ Prevent       │
│                           │                      │ insurer data  │
│                           │                      │ model from    │
│                           │                      │ leaking into  │
│                           │                      │ domain        │
└──────────────────────────────────────────────────────────────────┘
```

---

## 12. Architectural Patterns

### 12.1 Patterns Applied

```
┌──────────────────────────────────────────────────────────────────┐
│  ARCHITECTURAL PATTERN     │ APPLICATION                         │
├────────────────────────────┼─────────────────────────────────────┤
│                            │                                     │
│  Event-Driven Architecture │ Kafka as the backbone for all       │
│                            │ endorsement lifecycle events.       │
│                            │ Enables loose coupling between      │
│                            │ endorsement processing,             │
│                            │ reconciliation, notification,       │
│                            │ and analytics.                      │
│                            │                                     │
│  Microservices             │ Phase 2+: Endorsement Service,      │
│  (Evolutionary)            │ EA Balance Service, Insurer Gateway,│
│                            │ Notification Service, Dashboard     │
│                            │ BFF split as scale demands.         │
│                            │ Phase 1: Modular monolith.          │
│                            │                                     │
│  CQRS (Command Query       │ Write path: PostgreSQL (ACID for    │
│  Responsibility            │ financial data). Read path: Caffeine│
│  Segregation)              │ (L1) + Redis (L2 distributed) +    │
│                            │ Elasticsearch (search/analytics)    │
│                            │ for dashboard and search queries.   │
│                            │                                     │
│  Event Sourcing             │ Every endorsement state change is   │
│                            │ an immutable event. Full audit       │
│                            │ trail for insurance compliance.      │
│                            │ Enables rebuilding state from        │
│                            │ events.                              │
│                            │                                     │
│  Strangler Fig             │ Phase 1 starts as modular monolith. │
│  (for evolution)           │ Phase 2-4 progressively extracts     │
│                            │ services. API Gateway routes         │
│                            │ traffic. No big-bang rewrite.        │
│                            │                                     │
│  Hexagonal Architecture    │ Domain logic is framework-agnostic.  │
│  (Ports & Adapters)        │ Infrastructure concerns (DB, Kafka,  │
│                            │ insurer APIs) are adapters that      │
│                            │ implement domain ports.              │
│                            │                                     │
│  Saga Pattern              │ Endorsement creation involves:       │
│  (Choreography)            │ validate → check EA → grant          │
│                            │ provisional → submit to insurer →    │
│                            │ confirm/compensate. Each step is     │
│                            │ an event. Compensating actions on    │
│                            │ failure (release EA reserve, revoke  │
│                            │ provisional coverage after timeout). │
│                            │                                     │
│  Bulkhead                  │ Insurer adapter calls isolated per   │
│                            │ insurer. ICICI being down does not   │
│                            │ affect Bajaj processing. Separate    │
│                            │ thread pools / connection pools.     │
│                            │                                     │
│  Back-Pressure             │ Kafka consumer lag triggers          │
│                            │ autoscaling. Batch assembly          │
│                            │ respects insurer rate limits.        │
│                            │ EA balance checks prevent            │
│                            │ unfunded endorsements.               │
│                            │                                     │
│  Gateway Pattern           │ Insurer Gateway Service abstracts    │
│                            │ all insurer communication behind     │
│                            │ a unified internal API. Core         │
│                            │ endorsement service never talks      │
│                            │ directly to insurers.                │
│                            │                                     │
│  Sidecar Pattern           │ Observability (OTel collector),      │
│  (Phase 3+)                │ security (mTLS proxy), and log       │
│                            │ forwarding as K8s sidecars.          │
└──────────────────────────────────────────────────────────────────┘
```

---

## 13. Testing Strategy

### 13.1 Testing Pyramid

```
                          ┌───────────┐
                          │  E2E      │  5%   — Critical user journeys
                          │  Tests    │        (endorsement lifecycle)
                         ┌┴───────────┴┐
                         │ Integration  │ 25%  — Service boundaries,
                         │ Tests        │       DB, Kafka, Insurer mocks
                        ┌┴──────────────┴┐
                        │  Component     │ 20%  — API contract tests,
                        │  Tests         │       Spring context tests
                       ┌┴────────────────┴┐
                       │   Unit Tests      │ 50% — Domain logic,
                       │                   │       state machine,
                       │                   │       optimization algo
                       └───────────────────┘
```

### 13.2 Testing by Layer

```
UNIT TESTS (50% — fast, no I/O, pure logic):
├── EndorsementStateMachineTest
│   ├── testValidTransitions (CREATED → VALIDATED → PROVISIONALLY_COVERED → ...)
│   ├── testInvalidTransitions (CONFIRMED → CREATED should throw)
│   └── testGuardClauses (cannot submit without validation)
├── BatchSequencingOptimizerTest
│   ├── testDeletionsBeforeAdditions
│   ├── testEABalanceConstraint (skip additions when insufficient)
│   ├── testEligibilityDateOrdering
│   └── testBatchSizeLimits
├── EABalanceCalculatorTest
│   ├── testDebitOnAddition
│   ├── testCreditOnDeletion
│   ├── testReserveAndRelease
│   └── testInsufficientBalance
├── EndorsementValidatorTest
│   ├── testValidPayload
│   ├── testMissingRequiredFields
│   ├── testInvalidDateRanges
│   └── testDuplicateDetection

COMPONENT TESTS (20% — Spring context, mocked external deps):
├── EndorsementControllerTest (@WebMvcTest)
│   ├── testCreateEndorsement_201
│   ├── testCreateEndorsement_400_validation
│   ├── testCreateEndorsement_409_duplicate
│   ├── testGetEndorsement_200
│   ├── testGetEndorsement_404
│   └── testListEndorsements_pagination
├── WebhookControllerTest
│   ├── testInsurerCallback_validHMAC
│   └── testInsurerCallback_invalidHMAC_401

INTEGRATION TESTS (25% — real DB/Kafka/Redis via Testcontainers):
├── EndorsementLifecycleIntegrationTest
│   ├── testFullAdditionLifecycle (create → validate → submit → confirm)
│   ├── testFullDeletionLifecycle
│   ├── testBatchAssemblyAndSubmission
│   ├── testRetryOnTransientFailure
│   └── testDLQAfterMaxRetries
├── EABalanceIntegrationTest
│   ├── testDebitCreditConsistency
│   ├── testConcurrentBalanceUpdates (optimistic locking)
│   └── testReservationRelease
├── ReconciliationIntegrationTest
│   ├── testMatchingConfirmation
│   ├── testDiscrepancyDetection
│   └── testSLABreachAlert
├── KafkaEventFlowTest
│   ├── testEventPublishedOnStateChange
│   ├── testConsumerGroupProcessing
│   └── testEventOrdering (per employer_id partition)

END-TO-END TESTS (5% — full system, staging environment):
├── EmployerAddsEmployeeE2E
│   ├── HR submits addition → provisional coverage granted → insurer confirms → EA debited
├── EmployerDeletesEmployeeE2E
│   ├── HR submits deletion → insurer confirms → EA credited
├── BatchProcessingE2E
│   ├── Multiple endorsements → batch assembled → submitted → confirmed
├── FailureAndRetryE2E
│   ├── Insurer rejects → auto-retry → success on second attempt
└── InsufficientEABalanceE2E
    ├── Addition submitted → EA insufficient → employer alerted → top-up → processed

PERFORMANCE / LOAD TESTS (separate pipeline):
├── Tool: k6 or Gatling
├── Scenarios:
│   ├── Steady state: 100 endorsements/sec sustained for 1 hour
│   ├── Spike: 0 → 500 endorsements/sec in 30 seconds
│   ├── Batch storm: 10,000 endorsements queued, batch assembly under load
│   └── Dashboard under load: 500 concurrent dashboard users
├── Targets:
│   ├── API P99 < 200ms (create endorsement)
│   ├── API P99 < 100ms (get endorsement)
│   ├── Batch assembly < 30 seconds for 1000 endorsements
│   └── Dashboard refresh < 500ms

CONTRACT TESTS (insurer API compatibility):
├── Tool: Pact or Spring Cloud Contract
├── Provider contracts for each insurer adapter
├── Consumer contracts for our webhook endpoint
└── Run on every insurer adapter change
```

---

## 14. Observability Strategy

### 14.1 Three Pillars

```
┌─────────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY STACK                            │
│                                                                  │
│  METRICS (Prometheus + Grafana)                                  │
│  ├── Business Metrics:                                           │
│  │   ├── endorsements_created_total (counter, by type/insurer)   │
│  │   ├── endorsements_confirmed_total (counter)                  │
│  │   ├── endorsements_failed_total (counter, by failure_reason)  │
│  │   ├── endorsement_processing_duration_seconds (histogram)     │
│  │   ├── ea_balance_current (gauge, by employer/insurer)         │
│  │   ├── batch_size (histogram, by insurer)                      │
│  │   ├── batch_sla_compliance_ratio (gauge, by insurer)          │
│  │   ├── provisional_coverage_active (gauge)                     │
│  │   └── reconciliation_match_rate (gauge)                       │
│  │                                                               │
│  │── Technical Metrics:                                          │
│  │   ├── http_request_duration_seconds (histogram, by endpoint)  │
│  │   ├── kafka_consumer_lag (gauge, by consumer group)           │
│  │   ├── insurer_api_duration_seconds (histogram, by insurer)    │
│  │   ├── insurer_api_error_rate (counter, by insurer/status)     │
│  │   ├── db_connection_pool_active (gauge)                       │
│  │   ├── redis_operations_total (counter)                        │
│  │   ├── caffeine_cache_hit_rate (gauge, by cache name)          │
│  │   ├── caffeine_cache_eviction_count (counter)                 │
│  │   ├── elasticsearch_index_latency (histogram)                 │
│  │   ├── elasticsearch_search_latency (histogram)                │
│  │   └── jvm_memory_used_bytes, jvm_gc_pause_seconds,            │
│  │       jvm_threads_virtual_active (Java 21 virtual threads)    │
│  │                                                               │
│  LOGGING (Structured JSON → Elasticsearch/Loki)                  │
│  ├── Format: JSON with mandatory fields:                         │
│  │   {                                                           │
│  │     "timestamp": "ISO-8601",                                  │
│  │     "level": "INFO",                                          │
│  │     "service": "endorsement-service",                         │
│  │     "trace_id": "abc123",                                     │
│  │     "span_id": "def456",                                      │
│  │     "employer_id": "uuid",                                    │
│  │     "endorsement_id": "uuid",                                 │
│  │     "message": "Endorsement confirmed by insurer",            │
│  │     "insurer": "icici",                                       │
│  │     "duration_ms": 45                                         │
│  │   }                                                           │
│  │                                                               │
│  ├── PII Redaction: employee name, DOB, Aadhaar masked at ingest│
│  ├── Retention: 30 days hot (Elasticsearch), 1 year cold (S3)   │
│  └── Correlation: trace_id links logs across services            │
│                                                                  │
│  TRACING (see Section 15 for detailed strategy)                  │
│  └── OpenTelemetry → Jaeger/Tempo                                │
│                                                                  │
│  ALERTING (PagerDuty/OpsGenie):                                  │
│  ├── P0 (Page immediately):                                      │
│  │   ├── Endorsement service down (health check fails > 2 min)   │
│  │   ├── Kafka consumer lag > 10,000 (processing stuck)          │
│  │   ├── EA balance computation error rate > 5%                  │
│  │   └── Zero endorsements processed in 30 min (during biz hrs) │
│  │                                                               │
│  ├── P1 (Alert within 15 min):                                   │
│  │   ├── Insurer API error rate > 10% for 5 min                  │
│  │   ├── Batch SLA breach (any insurer)                          │
│  │   ├── DLQ depth > 50                                          │
│  │   └── Reconciliation match rate < 95%                         │
│  │                                                               │
│  ├── P2 (Alert, investigate during business hours):              │
│  │   ├── Endorsement P99 latency > 500ms                        │
│  │   ├── Dashboard query latency > 2s                            │
│  │   └── Provisional coverage active > 30 days (stale)          │
│  │                                                               │
│  DASHBOARDS (Grafana):                                           │
│  ├── System Overview: Health, throughput, error rates             │
│  ├── Per-Insurer: SLA compliance, rejection rates, latency       │
│  ├── EA Balance: Top employers by balance, low-balance alerts    │
│  ├── Pipeline: Endorsement funnel (created→confirmed), bottlenecks│
│  └── On-Call: Active alerts, recent incidents, DLQ status        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 15. Distributed Tracing Strategy

### 15.1 Trace Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                 DISTRIBUTED TRACING                               │
│                 (OpenTelemetry SDK → Jaeger/Tempo)                 │
│                                                                   │
│  Trace Context Propagation:                                       │
│  ├── HTTP: W3C Trace Context headers (traceparent, tracestate)    │
│  ├── Kafka: trace_id in message headers                           │
│  ├── Redis: trace_id in operation metadata                        │
│  └── Database: trace_id in SQL comments (for slow query analysis) │
│                                                                   │
│  Implementation:                                                  │
│  ├── Spring Boot 3.5 native OTel support via Micrometer Tracing  │
│  ├── Dependencies:                                                │
│  │   ├── io.micrometer:micrometer-tracing-bridge-otel            │
│  │   ├── io.opentelemetry:opentelemetry-exporter-otlp            │
│  │   └── io.opentelemetry.instrumentation:opentelemetry-          │
│  │       spring-boot-starter (auto-config)                        │
│  ├── Exporter: OTLP → Jaeger (dev/staging), Tempo (prod)         │
│  ├── OTel Collector sidecar for batching + retry + multi-export  │
│  │                                                               │
│  Instrumentation:                                                 │
│  ├── Auto-instrumentation: Spring Boot, Kafka, JDBC, Redis,      │
│  │   Elasticsearch (OpenTelemetry Java Agent)                     │
│  ├── Manual spans for business operations:                        │
│  │   ├── endorsement.create                                       │
│  │   ├── endorsement.validate                                     │
│  │   ├── provisional_coverage.grant                               │
│  │   ├── ea_balance.check                                         │
│  │   ├── ea_balance.reserve                                       │
│  │   ├── batch.assemble                                           │
│  │   ├── insurer.submit (per insurer)                             │
│  │   ├── insurer.poll_response                                    │
│  │   ├── reconciliation.match                                     │
│  │   └── notification.send                                        │
│  │                                                               │
│  └── Span attributes (searchable):                                │
│      ├── endorsement.id                                           │
│      ├── endorsement.type (ADD/DELETE/UPDATE)                     │
│      ├── employer.id                                              │
│      ├── insurer.id                                               │
│      ├── batch.id                                                 │
│      ├── insurer.sla_deadline                                     │
│      └── ea.balance_after                                         │
└──────────────────────────────────────────────────────────────────┘
```

### 15.2 Key Trace Scenarios

```
Trace 1: Real-Time Endorsement (Happy Path)
═══════════════════════════════════════════
[API Gateway]─────────────────────────────────────────── 120ms total
  └─[Endorsement Service: create]─────────────────────── 100ms
      ├─[validate]──────────────────────────────────── 5ms
      ├─[EA balance check]──────────────────────────── 8ms
      │   └─[Redis: get ea:{employer}:{insurer}]───── 2ms
      ├─[Grant provisional coverage]────────────────── 12ms
      │   └─[PostgreSQL: INSERT provisional_coverages] 10ms
      ├─[Submit to insurer (real-time)]─────────────── 45ms
      │   └─[ICICI Adapter: POST /endorsements]─────── 40ms
      ├─[Publish event: ENDORSEMENT_SUBMITTED]──────── 3ms
      │   └─[Kafka: produce to endorsement.lifecycle]── 2ms
      └─[Return response]──────────────────────────── 2ms

Trace 2: Batch Endorsement Lifecycle (hours-long trace)
═══════════════════════════════════════════════════════
[Batch Assembly Scheduler]──────────────────────────── spans over hours
  ├─[Assemble batch for Niva Bupa]──────────────────── 500ms
  │   ├─[Query pending endorsements]────────────────── 50ms
  │   ├─[Optimize sequencing]───────────────────────── 20ms
  │   ├─[Check EA balances for 12 employers]─────────── 100ms
  │   └─[Transform to Niva CSV format]──────────────── 30ms
  ├─[Submit batch via SFTP]─────────────────────────── 2s
  │   └─[Niva Adapter: SFTP upload]─────────────────── 1.8s
  │
  │   ... 18 hours later (same trace_id) ...
  │
  ├─[Reconciliation: poll Niva response]────────────── 3s
  │   ├─[Niva Adapter: SFTP download response]──────── 2s
  │   ├─[Parse response CSV]────────────────────────── 200ms
  │   └─[Match 45 endorsements]─────────────────────── 500ms
  │       ├─[42 CONFIRMED → update status]
  │       ├─[2 REJECTED → queue retry]
  │       └─[1 MISSING → flag discrepancy]
  └─[Publish reconciliation events]─────────────────── 50ms

Sampling Strategy:
├── 100% sampling for errors and slow traces (> 2x P50)
├── 10% sampling for normal operations at MVP scale
├── 1% sampling at full scale (1M endorsements/day)
├── Always sample: P0 alerts, DLQ entries, SLA breaches
└── Head-based sampling with tail-based upgrade for errors
```

---

## 16. Deployment Strategy

### 16.1 Environment Progression

```
┌──────────────────────────────────────────────────────────────┐
│               DEPLOYMENT PIPELINE                             │
│                                                               │
│  Developer      CI/CD          Environments                   │
│  ─────────      ─────          ────────────                   │
│                                                               │
│  git push  ──► GitHub Actions                                 │
│                 │                                              │
│                 ├── Build (Gradle + Java 21)                   │
│                 ├── Unit Tests (JUnit 5)                       │
│                 ├── Component Tests (@WebMvcTest)              │
│                 ├── Static Analysis (SonarQube)                │
│                 ├── Container Image (Docker multi-stage)       │
│                 ├── Image Scan (Trivy)                         │
│                 │                                              │
│                 ▼                                              │
│              ┌──────────┐                                     │
│              │   DEV     │  ← Auto-deploy on main branch      │
│              │           │    Docker Compose (local)           │
│              │           │    PG + Redis + Kafka + ES + Jaeger │
│              │           │    Mock insurer adapters            │
│              └─────┬─────┘                                    │
│                    │ Integration tests pass (Testcontainers)  │
│                    ▼                                           │
│              ┌──────────┐                                     │
│              │  STAGING  │  ← Railway (auto-deploy from branch)│
│              │ (Railway) │    Railway PostgreSQL plugin         │
│              │           │    Railway Redis plugin              │
│              │           │    Upstash Kafka (Railway addon)     │
│              │           │    Bonsai Elasticsearch (addon)      │
│              │           │    Sandbox insurer APIs              │
│              │           │    Synthetic load testing            │
│              └─────┬─────┘                                    │
│                    │ E2E tests pass + manual QA approval       │
│                    ▼                                           │
│              ┌──────────┐                                     │
│              │   PROD    │  ← Railway (production environment) │
│              │ (Railway) │    Railway managed services          │
│              │           │    Real insurer APIs                 │
│              │           │    Rolling deployment (zero-downtime)│
│              │           │    Health check gates                │
│              └──────────┘                                     │
│                                                               │
│  AWS MIGRATION PATH (Phase 4 — when scale demands):          │
│  ├── Same Docker images → deploy to EKS                       │
│  ├── Railway PostgreSQL → AWS RDS Aurora                       │
│  ├── Railway Redis → AWS ElastiCache                           │
│  ├── Upstash Kafka → AWS MSK                                  │
│  ├── Bonsai ES → AWS OpenSearch (ES-compatible)                │
│  ├── K8s manifests already in repo (k8s/ directory)           │
│  └── Only env vars change — zero application code changes     │
└──────────────────────────────────────────────────────────────┘
```

### 16.2 Railway Deployment Architecture

```
┌──────────────────────────────────────────────────────────────┐
│              RAILWAY DEPLOYMENT                               │
│                                                               │
│  Railway Project: plum-endorsements                           │
│                                                               │
│  Services (Docker-based):                                     │
│  ├── endorsement-service                                      │
│  │   ├── Docker image: multi-stage (Gradle → JRE 21 slim)    │
│  │   ├── Scaling: Railway auto-scaling (CPU/memory-based)     │
│  │   ├── Resources: 1 vCPU / 2GB memory per instance         │
│  │   ├── Health check: /actuator/health (Spring Boot Actuator)│
│  │   ├── Env vars: DATABASE_URL, REDIS_URL, KAFKA_BOOTSTRAP, │
│  │   │             ELASTICSEARCH_URL, OTEL_EXPORTER_ENDPOINT  │
│  │   └── Virtual threads enabled (handles 10K+ concurrent    │
│  │       insurer API calls without thread pool tuning)        │
│  │                                                            │
│  ├── frontend (React SPA)                                     │
│  │   ├── Docker image: nginx serving built React assets       │
│  │   ├── Static asset caching + CDN (Railway edge)            │
│  │   └── WebSocket proxy to endorsement-service               │
│  │                                                            │
│  ├── batch-processor                                          │
│  │   ├── Same codebase, different Spring profile              │
│  │   │   (--spring.profiles.active=batch-processor)           │
│  │   └── Dedicated instance for Kafka consumer workloads      │
│  │                                                            │
│  └── reconciliation-worker                                    │
│      ├── Same codebase, different Spring profile              │
│      │   (--spring.profiles.active=reconciliation)            │
│      └── Scheduled job runner (@Scheduled + ShedLock)         │
│                                                               │
│  Railway Plugins (Managed Services):                          │
│  ├── PostgreSQL 16 (Railway-managed)                          │
│  │   ├── Automatic backups, point-in-time recovery            │
│  │   └── Connection string injected as DATABASE_URL           │
│  ├── Redis 7 (Railway-managed)                                │
│  │   └── Connection string injected as REDIS_URL              │
│  ├── Kafka (Upstash Kafka — Railway addon)                    │
│  │   └── Serverless Kafka, auto-scales with throughput        │
│  └── Elasticsearch (Bonsai — Railway addon or self-hosted)    │
│      └── Connection string injected as ELASTICSEARCH_URL      │
│                                                               │
│  Observability on Railway:                                    │
│  ├── OTel Collector → Jaeger (self-hosted on Railway)         │
│  │   OR → Grafana Cloud (managed, recommended for prod)       │
│  ├── Railway built-in metrics (CPU, memory, network)          │
│  ├── Spring Boot Actuator + Micrometer → Prometheus format    │
│  └── Structured JSON logs → Railway log viewer                │
│                                                               │
│  Deployment Strategy:                                         │
│  ├── Railway handles rolling deploys (zero-downtime)          │
│  ├── Health check validation before routing traffic           │
│  ├── Instant rollback via Railway deployment history          │
│  └── Preview environments for PRs (Railway feature)           │
│                                                               │
│  PORTABILITY GUARANTEES:                                      │
│  ├── All services are standard Docker containers              │
│  ├── No Railway-proprietary APIs in application code          │
│  ├── Config via 12-factor env vars (DATABASE_URL, etc.)       │
│  ├── K8s manifests in k8s/ directory ready for EKS/GKE       │
│  ├── docker-compose.yml for full local development            │
│  └── Terraform scripts in infra/ for AWS migration:           │
│      ├── infra/aws/rds.tf       (PostgreSQL → Aurora)         │
│      ├── infra/aws/elasticache.tf (Redis)                     │
│      ├── infra/aws/msk.tf       (Kafka)                       │
│      ├── infra/aws/opensearch.tf (Elasticsearch-compatible)   │
│      └── infra/aws/eks.tf       (Kubernetes cluster)          │
└──────────────────────────────────────────────────────────────┘
```

### 16.3 Docker Configuration

```
# Multi-stage Dockerfile for endorsement-service

FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S plum && adduser -S plum -G plum
COPY --from=build /app/build/libs/*.jar app.jar
USER plum

# OpenTelemetry Java agent for auto-instrumentation
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar otel-agent.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-javaagent:otel-agent.jar", \
  "-Dspring.threads.virtual.enabled=true", \
  "-XX:+UseZGC", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
```

### 16.4 Docker Compose (Local Development)

```yaml
# docker-compose.yml — full local dev environment
services:
  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: endorsements
      POSTGRES_USER: plum
      POSTGRES_PASSWORD: plum_dev
    volumes:
      - pgdata:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  kafka:
    image: apache/kafka:3.7.0
    ports: ["9092:9092"]
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      CLUSTER_ID: local-dev-cluster

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.13.0
    ports: ["9200:9200"]
    environment:
      discovery.type: single-node
      xpack.security.enabled: "false"
      ES_JAVA_OPTS: "-Xms512m -Xmx512m"

  jaeger:
    image: jaegertracing/all-in-one:1.55
    ports:
      - "16686:16686"   # Jaeger UI
      - "4317:4317"     # OTLP gRPC
      - "4318:4318"     # OTLP HTTP

volumes:
  pgdata:
```

### 16.5 Database Migration Strategy

```
Schema Migrations:
├── Tool: Flyway (integrated with Spring Boot)
├── Naming: V{version}__{description}.sql
├── Execution: On application startup (dev/staging)
│              Manual approval + scheduled window (prod)
├── Rollback: Every migration has a corresponding undo script
│             (U{version}__{description}.sql)
└── Zero-downtime DDL:
    ├── ADD COLUMN: always with DEFAULT or NULL
    ├── DROP COLUMN: 3-step (stop writing → deploy → drop)
    ├── ADD INDEX: CONCURRENTLY (PostgreSQL)
    └── Table rename: never — use views as aliases
```

### 16.3 Database Migration Strategy

```
Schema Migrations:
├── Tool: Flyway (integrated with Spring Boot)
├── Naming: V{version}__{description}.sql
├── Execution: On application startup (dev/staging)
│              Manual approval + scheduled window (prod)
├── Rollback: Every migration has a corresponding undo script
│             (U{version}__{description}.sql)
└── Zero-downtime DDL:
    ├── ADD COLUMN: always with DEFAULT or NULL
    ├── DROP COLUMN: 3-step (stop writing → deploy → drop)
    ├── ADD INDEX: CONCURRENTLY (PostgreSQL)
    └── Table rename: never — use views as aliases
```

---

## 17. Security Model

### 17.1 Security Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                    SECURITY MODEL                                 │
│                                                                   │
│  AUTHENTICATION & AUTHORIZATION                                   │
│  ├── Employer Portal:                                             │
│  │   ├── OAuth 2.0 + OIDC (Keycloak or Auth0)                    │
│  │   ├── MFA mandatory for HR admins                              │
│  │   └── Session: JWT access token (15min) + refresh token (7d)   │
│  │                                                                │
│  ├── Insurer Webhooks:                                            │
│  │   ├── HMAC-SHA256 signature verification                       │
│  │   ├── IP allowlist per insurer                                 │
│  │   └── Mutual TLS (Phase 2+)                                    │
│  │                                                                │
│  ├── Internal Services:                                           │
│  │   ├── mTLS for service-to-service (Istio service mesh, Phase 3)│
│  │   ├── JWT propagation for user context                         │
│  │   └── Service accounts with least-privilege RBAC               │
│  │                                                                │
│  └── API Keys:                                                    │
│      ├── HRIS integration partners                                │
│      ├── Rate limited per key                                     │
│      └── Scoped to specific employer(s)                           │
│                                                                   │
│  RBAC (Role-Based Access Control)                                 │
│  ├── EMPLOYER_ADMIN: Full endorsement CRUD, EA balance view       │
│  ├── EMPLOYER_HR: Create/view endorsements, no EA management      │
│  ├── EMPLOYER_VIEWER: Read-only dashboard access                  │
│  ├── PLUM_OPS: All employers, ops dashboard, retry actions        │
│  ├── PLUM_ADMIN: System configuration, insurer management         │
│  └── INSURER_SYSTEM: Webhook delivery, batch response upload      │
│                                                                   │
│  DATA PROTECTION                                                  │
│  ├── Encryption at Rest:                                          │
│  │   ├── PostgreSQL: AWS RDS encryption (AES-256)                 │
│  │   ├── Redis: ElastiCache encryption at rest                    │
│  │   ├── Kafka: MSK encryption at rest                            │
│  │   └── S3: SSE-S3 or SSE-KMS                                   │
│  │                                                                │
│  ├── Encryption in Transit:                                       │
│  │   ├── TLS 1.3 for all external communication                   │
│  │   ├── TLS 1.2+ for insurer APIs (some legacy)                  │
│  │   └── In-cluster: mTLS via service mesh (Phase 3)              │
│  │                                                                │
│  ├── PII Handling:                                                │
│  │   ├── Employee PII (name, DOB, Aadhaar): encrypted at field    │
│  │   │   level in JSONB using application-level encryption         │
│  │   ├── Decryption only in endorsement-service (not in read path)│
│  │   ├── Dashboard shows masked PII: "Ra***l Ku***r"              │
│  │   └── Log redaction: PII fields replaced with hash             │
│  │                                                                │
│  └── Key Management:                                              │
│      ├── Phase 1-3 (Railway): HashiCorp Vault or env-var secrets  │
│      ├── Phase 4 (AWS): AWS KMS for master keys                   │
│      ├── 90-day key rotation                                      │
│      ├── Envelope encryption for field-level encryption           │
│      └── Separate keys per tenant (Phase 4)                       │
│                                                                   │
│  AUDIT                                                            │
│  ├── Every API call logged with: who, what, when, from_ip         │
│  ├── Every endorsement state change logged (event sourcing)       │
│  ├── Every EA balance change logged (double-entry ledger)         │
│  ├── Immutable audit log: append-only (S3 + Athena for queries)   │
│  ├── Retention: 7 years (insurance regulatory requirement)        │
│  └── Access to audit logs: PLUM_ADMIN only, MFA required          │
│                                                                   │
│  API SECURITY                                                     │
│  ├── Rate limiting: 100 req/s per employer, 1000 req/s global     │
│  ├── Input validation: Bean Validation + custom validators        │
│  ├── SQL injection: Parameterized queries (JPA)                   │
│  ├── XSS: React auto-escaping + CSP headers                      │
│  ├── CSRF: SameSite cookies + CSRF token for state-changing ops   │
│  └── Dependency scanning: Snyk/Dependabot (weekly)                │
│                                                                   │
│  COMPLIANCE                                                       │
│  ├── IRDAI data localization (India)                               │
│  ├── SOC 2 Type II readiness (Phase 3)                             │
│  ├── ISO 27001 alignment (Phase 4)                                 │
│  └── GDPR-ready data deletion (for global expansion, Phase 4)     │
└──────────────────────────────────────────────────────────────────┘
```

---

## 18. Strategy Evolution — MVP to Global Product

### 18.1 Evolution Roadmap

```
┌──────────────────────────────────────────────────────────────────────┐
│                    EVOLUTION STRATEGY                                  │
│                                                                       │
│  PHASE 1: MVP                    PHASE 2: SCALE                       │
│  (Weeks 1-4)                     (Weeks 5-10)                         │
│  ─────────────                   ──────────────                       │
│  ● Java 21 + Spring Boot 3.5    ● Extract Insurer Gateway Service    │
│  ● Modular Monolith             ● Multi-insurer adapters             │
│  ● Single insurer               ● EA optimization algorithm          │
│  ● Kafka events from Day 1      ● CQRS: Redis + Elasticsearch       │
│  ● PostgreSQL + Redis + Caffeine● Automated reconciliation           │
│  ● React dashboard (basic)      ● React dashboard (advanced)         │
│  ● Basic auth (JWT)             ● RBAC + audit logging               │
│  ● Docker + Railway deploy      ● Railway staging + prod              │
│  ● OTel tracing (Jaeger)        ● Contract tests + Gatling load tests│
│  ● Unit + integration tests     ● Caffeine+Redis two-tier cache tuned│
│       │                              │                                │
│       │                              │                                │
│       ▼                              ▼                                │
│  PHASE 3: INTELLIGENCE           PHASE 4: GLOBAL                      │
│  (Weeks 11-16)                   (Weeks 17-24)                        │
│  ──────────────────              ─────────────                        │
│  ● Spring AI + LangChain4j      ● Multi-region deployment (AWS EKS)  │
│  ● AI anomaly detection         ● Migrate Railway → AWS               │
│  ● Predictive EA forecasting    ● Multi-currency support              │
│  ● LangChain4j RAG error fix    ● Regulatory framework (pluggable)   │
│  ● Smart batch optimization     ● Self-service insurer onboarding    │
│  ● Process mining insights      ● Platform API marketplace            │
│  ● Canary deployments           ● Blue-green + multi-cluster          │
│  ● Full OTel + Grafana Cloud    ● Per-tenant encryption keys          │
│  ● Chaos engineering            ● SOC2/ISO27001 certification        │
│  ● 100K endorsements/day        ● 1M+ endorsements/day               │
└──────────────────────────────────────────────────────────────────────┘
```

### 18.2 Architecture Evolution

```
Phase 1: Modular Monolith (Java 21 + Spring Boot 3.5 on Railway)
┌─────────────────────────────────────────────┐
│          Single Deployment Unit (Docker)     │
│  ┌──────────┬──────────┬──────────┐         │
│  │Endorsement│ EA      │Dashboard │         │
│  │Module     │Balance  │Module    │         │
│  │          │Module   │          │         │
│  └──────────┴──────────┴──────────┘         │
│  PostgreSQL + Redis + Caffeine + Kafka + ES  │
│  OpenTelemetry → Jaeger                      │
│  Deployed on: Railway                        │
└─────────────────────────────────────────────┘

Phase 2: Service Extraction (Railway, multi-service)
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ Endorsement  │  │ Insurer      │  │ React SPA    │
│ Service      │──│ Gateway      │  │ (Dashboard)  │
└──────┬───────┘  └──────────────┘  └──────┬───────┘
       │              Kafka                 │
       └──────────────────────────────────┘
  PostgreSQL + Redis + Caffeine + Elasticsearch

Phase 3: Full Microservices (Railway → preparing AWS migration)
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│Endorsement│ │EA Balance│ │Insurer   │ │Reconcil. │
│Service   │ │Service   │ │Gateway   │ │Engine    │
└────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘
     │            │            │            │
     └────────────┴────────────┴────────────┘
                    Kafka
     ┌──────────┐ ┌──────────┐ ┌──────────────────┐
     │Notif.    │ │Analytics │ │AI/ML Service      │
     │Service   │ │Service   │ │(Spring AI +       │
     └──────────┘ └──────────┘ │ LangChain4j)      │
                               └──────────────────┘
     OTel → Grafana Cloud | Elasticsearch for search + vectors

Phase 4: Global Platform (AWS EKS — migrated from Railway)
                    ┌─────────────────┐
                    │  Global API     │
                    │  Gateway        │
                    │  (Multi-region) │
                    └────┬────────┬───┘
                         │        │
              ┌──────────▼┐  ┌───▼──────────┐
              │ Region:    │  │ Region:      │
              │ India (EKS)│  │ SEA (EKS)    │
              │            │  │              │
              │ Full stack │  │ Full stack   │
              │ RDS+MSK+   │  │ RDS+MSK+    │
              │ ElastiCache│  │ ElastiCache  │
              └──────┬─────┘  └──────┬───────┘
                     │               │
              ┌──────▼───────────────▼───────┐
              │  Cross-Region Event Bridge    │
              │  (Kafka MirrorMaker 2 /       │
              │   AWS EventBridge)            │
              └──────────────────────────────┘
```

### 18.3 Scaling Milestones & Quality Gates

```
┌──────────────────────────────────────────────────────────────────┐
│               QUALITY GATES PER PHASE                             │
│                                                                   │
│  Phase 1 → Phase 2 Gate:                                          │
│  ├── ✅ 10K endorsements/day processed successfully               │
│  ├── ✅ Zero coverage gaps (provisional coverage working)         │
│  ├── ✅ < 1% endorsement failure rate                              │
│  ├── ✅ Dashboard shows real-time status (< 5s delay)              │
│  ├── ✅ API P99 < 300ms                                            │
│  ├── ✅ 80%+ unit test coverage on domain model                    │
│  └── ✅ Security: auth, input validation, PII encryption           │
│                                                                   │
│  Phase 2 → Phase 3 Gate:                                          │
│  ├── ✅ 100K endorsements/day across 5+ insurers                   │
│  ├── ✅ EA optimization saves employers 15%+ capital               │
│  ├── ✅ Automated reconciliation match rate > 98%                   │
│  ├── ✅ Batch SLA compliance > 95% per insurer                     │
│  ├── ✅ Kafka event pipeline stable (consumer lag < 1000)          │
│  ├── ✅ Load test passes at 2x current load                        │
│  └── ✅ Contract tests for all insurer adapters                    │
│                                                                   │
│  Phase 3 → Phase 4 Gate:                                          │
│  ├── ✅ Anomaly detection catches 90%+ of fraud patterns           │
│  ├── ✅ EA forecast accuracy within 10% of actual                  │
│  ├── ✅ Auto error resolution handles 60%+ of rejections           │
│  ├── ✅ 95% STP rate                                                │
│  ├── ✅ Chaos engineering: system recovers from node failure < 30s │
│  ├── ✅ SOC 2 Type II audit passed                                  │
│  └── ✅ Canary deployment pipeline proven in production             │
│                                                                   │
│  Phase 4 Steady State:                                            │
│  ├── ✅ 1M+ endorsements/day                                       │
│  ├── ✅ Multi-region: < 100ms latency from any region              │
│  ├── ✅ 99.9% uptime (< 8.7 hours downtime/year)                  │
│  ├── ✅ Self-service insurer onboarding (< 1 week)                 │
│  ├── ✅ ISO 27001 certified                                         │
│  └── ✅ Platform API used by 5+ HRIS integrations                  │
└──────────────────────────────────────────────────────────────────┘
```

### 18.4 Risk Register

```
┌──────────────────────────────────────────────────────────────────┐
│  RISK                          │ IMPACT │ MITIGATION             │
├────────────────────────────────┼────────┼────────────────────────┤
│ Coverage gap during            │ HIGH   │ Provisional coverage   │
│ endorsement processing         │        │ mechanism + SLA        │
│                                │        │ monitoring             │
│                                │        │                        │
│ EA balance inconsistency       │ HIGH   │ Double-entry ledger,   │
│ (financial data corruption)    │        │ optimistic locking,    │
│                                │        │ reconciliation         │
│                                │        │                        │
│ Insurer API changes without    │ MED    │ Contract tests, adapter│
│ notice                         │        │ versioning, feature    │
│                                │        │ flags                  │
│                                │        │                        │
│ Insurer batch SLA breach       │ MED    │ SLA monitoring, auto-  │
│ (processing takes days)        │        │ escalation, provisional│
│                                │        │ coverage as safety net │
│                                │        │                        │
│ Kafka cluster failure          │ HIGH   │ Multi-AZ MSK, consumer │
│                                │        │ offset checkpoints,    │
│                                │        │ DLQ for failed messages│
│                                │        │                        │
│ Data breach (PII exposure)     │ CRIT   │ Field-level encryption,│
│                                │        │ PII redaction in logs, │
│                                │        │ network segmentation   │
│                                │        │                        │
│ Single-region outage           │ HIGH   │ Multi-AZ (Phase 1-3),  │
│                                │        │ multi-region (Phase 4) │
│                                │        │                        │
│ Team skill gap (event-driven   │ MED    │ Phase 1 training, pair │
│ architecture)                  │        │ programming, gradual   │
│                                │        │ complexity increase    │
└──────────────────────────────────────────────────────────────────┘
```

---

## Appendix A: Technology Stack Summary

| Layer | Phase 1 (MVP) | Phase 2-3 (Scale/Intelligence) | Phase 4 (Global) |
|-------|--------------|-------------------------------|-----------------|
| **Language** | Java 21 (virtual threads, records, sealed classes, pattern matching) | Java 21 | Java 21 |
| **Framework** | Spring Boot 3.5.x | Spring Boot 3.5.x | Spring Boot 3.5.x |
| **Frontend** | React 18 + TypeScript + Vite | React 18 + TypeScript + Vite | React 18 + TypeScript + Vite |
| **API** | REST (OpenAPI 3.1, springdoc) | REST + gRPC (internal) | REST + gRPC + GraphQL (dashboard) |
| **Primary DB** | PostgreSQL 16 | PostgreSQL 16 (partitioned) | PostgreSQL 16 (Citus/Aurora sharded) |
| **Distributed Cache** | Redis 7 | Redis 7 Cluster | Redis 7 Cluster (multi-region) |
| **In-Memory Cache** | Caffeine (L1, 30-60s TTL) | Caffeine (L1, tuned per cache) | Caffeine (L1, tuned per cache) |
| **Messaging** | Apache Kafka (KRaft / Upstash) | Apache Kafka (Upstash / MSK) | Apache Kafka (MSK) + EventBridge |
| **Search/Secondary** | Elasticsearch 8 | Elasticsearch 8 (multi-index) | Elasticsearch 8 (multi-tenant) |
| **AI/ML** | — | Spring AI + LangChain4j (RAG, anomaly detection, error resolution) | Spring AI + LangChain4j + custom models |
| **Tracing** | OpenTelemetry → Jaeger | OpenTelemetry → Jaeger/Tempo | OpenTelemetry → Grafana Cloud Tempo |
| **Observability** | Spring Actuator + Micrometer + OTel | Prometheus + Grafana + OTel + Jaeger | Full OTel + Grafana Cloud (metrics, logs, traces) |
| **Containerization** | Docker (multi-stage, JRE 21 slim) | Docker | Docker |
| **Orchestration** | Docker Compose (local) | Kubernetes (Railway / K8s manifests) | Kubernetes (AWS EKS, multi-cluster) |
| **Cloud** | Railway (Docker-based deploy) | Railway (staging + prod) | AWS (EKS + RDS + MSK + ElastiCache) |
| **CI/CD** | GitHub Actions | GitHub Actions + Railway auto-deploy | GitHub Actions + ArgoCD + Argo Rollouts |
| **IaC** | docker-compose.yml + railway.toml | docker-compose.yml + K8s manifests | Terraform + Helm + Crossplane |
| **Service Mesh** | — | — | Istio (on EKS) |

**Key Dependency Versions:**
```
Java:                 21 (Temurin)
Spring Boot:          3.5.x
Spring AI:            1.0.x
LangChain4j:          1.0.x (quarkus-langchain4j or standalone)
PostgreSQL:           16
Redis:                7.x
Kafka:                3.7+ (KRaft mode, no ZooKeeper)
Elasticsearch:        8.13+
OpenTelemetry:        1.36+ (Java agent + SDK)
Caffeine:             3.1.x
Resilience4j:         2.2.x (circuit breaker, retry, bulkhead)
Flyway:               10.x
Testcontainers:       1.19.x
JUnit:                5.10.x
Gatling:              3.10.x (load testing)
MapStruct:            1.5.x (entity mapping)
springdoc-openapi:    2.4.x (OpenAPI 3.1 docs)
Docker:               24+
Kubernetes:           1.28+
```

---

## Appendix B: Key Algorithms

### B.1 EA Balance Optimization — Pseudocode

```
function optimizeBatch(pendingEndorsements, eaBalance, insurer):
    deletions = filter(pendingEndorsements, type == DELETE)
    neutralUpdates = filter(pendingEndorsements, type == UPDATE, premiumDelta == 0)
    additions = filter(pendingEndorsements, type == ADD)
                  .sortBy(coverageStartDate ASC)  // earliest first
    premiumUpdates = filter(pendingEndorsements, type == UPDATE, premiumDelta != 0)

    batch = []
    projectedBalance = eaBalance

    // Step 1: All deletions (always process — they free up money)
    for d in deletions:
        batch.add(d)
        projectedBalance += d.creditAmount

    // Step 2: All neutral updates (no cost impact)
    for u in neutralUpdates:
        batch.add(u)

    // Step 3: Additions, funded by balance + recovered credits
    for a in additions:
        if projectedBalance >= a.premiumAmount:
            batch.add(a)
            projectedBalance -= a.premiumAmount
        else:
            // Cannot fund — alert employer, hold for next batch
            alertInsufficientBalance(a.employerId, a.premiumAmount - projectedBalance)

    // Step 4: Premium-affecting updates
    for pu in premiumUpdates:
        if pu.premiumDelta > 0 and projectedBalance >= pu.premiumDelta:
            batch.add(pu)
            projectedBalance -= pu.premiumDelta
        elif pu.premiumDelta <= 0:
            batch.add(pu)
            projectedBalance += abs(pu.premiumDelta)

    // Respect insurer batch size limit
    if batch.size() > insurer.maxBatchSize:
        return splitIntoBatches(batch, insurer.maxBatchSize)

    return batch
```

---

*This document serves as the comprehensive execution blueprint for the Endorsement Management System. Each phase builds on the previous, with clear quality gates ensuring readiness before progression. The architecture evolves from a pragmatic MVP to a world-class global platform through deliberate, measured steps.*
