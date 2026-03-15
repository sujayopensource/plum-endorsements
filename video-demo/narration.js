/**
 * Narration segments for the Plum Endorsement Service video demo.
 *
 * Each segment has:
 *   id         – unique key (maps to audio file name)
 *   text       – narration text spoken by macOS `say`
 *   section    – which demo section it belongs to
 *   pauseAfter – milliseconds of silence to insert after this segment
 *
 * Total target duration: ~25 minutes (expanded with C4, Ollama detail, vision).
 */

const SEGMENTS = [

  // ── SECTION 0: INTRO ───────────────────────────────────────────────────
  {
    id: "intro_title",
    section: "intro",
    text: "Welcome to the Plum Endorsement Management System demo. Over the next 25 minutes, I'll walk you through the architecture using C4 and hexagonal models, demonstrate a live endorsement lifecycle end to end, show the intelligence and AI features powered by Ollama, and close with a forward-looking vision. Let's begin.",
    pauseAfter: 2000,
  },

  // ── SECTION 1: PROBLEM STATEMENT ────────────────────────────────────────
  {
    id: "s1_title",
    section: "s1_problem",
    text: "Section 1. Problem Statement and Solution Overview.",
    pauseAfter: 2000,
  },
  {
    id: "s1_problem",
    section: "s1_problem",
    text: "When an employer adds a new employee mid-year, they need an endorsement, a change to the group insurance policy. At scale, this creates four hard problems.",
    pauseAfter: 1500,
  },
  {
    id: "s1_p1",
    section: "s1_problem",
    text: "Problem one: Coverage Gap. The employee joins, the endorsement is created, but the insurer takes days to confirm. During that window, the employee has no coverage.",
    pauseAfter: 1000,
  },
  {
    id: "s1_p2",
    section: "s1_problem",
    text: "Problem two: Financial Drain. Each ADD endorsement locks premium in an EA account. Without optimization, employers maintain a massive float.",
    pauseAfter: 1000,
  },
  {
    id: "s1_p3",
    section: "s1_problem",
    text: "Problem three: Multi-Insurer Chaos. Four or more insurers, each with different APIs: REST, SOAP, CSV over SFTP. Different SLAs, different batch constraints.",
    pauseAfter: 1000,
  },
  {
    id: "s1_p4",
    section: "s1_problem",
    text: "Problem four: Invisible Failures. Submissions fail silently. Nobody knows until month-end reconciliation.",
    pauseAfter: 1500,
  },
  {
    id: "s1_solution",
    section: "s1_problem",
    text: "Our system has 27 REST endpoints, 4 insurer integrations, 13 database tables, 4 Kafka topics, 5 AI modules with 2 using Ollama GenAI, 7 Grafana dashboards, 40 plus metrics, and over 800 tests. Let me start with the architecture.",
    pauseAfter: 2000,
  },

  // ── SECTION 2: ARCHITECTURE — C4 FIRST, THEN HEXAGONAL ─────────────────
  {
    id: "s2_title",
    section: "s2_architecture",
    text: "Section 2. Architecture — C4 Model and Hexagonal Design.",
    pauseAfter: 2000,
  },
  {
    id: "s2_c4_context",
    section: "s2_architecture",
    text: "Let me start with the C4 model, which provides a zoom-in view of the architecture. At the Context level, we have three actors. HR administrators and finance teams interact with the Plum Endorsement System through a React dashboard. The system integrates with 4 insurance providers: ICICI Lombard via REST, Niva Bupa via CSV SFTP, Bajaj Allianz via SOAP, and a Mock adapter for testing. Downstream, the system publishes domain events consumed by notification services and analytics pipelines.",
    pauseAfter: 2000,
  },
  {
    id: "s2_c4_container",
    section: "s2_architecture",
    text: "Zooming into the Container level. The frontend is a React 19 single-page application with TanStack Table and shadcn UI. It talks to a Spring Boot 3.4 backend running on Java 21 with virtual threads. The backend connects to PostgreSQL 16 for ACID financial operations, Redis 7 for distributed caching, and Kafka in KRaft mode for event streaming across 4 topics with 88 partitions. The observability stack includes Prometheus, Grafana with 7 dashboards, Jaeger for distributed tracing, and the ELK stack for log aggregation. All 9 services run in Docker Compose with health checks.",
    pauseAfter: 2000,
  },
  {
    id: "s2_c4_component",
    section: "s2_architecture",
    text: "At the Component level, the backend follows Hexagonal Architecture, also called Ports and Adapters. This is where C4 meets clean architecture.",
    pauseAfter: 1500,
  },
  {
    id: "s2_hex",
    section: "s2_architecture",
    text: "The API layer has 5 controllers with 27 endpoints and RFC 7807 error handling. The application layer has 3 CQRS handlers, 5 services, and 8 schedulers — all stateless. The domain core contains Endorsement with an 11-state lifecycle, EA Account, 18 port interfaces, a sealed event interface with 24 event types, and domain services. The critical rule: zero infrastructure imports in the domain.",
    pauseAfter: 2000,
  },
  {
    id: "s2_infra",
    section: "s2_architecture",
    text: "The infrastructure layer implements all ports. 10 JPA repository adapters with anti-corruption mappers. 4 insurer adapters. Kafka event publishing. Per-insurer circuit breakers. And the intelligence layer: 5 rule-based adapters plus 2 Ollama GenAI adapters.",
    pauseAfter: 2000,
  },
  {
    id: "s2_patterns",
    section: "s2_architecture",
    text: "Design patterns throughout. Strategy for insurer adapters: add a new insurer with one class and one database row. State pattern for the 11-state lifecycle with compile-time transition safety. Observer for domain events via Kafka. Factory for the insurer router. Adapter for all infrastructure translations. And CQRS separating commands from queries.",
    pauseAfter: 2000,
  },
  {
    id: "s2_tech",
    section: "s2_architecture",
    text: "Tech choices. Java 21 Virtual Threads. Spring Boot 3.4. PostgreSQL 16 with optimistic locking. Kafka KRaft with employer ID partitioning. Resilience4j circuit breakers. Flyway migrations. ZGC garbage collector.",
    pauseAfter: 2000,
  },

  // ── SECTION 3: ENDORSEMENT LIFECYCLE — FULL LOOP ───────────────────────
  {
    id: "s3_title",
    section: "s3_lifecycle",
    text: "Section 3. Live Demo: Complete Endorsement Lifecycle.",
    pauseAfter: 2000,
  },
  {
    id: "s3_dashboard",
    section: "s3_lifecycle",
    text: "Here's the dashboard. KPI cards show total endorsements, pending, confirmed, and failed counts. The EA Account card shows balance, reserved, and available funds.",
    pauseAfter: 3000,
  },
  {
    id: "s3_nav_create",
    section: "s3_lifecycle",
    text: "Let me create an endorsement. I'll navigate to the create endorsement page.",
    pauseAfter: 2000,
  },
  {
    id: "s3_create_form",
    section: "s3_lifecycle",
    text: "The form adapts based on endorsement type. ADD shows premium and employee fields. DELETE hides them. That's progressive disclosure.",
    pauseAfter: 2000,
  },
  {
    id: "s3_fill_form",
    section: "s3_lifecycle",
    text: "I'll fill in the form. Employee name: Priya Sharma. Coverage start date: tomorrow. Premium: 1500 rupees. Date of birth. The employer and insurer IDs are pre-filled.",
    pauseAfter: 4000,
  },
  {
    id: "s3_submit_create",
    section: "s3_lifecycle",
    text: "Clicking Create Endorsement. Three things happen atomically in one database transaction.",
    pauseAfter: 3000,
  },
  {
    id: "s3_detail",
    section: "s3_lifecycle",
    text: "Look at the status timeline. Created: record saved with idempotency key check. Validated: business rules verified. Provisionally Covered: the key innovation. The employee has coverage right now, not when the insurer confirms days later.",
    pauseAfter: 3000,
  },
  {
    id: "s3_submit_insurer",
    section: "s3_lifecycle",
    text: "Now the critical step. I'll click Submit to Insurer to complete the loop. This uses the Mock adapter with JSON REST. In production, the Insurer Router resolves to the correct adapter based on database configuration. The handler never knows which protocol it's using. Strategy pattern.",
    pauseAfter: 3000,
  },
  {
    id: "s3_confirmed",
    section: "s3_lifecycle",
    text: "And there it is. Status is now Confirmed. The insurer reference number is populated. Coverage upgraded from provisional to confirmed. That's the complete end-to-end real-time path: create, validate, provision coverage, submit to insurer, confirmed.",
    pauseAfter: 2000,
  },
  {
    id: "s3_batch",
    section: "s3_lifecycle",
    text: "For batch-only insurers like Niva Bupa, the flow continues: Queued for Batch, Batch Submitted every 15 minutes, then Confirmed. Coverage is active the entire time.",
    pauseAfter: 2000,
  },

  // ── SECTION 4: EA BALANCE — WITH LIVE LOOKUP ───────────────────────────
  {
    id: "s4_title",
    section: "s4_ea_balance",
    text: "Section 4. EA Balance Optimization.",
    pauseAfter: 2000,
  },
  {
    id: "s4_problem",
    section: "s4_ea_balance",
    text: "Without optimization, 60 ADDs at 1000 rupees requires 60,000 in the EA account. With 40 DELETEs generating 32,000 in credits, the optimized approach processes DELETEs first. Net requirement: 28,000 rupees. 53 percent savings.",
    pauseAfter: 2000,
  },
  {
    id: "s4_scoring",
    section: "s4_ea_balance",
    text: "The batch optimizer uses priority ordering: DELETEs first to free balance, then cost-neutral updates, then ADDs sorted by urgency. A composite score weighs urgency at 60 percent and EA impact at 40 percent. Implemented as a zero-one knapsack with dynamic programming.",
    pauseAfter: 2000,
  },
  {
    id: "s4_ea_lookup",
    section: "s4_ea_balance",
    text: "Let me demonstrate the EA account lookup. I'll enter the employer and insurer IDs and click Look Up. You can see the three KPI cards: total balance, reserved amount for pending endorsements, and available balance. The progress bar shows utilization visually.",
    pauseAfter: 3000,
  },

  // ── SECTION 5: REAL-TIME VISIBILITY — INTERACTIVE ──────────────────────
  {
    id: "s5_title",
    section: "s5_visibility",
    text: "Section 5. Real-Time Visibility Screens.",
    pauseAfter: 2000,
  },
  {
    id: "s5_list",
    section: "s5_visibility",
    text: "The endorsement list page. Sortable columns with TanStack Table. I'll sort by premium amount. Status filters give instant results. Notice the URL updates with query parameters. This is bookmarkable and shareable. CSV export downloads filtered data instantly.",
    pauseAfter: 4000,
  },
  {
    id: "s5_batches",
    section: "s5_visibility",
    text: "Batch Progress page showing batches with their status: Submitted, Completed, or Failed. Each batch shows the endorsement count and insurer reference.",
    pauseAfter: 3000,
  },
  {
    id: "s5_insurers",
    section: "s5_visibility",
    text: "Insurers page. All 4 configured insurers showing capabilities: real-time badge, batch badge, rate limits. Let me click into one to see the full configuration and reconciliation history.",
    pauseAfter: 3000,
  },
  {
    id: "s5_reconciliation",
    section: "s5_visibility",
    text: "Reconciliation page. Summary cards show Matched, Partial, Rejected, and Missing counts. You can drill into individual items and trigger manual reconciliation.",
    pauseAfter: 2000,
  },

  // ── SECTION 6: AI, OLLAMA & INTELLIGENCE — EXPANDED ────────────────────
  {
    id: "s6_title",
    section: "s6_intelligence",
    text: "Section 6. Intelligence, AI, and Ollama Integration.",
    pauseAfter: 2000,
  },
  {
    id: "s6_intro",
    section: "s6_intelligence",
    text: "This is one of the most important sections. We have 5 intelligence pillars, each behind a domain port interface. The architecture supports a 3-stage evolution: stage 1 is rule-based, which is the current default. Stage 2 is Ollama GenAI augmentation, already deployed on 2 pillars. Stage 3 is full ML with models like Isolation Forest, Prophet, and RAG pipelines. Each stage is just an adapter swap. The domain never changes.",
    pauseAfter: 2000,
  },
  {
    id: "s6_ollama_how",
    section: "s6_intelligence",
    text: "Let me explain how we use Ollama. Ollama runs as a Docker container with the llama 3.2 model. We use the Spring AI Ollama starter to integrate. The key architectural pattern: a Conditional On Property annotation activates the Ollama adapter when the ollama Spring profile is set. The OllamaAugmentedAnomalyDetector and OllamaErrorResolver are the two deployed adapters. Each wraps the rule-based logic and enriches it with LLM analysis. Both have Resilience4j circuit breakers and retry policies. If the LLM is slow or unavailable, they fall back gracefully to rule-based results. Temperature is set to 0.3 for deterministic output, with 512 token max.",
    pauseAfter: 2000,
  },
  {
    id: "s6_anomaly",
    section: "s6_intelligence",
    text: "Anomaly Detection. Five rules run every 5 minutes: Volume Spike, ADD DELETE Cycling, Suspicious Timing, Unusual Premium, and Dormancy Break. Each produces a score between 0 and 1.",
    pauseAfter: 2000,
  },
  {
    id: "s6_anomaly_ollama",
    section: "s6_intelligence",
    text: "With Ollama active, the detector runs the same 5 rules, then sends flagged anomalies with a score above 0.7 to the local LLM. The LLM generates a human-readable explanation of why this is anomalous and recommends an action. This is the GenAI narrative layer. It doesn't change the decision, it enriches it. The reviewer sees: Score 0.85, ADD DELETE Cycling, and then a paragraph explaining that employee X had 3 ADD followed by DELETE cycles in 25 days, suggesting possible premium arbitrage.",
    pauseAfter: 2000,
  },
  {
    id: "s6_forecast",
    section: "s6_intelligence",
    text: "Forecasts. Let me generate one. I'll enter the employer and insurer IDs and click Generate Forecast. It projects EA balance 30 days ahead using dual seasonality: day of week and monthly factors. Monday gets 1.2x, April gets 1.4x for fiscal year start.",
    pauseAfter: 3000,
  },
  {
    id: "s6_errors",
    section: "s6_intelligence",
    text: "Error Resolution. Five error patterns with confidence scores. Above 95 percent: auto-applied. Below that: suggested for human approval. Success and failure tracking enables a feedback loop.",
    pauseAfter: 2000,
  },
  {
    id: "s6_errors_ollama",
    section: "s6_intelligence",
    text: "The Ollama Error Resolver handles the ambiguous cases. For patterns below 95 percent confidence, the LLM analyzes the insurer rejection message and endorsement context to suggest a specific fix with reasoning. For example, a date format rejection gets: The insurer expects ISO 8601 format, the submitted date was DD slash MM slash YYYY, suggested fix is to reformat to YYYY dash MM dash DD. High-confidence fixes still auto-apply without touching the LLM.",
    pauseAfter: 2000,
  },
  {
    id: "s6_process",
    section: "s6_intelligence",
    text: "Process Mining. The STP rate shows what percentage of endorsements complete without human intervention. Per-insurer STP cards focus optimization. The trend chart shows 30-day trajectory. Transition metrics measure every state change.",
    pauseAfter: 3000,
  },
  {
    id: "s6_3stage",
    section: "s6_intelligence",
    text: "To summarize the 3-stage AI evolution. Stage 1 rule-based: 5 adapters deployed, production-ready, zero ML dependencies. Stage 2 Ollama GenAI: 2 adapters deployed for anomaly enrichment and error resolution, 3 more planned. Uses local LLM, no cloud API costs. Stage 3 full ML: Isolation Forest for anomaly detection, Prophet for time-series forecasting, RAG with vector databases for error resolution. Each stage is an adapter swap behind the same port interface. This is the hexagonal architecture payoff.",
    pauseAfter: 2000,
  },

  // ── SECTION 7: OBSERVABILITY ────────────────────────────────────────────
  {
    id: "s7_title",
    section: "s7_observability",
    text: "Section 7. Observability Stack.",
    pauseAfter: 2000,
  },
  {
    id: "s7_services",
    section: "s7_observability",
    text: "9 Docker Compose services with health checks. PostgreSQL, Redis, Kafka KRaft, Prometheus, Grafana, Jaeger, Elasticsearch, Logstash, Kibana.",
    pauseAfter: 2000,
  },
  {
    id: "s7_grafana",
    section: "s7_observability",
    text: "Grafana. Let me log in and show the dashboards. Application Overview: request rate, P95 latency, error rate, JVM heap, threads, DB pool.",
    pauseAfter: 3000,
  },
  {
    id: "s7_grafana2",
    section: "s7_observability",
    text: "Business Metrics dashboard. Creation rate by type, endorsements by status, insurer submission latency. Notice Bajaj at 250 milliseconds versus ICICI at 150.",
    pauseAfter: 3000,
  },
  {
    id: "s7_grafana3",
    section: "s7_observability",
    text: "Intelligence Monitoring. Anomaly detection rate, forecast shortfall detections, error auto-resolution gauge, STP trend, batch optimization savings.",
    pauseAfter: 3000,
  },
  {
    id: "s7_tracing",
    section: "s7_observability",
    text: "Distributed tracing with Jaeger. 100 percent sampling. Trace ID and span ID propagated through MDC. Search by endorsement ID to see the full request journey.",
    pauseAfter: 2000,
  },

  // ── SECTION 8: SCALABILITY ──────────────────────────────────────────────
  {
    id: "s8_title",
    section: "s8_scalability",
    text: "Section 8. Scalability and Resilience.",
    pauseAfter: 2000,
  },
  {
    id: "s8_scale",
    section: "s8_scalability",
    text: "Target: 1 million endorsements per day. Stateless services scale horizontally. Java 21 Virtual Threads. Kafka 32-partition per-employer ordering. Optimistic locking. Idempotency keys. Kubernetes HPA from 2 to 8 pods.",
    pauseAfter: 2000,
  },
  {
    id: "s8_resilience",
    section: "s8_scalability",
    text: "Per-insurer circuit breakers tuned to reliability profiles. ICICI: 50 percent threshold, 20-call window. Bajaj: 40 percent threshold, 45-second wait, because SOAP is more fragile. Every breaker has a typed fallback method.",
    pauseAfter: 2000,
  },

  // ── SECTION 9: TESTS ───────────────────────────────────────────────────
  {
    id: "s9_title",
    section: "s9_tests",
    text: "Section 9. Test Coverage.",
    pauseAfter: 2000,
  },
  {
    id: "s9_stats",
    section: "s9_tests",
    text: "381 unit tests. 105 API integration tests with Testcontainers. 75 BDD scenarios across 16 Cucumber feature files. 138 Playwright end-to-end tests. 6 Gatling performance simulations. Over 800 total. Zero failures.",
    pauseAfter: 2000,
  },
  {
    id: "s9_allure",
    section: "s9_tests",
    text: "Here's the Allure combined report. Segregated into API, BDD, E2E, and Performance sections. You can drill into any test to see assertions, payloads, and timeline.",
    pauseAfter: 2000,
  },

  // ── SECTION 10: VISION — 3 DOCUMENTS ────────────────────────────────────
  {
    id: "s10_title",
    section: "s10_vision",
    text: "Section 10. Forward-Looking Vision.",
    pauseAfter: 2000,
  },
  {
    id: "s10_ai_vision",
    section: "s10_vision",
    text: "AI Automation Vision. The current system runs rule-based intelligence with Ollama GenAI augmentation as a bridge to full ML. The vision document outlines 15 capabilities across the 5 pillars. For anomaly detection: Isolation Forest unsupervised learning, then Autoencoder for second-stage classification, then graph-based fraud network detection. For forecasting: Facebook Prophet for multi-seasonality, then LSTM neural networks, then ensemble methods combining multiple models. For error resolution: a RAG pipeline using Retrieval-Augmented Generation with a vector database of past resolutions. For process mining: PM4Py conformance checking via a Python sidecar. For batch optimization: Google OR-Tools linear programming replacing the current knapsack heuristic.",
    pauseAfter: 2000,
  },
  {
    id: "s10_ai_vision2",
    section: "s10_vision",
    text: "The rollout follows a proven 4-phase pattern. Phase 1: implement the new ML adapter behind the existing port. Phase 2: deploy with a feature flag using Conditional On Property. Phase 3: run in shadow mode alongside the current adapter. Phase 4: promote when metrics meet targets. This pattern is already proven with the 2 deployed Ollama adapters. Cross-cutting capabilities include a Feature Store, MLOps pipeline, A/B testing framework, and a natural language query interface.",
    pauseAfter: 2000,
  },
  {
    id: "s10_product_vision",
    section: "s10_vision",
    text: "Product Evolution Vision. Phase 4 transforms the system from a single-region India-focused platform into a global, multi-currency system. Key capabilities: advanced analytics completing the intelligence layer to 100 percent, multi-currency EA support for international employers, localized insurer integrations for new markets, country-specific regulatory compliance, a self-service insurer onboarding portal, a platform API marketplace, multi-region deployment for low latency globally, and mobile applications. The hexagonal architecture makes expansion structurally cheap: every new capability is a new port, new adapter, new controller, new migration.",
    pauseAfter: 2000,
  },
  {
    id: "s10_product_vision2",
    section: "s10_vision",
    text: "The product vision also covers authentication and authorization via JWT plus OAuth2 with role-based access control, all designed as a port plus adapter pair following the hexagonal pattern. The key finding: Phase 3 foundations already support 70 percent of advanced analytics. The remaining 30 percent and all new capabilities are purely additive — zero test breakage, zero existing code modification.",
    pauseAfter: 2000,
  },
  {
    id: "s10_arch_vision",
    section: "s10_vision",
    text: "Architectural Vision. This document defines a 5-year evolution roadmap grounded in two theoretical frameworks: Building Evolutionary Architectures by Neal Ford and Continuous Architecture in Practice by Erder and Pureur. Year 1, 2026: modular monolith hardening with fitness functions and distributed locking. Year 2, 2027: service extraction via the Strangler Fig pattern and multi-region deployment with CQRS read replicas. Year 3, 2028: platform maturity with an API marketplace and self-service insurer onboarding. Year 4, 2029: intelligence platform with data mesh and ML pipeline. Year 5, 2030: global scale with active-active multi-region and autonomous operations.",
    pauseAfter: 2000,
  },
  {
    id: "s10_arch_vision2",
    section: "s10_vision",
    text: "The architectural thesis: evolve from a well-structured modular monolith into a selectively-decomposed service-oriented platform, guided by automated fitness functions. Decomposition follows the last responsible moment principle: extract services only when scaling, team, or deployment independence demands it. The change patterns include Strangler Fig, Branch by Abstraction, Parallel Runs, Feature Toggles, and Expand-Contract Migrations. Governance via Architecture Decision Records and automated fitness function checks in CI.",
    pauseAfter: 2000,
  },

  // ── CLOSING ─────────────────────────────────────────────────────────────
  {
    id: "closing_title",
    section: "closing",
    text: "Closing. Mapping to Deliverables.",
    pauseAfter: 2000,
  },
  {
    id: "closing_map",
    section: "closing",
    text: "Deliverable 1: hexagonal architecture with C4 model views. Deliverable 2: provisional coverage at creation with 30-day safety net. Deliverable 3: EA balance optimization with 53 percent savings. Deliverable 4: 10 real-time visibility screens. Deliverable 5: 5 intelligence pillars, 2 Ollama GenAI adapters deployed, 3 planned, full ML vision documented. Deliverable 6: working prototype with over 800 tests and comprehensive documentation.",
    pauseAfter: 2000,
  },
  {
    id: "closing_final",
    section: "closing",
    text: "Production-grade code. Hexagonal architecture. Cloud-native patterns. Ollama GenAI augmentation deployed. Over 800 tests. And a clear 5-year architectural evolution vision. Thank you for watching.",
    pauseAfter: 3000,
  },
];

module.exports = { SEGMENTS };
