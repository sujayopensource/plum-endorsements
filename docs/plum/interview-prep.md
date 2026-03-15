# Plum - Principal Architect Interview Preparation

**Date:** March 13, 2026 (updated)
**Role:** Principal Architect
**Company:** Plum Benefits Private Limited (plumhq.com)
**Location:** Bengaluru, Whitefield (On-Site)
**Backed by:** Tiger Global, Peak XV Partners (Sequoia Surge)
**Interviewer:** Sankaralingam T. (SVP & Head of Engineering) — 25+ years at Amazon, Cisco, Intel, Jupiter

---

## About Plum - Know This Cold

Plum is an employee health insurance and benefits platform that makes group insurance simple, accessible, and inclusive for modern organizations.

**Key facts to know:**
- Covers **500,000+ lives** across **6,000+ companies** (Atlassian, Twilio, Zomato, Tata)
- Mission: **10 million lives by FY2030** (20x growth needed from current base)
- Revenue: Rs 60.6 crore FY25, 83% CAGR, approaching profitability
- Built telehealth platform in-house, not outsourced
- 80% of claims processed digitally
- ~450 employees, 31% YoY headcount growth
- Policy activation in under 1 hour (vs. weeks in traditional insurance)

**Their tech stack:** Node.js (Express), Python (Django), React, React Native, MongoDB, PostgreSQL, AWS, Kubernetes. They also list Java and Go in this job posting.

**Why this matters for you:** They are at an inflection point - scaling from 500K to 10M lives requires serious architectural overhaul. They need someone who has done this before. That is exactly what you have done across Atlassian, Whatfix, and Bizongo.

**Insurance Partners (relevant to your demo):** ICICI Lombard, Niva Bupa, Bajaj Allianz, Digit — your endorsement system has adapters for 3 of these 4.

---

## YOUR SECRET WEAPON: The Endorsement System Demo

You didn't just prepare talking points — **you built the system Plum actually needs**. The endorsement system at `/Users/sujayhegde/Interviews/plum-endorsements/` is a production-grade, runnable prototype that solves the exact design challenge from Plum's interview process.

### What You Built (by the numbers)

| Metric | Count |
|--------|-------|
| Automated tests | **657** (363 unit + 102 API + 74 BDD + 112 E2E + 6 perf) |
| Test pass rate | **100%** |
| REST API endpoints | 27 across 5 controllers |
| Kafka event types | 24 (sealed interface with exhaustive pattern matching) |
| Database tables | 15 (13 operational + 2 archive) |
| Flyway migrations | 15 |
| Insurer adapters | 4 (Mock, ICICI Lombard, Niva Bupa, Bajaj Allianz) |
| Intelligence adapters | 5 (anomaly detection, forecasting, error resolution, process mining, batch optimization) |
| Grafana dashboards | 7 |
| Frontend pages | 8 (React + TypeScript) |
| Scheduled jobs | 9 |
| Domain models | 20 |
| Port interfaces | 18 |
| Lines of Java | ~7,000+ |

### How to Demo It

```bash
./start.sh                    # One-click: Docker Compose + backend + frontend
# Frontend:      http://localhost:5173
# Swagger UI:    http://localhost:8080/swagger-ui
# Grafana:       http://localhost:3000  (admin/plum)
# Jaeger:        http://localhost:16686
# Kibana:        http://localhost:5601
./run-all-tests.sh            # Full test suite + Allure report at :5050
```

### Architecture Patterns That Map Directly to Plum

| Your Implementation | Plum's Need |
|---|---|
| Hexagonal/Ports-and-Adapters architecture | Clean separation when integrating 10+ insurers |
| Strategy pattern for insurer adapters (REST/CSV/SOAP/XML) | Each insurer has different API formats — same problem |
| 11-state endorsement lifecycle (state machine) | Claims processing IS a state machine problem |
| CQRS (separate command/query handlers) | Scalable read/write separation at 10M lives |
| Event-driven via Kafka (32 partitions, employerId key) | Async processing for 1M endorsements/day target |
| Circuit breakers per insurer (Resilience4j) | Insurer APIs go down — must degrade gracefully |
| Provisional coverage pattern (no coverage gaps) | "No loss of coverage" is a core business requirement |
| EA balance optimization (deletion-first sequencing) | Minimizing employer capital lockup |
| AI/automation layer behind port interfaces | Rule-based now, ML later — same API contract |
| Rate limiting, optimistic locking, data retention | Production-grade resilience at scale |

### Talking Points for Sankar

Given his Amazon/Cisco background, emphasize:

1. **"I built the system you're designing for"** — Not a whiteboard exercise. Running code, 657 tests, one-click demo. He can see the insurer adapter pattern, the state machine, the event pipeline.

2. **"Port/adapter pattern for insurer diversity"** — "Each insurer has different APIs. ICICI Lombard is REST/JSON, Niva Bupa is CSV/SFTP batch-only, Bajaj Allianz is SOAP/XML. The Strategy pattern behind `InsurerPort` means adding Digit or Star Health is a new adapter class + one DB row. Zero handler changes."

3. **"No loss of coverage guarantee"** — "Provisional coverage is granted immediately at endorsement creation, before insurer confirmation. 5 edge cases identified and fixed: stuck endorsements get retried, failed-permanent endorsements expire coverage with notification, cleanup scheduler checks endorsement status before expiring."

4. **"Event-driven with operational rigor"** — "24 Kafka event types, 88 partitions, `acks: all`. Every event carries sufficient state (event-carried state transfer) so consumers never query back. Partition key is `employerId` for per-employer ordering."

5. **"Observable from day one"** — "OpenTelemetry tracing with endorsementId/employerId baggage, 7 Grafana dashboards, ELK for structured logging, 20+ Prometheus custom metrics. Sankar can see the insurer circuit breaker state at `/actuator/circuitbreakers`."

### Design Challenge Deliverables (All 6 Complete)

| # | Deliverable | Document |
|---|-------------|----------|
| 1 | High-level architecture | `docs/deliverables/High_Level_Architecture.md` (61 KB) |
| 2 | No loss of coverage approach | `docs/deliverables/No_Loss_of_Coverage_Approach.md` (39 KB) |
| 3 | EA balance minimization algorithm | `docs/deliverables/EA_Balance_Minimization_Algorithm.md` (35 KB) |
| 4 | Real-time visibility user flows | `docs/deliverables/Real_Time_Visibility_User_Flows.md` (94 KB) |
| 5 | AI/automation approach | `docs/deliverables/AI_Automation_Approach.md` (58 KB) |
| 6 | Code/prototype | 657 tests, `./start.sh` one-click demo |

---

## The Five Interview Focus Areas

The talent team explicitly said the conversation will center on:

1. Past architecture and system design experience
2. Deep dive into backend stacks
3. Distributed systems / scalability decisions
4. Alignment and interest in Plum
5. Ownership and technical leadership approach

Below is preparation for each.

---

## 1. Architecture and System Design Experience

### Your Top Stories (pick 2-3, know them deeply)

**Story E: Endorsement Management System for Plum (STRONGEST — you built their system)**
- **Context:** Plum's design challenge: build an endorsement processing system for multi-insurer health insurance
- **What you did:** Designed and built a complete, production-grade system from scratch in ~1 week
- **Key decisions to articulate:**
  - Hexagonal architecture with 18 port interfaces — domain has zero infrastructure imports
  - Strategy pattern for insurer diversity (4 adapters: REST/JSON, CSV/SFTP, SOAP/XML, Mock)
  - 11-state endorsement lifecycle with state machine pattern — transitions validated at domain level
  - CQRS: separate `CreateEndorsementHandler` / `ProcessEndorsementHandler` (commands) from `EndorsementQueryHandler` (read-only, @Transactional(readOnly=true))
  - Event-driven via Kafka with 24 event types, sealed interface for compile-time exhaustiveness
  - 5-pillar AI/intelligence layer behind port/adapter interfaces (rule-based now, ML-swappable later)
  - Provisional coverage pattern for no-loss-of-coverage guarantee
  - EA balance optimization: deletion-first sequencing to free balance before additions
- **Be ready to demo:** `./start.sh` one-click startup, show frontend, Swagger, Grafana dashboards, run test suite
- **Impact:** 657 tests (6 layers), 27 API endpoints, complete observability stack, all 6 deliverables documented

**Story A: Data Abstraction Platform at Atlassian (strongest past experience story)**
- **Context:** Confluence needed a platform to abstract data access across products and microservices
- **What you did:** Architected, designed, and built it from scratch
- **Key decisions to articulate:**
  - Why a platform approach vs. per-service data access
  - API design (the Traversal API you designed)
  - How you handled soft deletes at scale (rearchitected for scalability and reliability to meet SLOs/SLAs)
  - CDC pipeline using DynamoDB Streams for analytics
  - How you made it operationally mature (SLIs, SLOs, dashboards, error rate reduction)
- **Be ready to draw this on a whiteboard**: Data flow from transactional store -> DynamoDB Streams -> analytics platform (Apache Airflow orchestration)

**Story B: DevSecOps Policy Platform at Whatfix**
- **Context:** Enterprise customers needed policy-driven security governance across CI/CD
- **What you did:** Designed and implemented a Policy Framework and Policy Engine
- **Key decisions:**
  - Policy-as-code architecture
  - Supporting on-prem, hybrid, and cloud deployment models (very relevant for insurance domain compliance)
  - How you enforced CI/CD guardrails without slowing developer workflows
  - Hands-on: you wrote the core policy evaluation logic, enforcement hooks, CI/CD integrations
- **Impact:** 60% reduction in security onboarding delays, 2x improvement in vulnerability remediation

**Story C: Lucene to OpenSearch Migration at Atlassian**
- **Context:** Confluence search needed to migrate from Lucene indexes to OpenSearch
- **Key decisions:** Migration strategy, zero-downtime approach, data consistency during migration
- **This maps to Plum's reality:** They likely have search and data retrieval challenges at scale that need similar migration thinking

**Story D: SDUI + Workflow Orchestration at Bizongo**
- **Context:** Took architectural ownership of multiple product platforms
- **What you did:** Server-Driven UI architecture, workflow orchestration engine, accounting stack, document processing
- **Key decisions:** Why SDUI (flexibility, deploy without app updates), workflow engine design choices
- **Plum relevance:** Insurance claims processing IS a workflow orchestration problem. Document processing IS central to insurance.

### How to Frame Architecture Answers

Use this structure for every architecture discussion:
1. **Context** - What was the business problem? What were the constraints?
2. **Options considered** - What trade-offs did you evaluate?
3. **Decision and rationale** - Why did you pick the approach you did?
4. **Implementation highlights** - What was technically interesting?
5. **Outcome** - Measurable impact (latency, throughput, cost, reliability)

---

## 2. Deep Dive into Backend Stacks

### Your Stack Expertise (map to their requirements)

| Plum Needs | Your Experience |
|---|---|
| Node.js | Plum's primary backend - be honest about your depth here |
| Java | **Deep** - Java 8/9/11/17/21, Spring ecosystem (Boot, Cloud, Data, Webflux, MVC) across Atlassian, Whatfix, Bizongo, Nasdaq |
| Python | Data pipelines at Bizongo (AWS Glue, HevoData, Airflow), ML at Xerox |
| Go | Be honest if limited - frame as eager to learn, quick ramp-up |
| Express, Spring Boot, Django | Spring Boot is your strongest; Django via Python data work at Bizongo |

### Key Backend Deep-Dive Topics to Prepare

**Spring Boot expertise (your strongest suit — now backed by a live demo):**
- Startup optimization: lazy loading, init logic refactoring, JVM tuning (30-50% improvement)
- **Endorsement system:** Spring Boot 3 + Java 21 virtual threads, ZGC, stateless session management
- Resilience4j circuit breakers per insurer (different thresholds for ICICI vs Bajaj), retry with exponential backoff
- Spring Data JPA with Flyway migrations (15 migrations), optimistic locking via @Version
- CQRS pattern with @Transactional(readOnly=true) for query handlers
- 9 @Scheduled jobs with configurable cron expressions and @ConditionalOnProperty feature flags

**Database expertise:**
- DynamoDB: Streams, CDC, data modeling at Atlassian
- PostgreSQL/MySQL: Query optimization (the 32s -> 4s P90 improvement at Atlassian Jira is gold)
- Redshift: Data warehousing at Bizongo
- MongoDB: Know it conceptually for Plum's stack
- **Be ready to discuss:** When to pick SQL vs. NoSQL, sharding strategies, read replicas, connection pooling

**API Design (explicitly called out in JD):**
- REST API design principles (you designed migration APIs at Atlassian, Traversal API for Data Platform)
- Versioning strategies
- Pagination, rate limiting, authentication/authorization patterns
- API security best practices (OWASP)

**Event-Driven Architecture (now with a live Kafka system to demo):**
- **Endorsement system Kafka:** 4 topics, 88 partitions, `acks: all`, employerId partition key for per-employer ordering
- 24 event types as sealed Java interface records — compile-time exhaustiveness
- Event-carried state transfer: each event carries all data consumers need (no query-back)
- DynamoDB Streams CDC pattern at Atlassian
- Idempotency keys on every create operation (DB unique constraint + duplicate check in handler)
- When to use events vs. synchronous calls (endorsement state changes are events; insurer submission is sync with circuit breaker)

---

## 3. Distributed Systems / Scalability Decisions

### Your Proven Scalability Stories

**Performance Wins (quantified - this is powerful):**

| What | Before | After | Improvement |
|---|---|---|---|
| Jira DropDown P90 | 32s | 4s | 800% via SQL + JQL optimization |
| CI/CD pipeline execution | baseline | 4x faster | Parallelization + caching |
| Post Trade Data (Nasdaq) | baseline | 200% faster | Disk Merge Sort algorithm |
| CellId matching (Huawei) | baseline | 30% faster | Prefix Trie vs HashMap |
| Spring Boot startup | baseline | 30-50% faster | Lazy loading + JVM tuning |
| Collaborative editing (Confluence) | baseline | 20% faster | Algorithm + scalability improvements |
| **Endorsement API (Gatling)** | — | **p50=8ms, p95=181ms, p99=200ms** | **100% success rate under load** |

### Distributed Systems Topics to Be Ready For

**Consistency and availability trade-offs:**
- Your Data Abstraction Platform: How did you handle consistency across consumers?
- Soft deletes at scale: What consistency guarantees did you provide?
- CDC pipeline: eventual consistency - how did you handle lag, ordering, failures?

**Fault tolerance and resilience:**
- How you acted as escalation point for complex distributed failures at Whatfix
- Concurrency issues, deployment regressions, startup failures - specific examples
- How you translated incidents into architectural improvements (not just hotfixes)
- Circuit breaker patterns, retry policies, graceful degradation

**Scalability patterns to discuss:**
- Horizontal vs. vertical scaling decisions you've made
- Database scaling: read replicas, sharding, connection pooling
- Caching strategies (you implemented intelligent caching in CI/CD)
- Kubernetes pod right-sizing based on real workload behavior
- Queue-based load leveling (Kafka)

**Plum-specific scalability challenge (you've already modeled this):**
- Target: 100K employers x 10 changes/day = **1M endorsements/day** (~11.5/sec steady, ~58/sec peak)
- Your endorsement system's architecture handles this:
  - Kafka 88 partitions across 4 topics (can scale to 256+ partitions)
  - Java 21 virtual threads (no thread pool bottleneck)
  - HPA autoscaling 2-8 pods (CPU 70%, memory 80%)
  - HikariCP pool (30 connections), Hibernate batch_size=25
  - Rate limiting (50 req/sec default, burst to 100)
  - Data retention scheduler (archive after 365 days, prevent unbounded table growth)
- Be ready to discuss: "At 10M lives, what else breaks?" — multi-region, sharding, distributed caching (Redis vs Caffeine)

### SLOs/SLIs/Observability
- You implemented SLOs and SLIs at Atlassian and reduced on-call noise
- Splunk + SignalFX monitoring experience at Whatfix
- Be ready to discuss: What SLOs would you set for a claims processing system? What SLIs would you track?

---

## 4. Alignment and Interest in Plum

### Why Plum? (have a genuine, specific answer)

**Craft your narrative around these themes:**

1. **Scale challenge is compelling:** 500K -> 10M lives requires the kind of architectural thinking you excel at. This isn't incremental - it's a fundamental platform evolution.

2. **Domain impact:** Insurance touches real lives. Your work would directly affect whether someone gets their claim processed quickly during a health emergency. This is more meaningful than most SaaS.

3. **Build vs. maintain ratio:** Plum built their telehealth platform in-house. They're a company that invests in engineering. As Principal Architect, you'd be shaping systems, not just maintaining them.

4. **Startup to scale-up transition:** Plum is at the stage (approaching profitability, strong growth) where architectural decisions compound. You've navigated this exact transition at Bizongo and seen what scale looks like at Atlassian.

5. **Tech + insurance is underserved:** Indian insurance is being digitized. Plum is leading this. The architectural challenges (compliance, multi-insurer integration, real-time pricing, claims automation) are genuinely interesting.

**Show you've done your homework (and built something):**
- "I was so interested in the endorsement processing problem that I built a working prototype — hexagonal architecture, 4 insurer adapters including ICICI Lombard and Bajaj Allianz, 657 automated tests. Would it be useful if I walked you through it?"
- "Sankar, you mentioned targeting 95% API-led claims automation. My endorsement system has a similar STP rate tracking pipeline — I'd love to hear how your claims routing compares."
- "The 10M lives target means roughly 20x growth — my system is architected for 1M endorsements/day with Kafka partitioning and HPA autoscaling. What parts of your current architecture do you see needing the most investment?"
- "I noticed you've been building AI-powered claims routing at 40% automation. My system has an intelligence layer — anomaly detection, error auto-resolution, process mining — behind port interfaces so you can swap rule-based for ML when you have training data."

### Questions to Ask Sankar (shows genuine interest + domain knowledge)

- "In my endorsement system, I used a Strategy pattern for insurer adapters — each insurer gets its own adapter class behind an `InsurerPort` interface. How does Plum currently handle insurer integration diversity? Is each insurer custom-coded?"
- "You've achieved 95% API-led claims automation. What's the architecture behind your claims pipeline? Event-driven, workflow-based, or synchronous?"
- "My system has an 11-state endorsement lifecycle with a state machine. How does Plum model claims processing states? Is there a formal state machine or is it implicit in business logic?"
- "What's the biggest technical bottleneck today as you scale toward 10M lives? Is it compute, data, or integration with external insurers?"
- "How do you approach multi-tenancy for companies of vastly different sizes? Schema-per-tenant or row-level security?"
- "What's the team structure I'd be working with? How many engineering teams, and how are they organized?"
- "You've got 70% AI-led query resolution — what models are you using? Custom-trained or off-the-shelf LLMs?"
- "How do you handle reconciliation with insurers? In my system, I built a reconciliation engine that cross-references batch results — is that a problem space Plum deals with?"

---

## 5. Ownership and Technical Leadership

### Your Ownership Stories

**Frame ownership as: "I saw a problem, I took it on end-to-end, I delivered measurable outcomes"**

**Strongest ownership examples:**

1. **Endorsement System for Plum (THIS interview)** - "I was so interested in the problem that I built the system end-to-end — 7,000+ lines of Java, 657 automated tests across 6 layers, hexagonal architecture with 18 port interfaces, 4 insurer adapters, 5 intelligence pillars, full observability stack. One person, one week, production-grade quality."

2. **DevSecOps at Whatfix** - "I didn't just design the architecture - I wrote the core policy evaluation logic, enforcement hooks, and CI/CD integrations myself. I was the final technical decision-maker for platform, CI/CD, and security architecture."

3. **Data Abstraction Platform at Atlassian** - "I architected, designed, AND built it from scratch. When soft deletes had scalability issues, I personally rearchitected and solved them to meet SLOs."

4. **CI/CD Optimization at Whatfix** - "I personally identified bottlenecks across build, test, scan, and deploy stages. I modified pipeline definitions, scripts, and tooling. 4x improvement."

5. **Cost Optimization across Whatfix and Bizongo** - "I didn't delegate this. I ran Kubernetes right-sizing experiments, tuned JVM parameters, refactored Spring Boot startup paths. 150% efficiency gain."

6. **Jira P90 fix at Atlassian** - "A dropdown taking 32 seconds. I dug into the SQL, found the faulty queries, optimized JQL. Got it to 4 seconds."

### Technical Leadership Approach

Be ready to articulate your leadership philosophy:

**How you lead without authority:**
- "I lead by example - I write production code, not just architecture docs"
- "I mentor engineers on system design, debugging, performance optimization, and architectural reasoning"
- "I've been actively involved in hiring, promotion, and retention decisions"
- "I review and refactor critical code paths to set quality standards"

**How you make architectural decisions:**
- "I evaluate trade-offs explicitly - I consider business constraints, team capability, timeline, and long-term maintainability"
- "I translate technical trade-offs into business-level impact and risk discussions for non-technical stakeholders"
- "I balance short-term delivery with long-term architectural sustainability"

**How you handle disagreements:**
- Bring data: benchmarks, prototypes, production metrics
- Consider the team's ability to maintain what's built
- Optimize for the business outcome, not personal preference

**Cross-functional collaboration:**
- Worked with Security, Platform, SRE, and Product leadership at Whatfix
- Collaborated with Product, Design, Sales, Engineering Leadership at Bizongo
- Security collaboration with engineers and customers at Atlassian

---

## Potential Technical Deep-Dive Questions and Talking Points

### "Design a claims processing system for Plum"
**You don't have to think through this hypothetically — you've BUILT an analogous system.** Walk Sankar through your endorsement system as the answer:

- **Ingestion:** REST API → validation → idempotency check → Kafka event (`endorsement-events`, 32 partitions) — *you built this: `CreateEndorsementHandler`*
- **Processing:** State machine (11 states) — CREATED → VALIDATED → PROVISIONALLY_COVERED → SUBMITTED → CONFIRMED/REJECTED — *you built this: `EndorsementStateMachine` + `EndorsementStatus` enum with validated transitions*
- **Insurer integration:** Strategy pattern per insurer (`InsurerPort` interface, 4 adapters), circuit breaker + retry per insurer — *you built this: `InsurerRouter` factory + `MockInsurerAdapter`, `IciciLombardAdapter`, `NivaBupaAdapter`, `BajajAllianzAdapter`*
- **Notifications:** Event-carried state transfer via Kafka → WebSocket broadcast to frontend + webhook adapter for external integrations — *you built this: `KafkaEventPublisher` + `WebSocketEventBroadcaster` + `WebhookNotificationAdapter`*
- **Intelligence:** Anomaly detection, error auto-resolution, STP rate tracking, balance forecasting — all behind port interfaces — *you built this: 5 intelligence adapters*
- **Reconciliation:** Automated verification against insurer batch results — *you built this: `ReconciliationEngine`*
- **Key concerns:** Idempotency keys (DB unique constraint), optimistic locking (@Version on EAAccount), provisional coverage for no coverage gaps, data retention (archive after 365 days)

### "How would you migrate from monolith to microservices?"
- Strangler fig pattern (your experience at multiple companies)
- Start with the domain that changes most frequently
- API gateway as the facade
- Shared database -> database-per-service migration
- Event-driven communication between services
- Feature flags for gradual rollout

### "How would you handle multi-tenancy?"
- Logical isolation (same infra, tenant ID in every query/event)
- Tenant-aware rate limiting and resource quotas
- Data isolation for compliance (PII, insurance regulations)
- Configurable per-tenant: plan types, insurer partnerships, approval workflows

### "How would you approach Gen AI integration?" (mentioned in JD)
- Claims auto-classification and routing
- Document extraction (OCR + LLM for unstructured insurance documents)
- Chatbot for employee queries about their benefits
- Fraud detection signals
- Mention your awareness of OpenAI/Anthropic APIs, LangChain, vector databases

---

## Gap Analysis - Be Honest, Show Learning Agility

| Gap | How to Address It |
|---|---|
| **Node.js / Express** is Plum's primary backend; your strength is Java/Spring | "My architectural principles are language-agnostic. I've worked across Java, Python, and have picked up new stacks quickly. At Xerox I moved into ML/NLP. The patterns (event-driven, microservices, API design) transfer directly. Also — Java is listed in the JD, and Spring Boot 3 with virtual threads is competitive with Node.js for I/O-bound workloads." |
| **Go** listed in JD | "I haven't used Go in production, but I understand its strengths for high-concurrency, low-latency services. I'm genuinely interested in using it where it fits." |
| **Gen AI / LLM experience** | "My endorsement system has an AI/automation layer with 5 intelligence pillars behind port interfaces. Currently rule-based, but the architecture supports ML swap-in: Spring AI for anomaly detection (Isolation Forest), LangChain4j for error resolution (RAG pipeline). I understand the patterns — the system thinking to make AI reliable and scalable." |
| **Insurance domain knowledge** | "I built a complete endorsement processing system with multi-insurer integration, EA balance management, provisional coverage guarantees, and reconciliation. I modeled ICICI Lombard, Niva Bupa, and Bajaj Allianz adapters with their actual protocol differences (REST/JSON, CSV/SFTP, SOAP/XML). Plus financial data at Nasdaq (post-trade processing, compliance) and enterprise SaaS at Atlassian." |

---

## Day-Before Checklist

- [ ] Re-read this document once more in the morning
- [ ] **Run `./start.sh` and verify the endorsement system demo works** — frontend, Swagger, Grafana
- [ ] **Run `./run-all-tests.sh` and verify 657 tests pass** — have Allure report ready at :5050
- [ ] Pick your top 3 architecture stories: **Endorsement System**, Data Abstraction Platform, DevSecOps
- [ ] For each story, know the specific numbers (657 tests, 27 endpoints, p50=8ms, 100% success)
- [ ] Have 4-5 genuine questions ready for Sankar (see section above)
- [ ] Know Plum's basic facts: 500K lives, 6000+ companies, 10M lives target, 95% API-led claims automation
- [ ] Know Sankar's background: Amazon, Cisco, Intel, Jupiter — values operational excellence, data-driven decisions
- [ ] Be ready to whiteboard the hexagonal architecture diagram from `docs/deliverables/High_Level_Architecture.md`
- [ ] Have laptop ready to demo (charge overnight)
- [ ] Dress appropriately (it's on-site at Whitefield)

---

## One-Line Summary for Each Role (quick recall)

| Company | Duration | One-Liner |
|---|---|---|
| **Plum (prep)** | 2026 | **Built their endorsement system: hexagonal architecture, 4 insurer adapters, 657 tests, production-grade** |
| **Whatfix** | 2025-present | DevSecOps policy platform architecture + 4x CI/CD acceleration + 150% cost efficiency |
| **Bizongo** | 2024-2025 | Architectural ownership of 5 product platforms (SDUI, workflow, accounting, docs, supply chain) + CDC data pipeline |
| **Atlassian** | 2019-2024 | Data Abstraction Platform from scratch + OpenSearch migration + MFA + collaborative editing performance + Jira P90 fix |
| **Nasdaq** | 2017-2019 | Post-trade data processing 200% perf improvement via Disk Merge Sort |
| **Xerox** | 2015-2017 | ML/NLP model integration + 25% performance improvement |
| **Intel Security** | 2013-2015 | Cloud firewall management system v1 architecture |
| **Huawei** | 2010-2013 | Prefix Trie optimization + bandwidth control |

---

## Quick Architecture Reference (for whiteboard)

Draw this hexagonal diagram from memory:

```
                    ┌─ REST API (27 endpoints, 5 controllers) ─┐
                    │                                            │
          ┌─────── Application Layer (Stateless, CQRS) ────────┐
          │   CreateEndorsementHandler    ProcessEndorsementHandler    │
          │   EndorsementQueryHandler     9 Schedulers                 │
          └──────────────── depends on ──────────────────────────┘
                              │
          ┌──────── Domain Core (ZERO infra imports) ──────────┐
          │   Endorsement (11-state machine)  EAAccount         │
          │   18 Port Interfaces   24 Event Types (sealed)      │
          └──────────────── implemented by ─────────────────────┘
                              │
  ┌────── Infrastructure (Adapters) ───────────────────────────┐
  │   JPA (10 adapters)         Kafka (4 topics, 88 partitions) │
  │   4 Insurer Adapters        5 Intelligence Adapters          │
  │   WebSocket + Webhook       Resilience4j (CB + Retry)        │
  └─────────────────────────────────────────────────────────────┘
```

Key numbers to remember: **657 tests, 27 endpoints, 24 events, 18 ports, 15 migrations, 11 states, 9 schedulers, 7 dashboards, 5 intelligence pillars, 4 insurer adapters**

---

*You don't just talk architecture — you build it. 15+ years of hands-on experience across companies at different scales, and now a working prototype of the exact system Plum needs. That's what sets you apart.*
