# Plum Interview Prep -- Session Context

## Interview Details

- **Company:** Plum (Insurtech, Bengaluru, India)
- **Interviewer:** Sankaralingam T. (Sankar) -- SVP & Head of Engineering
- **Focus:** System design for scaling insurance platform from 500K to 10M lives

---

## Interviewer Profile: Sankaralingam T.

- **LinkedIn:** https://www.linkedin.com/in/sankaralingamt/
- **Role:** SVP & Head of Engineering at Plum
- **Background:** 25+ years of engineering leadership at Amazon, Cisco, Intel, and Jupiter (fintech)
- **Education:** PSG College of Technology (1995-1999)
- **Location:** Bengaluru, India

### His Mandate at Plum

- Scale platform toward **10M users by 2030** (currently ~500K-600K lives, 6,000+ companies)
- **95% API-led claims automation** -- reduced settlement from 30+ days to <3 days
- **AI-powered claims routing** -- 40% automated today, targeting 95%
- Telehealth: one consultation every 3 minutes
- 70% AI-led query resolution
- India's fastest OPD claims: filed in under 1 minute, same-day payouts

### His Direct Quote

> "India's insurance and healthcare sectors are experiencing a profound transformation... our focus now is on scaling and evolving the platform to offer comprehensive healthcare products to more than 10+ million users."

### What His Background Means for the Interview

- **Amazon/Cisco experience** = values operational excellence, back-pressure handling, dead letter queues, chaos engineering
- **Data-driven** = expects specific numbers (SLAs, cache hit rates, latency budgets)
- **Incremental migration** = strangler fig pattern, dual-write reconciliation (not big-bang rewrites)
- **Cost awareness** = S3 tiered storage, compute right-sizing, autoscaling policies
- Builder who's scaled systems at Amazon-level -- go deep on tradeoffs, not surface-level buzzwords

---

## About Plum

- **Founded:** 2019 by Abhishek Poddar, Saurabh Arora (CTO), Harshwardhan Singh
- **Funding:** Series A, backed by Tiger Global; invested Rs 200 Cr in Healthcare
- **Revenue:** Rs. 60.6 Cr (FY ending March 2025), 83% CAGR
- **Employees:** 449 (31% YoY growth)
- **Insurance Partners:** ICICI Lombard, Niva Bupa, Bajaj Allianz, Digit
- **Three verticals:** Employee benefits, healthcare services, personal insurance
- **Platform capabilities:** Group health insurance, personal accident, term life, digital claims processing, teleconsultations, mental health support, health checkups, wellness programs, mobile access, HR dashboards, insurer integrations

---

## System Design Task

**Prompt:** Scale from 500K lives to 10M lives (20x growth). What breaks?

### Five Subsystems Designed

1. **Claims Processing Pipeline**
   - Event-sourced via Kafka, CQRS read models
   - Auto-adjudication router with complexity scoring (targeting 70-80% STP)
   - Back-pressure-aware consumers with dead letter queues
   - Sharded PostgreSQL (Citus, 8 shards at scale)
   - Partitioning: `hash(tenant_id + line_of_business)`

2. **Real-Time Pricing Engine**
   - 3-tier caching: L1 (Caffeine, 60s TTL) → L2 (Redis, 15min) → L3 (compute)
   - Rate tables sharded by `(state, product_line, effective_date)`
   - Independently cacheable computation steps (partial cache hits)
   - Zero-downtime rate filing updates via version-stamping with dual-version window
   - Latency budget: <200ms P99

3. **Telehealth Platform**
   - Multi-region SFU architecture (not MCU) -- O(N) vs O(N^2) CPU
   - Admission control to prevent overload-induced quality degradation
   - Externalized session state in Redis for crash recovery (<5s MTTR)
   - HIPAA-compliant: SRTP, DTLS, AES-256 at rest, audit logging

4. **Notification System**
   - Priority lanes: P0 (<30s SLA), P1 (<5min), P2 (<4hr)
   - Redis-based deduplication (24h window, content hash)
   - Pre-rendered templates for bulk volume (60%+ pre-rendered)
   - Channel failover: email → SMS → push

5. **Document Processing**
   - ML-based classification into fast/standard/complex paths
   - Fan-out splitting for large PDFs (50+ pages → 10-page chunks, parallel OCR)
   - Confidence-based extraction validation (>0.95 auto-accept, 0.70-0.95 human review)
   - Tiered S3 storage: hot (0-90d) → warm (90d-2yr) → cold (2-7yr) → archive (7yr+)

### Cross-Cutting Concerns

- **Data Platform:** CDC via Debezium → Kafka Connect → S3/Iceberg data lake → Spark/Trino analytics
- **Observability:** Prometheus + Grafana (metrics), OpenTelemetry + Jaeger (tracing), ELK/Loki (logging)
- **Security:** OAuth 2.0 + OIDC, mTLS service-to-service, PII tokenization, field-level encryption, 90-day key rotation
- **Multi-Tenancy:** Pooled compute (K8s with namespace isolation), siloed data (schema-per-tenant or tenant-ID partitioning with RLS)

### Migration Strategy (Strangler Fig, 5 Phases)

- Phase 1 (Months 1-3): Foundation -- Kafka, event sourcing, observability, 10% traffic
- Phase 2 (Months 4-6): Core migration -- dual-write, Redis cluster, 50% traffic
- Phase 3 (Months 7-9): Full cutover -- 100% new pipeline, multi-region telehealth
- Phase 4 (Months 10-12): Scale testing -- load test at 2x, chaos engineering
- Phase 5 (Ongoing): Growth -- autoscaling tuning, quarterly capacity reviews

### Alignment with Interviewer Priorities

| His Priority | Design Section |
|---|---|
| 500K → 10M scale | Entire architecture |
| Claims automation (40% → 95% STP) | Claims pipeline: auto-adjudication router |
| API-led claims processing | Claims pipeline: event-sourced with CQRS |
| Telehealth at scale | Telehealth: multi-region SFU, admission control |
| AI/ML for routing & fraud | Claims: ML fraud scoring, Docs: ML classification |
| Enterprise-grade security | Cross-cutting: HIPAA, encryption, audit trails |

---

## Full Architecture Document

The complete system design document is at:
`/Users/sujayhegde/IntelliJ/PRODUCTS/Interviews/plum/Scalable_Insurance_Platform_Architecture.md`

---

## Sources

- [Plum Names Sankaralingam T. as Head of Engineering (HR Today)](https://hrtoday.in/plum-names-veteran-tech-leader-sankaralingam-t-as-head-of-engineering-to-drive-multi-product-healthcare-expansion/)
- [Plum Appoints Sankaralingam as SVP Engineering (AdGully)](https://www.adgully.com/post/4781/plum-names-veteran-tech-leader-sankaralingam-as-svp-engineering-to-drive-multi-product-healthcare-expansion)
- [Plum Appoints Sankaralingam as SVP Engineering (AdTechToday)](https://adtechtoday.com/plum-appoints-sankaralingam-as-senior-vice-president-of-engineering/)
- [Plum expands into personal insurance (YourStory)](https://yourstory.com/2025/02/plum-expands-into-personal-insurance-to-invest-usd-6m-two-years)
- [LinkedIn: Sankaralingam T](https://www.linkedin.com/in/sankaralingamt/)
