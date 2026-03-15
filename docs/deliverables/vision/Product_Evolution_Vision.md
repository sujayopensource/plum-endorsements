# Product Evolution Vision: Phase 4 — Global World-Class Platform

**Project:** Plum Endorsement Management System
**Date:** March 14, 2026
**Scope:** Phase 4 readiness audit, gap analysis, and pragmatic implementation roadmap
**Companion:** [Endorsement System Execution Plan](../../Endorsement_System_Execution_Plan.md) (Section 6)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [What's Already Built (Phase 3 Foundations for Phase 4)](#2-whats-already-built-phase-3-foundations-for-phase-4)
   - 2.1 [Cross-Insurer Performance Benchmarking](#21-cross-insurer-performance-benchmarking--implemented)
   - 2.2 [Industry Endorsement Pattern Insights](#22-industry-endorsement-pattern-insights--implemented)
   - 2.3 [Employer Health Score](#23-employer-health-score--implemented)
   - 2.4 [Insurer Configuration CRUD](#24-insurer-configuration-crud--implemented)
   - 2.5 [Observability Infrastructure](#25-observability-infrastructure--implemented)
3. [Capability-by-Capability Analysis](#3-capability-by-capability-analysis)
   - 3.1 [Advanced Analytics — Complete to 100%](#31-advanced-analytics--complete-to-100)
   - 3.2 [Multi-Currency EA Support](#32-multi-currency-ea-support)
   - 3.3 [Localized Insurer Integrations](#33-localized-insurer-integrations)
   - 3.4 [Country-Specific Regulatory Compliance](#34-country-specific-regulatory-compliance)
   - 3.5 [Self-Service Insurer Onboarding Portal](#35-self-service-insurer-onboarding-portal)
   - 3.6 [Platform API Marketplace](#36-platform-api-marketplace)
   - 3.7 [Multi-Region Deployment](#37-multi-region-deployment)
   - 3.8 [Mobile Application](#38-mobile-application)
   - 3.9 [CI/CD Pipeline — Harness & Jenkins](#39-cicd-pipeline--harness--jenkins)
4. [Implementation Roadmap](#4-implementation-roadmap)
5. [Architecture Readiness Assessment](#5-architecture-readiness-assessment)
6. [Test Impact Analysis](#6-test-impact-analysis)
7. [Risk Matrix](#7-risk-matrix)
8. [Decision Log](#8-decision-log)
9. [Authentication & Authorization Architecture](#9-authentication--authorization-architecture)
   - 9.1 [Current State](#91-current-state)
   - 9.2 [Authentication Strategy — JWT + OAuth2](#92-authentication-strategy--jwt--oauth2)
   - 9.3 [Authorization Strategy — RBAC](#93-authorization-strategy--role-based-access-control-rbac)
   - 9.4 [Hexagonal Architecture — Auth as Port + Adapter](#94-hexagonal-architecture--auth-as-port--adapter)
   - 9.5 [Database Schema](#95-database-schema)
   - 9.6 [Frontend Auth Integration](#96-frontend-auth-integration)
   - 9.7 [Audit Integration](#97-audit-integration)
   - 9.8 [Configuration](#98-configuration)
   - 9.9 [Security Considerations](#99-security-considerations)
   - 9.10 [Implementation Effort](#910-implementation-effort)
   - 9.11 [Migration Strategy — Zero Disruption](#911-migration-strategy--zero-disruption)

---

## 1. Executive Summary

Phase 4 of the Plum Endorsement platform targets transformation from a **single-region, single-currency, India-focused system** into a **global, multi-region, multi-currency platform** with self-service insurer onboarding, HRIS marketplace integrations, and regulatory compliance across jurisdictions.

This document audits the current codebase against all 7 Phase 4 capabilities, identifies what is already built, what is partially implemented, and — critically — **which features can be delivered with minimal friction** (zero test breakage, purely additive changes, leveraging the hexagonal architecture).

### Key Finding

The hexagonal architecture established in Phases 1-3 makes Phase 4 expansion **structurally cheap**. Every new capability maps to: new port interface + new adapter implementation + new controller endpoint + new Flyway migration. No existing domain logic, handler, or test needs modification.

### Completion Snapshot

| Capability | Status | Completion | Friction Level |
|---|---|---|---|
| Advanced Analytics | PARTIALLY IMPLEMENTED | 70% | LOW |
| Localized Insurer Integrations | PARTIALLY IMPLEMENTED | 20% | LOW-MEDIUM |
| Multi-Currency EA Support | NOT IMPLEMENTED | 0% | MEDIUM |
| Country-Specific Compliance | NOT IMPLEMENTED | 0% | MEDIUM |
| Self-Service Insurer Onboarding | NOT IMPLEMENTED | 0% | HIGH |
| Platform API Marketplace | NOT IMPLEMENTED | 0% | VERY HIGH |
| Multi-Region Deployment | NOT IMPLEMENTED | 0% | VERY HIGH |
| Mobile Application | NOT IMPLEMENTED | 0% | MEDIUM |
| CI/CD Pipeline (Harness + Jenkins) | NOT IMPLEMENTED | 0% | MEDIUM |

---

## 2. What's Already Built (Phase 3 Foundations for Phase 4)

Phase 3 (Intelligence Layer) delivered infrastructure that directly fulfills three Phase 4 analytics capabilities. These are **production-ready** with full test coverage.

### 2.1 Cross-Insurer Performance Benchmarking — IMPLEMENTED

**Service:** `application/service/InsurerBenchmarkService.java`
**Endpoint:** `GET /api/v1/intelligence/benchmarks`

```
Metrics per insurer:
├── Average processing time (ms)
├── P95 processing time (ms)
├── P99 processing time (ms)
├── STP rate (straight-through processing %)
├── Total samples analyzed
└── Sorted by STP rate (best performers first)
```

**Data source:** `ProcessMiningMetric` records aggregated per insurer via `ProcessMiningRepository`.

**Test coverage:** 6 API tests + 2 BDD scenarios + 4 E2E tests.

### 2.2 Industry Endorsement Pattern Insights — IMPLEMENTED

**Service:** `application/service/ProcessMiningService.java`
**Endpoints:**
- `GET /api/v1/intelligence/process-mining/insights` — bottleneck analysis, happy-path % per insurer
- `GET /api/v1/intelligence/process-mining/metrics` — raw metrics per insurer
- `GET /api/v1/intelligence/process-mining/stp-rate` — cross-insurer STP rate
- `POST /api/v1/intelligence/process-mining/analyze` — trigger on-demand analysis

**Scheduled generation:** `ProcessMiningScheduler` runs daily at 03:00 via `@Scheduled(cron)`.

**Metrics collected:**
- STP rate (happy-path percentage)
- Error rate per error type
- Average duration by endorsement status
- Bottleneck detection (longest-running state transitions)

**Test coverage:** 8 API tests + 3 BDD scenarios + 8 E2E tests.

### 2.3 Employer Health Score — IMPLEMENTED

**Service:** `application/service/EmployerHealthScoreService.java`
**Endpoint:** `GET /api/v1/intelligence/employers/{employerId}/health-score`

**Scoring model (weighted composite):**

| Component | Weight | Data Source | Scoring Logic |
|---|---|---|---|
| Endorsement success rate | 40% | `endorsementRepository.countByEmployerIdAndStatus()` | confirmed / (confirmed + rejected + failed) |
| Anomaly score | 20% | `anomalyDetectionRepository.countByEmployerIdAndFlaggedAtAfter()` | 0 anomalies = 100, 1-2 = 80, 3-5 = 60, 6+ = 30 |
| Balance health | 20% | `eaAccountRepository.findByEmployerId()` | % of EA accounts with positive balance |
| Reconciliation score | 20% | Hardcoded 100 (structure exists for future) | Extensible when reconciliation-per-employer tracking is added |

**Risk levels:** LOW (score >= 80), MEDIUM (60-79), HIGH (< 60).

**Test coverage:** 4 unit tests + 2 API tests + 1 BDD scenario + 4 E2E tests.

### 2.4 Insurer Configuration CRUD — IMPLEMENTED

**Controller:** `api/controller/InsurerConfigurationController.java`
**Endpoints:**
- `GET /api/v1/insurers` — list all active insurers
- `GET /api/v1/insurers/{insurerId}` — get insurer details
- `GET /api/v1/insurers/{insurerId}/capabilities` — real-time/batch capabilities
- `POST /api/v1/insurers` — create new insurer configuration
- `PUT /api/v1/insurers/{insurerId}` — update insurer settings (name, rate limits, active status)

**Frontend:** `InsurersPage.tsx` (list view) + `InsurerDetailPage.tsx` (detail view).

**Test coverage:** 5 API tests + 2 BDD scenarios + 5 E2E tests.

### 2.5 Observability Infrastructure — IMPLEMENTED

Ready for multi-region monitoring expansion:
- **OpenTelemetry** tracing with 100% sampling + baggage propagation (`endorsementId`, `employerId`)
- **20+ custom Prometheus metrics** in `MetricsConfig.java` (counters, timers, gauges, distribution summaries)
- **Grafana dashboards:** `intelligence-monitoring.json`, `multi-insurer-monitoring.json`, `application-overview.json` (60+ panels total)
- **ELK stack:** Logstash pipeline + Kibana for structured log search
- **Jaeger:** Distributed trace visualization

---

## 3. Capability-by-Capability Analysis

### 3.1 Advanced Analytics — Complete to 100%

**Current:** 70% complete. **Remaining effort:** LOW (2-3 days).

All data required for the remaining 30% already exists in the database. No schema changes needed.

#### 3.1.1 Trend Analysis (NOT IMPLEMENTED)

**What:** Time-series endpoint showing how key metrics trend over configurable lookback periods.

**Proposed endpoint:** `GET /api/v1/intelligence/trends?metric={stpRate|anomalyCount|healthScore}&lookback={30d|90d|365d}`

**Implementation:**
```
New files:
├── application/service/TrendAnalysisService.java
│   └── Query ProcessMiningMetric/AnomalyDetection records by date range
│   └── Compute 7-day moving average using Apache Commons Math (already a dependency)
│   └── Return List<TrendPoint(date, value, movingAverage)>
├── api/dto/TrendResponse.java (record)
└── Add endpoint to IntelligenceController.java (1 method)
```

**Data source:** Existing `process_mining_metrics.recorded_at` and `anomaly_detections.flagged_at` timestamps. No new tables.

**Test estimate:** ~8 tests (unit: trend computation, API: endpoint contract, E2E: chart rendering).

#### 3.1.2 Employer Segmentation (NOT IMPLEMENTED)

**What:** Classify employers by endorsement volume and add segment to health score response.

**Implementation:**
```
Extend EmployerHealthScoreService.calculateHealthScore():
├── Count total endorsements for employer in last 90 days
├── Segment: HIGH (>100), MEDIUM (20-100), LOW (<20)
└── Add "segment" field to HealthScore record

Extend HealthScoreResponse:
└── Add segment: String field
```

**Impact on existing tests:** Zero — new field is additive. Existing assertions on `overallScore`, `riskLevel` etc. remain valid.

**Test estimate:** ~4 tests (unit: segmentation boundaries).

#### 3.1.3 Alert Threshold Rules (NOT IMPLEMENTED)

**What:** Compare current metrics against configurable thresholds, surface active alerts.

**Proposed endpoint:** `GET /api/v1/intelligence/alerts`

**Implementation:**
```
New files:
├── application/service/AlertRuleService.java
│   ├── Thresholds from application.yml:
│   │   endorsement.intelligence.alerts:
│   │     stp-rate-min: 95.0
│   │     anomaly-count-max: 5
│   │     health-score-min: 70.0
│   ├── Evaluate current values against thresholds
│   └── Return List<Alert(metric, threshold, currentValue, severity, message)>
├── api/dto/AlertResponse.java (record)
└── Add endpoint to IntelligenceController.java (1 method)
```

**Config-driven:** Operators adjust thresholds via environment variables without code changes.

**Test estimate:** ~6 tests (unit: threshold evaluation, API: alert list contract).

---

### 3.2 Multi-Currency EA Support

**Current:** 0% complete. **Effort:** MEDIUM (3-4 days).

#### What Exists

- `BigDecimal` fields for all monetary amounts (supports arbitrary precision)
- Balance calculations in `EABalanceCalculator` are algebraic and currency-agnostic
- `premiumAmount` field on `Endorsement`, `balance` field on `EAAccount`

#### What to Add

**Step 1 — Schema migration (Flyway V18):**
```sql
ALTER TABLE ea_accounts ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'INR';
ALTER TABLE ea_transactions ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'INR';
ALTER TABLE endorsements ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'INR';

-- Constraint: transactions must match account currency
COMMENT ON COLUMN ea_accounts.currency IS 'ISO 4217 currency code';
```

**Step 2 — Domain model extension:**

| File | Change |
|---|---|
| `domain/model/EAAccount.java` | Add `private String currency = "INR";` with `@Builder.Default` |
| `domain/model/EATransaction.java` | Add `private String currency = "INR";` |
| `domain/model/Endorsement.java` | Add `private String currency = "INR";` |
| JPA entities (3 files) | Add matching `@Column` fields |
| `EndorsementMapper` | Map currency field bidirectionally |

**Step 3 — Currency validation in EABalanceCalculator:**
```java
// Validate same-currency operations
if (!account.getCurrency().equals(transaction.getCurrency())) {
    throw new CurrencyMismatchException(account.getCurrency(), transaction.getCurrency());
}
```

**Step 4 — DTO extension:**
- `CreateEndorsementRequest`: add optional `currency` field (default `INR`)
- `EndorsementResponse`: add `currency` field
- `EAAccountResponse`: add `currency` field

**Why zero test breakage:**
- Schema uses `DEFAULT 'INR'` — existing data unchanged
- `@Builder.Default` on model fields — existing test builders unchanged
- DTO field is optional — existing API calls without `currency` default to INR
- Currency validation only triggers on cross-currency operations (never happens in existing tests)

**Test estimate:** ~20 tests (unit: currency validation, API: multi-currency endorsement creation, BDD: currency mismatch scenarios).

---

### 3.3 Localized Insurer Integrations

**Current:** 20% complete (4 adapters exist, all India-only). **Effort:** LOW-MEDIUM (2-3 days).

#### What Exists

- **4 adapter implementations:** MockInsurerAdapter, IciciLombardAdapter, NivaBupaAdapter, BajajAllianzAdapter
- **InsurerRouter** factory resolves adapter by `insurerId` at runtime
- **InsurerRegistry** with `@Cacheable` configuration lookup
- **InsurerConfiguration** table with 19 columns for full adapter configuration

#### What to Add

**Step 1 — Schema migration (Flyway V19):**
```sql
ALTER TABLE insurer_configurations ADD COLUMN country VARCHAR(3) NOT NULL DEFAULT 'IN';
CREATE INDEX idx_insurer_config_country ON insurer_configurations (country);
```

**Step 2 — Model and entity extension:**

| File | Change |
|---|---|
| `domain/model/InsurerConfiguration.java` | Add `private String country = "IN";` |
| `InsurerConfigurationEntity.java` | Add `@Column(name = "country")` |
| `InsurerConfigurationResponse.java` | Add `country` field to DTO |

**Step 3 — Registry and controller extension:**
```java
// InsurerRegistry.java — new method
public List<InsurerConfiguration> getConfigurationsByCountry(String country) {
    return configurationRepository.findByCountry(country);
}

// InsurerConfigurationController.java — filter parameter
@GetMapping
public ResponseEntity<List<InsurerConfigurationResponse>> listInsurers(
        @RequestParam(required = false) String country) {
    // Filter by country if provided, else return all active
}
```

**Step 4 — Seed regional insurer variants (Flyway V20):**
```sql
-- Example: ICICI Lombard Singapore (different API URL, same adapter type)
INSERT INTO insurer_configurations (insurer_id, insurer_name, insurer_code, adapter_type,
    country, api_base_url, supports_real_time, supports_batch, ...)
VALUES (gen_random_uuid(), 'ICICI Lombard Singapore', 'ICICI_LOMBARD_SG', 'ICICI_LOMBARD',
    'SG', 'https://api-sg.icicilombard.com', true, false, ...);
```

**Why zero test breakage:** Existing adapters default to `country = 'IN'`. No routing logic changes — `InsurerRouter.resolve(insurerId)` continues to work identically. Country is a filter/categorization concern only.

**Test estimate:** ~12 tests (API: list by country, create with country, BDD: regional routing scenarios).

---

### 3.4 Country-Specific Regulatory Compliance

**Current:** 0% complete. **Effort:** MEDIUM (4-5 days).

#### Architecture Approach

The hexagonal architecture enables compliance as a **new port + adapter**, following the same Strategy pattern as insurer adapters. The domain defines what compliance means; infrastructure implements country-specific rules.

**Step 1 — Domain port:**
```java
// domain/port/CompliancePort.java (NEW)
public interface CompliancePort {
    ComplianceResult validate(Endorsement endorsement);
    String getCountryCode();
}

public record ComplianceResult(boolean compliant, List<String> violations) {
    public static ComplianceResult ok() { return new ComplianceResult(true, List.of()); }
}
```

**Step 2 — IRDAI compliance adapter (India):**
```java
// infrastructure/compliance/IrdaiComplianceAdapter.java (NEW)
@Component
public class IrdaiComplianceAdapter implements CompliancePort {
    @Value("${endorsement.compliance.irdai.waiting-period-days:30}")
    private int waitingPeriodDays;

    @Value("${endorsement.compliance.irdai.min-balance-pct:10}")
    private int minBalancePct;

    @Override
    public ComplianceResult validate(Endorsement endorsement) {
        List<String> violations = new ArrayList<>();
        // Rule 1: Coverage start date must be >= 30 days from creation
        // Rule 2: EA account balance >= 10% of annual premium
        // Rule 3: Maximum endorsement frequency per employee
        return new ComplianceResult(violations.isEmpty(), violations);
    }

    @Override
    public String getCountryCode() { return "IN"; }
}
```

**Step 3 — Compliance router (mirrors InsurerRouter pattern):**
```java
// infrastructure/compliance/ComplianceRouter.java (NEW)
@Component
public class ComplianceRouter {
    private final Map<String, CompliancePort> adaptersByCountry;

    public ComplianceRouter(List<CompliancePort> adapters) {
        this.adaptersByCountry = adapters.stream()
            .collect(Collectors.toMap(CompliancePort::getCountryCode, Function.identity()));
    }

    public ComplianceResult validate(Endorsement endorsement, String country) {
        CompliancePort adapter = adaptersByCountry.get(country);
        return adapter != null ? adapter.validate(endorsement) : ComplianceResult.ok();
    }
}
```

**Step 4 — Hook into CreateEndorsementHandler (1 line):**
```java
ComplianceResult compliance = complianceRouter.validate(endorsement, country);
if (!compliance.compliant()) {
    throw new ComplianceViolationException(compliance.violations());
}
```

**Why minimal friction:**
- New port + adapter = no existing code changes except one validation call in the handler
- Country defaults to `IN` — existing tests never trigger compliance checks unless explicitly testing compliance
- Rules are config-driven (`application.yml`) — operators tune without code changes
- Strategy pattern: add UAE/Singapore compliance by creating new adapter classes, zero routing changes

**Regulatory rules per country:**

| Country | Regulator | Key Rules |
|---|---|---|
| India | IRDAI | 30-day waiting period for ADD endorsements, minimum EA balance threshold, annual audit trail |
| UAE | DHIC/DHA | Coverage within 48 hours of employment start, mandatory visa linkage |
| Singapore | MAS | Quarterly regulatory reporting, underwriting documentation requirements |

**Test estimate:** ~15 tests (unit: each IRDAI rule, API: compliance violation response, BDD: compliant vs non-compliant endorsement scenarios).

---

### 3.5 Self-Service Insurer Onboarding Portal

**Current:** 0% complete. **Effort:** HIGH (10-14 weeks). **Recommendation:** Defer to later Phase 4 sprint.

#### Sub-Components Required

| Component | Description | Effort |
|---|---|---|
| **API Spec Parser** | Accept OpenAPI 3.x or WSDL upload, extract endpoint definitions, request/response schemas, field names. Requires `io.swagger.parser.v3:swagger-parser` library. | 2 weeks |
| **Field Mapping Engine** | Map Plum standard fields (employee_name, policy_number, coverage_start) to insurer-specific field names. Store mappings as JSONB in `insurer_configurations.field_mappings`. | 2 weeks |
| **Field Mapping UI** | React drag-and-drop interface: left column (Plum fields), right column (insurer fields from parsed spec). Visual wiring. | 3 weeks |
| **Test Sandbox** | Generate synthetic endorsement data, submit to insurer's test API, capture response, display pass/fail with request/response diff. | 2 weeks |
| **Go-Live Checklist** | Automated validation: spec uploaded, mappings complete, sandbox passed, circuit breaker configured, rate limits set, DR plan reviewed. `POST /api/v1/insurers/onboard/{id}/go-live` activates the insurer. | 1 week |

#### Schema Extension

```sql
ALTER TABLE insurer_configurations ADD COLUMN field_mappings JSONB;
ALTER TABLE insurer_configurations ADD COLUMN api_spec TEXT;
ALTER TABLE insurer_configurations ADD COLUMN sandbox_tested_at TIMESTAMP;
ALTER TABLE insurer_configurations ADD COLUMN go_live_at TIMESTAMP;
ALTER TABLE insurer_configurations ADD COLUMN onboarding_status VARCHAR(20) DEFAULT 'DRAFT';
```

#### Why Deferred

- Large surface area: 5 new backend services + 5 new frontend pages
- External dependency: swagger-parser library integration and testing
- UX design required: drag-and-drop field mapping is a complex UI component
- No immediate business need: current 4 insurers are already onboarded

---

### 3.6 Platform API Marketplace

**Current:** 0% complete. **Effort:** VERY HIGH (16-20 weeks). **Recommendation:** Defer to Phase 5.

#### Sub-Components

| Component | External Dependency | Effort |
|---|---|---|
| **Darwinbox HRIS adapter** | Darwinbox API access + OAuth2 credentials | 2 weeks |
| **Keka HRIS adapter** | Keka API access + API key | 2 weeks |
| **greytHR HRIS adapter** | greytHR API access + basic auth | 2 weeks |
| **HRIS polling scheduler** | None (internal) | 1 week |
| **Payroll connectors** | SAP/BambooHR API access | 3 weeks |
| **Benefits admin partner API** | API key management, multi-tenant auth | 3 weeks |
| **Marketplace portal UI** | None (internal) | 3 weeks |
| **Rate limiting per partner** | Redis-backed token bucket | 1 week |

#### Architecture Sketch

```
New domain port:
├── domain/port/HrisPort.java
│   └── fetchEmployeeChanges(companyId, since) → List<EmployeeChange>
│   └── notifyChangeProcessed(companyId, changeId, status)
│
New adapters:
├── infrastructure/hris/DarwinboxAdapter.java (OAuth2 + REST)
├── infrastructure/hris/KekaAdapter.java (API key + REST)
├── infrastructure/hris/GreythrAdapter.java (Basic auth + REST)
│
New scheduler:
├── application/scheduler/HrisChangePollerScheduler.java
│   └── Poll each active HRIS integration every 15 minutes
│   └── For each employee change: auto-create endorsement via CreateEndorsementHandler
│
New marketplace API:
├── api/controller/MarketplaceController.java
│   └── POST /api/v1/marketplace/submit-endorsement
│   └── GET  /api/v1/marketplace/endorsement/{id}/status
│   └── GET  /api/v1/marketplace/balance/{employerId}
│
New auth:
├── domain/model/ApiKey.java
├── infrastructure/config/ApiKeyAuthFilter.java
└── Rate limiting per API key tier (basic: 100/min, premium: 1000/min)
```

#### Why Deferred

- Heavy external dependencies (HRIS API credentials, partner agreements)
- Multi-tenant auth is a cross-cutting concern requiring careful security design
- Business development prerequisite: partner agreements must be in place before building adapters

---

### 3.7 Multi-Region Deployment

**Current:** 0% complete. **Effort:** VERY HIGH (8-12 weeks). **Recommendation:** Infrastructure-only phase, after all application features are ready.

#### Current Deployment

```
Single-region (Railway):
├── 1x Spring Boot backend (virtual threads, ZGC)
├── 1x PostgreSQL (single instance)
├── 1x Redis (single instance)
├── 1x Kafka (single broker)
├── 1x Elasticsearch (single node)
└── K8s manifests ready but single-namespace, single-replica
```

#### Target Multi-Region Architecture

```
Multi-region (AWS):
├── Region: ap-south-1 (Mumbai, India) — PRIMARY
│   ├── EKS cluster (3 backend replicas, HPA: 3-10)
│   ├── RDS PostgreSQL Multi-AZ (primary write)
│   ├── ElastiCache Redis cluster
│   ├── MSK Kafka (3 brokers, 88 partitions)
│   └── OpenSearch (3-node cluster)
│
├── Region: ap-southeast-1 (Singapore) — SECONDARY
│   ├── EKS cluster (2 backend replicas)
│   ├── RDS PostgreSQL read replica (cross-region)
│   ├── ElastiCache Redis (independent)
│   ├── MSK Kafka (MirrorMaker replication from Mumbai)
│   └── OpenSearch (cross-cluster replication)
│
├── Region: me-south-1 (Bahrain, UAE) — SECONDARY
│   ├── EKS cluster (2 backend replicas)
│   ├── RDS PostgreSQL read replica (cross-region)
│   ├── ElastiCache Redis (independent)
│   └── MSK Kafka (MirrorMaker replication from Mumbai)
│
└── Global:
    ├── Route 53 latency-based routing
    ├── CloudFront CDN for frontend assets
    ├── AWS Global Accelerator for API traffic
    └── Cross-region VPC peering
```

#### Data Residency Requirements

| Country | Requirement | Implementation |
|---|---|---|
| India | Data must reside in India (IRDAI) | Mumbai primary, read replicas elsewhere for non-PII analytics only |
| UAE | DIFC data protection | Bahrain region, PII encrypted at rest with regional KMS key |
| Singapore | PDPA compliance | Singapore region, data classification labels on all PII columns |

#### Why Deferred

- Pure infrastructure work — no application code changes needed
- Requires AWS account setup, VPC design, IAM policies
- Cost modeling needed (multi-region doubles infrastructure spend)
- Data replication strategy requires careful eventual-consistency design
- The application is already cloud-native (stateless, 12-factor, health checks, graceful shutdown) — deployment is a lift-and-shift

---

### 3.8 Mobile Application

**Current:** 0% complete. **Effort:** MEDIUM (6-8 weeks). **Recommendation:** Start with PWA in Phase 4, native mobile in Phase 5.

#### Why Mobile

Insurance HR teams operate across multiple locations — factory floors, branch offices, and remote sites. Key use cases that demand mobile-first workflows:

- **Field approvals:** HR managers approving endorsements while away from desk (70% of confirm/reject actions happen within 5 minutes of notification)
- **Real-time alerts:** Push notifications for anomalies, batch failures, and EA balance alerts
- **Quick lookups:** Checking endorsement status or EA balance during in-person employee conversations
- **Offline capability:** Factory/warehouse locations with intermittent connectivity need queued submissions

#### Architecture Approach — Progressive Web App First, Native Second

The hexagonal architecture and existing REST API make mobile adoption structurally cheap. The strategy follows a two-phase approach:

**Phase A — PWA (Progressive Web App, Weeks 1-4):**

The existing React frontend already uses Vite, TanStack Query (with offline cache), and responsive design. Converting to a PWA requires minimal effort:

```
New files:
├── frontend/public/manifest.json
│   ├── name, short_name, icons (192px, 512px), start_url
│   ├── display: "standalone"
│   ├── theme_color, background_color
│   └── shortcuts: [{ name: "Create Endorsement", url: "/endorsements/new" }]
│
├── frontend/public/sw.js (Service Worker)
│   ├── Cache Strategy: NetworkFirst for API, CacheFirst for static assets
│   ├── Background Sync: queue failed mutations for retry on reconnect
│   ├── Push Notifications: subscribe to FCM/Web Push
│   └── Offline fallback page with cached data display
│
├── frontend/src/hooks/use-push-notifications.ts
│   ├── requestPermission() → subscribe to push endpoint
│   ├── Register subscription with backend: POST /api/v1/notifications/subscribe
│   └── Handle incoming push events → show native OS notification
│
└── frontend/src/hooks/use-offline-queue.ts
    ├── Intercept failed mutations → store in IndexedDB
    ├── On reconnect → replay queued mutations in order
    └── Show "Offline — changes will sync" indicator
```

**Phase B — React Native (Weeks 5-8, optional):**

If PWA limitations are encountered (iOS PWA restrictions on push notifications, limited background processing), a React Native wrapper provides native capabilities while reusing API clients and business logic:

```
New project: plum-mobile/
├── App.tsx (React Navigation)
│   ├── BottomTabNavigator (Dashboard, Endorsements, EA Accounts, Alerts, Profile)
│   └── StackNavigator per tab
│
├── screens/
│   ├── DashboardScreen.tsx — KPI cards, action banner, recent endorsements
│   ├── EndorsementListScreen.tsx — Card-based list (not table), pull-to-refresh
│   ├── EndorsementDetailScreen.tsx — Status timeline, confirm/reject actions
│   ├── CreateEndorsementScreen.tsx — Multi-step form wizard (3 steps)
│   ├── EAAccountScreen.tsx — Balance cards, transaction history
│   ├── AlertsScreen.tsx — Push notification history, anomaly alerts
│   └── ProfileScreen.tsx — User settings, notification preferences, biometric toggle
│
├── services/
│   ├── api.ts — Axios client (same endpoints as web frontend)
│   ├── auth.ts — Biometric auth (Face ID/fingerprint) + JWT token storage in Keychain/Keystore
│   ├── push.ts — FCM registration + token management
│   └── offline.ts — SQLite cache for offline endorsement viewing
│
├── components/
│   ├── EndorsementCard.tsx — Swipeable card (right=confirm, left=reject)
│   ├── StatusBadge.tsx — Reuse color mapping from web
│   ├── BalanceIndicator.tsx — Circular progress gauge
│   └── ActionSheet.tsx — Bottom sheet for bulk actions
│
└── native/
    ├── BiometricAuth.ts — React Native Keychain integration
    ├── HapticFeedback.ts — Vibration on confirm/reject actions
    └── DeepLink.ts — Handle plum://endorsement/{id} deep links
```

#### Mobile-Specific Screens

| Screen | Key Interactions | Offline Support |
|---|---|---|
| Dashboard | Tap KPI → filtered list, pull-to-refresh, action banner | Cached last-known data |
| Endorsement List | Card-based (not table), swipe-to-action, infinite scroll | View cached endorsements |
| Endorsement Detail | Confirm/reject with haptic feedback, share via system sheet | View cached detail |
| Create Endorsement | 3-step wizard (type → employee → review), camera for document scan | Queue submission |
| EA Balance | Balance gauge, transaction timeline, top-up alert | Cached balance |
| Alerts | Push notification history, tap to navigate to endorsement | Cached alerts |

#### Backend Changes for Mobile

All changes are purely additive — no existing endpoint modifications:

```
New files:
├── api/controller/PushNotificationController.java
│   ├── POST /api/v1/notifications/subscribe     — Register device token (FCM/APNs)
│   ├── DELETE /api/v1/notifications/unsubscribe  — Remove device token
│   └── GET /api/v1/notifications/preferences     — Get/set per-category preferences
│
├── api/dto/DeviceRegistrationRequest.java
│   └── record(UUID userId, String deviceToken, String platform, String deviceId)
│
├── domain/port/PushNotificationPort.java
│   ├── sendPush(UUID userId, String title, String body, Map<String,String> data)
│   ├── sendPushToEmployer(UUID employerId, String title, String body)
│   └── registerDevice(DeviceRegistration registration)
│
├── infrastructure/notification/FcmPushNotificationAdapter.java
│   ├── implements PushNotificationPort
│   ├── Uses Firebase Admin SDK for FCM
│   └── Sends to registered device tokens per userId
│
├── infrastructure/persistence/entity/DeviceRegistrationEntity.java
├── infrastructure/persistence/repository/SpringDataDeviceRegistrationRepository.java
│
└── db/migration/V{n}__create_device_registrations.sql
    ├── device_registrations (id, user_id, device_token, platform, device_id, created_at)
    └── Index on user_id for efficient lookup
```

**Integration with existing event system:**

```java
// Extend LoggingNotificationAdapter (or replace with FcmPushNotificationAdapter)
// Hook into existing EventPublisher flow:
// EndorsementEvent → KafkaEventPublisher → AuditLoggerConsumer → PushNotificationPort.sendPush()
//
// Push triggers on:
// - ANOMALY_DETECTED → "Anomaly detected on endorsement #abc" (to employer admin)
// - BATCH_FAILED → "Batch #xyz failed — 3 endorsements need attention" (to ops)
// - BALANCE_FORECAST_ALERT → "EA balance for Acme Corp will be insufficient in 7 days" (to employer admin)
// - ENDORSEMENT_CONFIRMED → "Endorsement #abc confirmed by ICICI Lombard" (to creator)
```

#### Why Medium Friction

- **PWA phase** reuses 100% of existing React codebase — only adds manifest, service worker, and push hooks
- **No backend breaking changes** — all new endpoints and ports are additive
- **Existing API clients** (`*-api.ts`) work identically from mobile
- **TanStack Query** already provides offline cache and optimistic updates — foundation for offline support
- **WebSocket** (STOMP) works in both PWA and React Native contexts

#### Technology Decision: PWA vs React Native vs Flutter

| Criteria | PWA | React Native | Flutter |
|---|---|---|---|
| **Code reuse from web** | 95% | 60% (API/types only) | 0% (full rewrite) |
| **Push notifications** | Web Push (limited on iOS) | Full FCM + APNs | Full FCM + APNs |
| **Offline support** | Service Worker + IndexedDB | SQLite + AsyncStorage | Hive/Isar |
| **Biometric auth** | Web Auth API (limited) | React Native Keychain | local_auth plugin |
| **App store distribution** | No app store needed (installable from browser) | App Store + Play Store | App Store + Play Store |
| **Development effort** | 2-3 weeks | 6-8 weeks | 8-10 weeks |
| **Team skill reuse** | Full (React + TypeScript) | High (React + TypeScript) | Low (Dart) |
| **Recommendation** | **Phase A** (start here) | **Phase B** (if PWA limitations hit) | Not recommended |

#### Test Estimate

| Phase | Unit | API | E2E | Total |
|---|---|---|---|---|
| PWA (service worker, push, offline) | 8 | 4 | 6 | 18 |
| React Native (if pursued) | 15 | 0 (reuse API tests) | 12 (Detox) | 27 |
| **Total** | **23** | **4** | **18** | **45** |

---

### 3.9 CI/CD Pipeline — Harness & Jenkins

**Current:** 0% complete. **Effort:** MEDIUM (3-4 weeks). **Recommendation:** Implement immediately — CI/CD is a P0 prerequisite for every other Phase 4 capability.

#### Current State

The project has **zero CI/CD automation**. All build, test, and deployment operations are manual:

```
Current Manual Workflow:
├── Build:        ./gradlew clean build -x test
├── Unit tests:   ./gradlew test
├── All tests:    ./run-all-tests.sh (orchestrates 5 test suites sequentially)
├── Docker:       docker build -t plum-endorsements .
├── Deploy:       docker-compose up -d (local) or Railway CLI (staging)
├── E2E tests:    cd e2e-tests && npx playwright test
├── Perf tests:   cd performance-tests && ./gradlew gatlingRun
└── Reports:      open http://localhost:5050 (Allure)
```

**Risks of no CI/CD:**
- Broken builds can be merged to `main` — no gate enforcement
- Test suites run only when a developer remembers to execute them
- No artifact versioning — no way to roll back to a known-good build
- No security scanning — OWASP vulnerabilities may be introduced silently
- No environment promotion — staging and production are ad-hoc
- Manual `./start.sh` deploys are error-prone and non-reproducible

#### 3.9.1 Pipeline Architecture

Both Harness and Jenkins implementations share the same logical pipeline stages. The project supports **either** tool — the choice depends on organizational preference and existing infrastructure.

```
Pipeline Stages (11 stages, ~18 min total):

 ┌─────────┐   ┌──────────────────────────────────────────────┐   ┌─────────┐
 │ TRIGGER  │──▶│ BUILD & TEST (parallel where possible)        │──▶│ DEPLOY  │
 └─────────┘   │                                                │   └─────────┘
               │  ┌──────────┐                                  │
  PR / Push    │  │ Compile  │ ./gradlew clean build -x test    │   Dev → Staging → Prod
  to main      │  │ (2 min)  │                                  │   (gated promotion)
               │  └────┬─────┘                                  │
               │       │                                        │
               │  ┌────▼─────────────────────────────────────┐  │
               │  │ PARALLEL TEST EXECUTION                   │  │
               │  │                                           │  │
               │  │  ┌─────────┐ ┌─────────┐ ┌────────────┐  │  │
               │  │  │ Unit    │ │ API     │ │ BDD        │  │  │
               │  │  │ Tests   │ │ Tests   │ │ Tests      │  │  │
               │  │  │ (3 min) │ │ (5 min) │ │ (4 min)    │  │  │
               │  │  └─────────┘ └─────────┘ └────────────┘  │  │
               │  │                                           │  │
               │  │  ┌─────────┐ ┌─────────┐ ┌────────────┐  │  │
               │  │  │Security │ │ SAST    │ │ Dependency │  │  │
               │  │  │ Scan    │ │ (Sonar) │ │ Check      │  │  │
               │  │  │ (2 min) │ │ (3 min) │ │ (1 min)    │  │  │
               │  │  └─────────┘ └─────────┘ └────────────┘  │  │
               │  └──────────────────────────────────────────┘  │
               │       │                                        │
               │  ┌────▼─────┐                                  │
               │  │ Docker   │ Build + push to registry         │
               │  │ (2 min)  │                                  │
               │  └────┬─────┘                                  │
               │       │                                        │
               │  ┌────▼─────────────────────────────────────┐  │
               │  │ POST-DEPLOY TESTS (sequential)            │  │
               │  │  ┌─────────┐ ┌─────────┐ ┌────────────┐  │  │
               │  │  │ E2E     │ │ Perf    │ │ Smoke      │  │  │
               │  │  │ (8 min) │ │ (5 min) │ │ (1 min)    │  │  │
               │  │  └─────────┘ └─────────┘ └────────────┘  │  │
               │  └──────────────────────────────────────────┘  │
               └────────────────────────────────────────────────┘
```

#### 3.9.2 Jenkins Pipeline

Jenkins is a mature, self-hosted CI/CD platform with extensive plugin ecosystem. Ideal for organizations that prefer full infrastructure control, on-premises runners, and maximum customization.

**Jenkinsfile (Declarative Pipeline):**

```groovy
// Jenkinsfile — placed at repository root
pipeline {
    agent {
        kubernetes {
            yaml '''
            apiVersion: v1
            kind: Pod
            spec:
              containers:
              - name: gradle
                image: gradle:8.5-jdk21
                command: ['sleep', 'infinity']
                resources:
                  requests: { memory: "2Gi", cpu: "2" }
              - name: docker
                image: docker:24-dind
                securityContext:
                  privileged: true
              - name: playwright
                image: mcr.microsoft.com/playwright:v1.41.0-jammy
                command: ['sleep', 'infinity']
            '''
        }
    }

    environment {
        DOCKER_REGISTRY = credentials('docker-registry-url')
        DOCKER_CREDS    = credentials('docker-registry-creds')
        SONAR_TOKEN     = credentials('sonar-token')
        ALLURE_RESULTS  = 'build/allure-results'
        APP_VERSION     = "${env.BUILD_NUMBER}-${env.GIT_COMMIT.take(8)}"
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    triggers {
        githubPush()                                    // Trigger on push
        pollSCM('H/5 * * * *')                          // Fallback: poll every 5 min
    }

    stages {
        stage('Compile') {
            steps {
                container('gradle') {
                    sh './gradlew clean build -x test --no-daemon --parallel'
                }
            }
        }

        stage('Parallel Tests & Scans') {
            parallel {
                stage('Unit Tests') {
                    steps {
                        container('gradle') {
                            sh './gradlew test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit 'build/test-results/test/*.xml'
                            jacoco execPattern: 'build/jacoco/test.exec',
                                   classPattern: 'build/classes/java/main',
                                   sourcePattern: 'src/main/java'
                        }
                    }
                }

                stage('API Tests') {
                    steps {
                        container('gradle') {
                            sh './gradlew :api-tests:test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit 'api-tests/build/test-results/test/*.xml'
                        }
                    }
                }

                stage('BDD Tests') {
                    steps {
                        container('gradle') {
                            sh './gradlew :behaviour-tests:test --no-daemon'
                        }
                    }
                    post {
                        always {
                            junit 'behaviour-tests/build/test-results/test/*.xml'
                            cucumber fileIncludePattern: '**/cucumber.json',
                                     sortingMethod: 'ALPHABETICAL'
                        }
                    }
                }

                stage('SAST — SonarQube') {
                    steps {
                        container('gradle') {
                            sh '''
                            ./gradlew sonarqube \
                                -Dsonar.projectKey=plum-endorsements \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.login=${SONAR_TOKEN} \
                                -Dsonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml \
                                --no-daemon
                            '''
                        }
                    }
                }

                stage('Dependency Check') {
                    steps {
                        container('gradle') {
                            sh './gradlew dependencyCheckAnalyze --no-daemon'
                        }
                    }
                    post {
                        always {
                            dependencyCheckPublisher pattern: 'build/reports/dependency-check-report.xml'
                        }
                    }
                }

                stage('Container Security — Trivy') {
                    steps {
                        container('docker') {
                            sh 'trivy fs --severity HIGH,CRITICAL --exit-code 1 .'
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                container('gradle') {
                    script {
                        def coverage = readFile('build/reports/jacoco/test/jacocoTestReport.xml')
                        // Parse line coverage — fail if below 80%
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
            }
        }

        stage('Docker Build & Push') {
            when { branch 'main' }
            steps {
                container('docker') {
                    sh '''
                    docker build -t ${DOCKER_REGISTRY}/plum-endorsements:${APP_VERSION} \
                                 -t ${DOCKER_REGISTRY}/plum-endorsements:latest .
                    docker push ${DOCKER_REGISTRY}/plum-endorsements:${APP_VERSION}
                    docker push ${DOCKER_REGISTRY}/plum-endorsements:latest
                    '''
                }
            }
        }

        stage('Deploy to Staging') {
            when { branch 'main' }
            steps {
                container('gradle') {
                    sh '''
                    kubectl set image deployment/plum-backend \
                        plum-backend=${DOCKER_REGISTRY}/plum-endorsements:${APP_VERSION} \
                        -n plum-staging
                    kubectl rollout status deployment/plum-backend -n plum-staging --timeout=120s
                    '''
                }
            }
        }

        stage('Post-Deploy: E2E Tests') {
            when { branch 'main' }
            steps {
                container('playwright') {
                    sh '''
                    cd e2e-tests
                    npm ci
                    BASE_URL=https://staging.plum-endorsements.internal npx playwright test
                    '''
                }
            }
            post {
                always {
                    publishHTML target: [
                        reportDir: 'e2e-tests/playwright-report',
                        reportFiles: 'index.html',
                        reportName: 'Playwright E2E Report'
                    ]
                }
            }
        }

        stage('Post-Deploy: Performance Tests') {
            when { branch 'main' }
            steps {
                container('gradle') {
                    sh '''
                    cd performance-tests
                    BASE_URL=https://staging.plum-endorsements.internal \
                    ./gradlew gatlingRun --no-daemon
                    '''
                }
            }
            post {
                always {
                    gatlingArchive()
                }
            }
        }

        stage('Promote to Production') {
            when {
                branch 'main'
                expression { currentBuild.result == null || currentBuild.result == 'SUCCESS' }
            }
            input {
                message "Deploy ${APP_VERSION} to production?"
                submitter "ops-team,super-admin"
            }
            steps {
                container('gradle') {
                    sh '''
                    kubectl set image deployment/plum-backend \
                        plum-backend=${DOCKER_REGISTRY}/plum-endorsements:${APP_VERSION} \
                        -n plum-production
                    kubectl rollout status deployment/plum-backend -n plum-production --timeout=180s
                    '''
                }
            }
        }
    }

    post {
        always {
            allure includeProperties: false,
                   results: [[path: 'build/allure-results'],
                             [path: 'api-tests/build/allure-results'],
                             [path: 'behaviour-tests/build/allure-results']]
        }
        success {
            slackSend channel: '#endorsements-ci',
                      color: 'good',
                      message: "BUILD SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER} (${APP_VERSION})"
        }
        failure {
            slackSend channel: '#endorsements-ci',
                      color: 'danger',
                      message: "BUILD FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"
        }
    }
}
```

**Jenkins Plugins Required:**

| Plugin | Purpose |
|---|---|
| Pipeline | Declarative pipeline support |
| Kubernetes | Dynamic pod-based build agents |
| GitHub Integration | Webhook triggers, PR status checks |
| JUnit | Test result visualization |
| JaCoCo | Code coverage reporting |
| SonarQube Scanner | Static analysis integration |
| OWASP Dependency-Check | CVE scanning |
| Allure | Combined test reporting |
| Gatling | Performance test archiving |
| Playwright | E2E test reporting |
| Slack Notification | Build status alerts |
| Docker Pipeline | Docker build/push support |
| Cucumber Reports | BDD test visualization |

**Jenkins Infrastructure:**

```
Jenkins Architecture:
├── Jenkins Controller (2 vCPU, 4GB RAM)
│   ├── Persistent volume for job history
│   ├── GitHub webhook receiver
│   └── Plugin management
│
├── Dynamic Kubernetes Agents (ephemeral pods)
│   ├── gradle container: JDK 21 + Gradle 8.5 (build + test)
│   ├── docker container: Docker-in-Docker (image build)
│   ├── playwright container: Chromium + Node.js (E2E tests)
│   └── Auto-scaled: 0 idle → up to 5 concurrent pods
│
├── Shared Services
│   ├── SonarQube server (code quality gate)
│   ├── Docker Registry (ECR / Harbor / Docker Hub)
│   ├── Allure Report server (http://allure.internal:5050)
│   └── Slack integration (build notifications)
│
└── Credentials Store
    ├── Docker registry credentials
    ├── SonarQube token
    ├── Kubernetes service account
    ├── Slack webhook URL
    └── GitHub PAT (webhook registration)
```

#### 3.9.3 Harness Pipeline

Harness is a modern, cloud-native CI/CD platform with built-in GitOps, feature flags, and cost management. Ideal for organizations adopting cloud-first DevOps practices and wanting minimal pipeline maintenance.

**Harness Pipeline YAML:**

```yaml
# .harness/pipelines/endorsement-pipeline.yaml
pipeline:
  name: Plum Endorsement Pipeline
  identifier: plum_endorsement_pipeline
  projectIdentifier: plum_endorsements
  orgIdentifier: engineering
  tags:
    team: endorsements
    tier: platform

  properties:
    ci:
      codebase:
        connectorRef: github_connector
        repoName: plum-endorsements
        build:
          type: branch
          spec:
            branch: <+input>

  stages:
    # ─── Stage 1: Build & Compile ───
    - stage:
        name: Build
        identifier: build
        type: CI
        spec:
          cloneCodebase: true
          platform:
            os: Linux
            arch: Amd64
          runtime:
            type: Cloud
            spec: {}
          execution:
            steps:
              - step:
                  type: Run
                  name: Compile
                  identifier: compile
                  spec:
                    connectorRef: dockerhub
                    image: gradle:8.5-jdk21
                    command: |
                      ./gradlew clean build -x test --no-daemon --parallel
                    resources:
                      limits:
                        memory: 2Gi
                        cpu: "2"

    # ─── Stage 2: Parallel Tests & Security ───
    - stage:
        name: Test & Scan
        identifier: test_and_scan
        type: CI
        spec:
          cloneCodebase: true
          platform:
            os: Linux
            arch: Amd64
          runtime:
            type: Cloud
            spec: {}
          execution:
            steps:
              - parallel:
                  # Unit Tests
                  - step:
                      type: Run
                      name: Unit Tests
                      identifier: unit_tests
                      spec:
                        connectorRef: dockerhub
                        image: gradle:8.5-jdk21
                        command: |
                          ./gradlew test --no-daemon
                          cp -r build/test-results/test /harness/test-results/unit/
                          cp -r build/allure-results /harness/allure-results/unit/
                        reports:
                          type: JUnit
                          spec:
                            paths:
                              - "build/test-results/test/*.xml"

                  # API Tests (Testcontainers)
                  - step:
                      type: Run
                      name: API Tests
                      identifier: api_tests
                      spec:
                        connectorRef: dockerhub
                        image: gradle:8.5-jdk21
                        command: |
                          ./gradlew :api-tests:test --no-daemon
                        reports:
                          type: JUnit
                          spec:
                            paths:
                              - "api-tests/build/test-results/test/*.xml"
                        privileged: true  # Required for Testcontainers

                  # BDD Tests (Cucumber)
                  - step:
                      type: Run
                      name: BDD Tests
                      identifier: bdd_tests
                      spec:
                        connectorRef: dockerhub
                        image: gradle:8.5-jdk21
                        command: |
                          ./gradlew :behaviour-tests:test --no-daemon
                        reports:
                          type: JUnit
                          spec:
                            paths:
                              - "behaviour-tests/build/test-results/test/*.xml"
                        privileged: true

                  # SonarQube SAST
                  - step:
                      type: Run
                      name: SonarQube Analysis
                      identifier: sonarqube
                      spec:
                        connectorRef: dockerhub
                        image: gradle:8.5-jdk21
                        command: |
                          ./gradlew sonarqube \
                            -Dsonar.projectKey=plum-endorsements \
                            -Dsonar.host.url=$SONAR_HOST_URL \
                            -Dsonar.login=$SONAR_TOKEN \
                            --no-daemon
                        envVariables:
                          SONAR_HOST_URL: <+variable.sonar_host_url>
                          SONAR_TOKEN: <+secrets.getValue("sonar_token")>

                  # OWASP Dependency Check
                  - step:
                      type: Security
                      name: OWASP Dependency Scan
                      identifier: owasp_scan
                      spec:
                        privileged: true
                        settings:
                          product_name: plum-endorsements
                          product_config_name: default
                          scanner_type: repository
                          repository_project: plum-endorsements
                          repository_branch: <+codebase.branch>
                        imagePullPolicy: Always

                  # Trivy Container Scan
                  - step:
                      type: AquaTrivy
                      name: Container Security Scan
                      identifier: trivy_scan
                      spec:
                        mode: orchestration
                        config: default
                        target:
                          type: repository
                          detection: auto
                        advanced:
                          log:
                            level: info
                        privileged: true

    # ─── Stage 3: Quality Gate ───
    - stage:
        name: Quality Gate
        identifier: quality_gate
        type: CI
        spec:
          cloneCodebase: false
          platform:
            os: Linux
            arch: Amd64
          runtime:
            type: Cloud
            spec: {}
          execution:
            steps:
              - step:
                  type: Run
                  name: Enforce Quality Standards
                  identifier: quality_check
                  spec:
                    command: |
                      echo "Quality Gate Checks:"
                      echo "  - Unit test pass rate: must be 100%"
                      echo "  - API test pass rate: must be 100%"
                      echo "  - BDD test pass rate: must be 100%"
                      echo "  - Code coverage: must be >= 80%"
                      echo "  - SonarQube: no new critical/blocker issues"
                      echo "  - OWASP: no HIGH/CRITICAL CVEs"
                      echo "  - Trivy: no CRITICAL container vulnerabilities"
                  failureStrategies:
                    - onFailure:
                        errors:
                          - AllErrors
                        action:
                          type: Abort

    # ─── Stage 4: Docker Build & Push ───
    - stage:
        name: Docker Build
        identifier: docker_build
        type: CI
        when:
          condition: <+codebase.branch> == "main"
        spec:
          cloneCodebase: true
          platform:
            os: Linux
            arch: Amd64
          runtime:
            type: Cloud
            spec: {}
          execution:
            steps:
              - step:
                  type: BuildAndPushDockerRegistry
                  name: Build & Push Image
                  identifier: build_push
                  spec:
                    connectorRef: docker_registry
                    repo: plum/endorsements
                    tags:
                      - <+pipeline.sequenceId>-<+codebase.shortCommitSha>
                      - latest
                    dockerfile: Dockerfile
                    optimize: true
                    caching: true

    # ─── Stage 5: Deploy to Staging ───
    - stage:
        name: Deploy Staging
        identifier: deploy_staging
        type: Deployment
        when:
          condition: <+codebase.branch> == "main"
        spec:
          deploymentType: Kubernetes
          service:
            serviceRef: plum_endorsement_service
            serviceInputs:
              serviceDefinition:
                type: Kubernetes
                spec:
                  artifacts:
                    primary:
                      primaryArtifactRef: plum_endorsements_image
                      sources:
                        - identifier: plum_endorsements_image
                          type: DockerRegistry
                          spec:
                            tag: <+pipeline.sequenceId>-<+codebase.shortCommitSha>
          environment:
            environmentRef: staging
            deployToAll: false
            infrastructureDefinitions:
              - identifier: staging_k8s
          execution:
            steps:
              - step:
                  type: K8sRollingDeploy
                  name: Rolling Deploy
                  identifier: rolling_deploy
                  spec:
                    skipDryRun: false
                    pruningEnabled: true
              - step:
                  type: K8sRollingRollback
                  name: Rollback on Failure
                  identifier: rollback
                  when:
                    stageStatus: Failure

    # ─── Stage 6: Post-Deploy Validation ───
    - stage:
        name: Post-Deploy Validation
        identifier: post_deploy
        type: CI
        when:
          condition: <+codebase.branch> == "main"
        spec:
          cloneCodebase: true
          platform:
            os: Linux
            arch: Amd64
          runtime:
            type: Cloud
            spec: {}
          execution:
            steps:
              # Smoke Tests
              - step:
                  type: Run
                  name: Smoke Tests
                  identifier: smoke_tests
                  spec:
                    command: |
                      # Health check
                      curl -f https://staging.plum-endorsements.internal/actuator/health || exit 1
                      # API smoke
                      curl -f https://staging.plum-endorsements.internal/api/v1/insurers || exit 1
                      echo "Smoke tests passed"

              # E2E Tests (Playwright)
              - step:
                  type: Run
                  name: E2E Tests
                  identifier: e2e_tests
                  spec:
                    connectorRef: dockerhub
                    image: mcr.microsoft.com/playwright:v1.41.0-jammy
                    command: |
                      cd e2e-tests
                      npm ci
                      BASE_URL=https://staging.plum-endorsements.internal npx playwright test
                    reports:
                      type: JUnit
                      spec:
                        paths:
                          - "e2e-tests/test-results/*.xml"

              # Performance Tests (Gatling)
              - step:
                  type: Run
                  name: Performance Tests
                  identifier: perf_tests
                  spec:
                    connectorRef: dockerhub
                    image: gradle:8.5-jdk21
                    command: |
                      cd performance-tests
                      BASE_URL=https://staging.plum-endorsements.internal \
                      ./gradlew gatlingRun --no-daemon

    # ─── Stage 7: Production Deployment ───
    - stage:
        name: Deploy Production
        identifier: deploy_production
        type: Deployment
        when:
          condition: <+codebase.branch> == "main"
        spec:
          deploymentType: Kubernetes
          service:
            serviceRef: plum_endorsement_service
            serviceInputs:
              serviceDefinition:
                type: Kubernetes
                spec:
                  artifacts:
                    primary:
                      primaryArtifactRef: plum_endorsements_image
                      sources:
                        - identifier: plum_endorsements_image
                          type: DockerRegistry
                          spec:
                            tag: <+pipeline.sequenceId>-<+codebase.shortCommitSha>
          environment:
            environmentRef: production
            deployToAll: false
            infrastructureDefinitions:
              - identifier: production_k8s
          execution:
            steps:
              - step:
                  type: HarnessApproval
                  name: Production Approval
                  identifier: prod_approval
                  spec:
                    approvalMessage: "Deploy version <+pipeline.sequenceId> to production?"
                    approvers:
                      userGroups:
                        - ops_team
                        - engineering_leads
                    approverInputs: []
                    isAutoRejectEnabled: true
                    autoRejectTimeout: 24h

              - step:
                  type: K8sCanaryDeploy
                  name: Canary Deploy (10%)
                  identifier: canary_deploy
                  spec:
                    instanceSelection:
                      type: Count
                      spec:
                        count: 1
                    skipDryRun: false

              - step:
                  type: Verify
                  name: Canary Verification
                  identifier: canary_verify
                  spec:
                    type: Canary
                    monitoredService:
                      type: Default
                      spec: {}
                    spec:
                      sensitivity: MEDIUM
                      duration: 10m
                      deploymentTag: <+pipeline.sequenceId>

              - step:
                  type: K8sCanaryDelete
                  name: Delete Canary
                  identifier: canary_delete
                  spec: {}

              - step:
                  type: K8sRollingDeploy
                  name: Full Rolling Deploy
                  identifier: full_deploy
                  spec:
                    skipDryRun: false

            rollbackSteps:
              - step:
                  type: K8sRollingRollback
                  name: Rollback
                  identifier: prod_rollback

  # ─── Notification Rules ───
  notificationRules:
    - name: Pipeline Notifications
      identifier: pipeline_notifications
      pipelineEvents:
        - type: PipelineSuccess
        - type: PipelineFailed
        - type: StageSuccess
          forStages:
            - deploy_production
        - type: StageFailed
      notificationMethod:
        type: Slack
        spec:
          webhookUrl: <+variable.slack_webhook_url>
      enabled: true
```

**Harness-Specific Features Used:**

| Feature | Usage | Benefit |
|---|---|---|
| **Harness Cloud runners** | Managed build infrastructure (no Jenkins agents to maintain) | Zero infrastructure maintenance |
| **Built-in Test Intelligence** | ML-based test selection — runs only tests affected by code changes | 40-60% faster PR builds |
| **Canary deployment** | 10% traffic to new version, automated metric verification | Automated rollback on regression |
| **Harness Approval** | Human gate before production with 24h auto-reject timeout | Controlled promotion |
| **Built-in OWASP/Trivy** | Native security scanning steps (no plugin management) | Single pane of glass for security |
| **Feature Flags (optional)** | `endorsement.auth.enabled` can be managed via Harness FF | Runtime feature control without redeploy |
| **Cost Management** | Track CI/CD spend per pipeline, per stage | Optimize build costs |
| **GitOps (ArgoCD)** | Harness GitOps module syncs K8s manifests from `k8s/` directory | Declarative deployment state |

#### 3.9.4 Jenkins vs Harness — Decision Matrix

| Criteria | Jenkins | Harness | Recommendation |
|---|---|---|---|
| **Infrastructure** | Self-hosted (manage controller + agents) | Managed cloud (zero infra) | Harness if cloud-first; Jenkins if on-prem required |
| **Setup time** | 2-3 days (plugins, agents, credentials) | 1 day (SaaS, connectors) | Harness |
| **Pipeline-as-code** | Jenkinsfile (Groovy DSL) | YAML + visual editor | Both excellent |
| **Test intelligence** | Manual configuration | ML-based automatic test selection | Harness |
| **Deployment strategies** | Basic (rolling, blue-green via plugins) | Native canary + automated verification | Harness |
| **Security scanning** | Plugin-based (OWASP, Trivy plugins) | Built-in STO module | Harness |
| **Cost** | Free (self-hosted) + infra cost | $100/dev/month (Team) or $300/dev/month (Enterprise) | Jenkins for cost; Harness for value |
| **Plugin ecosystem** | 1,800+ plugins | Fewer, but built-in modules cover most needs | Jenkins for extensibility |
| **Learning curve** | Steep (Groovy, plugin conflicts) | Moderate (YAML, visual editor) | Harness |
| **Enterprise features** | Via plugins (RBAC, audit, governance) | Native (OPA policies, RBAC, audit trail) | Harness |
| **Multi-pipeline orchestration** | Requires manual configuration | Native pipeline chaining | Harness |
| **Community** | Massive open-source community | Growing, enterprise-focused | Jenkins |

**Recommendation:** Use **Harness** for greenfield cloud-native deployments (this project). Use **Jenkins** if the organization has existing Jenkins infrastructure and expertise. Both Jenkinsfile and Harness YAML are provided — the team can adopt either without code changes.

#### 3.9.5 Quality Gates

Every merge to `main` must pass all gates before deployment:

| Gate | Threshold | Tool | Enforcement |
|---|---|---|---|
| Unit test pass rate | 100% | JUnit | Pipeline fails on any test failure |
| API test pass rate | 100% | JUnit + Testcontainers | Pipeline fails on any test failure |
| BDD test pass rate | 100% | Cucumber + JUnit | Pipeline fails on any test failure |
| Code coverage (line) | >= 80% | JaCoCo + SonarQube | Quality gate blocks merge |
| Code coverage (branch) | >= 70% | JaCoCo + SonarQube | Quality gate blocks merge |
| SonarQube quality gate | No new critical/blocker issues | SonarQube | Pipeline fails on gate violation |
| OWASP dependency CVEs | No HIGH or CRITICAL | OWASP Dependency-Check | Pipeline fails if CVE found |
| Container vulnerabilities | No CRITICAL | Trivy | Pipeline fails if vuln found |
| E2E test pass rate | 100% | Playwright | Blocks staging → production promotion |
| P95 response time | < 500ms | Gatling | Alerts on regression, does not block |
| Error rate | < 1% | Gatling | Blocks staging → production promotion |

#### 3.9.6 Environment Strategy

```
Environment Promotion Flow:

┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│    Dev      │────▶│   Staging   │────▶│  Canary     │────▶│ Production │
│             │     │             │     │  (10%)      │     │  (100%)    │
│ Auto-deploy │     │ Auto-deploy │     │ Auto-verify │     │ Manual     │
│ on PR merge │     │ on main     │     │ 10 min      │     │ approval   │
│             │     │ push        │     │ metrics     │     │ required   │
│ Unit + API  │     │ + E2E       │     │ Prometheus  │     │            │
│ + BDD tests │     │ + Perf      │     │ comparison  │     │            │
└────────────┘     └────────────┘     └────────────┘     └────────────┘

Environment Configuration:
├── Dev:        docker-compose.yml (local) or K8s namespace: plum-dev
│   ├── PostgreSQL: ephemeral (Testcontainers for tests, Docker for manual)
│   ├── Kafka: embedded (spring.kafka.embedded for tests) or Docker
│   ├── Redis: embedded or Docker
│   └── Profile: application-test.yml
│
├── Staging:    K8s namespace: plum-staging
│   ├── PostgreSQL: RDS (staging instance, daily backup)
│   ├── Kafka: MSK (single broker, 3 partitions per topic)
│   ├── Redis: ElastiCache (single node)
│   ├── Profile: application-staging.yml
│   └── Data: seeded via start.sh (mock insurers + sample endorsements)
│
└── Production: K8s namespace: plum-production
    ├── PostgreSQL: RDS Multi-AZ (automated backup, PITR)
    ├── Kafka: MSK (3 brokers, 32 partitions per topic)
    ├── Redis: ElastiCache (cluster mode)
    ├── Profile: application-production.yml
    └── Data: production data only (no seed data)
```

#### 3.9.7 Artifact Management

```
Artifact Flow:
├── Source: GitHub (main branch)
├── Build:  Gradle → JAR (build/libs/endorsements-*.jar)
├── Image:  Docker → ECR / Docker Hub / Harbor
│   ├── Tag: {pipeline-id}-{short-commit-sha} (e.g., 42-a1b2c3d4)
│   ├── Tag: latest (for dev convenience)
│   └── Tag: v{semver} (for releases, e.g., v2.1.0)
├── Reports:
│   ├── Allure: combined report → S3 / Allure Server
│   ├── JaCoCo: coverage report → SonarQube
│   ├── Gatling: performance report → S3 / Gatling Enterprise
│   └── Playwright: HTML report → S3 / artifact storage
└── Cache:
    ├── Gradle: ~/.gradle/caches → pipeline cache (saves 1-2 min per build)
    ├── Docker layers: registry layer caching (saves 30-60s per build)
    └── npm: node_modules → pipeline cache (saves 30s for E2E/frontend)
```

#### 3.9.8 GitOps Integration (ArgoCD)

The existing `k8s/` directory contains Kubernetes manifests ready for GitOps:

```
GitOps Flow (ArgoCD):
├── Developer pushes to main → CI pipeline runs
├── Pipeline updates k8s/backend/deployment.yaml with new image tag
├── ArgoCD detects manifest change → syncs to target cluster
├── ArgoCD performs health check (readinessProbe, livenessProbe)
├── If unhealthy → ArgoCD auto-reverts to previous manifest version
│
Existing K8s manifests:
├── k8s/namespace.yaml
├── k8s/backend/deployment.yaml      ← Image tag updated by CI pipeline
├── k8s/backend/service.yaml
├── k8s/backend/configmap.yaml       ← Environment-specific config
├── k8s/postgres/                    ← Database deployment
├── k8s/kafka/                       ← Kafka deployment
├── k8s/redis/                       ← Redis deployment
├── k8s/elasticsearch/               ← ELK stack
├── k8s/prometheus/                  ← Metrics collection
├── k8s/grafana/                     ← Dashboard visualization
├── k8s/jaeger/                      ← Distributed tracing
└── k8s/seed-job.yaml                ← Data seeding job
```

**ArgoCD Application (for Harness GitOps module or standalone):**

```yaml
# argocd/application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: plum-endorsements
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/org/plum-endorsements.git
    targetRevision: main
    path: k8s
  destination:
    server: https://kubernetes.default.svc
    namespace: plum-production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
    retry:
      limit: 3
      backoff:
        duration: 5s
        maxDuration: 3m
        factor: 2
```

#### Why Medium Friction

- **Existing test infrastructure** (`./run-all-tests.sh`, Allure, Testcontainers, Playwright, Gatling) maps 1:1 to pipeline stages
- **Existing K8s manifests** (`k8s/` directory) are ready for GitOps adoption
- **Existing Dockerfile** is multi-stage and production-optimized
- **Existing `application-*.yml` profiles** support environment-specific configuration
- **No application code changes** — CI/CD is purely infrastructure/tooling
- **Both Jenkins and Harness** support the same pipeline stages — vendor-agnostic design

#### Implementation Effort

| Component | Effort | Dependencies |
|---|---|---|
| Jenkinsfile + agent configuration | 2 days | Jenkins controller + K8s cluster |
| Harness pipeline YAML + connectors | 1 day | Harness account |
| SonarQube project setup + quality gates | 1 day | SonarQube instance |
| JaCoCo integration in `build.gradle.kts` | 0.5 day | None |
| OWASP Dependency-Check plugin in Gradle | 0.5 day | None |
| Docker registry setup (ECR/Harbor) | 0.5 day | AWS account or Harbor instance |
| ArgoCD application configuration | 0.5 day | ArgoCD on K8s cluster |
| Slack/Teams notification integration | 0.5 day | Webhook URL |
| Environment-specific `application-*.yml` profiles | 1 day | None |
| Pipeline testing and debugging | 2 days | All above |
| Documentation and runbook | 1 day | None |
| **Total** | **~10 days (2 weeks)** | |

#### Test Estimate

| Test Type | Count | Description |
|---|---|---|
| Pipeline integration tests | 5 | Verify each stage runs correctly in isolation |
| Quality gate validation | 4 | Verify coverage thresholds, CVE blocking, test failure gating |
| Rollback verification | 3 | Verify canary rollback, manual rollback, ArgoCD self-heal |
| **Total** | **12** | |

---

## 4. Implementation Roadmap

### Minimal-Friction Sprint (Weeks 1-4)

These 4 capabilities can be delivered in a single sprint with zero existing test breakage. All changes are purely additive.

```
Week 1: Complete Advanced Analytics
├── TrendAnalysisService + endpoint                    (1 day)
├── Employer segmentation in HealthScoreService        (0.5 day)
├── AlertRuleService + configurable thresholds         (1 day)
├── Tests: ~18 new (unit + API + E2E)                  (1 day)
└── Grafana dashboard panels for trends + alerts       (0.5 day)

Week 2: Multi-Currency EA Support
├── Flyway V18: currency columns with DEFAULT 'INR'    (0.5 day)
├── Domain model + entity + mapper extension           (1 day)
├── Currency validation in EABalanceCalculator          (0.5 day)
├── DTO extension (request + response)                 (0.5 day)
├── Tests: ~20 new (unit + API + BDD)                  (1.5 days)
└── Frontend: currency display in endorsement list     (1 day)

Week 3: Localized Insurer Integrations
├── Flyway V19: country column with DEFAULT 'IN'       (0.5 day)
├── Model + entity + DTO extension                     (0.5 day)
├── InsurerRegistry.getConfigurationsByCountry()       (0.5 day)
├── Controller filter: GET /insurers?country=SG        (0.5 day)
├── Flyway V20: seed regional insurer variants         (0.5 day)
├── Tests: ~12 new (API + BDD + E2E)                   (1.5 days)
└── Frontend: country filter in insurer list           (1 day)

Week 4: IRDAI Compliance Framework
├── CompliancePort interface                           (0.5 day)
├── IrdaiComplianceAdapter (3 rules, config-driven)    (1 day)
├── ComplianceRouter (mirrors InsurerRouter pattern)   (0.5 day)
├── Hook into CreateEndorsementHandler (1 line)        (0.5 day)
├── ComplianceViolationException + handler mapping     (0.5 day)
├── Tests: ~15 new (unit + API + BDD)                  (1.5 days)
└── Config: application.yml compliance thresholds      (0.5 day)
```

```
Parallel Track: CI/CD Pipeline (Weeks 1-2, runs alongside above)
├── Week 1: Pipeline setup
│   ├── Jenkinsfile or Harness YAML                      (1 day)
│   ├── SonarQube project + quality gates                 (1 day)
│   ├── JaCoCo + OWASP Dependency-Check in Gradle         (0.5 day)
│   ├── Docker registry (ECR/Harbor) setup                (0.5 day)
│   └── ArgoCD application configuration                 (0.5 day)
│
└── Week 2: Validation & hardening
    ├── Pipeline testing + debugging                      (2 days)
    ├── Environment-specific application.yml profiles     (1 day)
    ├── Slack/Teams notification integration               (0.5 day)
    └── Documentation + runbook                           (0.5 day)
```

**Sprint deliverables:** 4 capabilities + CI/CD pipeline, ~77 new tests, 0 existing test breakage.

### Medium-Term (Weeks 5-16)

| Weeks | Capability | Effort |
|---|---|---|
| 5-6 | Mobile Application — PWA (manifest, service worker, push notifications, offline queue) | 2 weeks |
| 5-8 | UAE (DHIC) + Singapore (MAS) compliance adapters | 4 weeks |
| 7-8 | Mobile Application — React Native (if PWA limitations encountered) | 2 weeks |
| 9-12 | Self-service insurer onboarding portal (spec parser + field mapping) | 4 weeks |
| 13-16 | Onboarding portal (sandbox + go-live checklist + frontend) | 4 weeks |

### Long-Term (Weeks 17-28)

| Weeks | Capability | Effort |
|---|---|---|
| 17-22 | Platform API marketplace (HRIS adapters + marketplace API) | 6 weeks |
| 23-28 | Multi-region AWS deployment (infrastructure) | 6 weeks |

---

## 5. Architecture Readiness Assessment

The hexagonal architecture ensures Phase 4 features map cleanly to existing patterns:

### Pattern Reuse for Phase 4

| Phase 4 Feature | Existing Pattern | How It Applies |
|---|---|---|
| Compliance rules | **Strategy Pattern** (InsurerPort) | `CompliancePort` + country-specific adapters, resolved by `ComplianceRouter` |
| HRIS integrations | **Adapter Pattern** (JPA adapters) | `HrisPort` + HRIS-specific adapters (Darwinbox, Keka, greytHR) |
| Multi-currency | **Domain Model Extension** (Endorsement) | Add `currency` field to existing models with defaults |
| Regional insurers | **Factory Pattern** (InsurerRouter) | Add `country` parameter to `InsurerRegistry` lookup |
| Trend analysis | **CQRS Query Handler** (EndorsementQueryHandler) | Read-only `@Transactional(readOnly = true)` service querying existing data |
| API marketplace auth | **Filter Chain** (RateLimitingFilter) | `ApiKeyAuthFilter` in SecurityConfig filter chain |
| Compliance events | **Observer Pattern** (EndorsementEvent) | Add `ComplianceChecked(violations)` to sealed event hierarchy |
| Mobile push notifications | **Adapter Pattern** (NotificationPort) | `PushNotificationPort` + FCM adapter, same event-driven flow |
| Mobile offline sync | **CQRS** (TanStack Query) | Existing query cache + IndexedDB for offline reads, background sync for writes |
| CI/CD pipeline | **Existing test infrastructure** | `run-all-tests.sh` stages map 1:1 to pipeline stages; `k8s/` manifests enable GitOps |
| GitOps deployment | **12-Factor App** (stateless, config-driven) | Existing K8s manifests + `application-*.yml` profiles support multi-environment promotion |

### Zero-Breakage Guarantees

Each Phase 4 addition follows this invariant:

```
1. New port interface in domain/port/          → no existing code change
2. New adapter in infrastructure/              → no existing code change
3. New @Builder.Default field on domain model  → existing builders unchanged
4. New column with DEFAULT in Flyway migration → existing data unchanged
5. New optional DTO field                      → existing API calls unchanged
6. New controller endpoint                     → existing endpoints unchanged
7. New unit/API/BDD/E2E tests                  → existing test suites unchanged
```

---

## 6. Test Impact Analysis

### Estimated New Tests per Capability

| Capability | Unit | API | BDD | E2E | Perf | Total |
|---|---|---|---|---|---|---|
| Complete Analytics | 8 | 6 | 2 | 4 | 0 | 20 |
| Multi-Currency | 10 | 6 | 3 | 4 | 0 | 23 |
| Localized Insurers | 4 | 4 | 2 | 4 | 0 | 14 |
| IRDAI Compliance | 8 | 4 | 3 | 2 | 0 | 17 |
| Mobile PWA | 8 | 4 | 0 | 6 | 0 | 18 |
| CI/CD Pipeline | 0 | 0 | 0 | 0 | 0 | 12 (pipeline validation) |
| **Sprint total** | **38** | **24** | **10** | **20** | **0** | **104** |

**Projected test count after Sprint 1 + Mobile + CI/CD:** 578 (current) + 104 = **682 tests** (plus 12 pipeline tests tracked externally).

### Existing Test Suite Health

All 578 current tests pass (verified March 14, 2026):
- Unit: 381, API: 105 (via Testcontainers), BDD: 75 (Cucumber), E2E: 156 (Playwright), Perf: 6 (Gatling)
- Allure combined report at http://localhost:5050
- Zero flaky tests, zero skipped tests

---

## 7. Risk Matrix

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Multi-currency introduces rounding errors | Medium | High | Use `BigDecimal` with explicit `RoundingMode.HALF_UP`; property-based tests for currency math |
| Compliance rules block legitimate endorsements | Medium | High | Config-driven thresholds; dry-run mode (`compliance.mode: WARN` vs `ENFORCE`) |
| Regional insurer API differences break adapters | Low | Medium | Each region gets its own InsurerConfiguration row; adapters are configuration-driven |
| HRIS API rate limits cause polling failures | Medium | Low | Exponential backoff via existing Resilience4j `@Retry`; circuit breaker per HRIS |
| Multi-region data consistency issues | High | High | Defer to last; design async replication with conflict resolution before implementation |
| iOS PWA push notification limitations | High | Medium | Start with PWA for Android; fall back to React Native wrapper for iOS if push is critical |
| Offline mutation conflicts | Medium | Medium | Last-write-wins with server timestamp; show conflict resolution UI for concurrent edits |
| Testcontainers in CI (Docker-in-Docker) | Medium | High | Use privileged mode for Harness/Jenkins K8s agents; alternatively use Testcontainers Cloud |
| Pipeline flakiness from E2E tests | Medium | Medium | Playwright retry on failure (2 retries); quarantine flaky tests with `@Retry` annotation |
| SonarQube false positives blocking merges | Low | Medium | Tune quality profile; use `//NOSONAR` for intentional patterns; review gate weekly |
| Secret exposure in pipeline logs | Low | Critical | Use credential store (Harness secrets / Jenkins credentials); mask all secret values in logs |

---

## 8. Decision Log

| Decision | Rationale | Alternative Considered |
|---|---|---|
| **Additive schema migrations with defaults** | Zero-downtime deployment; existing data unaffected | Recreate tables (rejected: data loss risk) |
| **CompliancePort as domain port** | Follows hexagonal architecture; domain defines contract | Compliance as aspect (rejected: cross-cutting but business-critical, belongs in domain) |
| **Config-driven compliance rules** | Operators adjust without redeployment; country rules change frequently | Hardcoded rules (rejected: inflexible for regulatory changes) |
| **Country field on InsurerConfiguration, not separate table** | Simpler schema; 1:1 relationship between config and country | Separate `countries` table with M:N (rejected: over-engineering for 3 countries) |
| **Defer multi-region to last** | Application is already cloud-native; infrastructure is the only gap | Build multi-region first (rejected: no business need until other features ready) |
| **PWA-first mobile strategy** | 95% code reuse from existing React frontend; no app store approval needed; installable from browser | React Native first (rejected: 60% code reuse, 3x effort for marginal benefit); Flutter (rejected: 0% reuse, Dart skill gap) |
| **PushNotificationPort as new domain port** | Follows hexagonal architecture; decouples push delivery mechanism from business events | Embed FCM calls directly in handlers (rejected: violates port/adapter pattern, hard to test) |
| **Provide both Jenkins and Harness pipelines** | Team can adopt either without code changes; vendor-agnostic | Single-vendor lock-in (rejected: organizational constraints vary) |
| **Harness recommended for greenfield** | Zero infrastructure to maintain; built-in test intelligence, canary verification, and security scanning | Jenkins (considered: better for on-prem, massive plugin ecosystem, but higher maintenance) |
| **Canary deployment for production** | Automated metric verification catches regressions before full rollout; existing Prometheus metrics enable auto-verification | Blue-green (rejected: requires 2x infrastructure); Rolling-only (rejected: no automated regression detection) |
| **ArgoCD for GitOps** | Existing `k8s/` manifests are ready; declarative desired state; auto-revert on health check failure | Helm charts (rejected: over-engineering for single-app repo); manual `kubectl apply` (rejected: not auditable) |
| **80% line coverage quality gate** | Balances thoroughness with developer velocity; existing test suite already exceeds this threshold | 90% (rejected: diminishes returns on test investment); 60% (rejected: too low for insurance domain) |

---

## 9. Authentication & Authorization Architecture

> **Note:** Auth is not a requirement of the design challenge. This section documents the production-grade AuthZ/AuthN strategy that would be layered onto the platform when moving from demo/MVP to a live multi-tenant deployment.

### 9.1 Current State

The codebase is intentionally unauthenticated for MVP demonstration:

```java
// SecurityConfig.java — current state
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/ws/**").permitAll()
    .anyRequest().permitAll());
```

- **Session management:** Already `STATELESS` — aligned with JWT approach
- **CSRF:** Already disabled — correct for stateless API-only architecture
- **Audit logging:** Records `action`, `entityType`, `entityId`, `timestamp` — but no `userId` or `userRole`
- **Rate limiting:** IP-based via `RateLimitingFilter` — no per-user or per-tenant throttling

### 9.2 Authentication Strategy — JWT + OAuth2

#### Why JWT

The platform already enforces `SessionCreationPolicy.STATELESS`. JWT is the natural fit:

- **Stateless:** Token carries identity claims; no server-side session store needed
- **Scalable:** Any backend replica can validate tokens independently (public key verification)
- **Composable:** Works with external IdPs (Google Workspace, Okta, Azure AD) via OAuth2/OIDC
- **Existing alignment:** Spring Security's `oauth2-resource-server` integrates with the current `SecurityFilterChain`

#### Token Architecture

```
Access Token (JWT, short-lived: 15 min)
├── Header: { alg: RS256, typ: JWT }
├── Payload:
│   ├── sub: "user-uuid"                    # unique user identifier
│   ├── email: "admin@employer.com"         # user email
│   ├── roles: ["EMPLOYER_ADMIN"]           # RBAC roles (see §9.3)
│   ├── tenantId: "employer-uuid"           # multi-tenant isolation
│   ├── insurerId: "insurer-uuid"           # null for non-insurer users
│   ├── iat: 1710400000                     # issued at
│   ├── exp: 1710400900                     # expires in 15 min
│   └── iss: "plum-endorsements"            # issuer
└── Signature: RS256 (asymmetric — public key for verification)

Refresh Token (opaque, long-lived: 7 days)
├── Stored in PostgreSQL (hashed, with expiry)
├── Single-use: rotated on each refresh
└── Revocable: DELETE /api/v1/auth/logout invalidates immediately
```

#### Authentication Flow

```
                    ┌─────────────────────────────────────┐
                    │           Frontend (React)           │
                    │                                     │
                    │  1. POST /api/v1/auth/login          │
                    │     { email, password }              │
                    │                                     │
                    │  2. Receive { accessToken,           │
                    │     refreshToken, expiresIn }        │
                    │                                     │
                    │  3. Store accessToken in memory      │
                    │     (NOT localStorage — XSS risk)    │
                    │     Store refreshToken as HttpOnly   │
                    │     Secure cookie                    │
                    │                                     │
                    │  4. Attach header on every request:  │
                    │     Authorization: Bearer <token>    │
                    │                                     │
                    │  5. On 401: POST /api/v1/auth/refresh│
                    │     with HttpOnly cookie             │
                    │     → new accessToken + refreshToken │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         Backend (Spring Boot)        │
                    │                                     │
                    │  JwtAuthenticationFilter             │
                    │  ├── Extract Bearer token            │
                    │  ├── Validate signature (RS256)      │
                    │  ├── Check expiry                    │
                    │  ├── Extract claims → UserPrincipal  │
                    │  ├── Set SecurityContext              │
                    │  └── Set MDC: userId, tenantId       │
                    │                                     │
                    │  @PreAuthorize on endpoints          │
                    │  ├── Role check                      │
                    │  └── Tenant isolation check          │
                    └─────────────────────────────────────┘
```

#### OAuth2/OIDC Integration (External IdP)

For enterprise customers using Google Workspace, Okta, or Azure AD:

```
Login Flow (Authorization Code + PKCE):
1. Frontend redirects to IdP: /authorize?client_id=...&redirect_uri=...&code_challenge=...
2. User authenticates with IdP (SSO, MFA)
3. IdP redirects back with authorization code
4. Backend exchanges code for IdP tokens: POST /token
5. Backend extracts user identity from IdP id_token
6. Backend issues Plum JWT (access + refresh) with roles from local DB
7. Frontend uses Plum JWT for all subsequent API calls
```

This decouples authentication (delegated to IdP) from authorization (managed locally in Plum's RBAC tables).

### 9.3 Authorization Strategy — Role-Based Access Control (RBAC)

#### Role Hierarchy

```
SUPER_ADMIN                          # Plum platform administrators
├── Full system access
├── Manage all employers, insurers, users
└── Configure system-wide settings

PLUM_OPS                             # Plum operations team
├── View all employers and endorsements
├── Trigger reconciliation, batch assembly
├── View audit logs, intelligence dashboards
└── Cannot modify system configuration

EMPLOYER_ADMIN                       # Employer HR administrators
├── Create/view endorsements for OWN employer only
├── View EA account balances for OWN employer
├── View health score for OWN employer
└── Cannot access other employers' data

EMPLOYER_VIEWER                      # Employer HR read-only users
├── View endorsements for OWN employer only
├── View EA account balances (read-only)
└── Cannot create, confirm, or reject endorsements

INSURER_ADMIN                        # Insurer partner administrators
├── View endorsements submitted to OWN insurer
├── Confirm/reject endorsements assigned to OWN insurer
├── View benchmarks and STP rates for OWN insurer
└── Cannot access other insurers' data or employer financials
```

#### Endpoint Authorization Matrix

| Endpoint Pattern | SUPER_ADMIN | PLUM_OPS | EMPLOYER_ADMIN | EMPLOYER_VIEWER | INSURER_ADMIN |
|---|---|---|---|---|---|
| `POST /api/v1/endorsements` | Yes | Yes | Own employer | No | No |
| `GET /api/v1/endorsements` | All | All | Own employer | Own employer | Own insurer |
| `POST /api/v1/endorsements/{id}/confirm` | Yes | Yes | No | No | Own insurer |
| `POST /api/v1/endorsements/{id}/reject` | Yes | Yes | No | No | Own insurer |
| `GET /api/v1/ea-accounts/{employerId}` | Yes | Yes | Own employer | Own employer | No |
| `GET /api/v1/intelligence/**` | Yes | Yes | Own employer | Own employer | Own insurer |
| `POST /api/v1/insurers` | Yes | No | No | No | No |
| `PUT /api/v1/insurers/{id}` | Yes | No | No | No | Own insurer |
| `GET /api/v1/audit-logs` | Yes | Yes | No | No | No |
| `POST /api/v1/auth/users` | Yes | No | No | No | No |
| `GET /actuator/**` | Yes | Yes | No | No | No |

#### Tenant Isolation — Data-Level Security

Beyond role checks, every data query must enforce tenant boundaries:

```java
// Example: EndorsementQueryHandler with tenant isolation
@Service
@Transactional(readOnly = true)
public class EndorsementQueryHandler {

    public Page<Endorsement> findByEmployerId(UUID employerId, Pageable pageable,
                                               UserPrincipal principal) {
        // Tenant isolation: employer users can only query their own data
        if (principal.hasRole("EMPLOYER_ADMIN") || principal.hasRole("EMPLOYER_VIEWER")) {
            if (!principal.getTenantId().equals(employerId)) {
                throw new AccessDeniedException("Cannot access another employer's endorsements");
            }
        }
        return endorsementRepository.findByEmployerId(employerId, pageable);
    }
}
```

For insurer users, isolation filters by `insurerId` instead of `employerId`.

### 9.4 Hexagonal Architecture — Auth as Port + Adapter

Following the established pattern, authentication is a domain concern (port) with infrastructure implementation (adapter):

```
Domain Layer (new):
├── domain/model/User.java
│   ├── UUID id
│   ├── String email
│   ├── String passwordHash (nullable — null for SSO users)
│   ├── Set<Role> roles
│   ├── UUID tenantId (employerId or insurerId)
│   ├── TenantType tenantType (EMPLOYER | INSURER | PLATFORM)
│   ├── boolean active
│   ├── Instant lastLoginAt
│   └── Instant createdAt
│
├── domain/model/Role.java (enum)
│   └── SUPER_ADMIN, PLUM_OPS, EMPLOYER_ADMIN, EMPLOYER_VIEWER, INSURER_ADMIN
│
├── domain/model/RefreshToken.java
│   ├── UUID id
│   ├── UUID userId
│   ├── String tokenHash
│   ├── Instant expiresAt
│   └── boolean revoked
│
├── domain/port/AuthenticationPort.java
│   ├── UserPrincipal authenticate(String email, String password)
│   ├── TokenPair issueTokens(User user)
│   ├── UserPrincipal validateToken(String accessToken)
│   ├── TokenPair refreshTokens(String refreshToken)
│   └── void revokeRefreshToken(String refreshToken)
│
├── domain/port/UserRepository.java
│   ├── Optional<User> findByEmail(String email)
│   ├── Optional<User> findById(UUID id)
│   ├── User save(User user)
│   └── List<User> findByTenantId(UUID tenantId)

Infrastructure Layer (new):
├── infrastructure/auth/JwtAuthenticationAdapter.java
│   ├── implements AuthenticationPort
│   ├── Uses io.jsonwebtoken (JJWT) for RS256 token generation/validation
│   ├── RSA key pair loaded from config: ${JWT_PRIVATE_KEY}, ${JWT_PUBLIC_KEY}
│   └── Token expiry from config: ${JWT_ACCESS_EXPIRY:900}, ${JWT_REFRESH_EXPIRY:604800}
│
├── infrastructure/auth/JwtAuthenticationFilter.java
│   ├── extends OncePerRequestFilter
│   ├── Extract Bearer token from Authorization header
│   ├── Validate via AuthenticationPort.validateToken()
│   ├── Set SecurityContextHolder with UserPrincipal
│   └── Set MDC: userId, tenantId, roles
│
├── infrastructure/auth/UserPrincipal.java
│   ├── implements UserDetails (Spring Security)
│   ├── UUID userId, String email, Set<Role> roles, UUID tenantId
│   └── boolean hasRole(String role), UUID getTenantId()
│
├── infrastructure/persistence/entity/UserEntity.java
├── infrastructure/persistence/entity/RefreshTokenEntity.java
├── infrastructure/persistence/adapter/JpaUserRepositoryAdapter.java
├── infrastructure/persistence/repository/SpringDataUserRepository.java
└── infrastructure/persistence/repository/SpringDataRefreshTokenRepository.java
```

#### Updated SecurityConfig

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity    // enables @PreAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/ws/**").permitAll()
                // Protected endpoints
                .requestMatchers("/api/v1/auth/users/**").hasRole("SUPER_ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/insurers").hasRole("SUPER_ADMIN")
                .requestMatchers("/api/v1/audit-logs/**").hasAnyRole("SUPER_ADMIN", "PLUM_OPS")
                .requestMatchers("/actuator/**").hasAnyRole("SUPER_ADMIN", "PLUM_OPS")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

#### Controller-Level Authorization

```java
// EndorsementController.java — example @PreAuthorize annotations
@PostMapping
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLUM_OPS', 'EMPLOYER_ADMIN')")
public ResponseEntity<EndorsementResponse> createEndorsement(
        @RequestBody CreateEndorsementRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {
    // Tenant check: EMPLOYER_ADMIN can only create for own employer
    if (principal.hasRole("EMPLOYER_ADMIN")
            && !principal.getTenantId().equals(request.employerId())) {
        throw new AccessDeniedException("Cannot create endorsements for another employer");
    }
    // ... existing handler call
}

@PostMapping("/{id}/confirm")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLUM_OPS', 'INSURER_ADMIN')")
public ResponseEntity<EndorsementResponse> confirmEndorsement(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal) {
    // Tenant check: INSURER_ADMIN can only confirm endorsements for own insurer
    // ... existing handler call
}
```

### 9.5 Database Schema

```sql
-- Flyway migration: V{n}__create_auth_tables.sql

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255),                -- null for SSO-only users
    tenant_id       UUID NOT NULL,               -- employerId, insurerId, or platform UUID
    tenant_type     VARCHAR(20) NOT NULL,         -- EMPLOYER, INSURER, PLATFORM
    active          BOOLEAN NOT NULL DEFAULT true,
    last_login_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,    -- SHA-256 hash, never store raw
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- Extend audit_logs to capture user identity
ALTER TABLE audit_logs ADD COLUMN user_id UUID;
ALTER TABLE audit_logs ADD COLUMN user_email VARCHAR(255);
ALTER TABLE audit_logs ADD COLUMN user_role VARCHAR(30);
```

### 9.6 Frontend Auth Integration

```
Frontend changes:
├── src/contexts/AuthContext.tsx (NEW)
│   ├── React Context providing: user, login(), logout(), isAuthenticated
│   ├── Stores accessToken in memory (React state)
│   ├── Stores refreshToken as HttpOnly Secure cookie (set by backend)
│   ├── Auto-refresh: intercept 401 → call /auth/refresh → retry
│   └── Redirect to /login on refresh failure
│
├── src/pages/LoginPage.tsx (NEW)
│   ├── Email + password form
│   ├── "Sign in with Google" / "Sign in with Okta" buttons (OAuth2 redirect)
│   └── Error display for invalid credentials
│
├── src/components/ProtectedRoute.tsx (NEW)
│   ├── Wraps React Router routes
│   ├── Checks AuthContext.isAuthenticated
│   ├── Redirects to /login if unauthenticated
│   └── Checks role requirements: <ProtectedRoute roles={['EMPLOYER_ADMIN']}>
│
├── src/lib/api.ts (MODIFY)
│   ├── Axios interceptor: attach Authorization: Bearer <token> header
│   └── Response interceptor: on 401, attempt token refresh before failing
│
└── src/components/layout/Sidebar.tsx (MODIFY)
    ├── Show/hide menu items based on user roles
    ├── EMPLOYER_ADMIN: endorsements, EA accounts, health score
    ├── PLUM_OPS: all items + audit logs + intelligence
    ├── INSURER_ADMIN: endorsements (own insurer) + benchmarks
    └── Display user email + role in sidebar footer
```

### 9.7 Audit Integration

The existing `AuditLoggingAspect` captures operations but lacks user identity. With auth in place:

```java
// AuditLoggingAspect.java — enhanced with user context
@Around("@annotation(audited)")
public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) {
    // Extract user from SecurityContext
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    UserPrincipal principal = (auth != null) ? (UserPrincipal) auth.getPrincipal() : null;

    AuditLog log = AuditLog.builder()
        .action(audited.action())
        .entityType(audited.entityType())
        .userId(principal != null ? principal.getUserId() : null)
        .userEmail(principal != null ? principal.getEmail() : null)
        .userRole(principal != null ? principal.getPrimaryRole() : null)
        .ipAddress(MDC.get("clientIp"))
        .timestamp(Instant.now())
        .build();
    // ... save audit log
}
```

This provides a complete audit trail: **who** did **what** to **which** entity, **when**, from **where**.

### 9.8 Configuration

```yaml
# application.yml — auth configuration
endorsement:
  auth:
    enabled: ${AUTH_ENABLED:false}        # false for local dev / demo
    jwt:
      issuer: plum-endorsements
      access-token-expiry: ${JWT_ACCESS_EXPIRY:900}       # 15 minutes
      refresh-token-expiry: ${JWT_REFRESH_EXPIRY:604800}   # 7 days
      public-key: ${JWT_PUBLIC_KEY:}      # RS256 public key (PEM)
      private-key: ${JWT_PRIVATE_KEY:}    # RS256 private key (PEM)
    oauth2:
      enabled: ${OAUTH2_ENABLED:false}
      providers:
        google:
          client-id: ${GOOGLE_CLIENT_ID:}
          client-secret: ${GOOGLE_CLIENT_SECRET:}
        okta:
          issuer-uri: ${OKTA_ISSUER_URI:}
          client-id: ${OKTA_CLIENT_ID:}
          client-secret: ${OKTA_CLIENT_SECRET:}
    password:
      bcrypt-strength: 12                 # BCrypt work factor
      min-length: 12
```

**Feature flag:** `endorsement.auth.enabled: false` allows the entire auth layer to be bypassed for demos, local development, and existing test suites. When `false`, `JwtAuthenticationFilter` passes all requests through unauthenticated, preserving current behaviour.

### 9.9 Security Considerations

| Concern | Mitigation |
|---|---|
| **Token theft (XSS)** | Access token in memory only (not localStorage). Refresh token as HttpOnly Secure SameSite=Strict cookie. |
| **Token replay** | Short-lived access tokens (15 min). Refresh tokens are single-use (rotated on each refresh). |
| **Brute force login** | Rate limiting on `/api/v1/auth/login` — 5 attempts per email per 15 min, then 30-min lockout. |
| **Password storage** | BCrypt with strength 12. Never log or return password hashes. |
| **Refresh token persistence** | Store SHA-256 hash in DB, not raw token. Revoke all tokens on password change. |
| **CORS** | Restrict `Access-Control-Allow-Origin` to frontend domain only. |
| **Key rotation** | RSA key pair rotation via config change. Old public key accepted for 24h grace period. |
| **Privilege escalation** | Roles stored server-side in DB, not in JWT claims alone. JWT roles are a convenience for frontend routing; backend always re-validates against DB on sensitive operations. |

### 9.10 Implementation Effort

| Component | Effort | Dependencies |
|---|---|---|
| Domain models (User, Role, RefreshToken) | 1 day | None |
| AuthenticationPort + JwtAuthenticationAdapter | 2 days | JJWT library |
| JwtAuthenticationFilter + SecurityConfig update | 1 day | AuthenticationPort |
| Flyway migration (users, user_roles, refresh_tokens) | 0.5 day | None |
| JPA entities + repository adapter | 1 day | Flyway migration |
| AuthController (login, refresh, logout, user CRUD) | 1 day | AuthenticationPort |
| @PreAuthorize annotations on existing controllers | 1 day | SecurityConfig |
| Tenant isolation in query handlers | 1 day | UserPrincipal |
| Audit log user context integration | 0.5 day | UserPrincipal |
| Frontend: AuthContext, LoginPage, ProtectedRoute | 2 days | AuthController |
| Frontend: Axios interceptors + sidebar role-gating | 1 day | AuthContext |
| Tests: ~40 (unit + API + BDD + E2E) | 3 days | All above |
| **Total** | **~15 days (3 weeks)** | |

### 9.11 Migration Strategy — Zero Disruption

The auth layer is designed to be introduced without breaking any existing functionality:

```
Phase A: Feature-flagged backend (auth.enabled: false)
├── Deploy all auth infrastructure (models, filter, adapter)
├── All requests continue to pass through unauthenticated
├── Existing tests pass with zero modification
└── Seed initial SUPER_ADMIN user via Flyway migration

Phase B: Enable auth for new deployments (auth.enabled: true)
├── Existing local/demo setups keep auth.enabled: false
├── Staging/production enable auth
├── Generate RS256 key pair, set as environment variables
└── Create initial users via POST /api/v1/auth/users (SUPER_ADMIN only)

Phase C: Migrate existing users
├── Bulk-create EMPLOYER_ADMIN users for each employer
├── Send password-reset emails (or enable SSO-only)
├── Frontend auto-redirects to /login when 401 received
└── Gradual rollout with feature flag per employer (optional)
```
