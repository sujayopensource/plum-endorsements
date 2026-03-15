# Architectural Vision: Evolutionary & Continuous Architecture

**Project:** Plum Endorsement Management System
**Date:** March 14, 2026
**Scope:** 5-Year Architectural Evolution Roadmap (2026–2031)
**Theoretical Foundation:**
- *Building Evolutionary Architectures* — Neal Ford, Rebecca Parsons, Patrick Kua, Pramod Sadalage (O'Reilly, 2nd ed. 2023)
- *Continuous Architecture in Practice* — Murat Erder, Pierre Pureur, Eoin Woods (Addison-Wesley, 2021)

**Companion Documents:**
- [Endorsement System Execution Plan](../../Endorsement_System_Execution_Plan.md)
- [Product Evolution Vision](./Product_Evolution_Vision.md)
- [High-Level Architecture](../High_Level_Architecture.md)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Theoretical Foundations](#2-theoretical-foundations)
   - 2.1 [Building Evolutionary Architectures — Core Concepts](#21-building-evolutionary-architectures--core-concepts)
   - 2.2 [Continuous Architecture in Practice — Core Concepts](#22-continuous-architecture-in-practice--core-concepts)
   - 2.3 [Synthesis: Where Both Frameworks Meet](#23-synthesis-where-both-frameworks-meet)
3. [Current Architecture Assessment](#3-current-architecture-assessment)
   - 3.1 [Architectural Quantum Analysis](#31-architectural-quantum-analysis)
   - 3.2 [Connascence Audit](#32-connascence-audit)
   - 3.3 [Evolvability Scorecard](#33-evolvability-scorecard)
   - 3.4 [Technical Debt Inventory](#34-technical-debt-inventory)
4. [Fitness Functions](#4-fitness-functions)
   - 4.1 [Atomic Fitness Functions](#41-atomic-fitness-functions)
   - 4.2 [Holistic Fitness Functions](#42-holistic-fitness-functions)
   - 4.3 [Temporal Fitness Functions](#43-temporal-fitness-functions)
   - 4.4 [Fitness Function Implementation Plan](#44-fitness-function-implementation-plan)
5. [Architectural Runway](#5-architectural-runway)
   - 5.1 [Current Runway Capacity](#51-current-runway-capacity)
   - 5.2 [Runway Extension Plan](#52-runway-extension-plan)
   - 5.3 [Enabler Epics](#53-enabler-epics)
6. [Five-Year Evolution Roadmap](#6-five-year-evolution-roadmap)
   - 6.1 [Year 1 (2026): Modular Monolith Hardening](#61-year-1-2026-modular-monolith-hardening)
   - 6.2 [Year 2 (2027): Service Extraction & Multi-Region](#62-year-2-2027-service-extraction--multi-region)
   - 6.3 [Year 3 (2028): Platform Maturity & Marketplace](#63-year-3-2028-platform-maturity--marketplace)
   - 6.4 [Year 4 (2029): Intelligence Platform & Data Mesh](#64-year-4-2029-intelligence-platform--data-mesh)
   - 6.5 [Year 5 (2030–2031): Global Scale & Autonomous Operations](#65-year-5-20302031-global-scale--autonomous-operations)
7. [Quality Attribute Evolution](#7-quality-attribute-evolution)
   - 7.1 [Security Evolution](#71-security-evolution)
   - 7.2 [Scalability Evolution](#72-scalability-evolution)
   - 7.3 [Performance Evolution](#73-performance-evolution)
   - 7.4 [Resilience Evolution](#74-resilience-evolution)
   - 7.5 [Observability Evolution](#75-observability-evolution)
8. [Data Architecture Evolution](#8-data-architecture-evolution)
   - 8.1 [Current: Single PostgreSQL](#81-current-single-postgresql)
   - 8.2 [Phase 1: Read Replicas & CQRS Materialisation](#82-phase-1-read-replicas--cqrs-materialisation)
   - 8.3 [Phase 2: Domain-Owned Data Products](#83-phase-2-domain-owned-data-products)
   - 8.4 [Phase 3: Data Mesh](#84-phase-3-data-mesh)
   - 8.5 [Evolutionary Database Design](#85-evolutionary-database-design)
9. [Incremental Change Patterns](#9-incremental-change-patterns)
   - 9.1 [Strangler Fig Pattern](#91-strangler-fig-pattern)
   - 9.2 [Branch by Abstraction](#92-branch-by-abstraction)
   - 9.3 [Parallel Runs](#93-parallel-runs)
   - 9.4 [Feature Toggles](#94-feature-toggles)
   - 9.5 [Expand-Contract Migrations](#95-expand-contract-migrations)
10. [Governance Model](#10-governance-model)
    - 10.1 [Automated Governance via Fitness Functions](#101-automated-governance-via-fitness-functions)
    - 10.2 [Architecture Decision Records (ADRs)](#102-architecture-decision-records-adrs)
    - 10.3 [Architecture Guardrails](#103-architecture-guardrails)
    - 10.4 [Lightweight Review Process](#104-lightweight-review-process)
11. [Team Topology Evolution](#11-team-topology-evolution)
    - 11.1 [Current: Single Team](#111-current-single-team)
    - 11.2 [Phase 1: Stream-Aligned Teams](#112-phase-1-stream-aligned-teams)
    - 11.3 [Phase 2: Platform Team Emergence](#113-phase-2-platform-team-emergence)
    - 11.4 [Inverse Conway Maneuver](#114-inverse-conway-maneuver)
12. [Architecture Decision Records](#12-architecture-decision-records)
13. [Risk Matrix & Mitigation](#13-risk-matrix--mitigation)
14. [References](#14-references)

---

## 1. Executive Summary

The Plum Endorsement platform has evolved through three successful phases — from MVP to multi-insurer scale to an intelligence layer — following a hexagonal (ports & adapters) architecture. This document defines how the architecture will continue to evolve over the next five years, grounded in two complementary theoretical frameworks:

- **Building Evolutionary Architectures** (Ford, Parsons, Kua, Sadalage): Provides the mechanism for *guided, incremental change* through **fitness functions** — automated checks that ensure architectural characteristics are preserved as the system evolves. The key insight: architecture must be designed to change, not merely to work.

- **Continuous Architecture in Practice** (Erder, Pureur, Woods): Provides the *decision-making framework* through six principles, four essential activities (architectural decisions, quality attributes, technical debt management, feedback loops), and architectural tactics for security, scalability, performance, and resilience.

### Key Architectural Thesis

> The Plum Endorsement platform will evolve from a **well-structured modular monolith** into a **selectively-decomposed service-oriented platform** over 5 years, guided by fitness functions that protect critical quality attributes, and governed by lightweight ADR-based decision processes. Decomposition will follow the **last responsible moment** principle — services are extracted only when scaling, team, or deployment independence demands it.

### Evolution Timeline

```
2026 ──────── 2027 ──────── 2028 ──────── 2029 ──────── 2030-31
   │              │              │              │              │
   ▼              ▼              ▼              ▼              ▼
 MODULAR       SERVICE        PLATFORM      INTELLIGENCE    GLOBAL
 MONOLITH     EXTRACTION      MATURITY       PLATFORM       SCALE
 HARDENING   & MULTI-REGION  & MARKETPLACE  & DATA MESH    & AUTONOMY
   │              │              │              │              │
   │  Fitness     │  Strangler   │  API         │  Data Mesh   │  Multi-region
   │  functions   │  fig for     │  marketplace │  per-domain  │  active-active
   │  established │  insurer     │  with rate   │  data        │  99.99% SLA
   │              │  gateway     │  limiting    │  products    │
   │  Auth +      │              │              │              │
   │  ShedLock    │  CQRS read   │  Self-service│  ML pipeline │  Autonomous
   │  production- │  replicas    │  insurer     │  platform    │  operations
   │  ready       │              │  onboarding  │              │
```

---

## 2. Theoretical Foundations

### 2.1 Building Evolutionary Architectures — Core Concepts

> *"An evolutionary architecture supports guided, incremental change across multiple dimensions."*
> — Ford, Parsons, Kua (2023)

#### 2.1.1 The Three Pillars

| Pillar | Definition | Application to Plum |
|--------|-----------|-------------------|
| **Guided** | Architecture doesn't evolve randomly; it's directed by fitness functions that protect critical characteristics | Fitness functions for response time, coupling metrics, security compliance, and zero-coverage-gap invariant |
| **Incremental** | Changes are small, testable, and reversible; no big-bang rewrites | Strangler fig pattern for service extraction; expand-contract for schema migration; feature toggles for gradual rollout |
| **Multiple dimensions** | Evolution must consider all architectural characteristics simultaneously, not just one | Balancing performance, security, scalability, and domain integrity as the system grows |

#### 2.1.2 Fitness Functions

An **architectural fitness function** is any mechanism that performs an objective integrity assessment of some architecture characteristic or combination of architecture characteristics. Fitness functions are the evolutionary architecture equivalent of unit tests for code — they make architectural intent executable.

**Classification Matrix:**

| Dimension | Atomic | Holistic |
|-----------|--------|----------|
| **Triggered** | Unit test verifying response time < 200ms | Integration test verifying end-to-end endorsement lifecycle < 5s |
| **Continuous** | Prometheus alert on P99 latency > 500ms | Synthetic transaction monitoring the full endorsement flow in production |
| **Automated** | ArchUnit test preventing domain→infrastructure imports | CI pipeline rejecting deployments where circuit breaker open rate > 10% |
| **Manual** | Quarterly security review against OWASP Top 10 | Annual architecture review against business scaling projections |

#### 2.1.3 Architectural Quantum

An **architectural quantum** is the smallest independently deployable unit with high functional cohesion, which includes all the structural elements required for the system to function properly. The quantum defines the scope of coupling.

**Plum's current quantum:** The entire Spring Boot application + PostgreSQL + Redis + Kafka. This is a *single quantum* — a modular monolith. The evolution strategy is to selectively decompose this into multiple quanta where deployment independence justifies the distributed systems complexity.

#### 2.1.4 Connascence

Connascence measures the degree and type of dependency between software components across three dimensions:

| Dimension | Measure | Goal |
|-----------|---------|------|
| **Strength** | How difficult is the coupling to refactor? | Convert strong forms to weaker forms |
| **Locality** | How close are the coupled components? | Stronger connascence only within modules; weaker across boundaries |
| **Degree** | How many components are affected? | Minimise the number of components involved in any coupling |

**Jim Weirich's Rules:**
1. **Rule of Degree:** Convert strong forms of connascence into weaker forms
2. **Rule of Locality:** As distance between elements increases, use weaker forms of connascence

#### 2.1.5 Incremental Change Patterns

| Pattern | Use Case | Risk Level |
|---------|----------|-----------|
| **Strangler Fig** | Gradually replace a monolith component with a new service | LOW — old and new run side by side |
| **Branch by Abstraction** | Replace an implementation behind an interface without disruption | LOW — the port/adapter pattern is this |
| **Parallel Runs** | Run old and new implementations simultaneously, compare results | MEDIUM — requires result comparison infrastructure |
| **Feature Toggles** | Release code to production without activating it | LOW — but toggle management complexity grows |
| **Expand-Contract** | Database schema changes that are always backwards-compatible | LOW — Flyway migrations already follow this |

#### 2.1.6 Anti-Patterns to Avoid

| Anti-Pattern | Description | How Plum Avoids It |
|-------------|-------------|-------------------|
| **Vendor Lock-In** | Over-reliance on proprietary cloud services | Port/adapter pattern isolates all infrastructure behind interfaces |
| **Last 10% Trap** | "Almost done" rewrites that never ship | Incremental extraction via strangler fig, never big-bang rewrite |
| **Inappropriate Governance** | Either too much (architecture review board bottleneck) or too little (no guardrails) | Automated fitness functions + lightweight ADR process |
| **Reporting Atop System of Record** | Building analytics on the operational database | CQRS separation + dedicated analytics data products |
| **Leaky Abstractions** | Infrastructure concerns bleeding into domain | Hexagonal architecture + ArchUnit tests enforcing dependency direction |

---

### 2.2 Continuous Architecture in Practice — Core Concepts

> *"Software architecture is a set of structures needed to reason about a software system and the discipline of creating such structures."*
> — Erder, Pureur, Woods (2021)

#### 2.2.1 The Six Principles

| # | Principle | Application to Plum |
|---|-----------|-------------------|
| 1 | **Architect products, not solutions** | The endorsement platform is a product with a multi-year roadmap, not a one-off solution. Architecture decisions serve the product lifecycle. |
| 2 | **Focus on quality attributes, not functional requirements** | Functional requirements (create endorsement, submit batch) change frequently. Quality attributes (< 200ms response time, zero coverage gap, 99.9% availability) are the enduring architectural drivers. |
| 3 | **Delay design decisions until absolutely necessary** | Follow the **last responsible moment** principle. Don't decompose into microservices until scaling or team independence demands it. Don't choose a multi-region strategy until the business enters a second geography. |
| 4 | **Architect for change — leverage the power of small** | Small, reversible changes: Flyway migrations, feature toggles, expand-contract schemas. Every port interface is a seam where a new adapter can be injected without modifying existing code. |
| 5 | **Architect for build, test, deploy, and operate** | CI/CD pipeline, Docker packaging, K8s manifests, Prometheus metrics, Grafana dashboards, and structured logging are first-class architectural concerns, not afterthoughts. |
| 6 | **Model the organization of your teams after the architecture** | Conway's Law as a design tool. As the platform grows, team boundaries should mirror module boundaries (Inverse Conway Maneuver). |

#### 2.2.2 Four Essential Activities

| Activity | Definition | Current State at Plum |
|----------|-----------|---------------------|
| **Architectural Decisions** | The primary unit of architectural work. Decisions are recorded, reasoned, and reversible where possible. | Currently informal. ADR process proposed (Section 10.2). |
| **Quality Attributes** | The cross-cutting requirements that drive architecture. Performance, security, scalability, resilience, observability. | Well-instrumented: 20+ Prometheus metrics, Grafana dashboards, Resilience4j circuit breakers. Formal quality attribute scenarios needed. |
| **Technical Debt** | Conscious tracking and management of shortcuts and deferred work. | GAPS.md tracks known issues. Need formal debt classification and paydown scheduling. |
| **Feedback Loops** | Mechanisms for learning from production and feeding insights back into architecture decisions. | Prometheus/Grafana/ELK provide data. Need to close the loop: automated alerts → architectural response. |

#### 2.2.3 Architectural Tactics

Erder, Pureur, and Woods organise architectural tactics by quality attribute. Each tactic is a design decision that influences the achievement of a quality attribute:

**Security Tactics:**
- Detect attacks (intrusion detection, security scanning)
- Resist attacks (authentication, authorisation, input validation, encryption)
- React to attacks (rate limiting, circuit breaking, audit logging)
- Recover from attacks (backup/restore, incident response)

**Scalability Tactics:**
- Horizontal scaling (stateless processes, container orchestration)
- Load distribution (load balancing, request routing)
- Data partitioning (sharding, read replicas)
- Asynchronous processing (event-driven, message queues)
- Caching (application-level, distributed, CDN)

**Performance Tactics:**
- Reduce computational overhead (algorithm optimisation, caching)
- Manage resources (connection pooling, thread management)
- Reduce latency (async I/O, virtual threads, response compression)
- Data access optimisation (indexes, query tuning, batch reads)

**Resilience Tactics:**
- Fault detection (health checks, heartbeats, monitoring)
- Fault recovery (retry, circuit breaker, bulkhead, fallback)
- Fault prevention (redundancy, graceful degradation, chaos engineering)

#### 2.2.4 Minimum Viable Architecture (MVA)

The MVA concept aligns with Principle 3 (delay decisions). At each phase, we define the minimum architecture needed to support the current and near-term business requirements:

```
Phase 1 MVA: Single monolith + single database + single insurer
Phase 2 MVA: Modular monolith + Kafka + multi-insurer adapters + Redis cache
Phase 3 MVA: Intelligence services + observability stack + batch optimisation
Phase 4 MVA: Authentication + service gateway + CQRS read replicas
Phase 5 MVA: Multi-region + data mesh + service extraction
```

Each phase adds architectural capability only when needed — the architecture runway grows to support the next 2-3 quarters of feature delivery.

---

### 2.3 Synthesis: Where Both Frameworks Meet

The two books are complementary, not competing:

| Concern | Building Evolutionary Architectures | Continuous Architecture in Practice | Synthesis for Plum |
|---------|-------------------------------------|-------------------------------------|-------------------|
| **How to ensure architecture quality** | Fitness functions (automated checks) | Quality attribute scenarios (documented requirements) | Define quality attribute scenarios → implement as fitness functions |
| **How to make changes safely** | Incremental change patterns (strangler fig, branch by abstraction) | Architectural tactics (design decisions for quality) | Apply tactics through incremental patterns |
| **How to govern** | Automated governance via CI/CD fitness functions | ADRs + lightweight review process | Fitness functions in CI/CD + ADR log for significant decisions |
| **How to manage coupling** | Connascence analysis + architectural quanta | Principle 4 (power of small) + team topology | Connascence audit drives decomposition decisions |
| **How to handle data** | Evolutionary database design (expand-contract) | Data as an architectural concern (Ch. 3) | Flyway migrations + data mesh evolution |
| **How to scale teams** | Quantum boundaries define deployment units | Principle 6 (Conway's Law) + team topologies | Inverse Conway: team boundaries mirror module boundaries |

---

## 3. Current Architecture Assessment

### 3.1 Architectural Quantum Analysis

**Current State: Single Quantum**

```
┌─────────────────────────────────────────────────────────────────┐
│                    SINGLE ARCHITECTURAL QUANTUM                  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Spring Boot Application                      │   │
│  │  6 Controllers │ 3 Handlers │ 8 Services │ 9 Schedulers  │   │
│  │  21 Models │ 18 Ports │ 10 JPA Adapters │ 4 Insurers     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌────────┐  ┌───────┐  ┌──────┐  ┌─────────┐  ┌──────────┐   │
│  │Postgres│  │ Redis │  │Kafka │  │Prometheus│  │   ELK    │   │
│  │ (17    │  │(cache)│  │(5    │  │+ Grafana │  │  Stack   │   │
│  │ tables)│  │       │  │topics)│ │          │  │          │   │
│  └────────┘  └───────┘  └──────┘  └─────────┘  └──────────┘   │
│                                                                  │
│  Deployment: Single Docker container + infrastructure services   │
│  Scaling: Vertical only (no distributed coordination yet)        │
└─────────────────────────────────────────────────────────────────┘
```

**Quantum Characteristics:**

| Characteristic | Value | Evolvability Impact |
|---------------|-------|-------------------|
| Deployability | Single artifact (JAR) | POSITIVE — simple, fast deployments |
| Testability | 705+ tests, all passing | POSITIVE — high confidence in changes |
| Modularity | Hexagonal with clear port/adapter boundaries | POSITIVE — ready for extraction |
| Coupling | Internal (method calls); external via Kafka only | NEUTRAL — tight internal, loose external |
| Scalability | Limited by single-process bottleneck | NEGATIVE — must scale entire app for any hotspot |

**Target State (Year 3): Three Quanta**

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────┐
│  ENDORSEMENT CORE    │  │  INSURER GATEWAY     │  │  INTELLIGENCE    │
│                      │  │                      │  │  PLATFORM        │
│  Endorsement CRUD    │  │  Adapter routing     │  │  Anomaly detect  │
│  State machine       │  │  Circuit breakers    │  │  Forecasting     │
│  EA balance          │  │  Format translation  │  │  Process mining  │
│  Batch assembly      │  │  Rate limiting       │  │  Health scoring  │
│  Reconciliation      │  │  Insurer onboarding  │  │  Benchmarking    │
│                      │  │                      │  │                  │
│  PostgreSQL (core)   │  │  PostgreSQL (config) │  │  TimescaleDB     │
│  Redis               │  │  Redis               │  │  ClickHouse      │
└─────────┬────────────┘  └──────────┬───────────┘  └────────┬─────────┘
          │                          │                        │
          └──────────────────────────┴────────────────────────┘
                              Kafka Event Bus
```

### 3.2 Connascence Audit

**Current Connascence Analysis:**

| Connascence Type | Strength | Locality | Instances in Codebase | Assessment |
|-----------------|----------|----------|----------------------|------------|
| **Name** (CoN) | WEAK | Within modules | All interface references: `EndorsementRepository`, `InsurerPort` | ACCEPTABLE — standard OO programming |
| **Type** (CoT) | WEAK | Within modules | Method parameter types, return types | ACCEPTABLE — Java's type system manages this |
| **Meaning** (CoM) | MODERATE | Cross-module | `EndorsementStatus` enum used by handlers, controllers, persistence | ACCEPTABLE — centralised in domain model |
| **Position** (CoP) | MODERATE | Within adapters | JPA entity field ordering, CSV column ordering in `NivaBupaCsvMapper` | MONITOR — could become fragile |
| **Algorithm** (CoA) | STRONG | Cross-module | Idempotency key generation (must match between producer and consumer) | REFACTOR CANDIDATE — centralise algorithm |
| **Execution** (CoE) | STRONG | Cross-scheduler | Scheduler execution order assumptions (batch assembly → batch submission → reconciliation) | RISK — no formal ordering guarantee |
| **Timing** (CoTm) | STRONG | Cross-system | Insurer API timeout assumptions baked into circuit breaker config | MONITOR — configuration manages this |
| **Identity** (CoI) | STRONG | Within domain | `Endorsement` entity identity (`UUID id`) shared across all layers | ACCEPTABLE — fundamental to the domain |

**Action Items:**
1. **CoA (Algorithm):** Extract idempotency key generation into a shared domain service with a single implementation
2. **CoE (Execution):** Introduce explicit scheduler dependency declarations or a workflow orchestrator for scheduler chains
3. **CoP (Position):** Add integration tests that verify CSV column ordering against the Niva Bupa specification document

### 3.3 Evolvability Scorecard

| Dimension | Score (1-5) | Evidence | Target (Year 2) |
|-----------|-------------|---------|-----------------|
| **Deployability** | 4 | Docker + K8s manifests, health checks, rolling updates | 5 — Blue-green with canary |
| **Testability** | 5 | 705+ tests across 5 levels (unit, API, BDD, E2E, perf) | 5 — Maintain |
| **Modularity** | 4 | Hexagonal architecture, clear port/adapter boundaries | 5 — Module-level build separation |
| **Appropriate Coupling** | 3 | Good domain isolation, but schedulers tightly coupled to repositories | 4 — Extract scheduler coordination |
| **Incremental Change** | 4 | Flyway migrations, feature flags via `@Value` | 5 — Full feature toggle system |
| **Observability** | 4 | Prometheus, Grafana, Jaeger, ELK, 20+ custom metrics | 5 — SLO-based alerting |
| **Security** | 2 | Rate limiting, audit logging, CORS — but no auth/authz | 4 — JWT + OAuth2 + RBAC |
| **Resilience** | 3 | Circuit breakers, retry, fallbacks — but no graceful shutdown, no chaos testing | 4 — Graceful shutdown + chaos experiments |
| **Data Evolvability** | 3 | Flyway, expand-contract — but single database, no read replicas | 4 — CQRS read model |
| **Team Scalability** | 2 | Single-team monolith with shared codebase | 3 — Module ownership conventions |
| **Overall** | **3.4** | | **4.3** |

### 3.4 Technical Debt Inventory

Applying the Continuous Architecture framework for technical debt classification:

| Debt Item | Type | Impact | Cost to Fix | Priority | Phase |
|-----------|------|--------|-------------|----------|-------|
| No authentication/authorisation | **Deliberate-Prudent** | HIGH | MEDIUM | P0 | Year 1 |
| No graceful shutdown (`server.shutdown: graceful`) | **Inadvertent-Prudent** | MEDIUM | LOW | P0 | Year 1 |
| No distributed scheduler coordination (ShedLock partially done) | **Deliberate-Prudent** | HIGH | LOW | P0 | Year 1 |
| Schedulers with tight repository coupling | **Inadvertent-Imprudent** | MEDIUM | MEDIUM | P1 | Year 1 |
| No formal API versioning strategy | **Deliberate-Prudent** | MEDIUM | LOW | P1 | Year 1 |
| Single database for OLTP + analytics queries | **Deliberate-Prudent** | HIGH | HIGH | P2 | Year 2 |
| No contract testing between modules | **Inadvertent-Prudent** | MEDIUM | MEDIUM | P2 | Year 2 |
| No chaos engineering or fault injection testing | **Deliberate-Prudent** | MEDIUM | MEDIUM | P2 | Year 2 |
| Mock insurer adapters simulate rather than validate real protocols | **Deliberate-Prudent** | LOW | HIGH | P3 | Year 2 |
| No multi-currency support in EA balance calculation | **Deliberate-Prudent** | HIGH | HIGH | P3 | Year 2 |

**Debt Classification Legend (Martin Fowler's Quadrant):**
- **Deliberate-Prudent:** "We know this is debt, but we chose to defer it for business reasons."
- **Inadvertent-Prudent:** "We didn't know this was a problem until we learned more."
- **Inadvertent-Imprudent:** "We didn't know what we were doing."
- **Deliberate-Imprudent:** "We don't have time for design." (None identified — the codebase is well-designed.)

---

## 4. Fitness Functions

> *"An architectural fitness function is any mechanism that performs an objective integrity assessment of some architecture characteristic or combination of architecture characteristics."*
> — Ford, Parsons, Kua (2023)

### 4.1 Atomic Fitness Functions

Atomic fitness functions test a single architectural characteristic in isolation.

| ID | Characteristic | Fitness Function | Trigger | Tool | Current Status |
|----|---------------|-----------------|---------|------|---------------|
| FF-01 | **Modularity** | No domain model imports `jakarta.persistence.*` or `org.springframework.*` | Every commit | ArchUnit | NOT IMPLEMENTED |
| FF-02 | **Modularity** | No handler imports `infrastructure.*` packages | Every commit | ArchUnit | NOT IMPLEMENTED |
| FF-03 | **Modularity** | All handler fields are `private final` (stateless check) | Every commit | ArchUnit | NOT IMPLEMENTED |
| FF-04 | **Test Coverage** | Line coverage >= 80% for `domain/` and `application/` packages | Every commit | JaCoCo | NOT IMPLEMENTED |
| FF-05 | **Performance** | Unit test: `EABalanceCalculator` completes 1000 calculations in < 100ms | Every commit | JUnit 5 | NOT IMPLEMENTED |
| FF-06 | **Security** | No PII fields (`employeeName`, `dateOfBirth`) appear in log output | Every commit | Custom Grep test | NOT IMPLEMENTED |
| FF-07 | **API Stability** | OpenAPI spec diff shows no breaking changes (removed endpoints, changed types) | Every commit | openapi-diff | NOT IMPLEMENTED |
| FF-08 | **Data Integrity** | Flyway migration naming follows `V{n}__{description}.sql` pattern | Every commit | Custom script | PARTIAL (convention exists) |
| FF-09 | **Dependency** | No circular dependencies between packages | Every commit | ArchUnit | NOT IMPLEMENTED |
| FF-10 | **Security** | All `@RestController` methods have `@PreAuthorize` annotation | Every commit | ArchUnit | NOT IMPLEMENTED (no auth yet) |

### 4.2 Holistic Fitness Functions

Holistic fitness functions test multiple architectural characteristics together.

| ID | Characteristics | Fitness Function | Trigger | Tool | Current Status |
|----|----------------|-----------------|---------|------|---------------|
| FF-H1 | **Performance + Correctness** | End-to-end endorsement creation: API response < 200ms AND correct status returned AND event published to Kafka | Integration test suite | Spring Boot Test + Testcontainers | PARTIAL (API tests exist, timing not asserted) |
| FF-H2 | **Resilience + Performance** | With insurer circuit breaker OPEN, endorsement creation still succeeds within 500ms (queues for batch) | Integration test | Resilience4j test + Testcontainers | NOT IMPLEMENTED |
| FF-H3 | **Security + Observability** | Audit log entry created for every state transition AND contains traceId AND no PII | Integration test | Custom test | PARTIAL (audit logs exist, PII check missing) |
| FF-H4 | **Scalability + Correctness** | Under 100 concurrent requests, zero endorsement duplicates (idempotency key holds) | Performance test | Gatling | NOT IMPLEMENTED |
| FF-H5 | **Data Integrity + Resilience** | EA balance remains correct after crash recovery mid-transaction | Integration test | Testcontainers + kill container | NOT IMPLEMENTED |

### 4.3 Temporal Fitness Functions

Temporal fitness functions execute on a schedule rather than per-commit, monitoring architectural drift over time.

| ID | Characteristic | Fitness Function | Schedule | Tool | Current Status |
|----|---------------|-----------------|----------|------|---------------|
| FF-T1 | **Performance** | P99 API response time < 500ms over rolling 24h window | Continuous | Prometheus + Grafana alert | PARTIAL (metrics exist, alert rule not configured) |
| FF-T2 | **Reliability** | Circuit breaker open events < 5 per hour per insurer | Continuous | Prometheus + Grafana alert | NOT IMPLEMENTED |
| FF-T3 | **Security** | CVE scan of all dependencies: zero CRITICAL, < 5 HIGH | Weekly | OWASP Dependency-Check | NOT IMPLEMENTED |
| FF-T4 | **Coupling** | Afferent/efferent coupling metrics for each package stay within thresholds | Monthly | JDepend or ArchUnit | NOT IMPLEMENTED |
| FF-T5 | **Deployment** | Mean time to deploy (commit → production) < 15 minutes | Continuous | CI/CD metrics | NOT IMPLEMENTED (no CI/CD pipeline yet) |
| FF-T6 | **Availability** | Synthetic transaction (create → confirm endorsement) succeeds in production every 5 minutes | Continuous | Custom health check / Synthetic monitoring | NOT IMPLEMENTED |
| FF-T7 | **Data Freshness** | Process mining metrics updated within last 24 hours | Daily | Custom health check | NOT IMPLEMENTED |
| FF-T8 | **Encryption** | TLS certificates have > 30 days until expiry | Daily | Custom check | NOT IMPLEMENTED |

### 4.4 Fitness Function Implementation Plan

**Year 1 Priority (implement first):**
1. FF-01, FF-02, FF-03 (ArchUnit modularity) — protect hexagonal architecture
2. FF-06 (PII in logs) — security baseline
3. FF-07 (API stability) — prevent accidental breaking changes
4. FF-T1 (P99 latency alert) — production observability
5. FF-T3 (CVE scan) — security hygiene

**Year 2 Priority:**
1. FF-H1, FF-H2 (holistic performance + resilience)
2. FF-H4 (concurrent idempotency)
3. FF-T4 (coupling metrics)
4. FF-T5 (deployment velocity)
5. FF-T6 (synthetic transactions in production)

**Year 3+ Priority:**
1. FF-H5 (crash recovery data integrity)
2. FF-T2 (circuit breaker alerting)
3. All remaining functions

---

## 5. Architectural Runway

> *"The architectural runway consists of the existing code, components, and technical infrastructure necessary to implement near-term features without excessive redesign and delay."*
> — SAFe / Continuous Architecture

### 5.1 Current Runway Capacity

The hexagonal architecture established in Phases 1-3 provides significant runway:

| Near-Term Feature | Runway Available? | Evidence |
|-------------------|------------------|----------|
| New insurer integration (e.g., HDFC Ergo) | YES | `InsurerPort` interface + `InsurerRouter` auto-discovery. Add adapter class + DB row. |
| Authentication & authorisation | YES | `SecurityConfig.java` already has STATELESS session config. `AuthenticationPort` interface can be added to domain. |
| New endorsement type (e.g., salary revision) | YES | `EndorsementType` enum is extensible. State machine handles all types uniformly. |
| Multi-currency EA balance | PARTIAL | `EABalanceCalculator` uses `BigDecimal` but no currency field. Requires schema migration + calculator update. |
| CQRS read model | PARTIAL | Query handler is already `@Transactional(readOnly = true)`. Needs read-replica infrastructure. |
| Service extraction (insurer gateway) | YES | `InsurerPort`, `InsurerRouter`, `InsurerRegistry` are self-contained behind interfaces. Clean extraction boundary. |
| Multi-region deployment | NO | Single-database assumption throughout. Requires significant data replication strategy. |
| API marketplace | NO | No API gateway, no rate limiting per tenant, no API key management. |

### 5.2 Runway Extension Plan

Each quarter, invest 20-30% of engineering capacity in **enabler stories** that extend the runway for the next 2-3 quarters of feature delivery:

```
Q2 2026: Fitness function infrastructure (ArchUnit, JaCoCo, openapi-diff)
         Auth port + adapter (JWT + OAuth2)
         Graceful shutdown + ShedLock hardening

Q3 2026: API versioning strategy (URL path versioning: /api/v1/, /api/v2/)
         Contract testing framework (Spring Cloud Contract or Pact)
         Feature toggle system (database-backed, not just @Value)

Q4 2026: Read replica infrastructure (PostgreSQL streaming replication)
         API gateway evaluation (Spring Cloud Gateway vs Kong)
         Chaos engineering framework (Chaos Monkey for Spring Boot)

Q1 2027: Service extraction seam preparation (insurer gateway module)
         Multi-region data strategy (active-passive vs active-active)
         Event schema registry (Confluent Schema Registry or equivalent)
```

### 5.3 Enabler Epics

| Epic | Quarter | Business Value | Runway Extended For |
|------|---------|---------------|-------------------|
| E1: Automated Governance Pipeline | Q2 2026 | Prevents architectural drift; reduces manual review burden | All future development |
| E2: Authentication & Authorisation | Q2 2026 | Unlocks multi-tenant, role-based access | Phase 4 features |
| E3: API Versioning | Q3 2026 | Enables backwards-compatible API evolution | API marketplace |
| E4: Feature Toggle System | Q3 2026 | Enables gradual rollout, A/B testing, kill switches | All feature development |
| E5: CQRS Read Infrastructure | Q4 2026 | Decouples read/write scaling; reduces OLTP load from analytics | Intelligence platform |
| E6: Service Extraction Preparation | Q1 2027 | Clean seams for insurer gateway extraction | Multi-region, independent scaling |
| E7: Event Schema Evolution | Q1 2027 | Backward-compatible event format changes | Service decomposition |

---

## 6. Five-Year Evolution Roadmap

### 6.1 Year 1 (2026): Modular Monolith Hardening

**Theme:** *Make the monolith production-grade and governable*

**Continuous Architecture Principle Applied:** #5 (Architect for build, test, deploy, and operate)

**Key Initiatives:**

| Initiative | Effort | Description |
|-----------|--------|-------------|
| **Fitness Function Pipeline** | 2 weeks | ArchUnit tests (FF-01 to FF-10), JaCoCo coverage gates, openapi-diff in CI |
| **JWT + OAuth2 Authentication** | 3 weeks | `AuthenticationPort` in domain; JWT adapter in infrastructure; Spring Security integration; RBAC with 4 roles |
| **Graceful Shutdown** | 1 day | `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s` |
| **ShedLock Hardening** | 1 week | Lock all 9 schedulers with appropriate lock durations; test multi-instance behaviour |
| **API Versioning** | 1 week | URL path versioning (`/api/v1/`); document versioning strategy in ADR |
| **ADR Process** | 1 week | Template, storage in `docs/adr/`, numbering convention, review process |
| **Contract Testing** | 2 weeks | Spring Cloud Contract for internal module boundaries |
| **Feature Toggle System** | 2 weeks | Database-backed toggles with admin API; replace `@Value` boolean flags |

**Fitness Functions Added:** FF-01 through FF-10, FF-T1, FF-T3

**Architectural Quantum:** Remains single quantum. Internal modularity strengthened.

**Exit Criteria:**
- All 10 atomic fitness functions green in CI
- Authentication deployed and functional
- Zero P0 technical debt items remaining
- ADR log established with first 5 decisions recorded

---

### 6.2 Year 2 (2027): Service Extraction & Multi-Region

**Theme:** *Selectively decompose where scaling demands it; prepare for geographic expansion*

**Continuous Architecture Principle Applied:** #3 (Delay decisions until absolutely necessary) — decomposition happens now because insurer integration scaling and regional requirements diverge from core endorsement processing.

**Evolutionary Architecture Pattern:** Strangler Fig for insurer gateway extraction.

**Key Initiatives:**

| Initiative | Effort | Description |
|-----------|--------|-------------|
| **Insurer Gateway Service Extraction** | 6 weeks | Extract `InsurerPort` adapters, `InsurerRouter`, `InsurerRegistry` into standalone service. Strangler fig via API gateway routing. |
| **API Gateway** | 3 weeks | Spring Cloud Gateway or Kong as entry point. Rate limiting per client. SSL termination. |
| **CQRS Read Replicas** | 4 weeks | PostgreSQL streaming replication. `EndorsementQueryHandler` routes to read replica. |
| **Event Schema Registry** | 2 weeks | Apache Avro or JSON Schema for `EndorsementEvent`. Backward compatibility enforcement. |
| **Multi-Region Preparation** | 4 weeks | Active-passive with PostgreSQL logical replication. DNS-based failover. |
| **Chaos Engineering** | 2 weeks | Chaos Monkey for Spring Boot. Monthly game days. |
| **Multi-Currency EA Support** | 3 weeks | Currency field in EA schema. Exchange rate service port. Balance calculator update. |

**Strangler Fig Execution Plan:**

```
BEFORE (Single Quantum):
┌──────────────────────────────┐
│  Endorsement Service         │
│  ┌─────────┐ ┌────────────┐ │
│  │ Core    │ │ Insurer    │ │
│  │ Logic   │→│ Adapters   │ │
│  └─────────┘ └────────────┘ │
└──────────────────────────────┘

DURING (Strangler Fig — parallel operation):
┌────────────────┐           ┌─────────────────┐
│  API Gateway   │           │                 │
│  ┌──────────┐  │           │  Insurer Gateway│
│  │ Routing  │──┼──────────→│  (new service)  │
│  │ Rules    │  │           │  ┌───────────┐  │
│  └──────────┘  │           │  │ Adapters  │  │
│      │         │           │  └───────────┘  │
│      ▼         │           └─────────────────┘
│  ┌──────────┐  │
│  │ Legacy   │──┼──→ (still handles insurers
│  │ Monolith │  │     not yet migrated)
│  └──────────┘  │
└────────────────┘

AFTER (Two Quanta):
┌────────────────┐           ┌─────────────────┐
│  Endorsement   │  Kafka    │  Insurer Gateway│
│  Core Service  │◄────────►│  Service         │
│  (PostgreSQL)  │           │  (PostgreSQL)    │
└────────────────┘           └─────────────────┘
```

**Fitness Functions Added:** FF-H1, FF-H2, FF-H4, FF-T4, FF-T5

**Architectural Quanta:** Transition from 1 to 2 (Endorsement Core + Insurer Gateway)

**Exit Criteria:**
- Insurer gateway handles 100% of insurer traffic independently
- Read replica reduces OLTP query load by > 50%
- Chaos game day completed successfully
- Multi-region failover tested (< 5 minute RTO)

---

### 6.3 Year 3 (2028): Platform Maturity & Marketplace

**Theme:** *Open the platform to external consumers; self-service insurer onboarding*

**Continuous Architecture Principle Applied:** #1 (Architect products, not solutions) — the platform becomes a product other teams and companies consume.

**Key Initiatives:**

| Initiative | Effort | Description |
|-----------|--------|-------------|
| **API Marketplace** | 8 weeks | Developer portal, API key management, rate limiting per tenant, usage analytics |
| **Self-Service Insurer Onboarding** | 6 weeks | Configuration wizard, adapter specification validation, sandbox environment |
| **Webhook Notification System** | 3 weeks | Replace `LoggingNotificationAdapter` with real webhook delivery, retry, and DLQ |
| **Mobile BFF (Backend-for-Frontend)** | 4 weeks | Dedicated API layer for mobile clients (PWA + React Native) |
| **SLO-Based Alerting** | 2 weeks | Error budget tracking, burn rate alerts, SLO dashboards |
| **Multi-Region Active-Active** | 6 weeks | CockroachDB or Citus for distributed PostgreSQL. Region-aware routing. |

**Fitness Functions Added:** FF-H5, FF-T6 (synthetic transactions in production)

**Architectural Quanta:** 3 (Endorsement Core + Insurer Gateway + API Platform)

---

### 6.4 Year 4 (2029): Intelligence Platform & Data Mesh

**Theme:** *Extract intelligence capabilities into a dedicated platform; adopt data mesh principles*

**Continuous Architecture Principle Applied:** #2 (Focus on quality attributes) — intelligence queries have fundamentally different performance profiles (analytical vs transactional) requiring different architectural tactics.

**Key Initiatives:**

| Initiative | Effort | Description |
|-----------|--------|-------------|
| **Intelligence Service Extraction** | 6 weeks | Extract anomaly detection, forecasting, process mining, health scoring into dedicated service |
| **Data Mesh Foundation** | 8 weeks | Domain-owned data products with standardised interfaces; self-serve data infrastructure |
| **ML Pipeline Platform** | 6 weeks | Feature store, model training infrastructure, A/B model comparison |
| **TimescaleDB for Time-Series** | 3 weeks | Migrate `ProcessMiningMetric`, `AnomalyDetection`, `BalanceForecast` to time-series optimised storage |
| **Real-Time Analytics** | 4 weeks | ClickHouse or Apache Druid for sub-second analytical queries |
| **Predictive Alerting** | 3 weeks | ML-driven anomaly prediction (not just detection) |

**Data Mesh Principles Applied:**

| Principle | Implementation |
|-----------|---------------|
| **Domain-Oriented Ownership** | Endorsement team owns endorsement data product; Intelligence team owns analytics data product |
| **Data as a Product** | Each data product has SLOs, documentation, schema versioning, quality checks |
| **Self-Serve Data Infrastructure** | Shared platform for data product creation, discovery, and consumption |
| **Federated Computational Governance** | Automated policies for PII handling, retention, access control |

**Architectural Quanta:** 4 (Endorsement Core + Insurer Gateway + API Platform + Intelligence Platform)

---

### 6.5 Year 5 (2030–2031): Global Scale & Autonomous Operations

**Theme:** *Multi-region active-active with autonomous operations and self-healing capabilities*

**Continuous Architecture Principle Applied:** #4 (Architect for change — power of small) — the system handles operational complexity autonomously through small, automated interventions.

**Key Initiatives:**

| Initiative | Effort | Description |
|-----------|--------|-------------|
| **Multi-Region Active-Active** | 12 weeks | Active-active in 3 regions (India, Southeast Asia, Middle East); region-aware data routing |
| **Autonomous Operations** | 8 weeks | Self-healing: automated incident response, automated scaling, automated failover |
| **Edge Computing** | 4 weeks | Edge nodes for insurer integration in low-latency regions |
| **Global Compliance Engine** | 6 weeks | Per-jurisdiction regulatory rules engine; automated compliance checking |
| **AI-Assisted Architecture** | 4 weeks | ML-driven capacity planning, automated performance optimisation |

**Target Architecture (Year 5):**

```
                    ┌─────────────────────┐
                    │   Global Load       │
                    │   Balancer          │
                    └────────┬────────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
    ┌────▼────┐        ┌────▼────┐        ┌────▼────┐
    │ INDIA   │        │  SEA   │        │  MEA   │
    │ REGION  │        │ REGION │        │ REGION │
    │         │        │        │        │        │
    │ ┌─────┐ │  Kafka │ ┌─────┐│  Kafka │ ┌─────┐│
    │ │Core │◄├───────►│ │Core ││◄──────►│ │Core ││
    │ └─────┘ │        │ └─────┘│        │ └─────┘│
    │ ┌─────┐ │        │ ┌─────┐│        │ ┌─────┐│
    │ │Gate │ │        │ │Gate ││        │ │Gate ││
    │ │way  │ │        │ │way  ││        │ │way  ││
    │ └─────┘ │        │ └─────┘│        │ └─────┘│
    │ ┌─────┐ │        │ ┌─────┐│        │ ┌─────┐│
    │ │Intel│ │        │ │Intel││        │ │Intel││
    │ └─────┘ │        │ └─────┘│        │ └─────┘│
    │         │        │        │        │        │
    │ CockDB  │        │CockDB │        │CockDB │
    │ (node)  │        │(node) │        │(node) │
    └─────────┘        └────────┘        └────────┘
```

**Fitness Functions Added:** All remaining. Complete coverage across all quality dimensions.

**Architectural Quanta:** 4 per region × 3 regions = 12 deployment units

**Exit Criteria:**
- 99.99% availability across all regions
- < 100ms P99 response time for any region-local request
- Zero-downtime deployments with canary analysis
- Autonomous recovery from single-region failure within 30 seconds

---

## 7. Quality Attribute Evolution

### 7.1 Security Evolution

**Architectural Tactics Applied** (per Erder, Pureur, Woods Ch. 4):

| Year | Tactic | Implementation |
|------|--------|---------------|
| **Year 1** | **Authenticate actors** | JWT tokens with RS256 signing; OAuth2 authorization server integration |
| **Year 1** | **Authorise actors** | RBAC with 4 roles: ADMIN, OPERATIONS, INSURER, VIEWER |
| **Year 1** | **Detect attacks** | Rate limiting (existing `RateLimitingFilter`); OWASP dependency scan |
| **Year 1** | **Audit** | Existing `AuditLogService` + extend with authentication events |
| **Year 2** | **Encrypt data in transit** | TLS 1.3 everywhere; mutual TLS for inter-service communication |
| **Year 2** | **Encrypt data at rest** | PostgreSQL TDE; Redis encryption; Kafka topic encryption |
| **Year 3** | **Zero Trust** | Service mesh (Istio/Linkerd) with mTLS; no implicit trust between services |
| **Year 4** | **Data classification** | PII tagging in schema; automated PII redaction in logs and analytics |
| **Year 5** | **Compliance automation** | Per-jurisdiction rules engine; automated GDPR/DPDP compliance checking |

**Fitness Functions Protecting Security:**
- FF-06: No PII in logs (per-commit)
- FF-10: All controller methods require auth annotation (per-commit)
- FF-T3: Zero CRITICAL CVEs (weekly)
- FF-T8: TLS certificate validity > 30 days (daily)

### 7.2 Scalability Evolution

**Architectural Tactics Applied** (per Erder, Pureur, Woods Ch. 5):

| Year | Tactic | Implementation | Target Scale |
|------|--------|---------------|--------------|
| **Year 1** | **Stateless processes** | Already implemented. All handlers stateless, `SessionCreationPolicy.STATELESS`. | 10K endorsements/day |
| **Year 1** | **Horizontal scaling** | K8s HPA based on CPU/memory. ShedLock prevents scheduler conflicts. | 50K/day |
| **Year 2** | **CQRS read replicas** | PostgreSQL streaming replication. Query handler routes to replica. | 100K/day |
| **Year 2** | **Async processing** | Kafka event-driven (already implemented). Add consumer group scaling. | 200K/day |
| **Year 3** | **Caching** | Redis cache for insurer configs (exists). Add query result caching. CDN for frontend. | 500K/day |
| **Year 3** | **Load distribution** | API gateway with weighted routing. Region-based request distribution. | 500K/day |
| **Year 4** | **Data partitioning** | Employer-based sharding. Partition key: `employerId`. | 1M/day |
| **Year 5** | **Multi-region** | Active-active with CockroachDB. Region-local reads, global writes. | 5M+/day |

### 7.3 Performance Evolution

**Architectural Tactics Applied** (per Erder, Pureur, Woods Ch. 6):

| Year | Tactic | Implementation | Target |
|------|--------|---------------|--------|
| **Year 1** | **Virtual threads** | Already enabled: `-Dspring.threads.virtual.enabled=true` | P99 < 500ms |
| **Year 1** | **Connection pooling** | HikariCP (Spring Boot default). Tune pool size for read/write split. | P99 < 300ms |
| **Year 2** | **Query optimisation** | Index audit for all query handler methods. N+1 detection fitness function. | P99 < 200ms |
| **Year 2** | **Response compression** | Gzip/Brotli at API gateway level | P99 < 150ms |
| **Year 3** | **Async I/O** | WebFlux for read-heavy intelligence endpoints. Keep WebMVC for transactional. | P99 < 100ms |
| **Year 4** | **Edge caching** | CDN for static data (insurer list, enum values). Cache-Control headers. | P99 < 50ms (cached) |
| **Year 5** | **Predictive scaling** | ML-based traffic prediction → pre-scale infrastructure | P99 < 100ms (global) |

### 7.4 Resilience Evolution

**Architectural Tactics Applied** (per Erder, Pureur, Woods Ch. 7):

| Year | Tactic | Implementation |
|------|--------|---------------|
| **Year 1** | **Circuit breaker** | Already implemented (Resilience4j, per-insurer instances) |
| **Year 1** | **Retry** | Already implemented (exponential backoff, per-adapter) |
| **Year 1** | **Graceful shutdown** | Add `server.shutdown: graceful` with 30s timeout |
| **Year 1** | **Health checks** | Enhance with custom `HealthIndicator` per dependency |
| **Year 2** | **Bulkhead** | Thread pool isolation for insurer calls. Prevent one slow insurer from blocking all. |
| **Year 2** | **Fallback** | Queue-to-batch fallback when real-time circuit opens (partially exists) |
| **Year 2** | **Chaos testing** | Monthly chaos game days. Inject: network latency, pod kills, disk full, DB timeout. |
| **Year 3** | **Redundancy** | Multi-AZ deployment. No single point of failure for any component. |
| **Year 4** | **Self-healing** | Automated incident response: detect anomaly → isolate → recover → alert. |
| **Year 5** | **Multi-region failover** | Automatic failover between regions. RTO < 30 seconds. RPO < 1 second. |

### 7.5 Observability Evolution

| Year | Capability | Implementation |
|------|-----------|---------------|
| **Current** | **Metrics** | 20+ Prometheus metrics + Grafana (7 dashboards) |
| **Current** | **Tracing** | OpenTelemetry + Jaeger (100% sampling) |
| **Current** | **Logging** | Structured JSON via Logstash → Elasticsearch → Kibana |
| **Year 1** | **SLO tracking** | Define SLOs → error budget → burn rate alerts |
| **Year 2** | **Distributed tracing** | Cross-service tracing when insurer gateway is extracted |
| **Year 2** | **Anomaly detection** | ML-based alerting that learns normal patterns |
| **Year 3** | **Business observability** | Business KPI dashboards: endorsement throughput, STP rate trends, revenue impact |
| **Year 4** | **Correlation** | Automatic correlation of incidents across services, infrastructure, and business impact |
| **Year 5** | **AIOps** | AI-driven root cause analysis, automated remediation recommendations |

---

## 8. Data Architecture Evolution

### 8.1 Current: Single PostgreSQL

```
┌────────────────────────────────────────────┐
│              PostgreSQL 15                  │
│                                            │
│  17 tables, 17 Flyway migrations           │
│  OLTP + analytics on same instance         │
│  JPA/Hibernate with open-in-view=false     │
│  Connection pool: HikariCP (default)       │
│                                            │
│  Tables:                                   │
│  endorsements, endorsement_events,         │
│  endorsement_batches, ea_accounts,         │
│  ea_transactions, insurer_configurations,  │
│  provisional_coverages, audit_logs,        │
│  anomaly_detections, balance_forecasts,    │
│  error_resolutions, process_mining_metrics,│
│  reconciliation_runs, reconciliation_items,│
│  shedlock, archive_endorsements,           │
│  archive_endorsement_events               │
└────────────────────────────────────────────┘
```

### 8.2 Phase 1: Read Replicas & CQRS Materialisation

**When:** Year 2 (2027)
**Why:** Analytics queries (health score, benchmarks, process mining) compete with OLTP transactions for database resources.
**Pattern:** Expand-contract migration + CQRS read model

```
┌──────────────────────┐         ┌──────────────────────┐
│  PostgreSQL Primary  │────────→│  PostgreSQL Replica   │
│  (Read-Write)        │ stream  │  (Read-Only)          │
│                      │ repl    │                        │
│  Command handlers    │         │  Query handlers        │
│  write here          │         │  read from here        │
└──────────────────────┘         └──────────────────────┘
```

### 8.3 Phase 2: Domain-Owned Data Products

**When:** Year 4 (2029)
**Why:** Intelligence team needs different data models (time-series, aggregations) than core endorsement team (transactional).

```
┌──────────────────────┐    ┌──────────────────────┐
│ Endorsement Data     │    │ Intelligence Data     │
│ Product              │    │ Product               │
│                      │    │                       │
│ PostgreSQL           │    │ TimescaleDB           │
│ (endorsements,       │───→│ (metrics, forecasts,  │
│  batches, EA, prov)  │ CDC│  anomalies, mining)   │
│                      │    │                       │
│ Owner: Core Team     │    │ Owner: Intelligence   │
│ SLO: 99.9% write    │    │ SLO: 99.5% query     │
│      availability    │    │      < 200ms P95      │
└──────────────────────┘    └──────────────────────┘
```

### 8.4 Phase 3: Data Mesh

**When:** Year 5 (2030)
**Why:** Multiple teams, multiple regions, multiple data consumers require federated data governance.

**Data Mesh Principles Applied:**

```
┌─────────────────────────────────────────────────────────────┐
│                    FEDERATED GOVERNANCE                      │
│  PII policies │ Retention rules │ Access control │ SLOs     │
└────────────────────────────┬────────────────────────────────┘
                             │
     ┌───────────────────────┼───────────────────────┐
     │                       │                       │
┌────▼──────────┐    ┌──────▼───────┐    ┌──────────▼────────┐
│ Endorsement   │    │ Intelligence │    │ Insurer           │
│ Data Product  │    │ Data Product │    │ Data Product      │
│               │    │              │    │                   │
│ Transactional │    │ Time-series  │    │ Configuration +   │
│ data          │    │ analytics    │    │ performance data  │
│               │    │              │    │                   │
│ PostgreSQL    │    │ TimescaleDB  │    │ PostgreSQL        │
│               │    │ + ClickHouse │    │                   │
│ Schema: Avro  │    │ Schema: Avro │    │ Schema: Avro      │
│ API: REST+Kafka│   │ API: REST+   │    │ API: REST         │
│               │    │ GraphQL      │    │                   │
│ Team: Core    │    │ Team: Intel  │    │ Team: Integration │
└───────────────┘    └──────────────┘    └───────────────────┘
```

### 8.5 Evolutionary Database Design

Following the principles from both books, database evolution follows expand-contract pattern:

**Rule 1: Every schema change is a Flyway migration.**
No manual DDL. Ever. The migration scripts are the single source of truth for schema state.

**Rule 2: Every migration is backwards-compatible.**
Phase 1 (expand): Add new column/table alongside old. Both work simultaneously.
Phase 2 (migrate): Application code reads from new, writes to both.
Phase 3 (contract): Remove old column/table once all consumers are updated.

**Rule 3: Small migrations compose.**
Each migration does one thing. `V18__add_currency_to_ea_accounts.sql` adds the column.
`V19__backfill_currency_inr.sql` fills existing rows with `INR`.
`V20__make_currency_not_null.sql` adds the NOT NULL constraint.

**Example — Adding Multi-Currency Support:**

```sql
-- V18 (Expand): Add nullable column
ALTER TABLE ea_accounts ADD COLUMN currency VARCHAR(3);

-- V19 (Migrate): Backfill existing data
UPDATE ea_accounts SET currency = 'INR' WHERE currency IS NULL;

-- V20 (Contract): Make non-nullable
ALTER TABLE ea_accounts ALTER COLUMN currency SET NOT NULL;
ALTER TABLE ea_accounts ALTER COLUMN currency SET DEFAULT 'INR';
```

**Fitness Function:** FF-08 ensures migration naming convention. Add FF-DB1: "No migration removes a column that was added in the previous 2 releases" (safety window).

---

## 9. Incremental Change Patterns

### 9.1 Strangler Fig Pattern

**Use Case:** Extracting the Insurer Gateway service from the monolith (Year 2).

**How It Works:**

1. **Identify the seam:** `InsurerPort` interface, `InsurerRouter`, and all adapter classes form a natural boundary.
2. **Deploy the fig:** Create a new service implementing `InsurerPort`. Route traffic through API gateway.
3. **Grow the fig:** Migrate one insurer adapter at a time to the new service (ICICI first, then Bajaj, then Niva Bupa).
4. **Remove the dead tree:** Once all adapters are in the new service, remove them from the monolith.

**Key Insight:** The hexagonal architecture makes Strangler Fig almost trivial. The `InsurerPort` interface is already the abstraction boundary. The API gateway routes requests based on insurer ID to either the old or new implementation.

**Risk Mitigation:** Parallel runs (Section 9.3) during migration. Both old and new implementations process the same endorsement; results are compared before switching over.

### 9.2 Branch by Abstraction

**Use Case:** Replacing the batch optimisation algorithm without disrupting production.

**How It Works:**

The `BatchOptimizerPort` interface is already the abstraction. To replace the implementation:

1. Create new implementation: `MLBatchOptimizer implements BatchOptimizerPort`
2. Add feature toggle: `endorsement.intelligence.batch-optimizer.impl=constraint` vs `ml`
3. Wire both implementations with `@ConditionalOnProperty`
4. Switch traffic gradually via configuration change (no redeploy)

**Current Codebase Advantage:** Every domain port is already a branch-by-abstraction seam. No refactoring needed to introduce alternative implementations.

### 9.3 Parallel Runs

**Use Case:** Validating a new insurer adapter against the existing mock adapter.

**How It Works:**

1. **Shadow mode:** New adapter receives a copy of every request sent to the existing adapter
2. **Result comparison:** Responses from both are compared; discrepancies logged
3. **Metrics:** Track agreement rate over time; switch when > 99.9% agreement
4. **Synthetic transactions:** In production, use flagged transactions that follow the full flow but don't commit

**Implementation Pattern:**

```java
// ProcessEndorsementHandler — parallel run wrapper
InsurerPort primary = insurerRouter.resolve(insurerId);      // current
InsurerPort shadow = insurerRouter.resolveShadow(insurerId); // new

SubmissionResult primaryResult = primary.submitRealTime(endorsementId, data);

// Fire-and-forget shadow call (async, non-blocking)
CompletableFuture.runAsync(() -> {
    SubmissionResult shadowResult = shadow.submitRealTime(endorsementId, data);
    parallelRunComparator.compare(endorsementId, primaryResult, shadowResult);
});

return primaryResult; // always return primary
```

### 9.4 Feature Toggles

**Current State:** Simple `@Value("${feature.enabled:true}")` booleans.

**Evolution:**

| Year | Toggle Type | Implementation |
|------|-----------|---------------|
| Year 1 | **Release toggles** | Database-backed toggles. Admin API to enable/disable. |
| Year 2 | **Experiment toggles** | Percentage-based rollout (10% → 50% → 100%). A/B testing. |
| Year 3 | **Ops toggles** | Kill switches for features under load. Circuit breaker integration. |
| Year 4 | **Permission toggles** | Per-tenant feature availability. Tied to subscription tier. |

**Hygiene Rule:** Every toggle has an expiry date. Expired toggles are removed in the next sprint. Fitness function FF-FT1: "No toggle older than 90 days without explicit extension."

### 9.5 Expand-Contract Migrations

See Section 8.5 for database-specific expand-contract. The pattern applies more broadly:

**API Expand-Contract:**
1. **Expand:** Add new endpoint `/api/v2/endorsements` alongside existing `/api/v1/endorsements`
2. **Migrate:** Clients move to v2 over 2 release cycles
3. **Contract:** Deprecate and remove v1 after migration window

**Event Schema Expand-Contract:**
1. **Expand:** Add new fields to `EndorsementEvent.Confirmed` (e.g., `processingDurationMs`)
2. **Migrate:** Consumers start reading new fields (ignore if absent for backward compatibility)
3. **Contract:** After all consumers upgraded, make field required in schema

---

## 10. Governance Model

### 10.1 Automated Governance via Fitness Functions

> *"Traditionally, architects had few tools to enforce governance policies outside manual code reviews and architecture review boards. With automated fitness functions, architects can create automated governance policies."*
> — Ford, Parsons, Kua (2023)

**Governance Pipeline:**

```
Developer Commit
       │
       ▼
┌─────────────────┐
│  CI Pipeline     │
│                  │
│  ArchUnit Tests  │── FF-01 to FF-10 (modularity, coupling, dependency rules)
│  JaCoCo          │── FF-04 (coverage >= 80%)
│  openapi-diff    │── FF-07 (no breaking API changes)
│  OWASP Dep Check │── FF-T3 (zero CRITICAL CVEs)
│  PII grep        │── FF-06 (no PII in logs)
│  Contract Tests  │── Module boundary validation
│                  │
│  ALL MUST PASS   │
└────────┬─────────┘
         │
         ▼
┌─────────────────┐
│  Deploy to       │
│  Staging         │
│                  │
│  Integration     │── FF-H1 to FF-H5 (holistic fitness functions)
│  Tests           │
│  Perf Tests      │── Response time thresholds
│                  │
└────────┬─────────┘
         │
         ▼
┌─────────────────┐
│  Production      │
│  Monitoring      │
│                  │
│  Prometheus      │── FF-T1 (P99 < 500ms)
│  Synthetic txns  │── FF-T6 (e2e success every 5min)
│  SLO dashboards  │── Error budget tracking
│                  │
└──────────────────┘
```

### 10.2 Architecture Decision Records (ADRs)

**Format:** Nygard lightweight template

```markdown
# ADR-{NNN}: {Title}

**Date:** YYYY-MM-DD
**Status:** Proposed | Accepted | Deprecated | Superseded by ADR-{NNN}

## Context
{Forces at play — technical, business, political, social}

## Decision
{What we decided and why}

## Consequences
{Positive, negative, and neutral consequences}

## Fitness Functions
{Which fitness functions protect this decision}
```

**Storage:** `docs/adr/` directory, numbered sequentially.

**Initial ADR Backlog:**

| ADR | Title | Status |
|-----|-------|--------|
| ADR-001 | Use Hexagonal Architecture (Ports & Adapters) | Accepted |
| ADR-002 | Kafka for event-driven communication | Accepted |
| ADR-003 | PostgreSQL as primary data store | Accepted |
| ADR-004 | Resilience4j for circuit breakers and retries | Accepted |
| ADR-005 | Stateless processes with Spring Boot | Accepted |
| ADR-006 | CQRS with separate command/query handlers | Accepted |
| ADR-007 | ShedLock for distributed scheduler coordination | Accepted |
| ADR-008 | URL path versioning for REST APIs | Proposed |
| ADR-009 | JWT + OAuth2 for authentication | Proposed |
| ADR-010 | Strangler Fig for insurer gateway extraction | Proposed |

### 10.3 Architecture Guardrails

Guardrails are **not mandates** — they are strong recommendations that teams can deviate from with documented justification (via ADR).

| Guardrail | Enforcement | Override Process |
|-----------|------------|-----------------|
| Domain models must have zero infrastructure imports | ArchUnit (automated) | ADR explaining why + team lead approval |
| All external calls must have circuit breaker + retry | Code review checklist | ADR documenting risk acceptance |
| All state transitions go through `EndorsementStateMachine` | Code review + ArchUnit | Not overridable — invariant |
| All database changes are Flyway migrations | Convention + CI check | Not overridable — invariant |
| All new REST endpoints require OpenAPI documentation | CI check (Springdoc) | ADR for internal-only endpoints |
| No service holds mutable instance state | ArchUnit (automated) | ADR explaining thread-safety approach |
| All secrets from environment variables, never hardcoded | CI secret scanner | Not overridable — invariant |

### 10.4 Lightweight Review Process

**For changes within existing patterns:**
- Standard code review (1 reviewer)
- Automated fitness functions must pass
- No architecture review needed

**For changes that introduce new patterns (new technology, new communication style, new data store):**
- ADR required
- Architecture review by 2 senior engineers
- Fitness functions updated to protect the new pattern

**For changes that alter fundamental architecture (service extraction, database split, new quantum):**
- ADR required
- Architecture review by all senior engineers
- Proof of concept with fitness function results
- Rollback plan documented

---

## 11. Team Topology Evolution

### 11.1 Current: Single Team

```
┌──────────────────────────────────────┐
│          SINGLE TEAM                 │
│                                      │
│  Full-stack ownership of:            │
│  - Backend (Java/Spring Boot)        │
│  - Frontend (React/TypeScript)       │
│  - Infrastructure (Docker/K8s)       │
│  - Tests (all 5 levels)             │
│  - Observability (Grafana/ELK)      │
│                                      │
│  Cognitive Load: HIGH                │
│  Deployment Independence: N/A        │
└──────────────────────────────────────┘
```

### 11.2 Phase 1: Stream-Aligned Teams

**When:** Year 2 (as the team grows beyond 8 engineers)
**Pattern:** Stream-aligned teams aligned to business domains

```
┌────────────────────┐  ┌────────────────────┐  ┌──────────────────┐
│ ENDORSEMENT CORE   │  │ INSURER INTEGRATION│  │ INTELLIGENCE     │
│ (Stream-Aligned)   │  │ (Stream-Aligned)   │  │ (Stream-Aligned) │
│                    │  │                    │  │                  │
│ Endorsement CRUD   │  │ Adapter framework  │  │ Anomaly detect   │
│ State machine      │  │ Insurer onboarding │  │ Forecasting      │
│ EA balance         │  │ Format translation │  │ Process mining   │
│ Batch processing   │  │ Rate limiting      │  │ Health scoring   │
│ Reconciliation     │  │ Circuit breakers   │  │ ML pipeline      │
│                    │  │                    │  │                  │
│ Owns: Core domain  │  │ Owns: InsurerPort  │  │ Owns: Analytics  │
│ models, handlers,  │  │ adapters, router,  │  │ services, data   │
│ core tests         │  │ insurer tests      │  │ products, models │
└────────────────────┘  └────────────────────┘  └──────────────────┘
```

**Interaction Mode:** Collaboration (shared codebase initially) → X-as-a-Service (after service extraction)

### 11.3 Phase 2: Platform Team Emergence

**When:** Year 3 (when platform concerns consume > 30% of stream-aligned team capacity)

```
┌─────────────────────────────────────────────────────────────┐
│                     PLATFORM TEAM                            │
│                                                              │
│  CI/CD pipeline │ Observability stack │ K8s platform          │
│  API gateway    │ Feature toggle system │ Secret management   │
│  Schema registry │ Shared libraries │ Developer experience    │
│                                                              │
│  Interaction: X-as-a-Service (platform services consumed     │
│  by stream-aligned teams)                                    │
└─────────────────────────────────────────────────────────────┘

┌────────────────┐  ┌────────────────┐  ┌────────────────┐
│ Endorsement    │  │ Integration    │  │ Intelligence   │
│ Core Team      │  │ Team           │  │ Team           │
│ (Stream)       │  │ (Stream)       │  │ (Stream)       │
└────────────────┘  └────────────────┘  └────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                   ENABLING TEAM                              │
│                                                              │
│  Security practices │ Performance engineering │               │
│  Architecture guidance │ New technology evaluation            │
│                                                              │
│  Interaction: Facilitating (time-boxed engagements with      │
│  stream-aligned teams to uplift capabilities)                │
└─────────────────────────────────────────────────────────────┘
```

### 11.4 Inverse Conway Maneuver

> *"The Inverse Conway Maneuver involves deliberately altering the development team's organization structure to encourage the desired software architecture."*

**Year 1:** Single team → establish module ownership conventions within the monolith
**Year 2:** Split teams → team boundaries mirror the three planned quanta (Core, Integration, Intelligence)
**Year 3:** Team structure drives service boundaries → each team owns and deploys its services independently
**Year 4:** Platform team → shared infrastructure becomes a product consumed by domain teams
**Year 5:** Regional teams → teams in each geography own their regional deployment

**Key Principle:** Team boundaries and service boundaries evolve together. Don't split services before splitting teams. Don't split teams before the cognitive load justifies it.

---

## 12. Architecture Decision Records

### ADR-001: Hexagonal Architecture (Ports & Adapters)

**Date:** March 7, 2026
**Status:** Accepted

**Context:** The endorsement platform integrates with multiple insurance providers, each with different API protocols (REST, SOAP, CSV/SFTP). The system must be testable in isolation from external dependencies and extensible to new insurers without modifying core business logic.

**Decision:** Adopt Hexagonal Architecture (Ports & Adapters) as the primary architectural style. The domain core defines port interfaces; infrastructure adapters implement them. Dependencies point inward. The domain has zero infrastructure imports.

**Consequences:**
- POSITIVE: New insurer integration = new adapter + DB row. Zero changes to domain or handlers.
- POSITIVE: All business logic testable with mock adapters. 381 unit tests prove this.
- POSITIVE: Technology decisions (JPA, Kafka, Redis) are replaceable behind port interfaces.
- NEGATIVE: More interfaces and indirection than a simpler layered architecture.
- NEGATIVE: Learning curve for developers unfamiliar with the pattern.

**Fitness Functions:** FF-01 (no domain→infrastructure imports), FF-02 (no handler→infrastructure imports), FF-09 (no circular dependencies).

---

### ADR-010: Strangler Fig for Insurer Gateway Extraction

**Date:** March 14, 2026
**Status:** Proposed

**Context:** As the platform scales to 10+ insurers across multiple countries, the insurer integration layer has different scaling, deployment, and release cadence requirements than the core endorsement engine. The `InsurerPort` interface and `InsurerRouter` already form a clean boundary.

**Decision:** Extract the insurer integration layer into a separate service using the Strangler Fig pattern. An API gateway will route insurer-bound traffic to either the monolith (during migration) or the new gateway service (after migration). Migration will proceed one insurer at a time.

**Consequences:**
- POSITIVE: Independent scaling of insurer integration (e.g., 3 instances for ICICI during peak hours).
- POSITIVE: Independent deployment cadence (insurer adapter updates don't require full system deploy).
- POSITIVE: Failure isolation (insurer gateway crash doesn't take down endorsement creation).
- NEGATIVE: Distributed system complexity (network calls, eventual consistency, distributed tracing).
- NEGATIVE: Operational overhead (two services to monitor, deploy, and debug).

**Fitness Functions:** FF-H2 (resilience under circuit breaker open), FF-T5 (deployment velocity), new FF-SG1 (strangler fig progress — % of insurer traffic routed to new service).

---

## 13. Risk Matrix & Mitigation

| Risk | Probability | Impact | Mitigation | Fitness Function |
|------|------------|--------|------------|-----------------|
| Premature decomposition (extracting services before needed) | MEDIUM | HIGH | Follow "last responsible moment" principle. Decompose only when scaling/team needs demand it. | FF-T4 (coupling metrics — don't decompose if coupling is manageable) |
| Fitness function maintenance burden | MEDIUM | MEDIUM | Start with high-value fitness functions (modularity, security). Add incrementally. Automate fully. | N/A (meta-risk) |
| Database schema drift during expand-contract | LOW | HIGH | Automated migration testing. Rollback scripts for every migration. | FF-08 (migration naming), FF-DB1 (no premature column removal) |
| Loss of development velocity from governance overhead | MEDIUM | HIGH | Lightweight ADR process. Automated fitness functions replace manual reviews. | FF-T5 (deployment velocity) |
| Team cognitive overload during service extraction | MEDIUM | HIGH | Inverse Conway: split teams before splitting services. Platform team absorbs infrastructure complexity. | N/A (organisational risk) |
| Vendor lock-in as platform matures | LOW | HIGH | Port/adapter pattern for all infrastructure. Regular "could we switch?" exercises. | FF-01, FF-02 (dependency direction enforcement) |
| Data consistency issues in multi-region | HIGH | CRITICAL | Start active-passive. Move to active-active only when latency requirements demand it. Extensive chaos testing. | FF-H5 (data integrity under failure) |
| Security breach due to delayed auth implementation | MEDIUM | CRITICAL | Prioritise auth as P0 in Year 1. Rate limiting (existing) provides interim protection. | FF-10 (auth annotations), FF-T3 (CVE scanning) |

---

## 14. References

### Primary Sources

1. Ford, N., Parsons, R., Kua, P., & Sadalage, P. (2023). *Building Evolutionary Architectures: Automated Software Governance* (2nd ed.). O'Reilly Media.
   - Chapter 2: Fitness Functions
   - Chapter 4: Automating Architectural Governance
   - Chapter 5: Evolutionary Architecture Topologies
   - Chapter 6: Evolutionary Data

2. Erder, M., Pureur, P., & Woods, E. (2021). *Continuous Architecture in Practice: Software Architecture in the Age of Agility and DevOps*. Addison-Wesley.
   - Chapter 2: Architecture in Practice: Essential Activities
   - Chapter 4: Security as an Architectural Concern
   - Chapter 5: Scalability as an Architectural Concern
   - Chapter 6: Performance in the Architectural Context
   - Chapter 7: Resilience in an Architectural Context

### Supplementary Sources

3. Davis, C. (2019). *Cloud Native Patterns*. Manning Publications. — Applied in this codebase (see CLAUDE.md Part A).

4. Freeman, E., & Robson, E. (2020). *Head First Design Patterns* (2nd ed.). O'Reilly Media. — Applied in this codebase (see CLAUDE.md Part B).

5. Skelton, M., & Pais, M. (2019). *Team Topologies: Organizing Business and Technology Teams for Fast Flow*. IT Revolution Press. — Referenced for team evolution (Section 11).

6. Nygard, M. (2011). "Documenting Architecture Decisions." — ADR template (Section 10.2).

7. Page-Jones, M. (1996). *What Every Programmer Should Know About Object-Oriented Design*. Dorset House. — Connascence framework (Section 3.2).

8. Dehghani, Z. (2022). *Data Mesh: Delivering Data-Driven Value at Scale*. O'Reilly Media. — Data mesh evolution (Section 8.4).

### Codebase References

- Architecture documentation: `docs/Architecture_Hexagonal_Ports_and_Adapters.md`
- Execution plan: `docs/Endorsement_System_Execution_Plan.md`
- Product evolution: `docs/deliverables/vision/Product_Evolution_Vision.md`
- Technical gaps: `GAPS.md`
- Design patterns: `CLAUDE.md` (Parts A, B, C, D)

---

*This document is a living artifact. It should be reviewed and updated quarterly as architectural decisions are made and the system evolves. Each significant architectural change should be accompanied by an ADR and, where possible, a new or updated fitness function.*
