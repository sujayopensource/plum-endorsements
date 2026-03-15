/*
 * Plum Endorsement Management System — Structurizr Architecture Model
 *
 * Comprehensive C4 model covering:
 *   Level 1: System Context — actors, external systems
 *   Level 2: Container — deployable units and protocols
 *   Level 3: Component — hexagonal architecture internals (API, Application, Domain, Infrastructure)
 *   Dynamic views — endorsement creation, insurer submission, batch assembly, anomaly detection
 *   Deployment views — Docker Compose (dev) and Kubernetes (production)
 *
 * Run:  ./start.sh   (launches Structurizr Lite on http://localhost:8200)
 */

workspace "Plum Endorsement Management System" "Insurance endorsement lifecycle platform built with Hexagonal Architecture, Cloud-Native Patterns, and GenAI augmentation via Ollama." {

    !identifiers hierarchical
    !impliedRelationships true

    // ── Constants ────────────────────────────────────────────────────────────

    !const SPRING_BOOT "Spring Boot 3.4, Java 21"
    !const REACT "React 19, TypeScript, Vite"
    !const POSTGRESQL "PostgreSQL 16"
    !const KAFKA "Apache Kafka 3.7 (KRaft)"
    !const REDIS "Redis 7"
    !const OLLAMA "Ollama (llama3.2)"
    !const RESILIENCE4J "Resilience4j"
    !const FLYWAY "Flyway"

    // ── Model ────────────────────────────────────────────────────────────────

    model {

        // ── People ───────────────────────────────────────────────────────────

        hrAdmin = person "HR Administrator" "Creates and manages employee endorsements (ADD/DELETE/UPDATE) for group health insurance policies." "User"
        financeTeam = person "Finance Team" "Monitors EA (Endorsement Account) balances, reviews forecasts, and manages fund top-ups." "User"
        opsTeam = person "Operations Team" "Monitors system health, reviews anomalies, resolves errors, and manages insurer relationships." "User"

        // ── External Systems ─────────────────────────────────────────────────

        iciciLombard = softwareSystem "ICICI Lombard API" "Insurance provider — REST/JSON, real-time only." "External System,Insurer"
        nivaBupa = softwareSystem "Niva Bupa SFTP" "Insurance provider — CSV/SFTP, batch only." "External System,Insurer"
        bajajAllianz = softwareSystem "Bajaj Allianz API" "Insurance provider — SOAP/XML, real-time + batch." "External System,Insurer"
        ollamaService = softwareSystem "Ollama LLM" "Local large language model for GenAI anomaly enrichment and error resolution." "External System,AI"

        // ── Primary Software System ──────────────────────────────────────────

        plumEndorsement = softwareSystem "Plum Endorsement System" "Manages the complete endorsement lifecycle: creation, validation, provisional coverage, multi-insurer submission, reconciliation, and intelligence analytics. 27 REST endpoints, 11-state lifecycle, 5 AI pillars." {

            // ── Frontend ─────────────────────────────────────────────────────

            spa = container "React Dashboard" "10-screen SPA: dashboard, endorsement CRUD, EA lookup, intelligence hub, insurer config, reconciliation, audit log." "${REACT}" "WebBrowser"

            // ── Backend Service ──────────────────────────────────────────────

            backend = container "Endorsement Service" "Core backend: 6 controllers (27 endpoints), 3 CQRS handlers, 8 services, 9 schedulers. Hexagonal architecture with zero infrastructure imports in domain core." "${SPRING_BOOT}" "SpringBoot" {

                // ── API Layer (Driving Side) ─────────────────────────────

                group "API Layer" {
                    endorsementController = component "EndorsementController" "9 endpoints: create, get, list, submit, confirm, reject, coverage, batches, outstanding." "Spring MVC REST" "Controller"
                    eaAccountController = component "EAAccountController" "EA account balance lookup by employer+insurer." "Spring MVC REST" "Controller"
                    intelligenceController = component "IntelligenceController" "17 endpoints: anomalies, forecasts, error resolutions, process mining, health scores, benchmarks." "Spring MVC REST" "Controller"
                    insurerConfigController = component "InsurerConfigController" "5 endpoints: list, get, capabilities, create, update insurer configurations." "Spring MVC REST" "Controller"
                    reconciliationController = component "ReconciliationController" "3 endpoints: list runs, get items, trigger reconciliation." "Spring MVC REST" "Controller"
                    auditLogController = component "AuditLogController" "Query immutable audit trail entries with filters." "Spring MVC REST" "Controller"
                    globalExceptionHandler = component "GlobalExceptionHandler" "RFC 7807 ProblemDetail error responses. Maps 9 exception types to HTTP status codes." "Spring MVC" "CrossCutting"
                }

                // ── Application Layer ────────────────────────────────────

                group "Application Layer" {
                    createEndorsementHandler = component "CreateEndorsementHandler" "Command handler: validate, idempotency check, debit EA, grant provisional coverage, publish Created event. @Transactional." "Spring Service (CQRS Command)" "Handler"
                    processEndorsementHandler = component "ProcessEndorsementHandler" "Command handler: route to insurer (RT/batch), confirm, reject, retry, state transitions, publish lifecycle events." "Spring Service (CQRS Command)" "Handler"
                    endorsementQueryHandler = component "EndorsementQueryHandler" "Query handler: find by ID, list by employer, paginate, filter. @Transactional(readOnly=true)." "Spring Service (CQRS Query)" "Handler"

                    anomalyDetectionService = component "AnomalyDetectionService" "Orchestrates anomaly detection across employers. Delegates to AnomalyDetectionPort." "Spring Service" "AppService"
                    balanceForecastService = component "BalanceForecastService" "Projects EA balance 30 days ahead with dual seasonality." "Spring Service" "AppService"
                    errorResolutionService = component "ErrorResolutionService" "Suggests/auto-applies fixes for rejected endorsements. 95% confidence threshold." "Spring Service" "AppService"
                    processMiningService = component "ProcessMiningService" "Computes STP rates, bottleneck analysis, transition metrics." "Spring Service" "AppService"
                    employerHealthScoreService = component "EmployerHealthScoreService" "Composite health score from endorsement patterns, EA utilization, error rates." "Spring Service" "AppService"
                    insurerBenchmarkService = component "InsurerBenchmarkService" "Compares insurer performance: latency, success rates, SLA compliance." "Spring Service" "AppService"
                    auditLogService = component "AuditLogService" "Records and queries immutable audit trail entries." "Spring Service" "AppService"
                    reconciliationEngine = component "ReconciliationEngine" "Matches system records against insurer records, flags discrepancies." "Spring Service" "AppService"

                    batchAssemblyScheduler = component "BatchAssemblyScheduler" "Assembles queued endorsements into batches per insurer. Runs every 15 min. @SchedulerLock." "Spring Scheduler" "Scheduler"
                    anomalyDetectionScheduler = component "AnomalyDetectionScheduler" "Scans for anomalous endorsement patterns. Runs every 5 min." "Spring Scheduler" "Scheduler"
                    balanceForecastScheduler = component "BalanceForecastScheduler" "Generates balance forecasts for all employers. Daily at 06:00." "Spring Scheduler" "Scheduler"
                    reconciliationScheduler = component "ReconciliationScheduler" "Nightly reconciliation against insurer records." "Spring Scheduler" "Scheduler"
                    processMiningScheduler = component "ProcessMiningScheduler" "Computes STP rates and process metrics. Daily at 03:00." "Spring Scheduler" "Scheduler"
                    provisionalCoverageCleanup = component "ProvisionalCoverageCleanup" "Expires provisional coverages past 30-day window." "Spring Scheduler" "Scheduler"
                    stuckRetryScheduler = component "StuckEndorsementRetryScheduler" "Retries endorsements stuck in intermediate states." "Spring Scheduler" "Scheduler"
                    dataRetentionScheduler = component "DataRetentionScheduler" "Archives old data per retention policy. Weekly." "Spring Scheduler" "Scheduler"
                }

                // ── Domain Core ──────────────────────────────────────────

                group "Domain Core" {
                    endorsementModel = component "Endorsement" "Aggregate root: 11-state lifecycle, optimistic locking, idempotency key, retry count. Rich domain model with transitionTo() validation." "Java Domain Model" "DomainModel"
                    eaAccountModel = component "EAAccount" "Aggregate root: composite key (employerId, insurerId), balance/reserved/available, debit/credit/reserve operations." "Java Domain Model" "DomainModel"
                    endorsementEvent = component "EndorsementEvent" "Sealed interface with 24 event record types: lifecycle (11), financial (2), reconciliation (3), intelligence (6), coverage (2)." "Java Sealed Interface" "DomainModel"
                    endorsementStatus = component "EndorsementStatus" "11-state enum with EnumSet-based O(1) transition validation. canTransitionTo(), isTerminal()." "Java Enum" "DomainModel"
                    stateMachine = component "EndorsementStateMachine" "Enforces valid state transitions. Single entry point for all state changes." "Domain Service" "DomainService"
                    eaBalanceCalculator = component "EABalanceCalculator" "Batch priority sequencing: DELETEs first (P0), cost-neutral UPDATEs (P1), ADDs by urgency (P2). 0/1 Knapsack DP." "Domain Service" "DomainService"
                    insurerRegistry = component "InsurerRegistry" "@Cacheable insurer config lookup with @CacheEvict on updates. Caffeine 60s TTL." "Domain Service" "DomainService"

                    // Port interfaces
                    endorsementRepo = component "EndorsementRepository" "Port: CRUD + findByIdempotencyKey, findByStatus, findByEmployerId." "Domain Port Interface" "Port"
                    eaAccountRepo = component "EAAccountRepository" "Port: findByEmployerIdAndInsurerId, save with optimistic lock." "Domain Port Interface" "Port"
                    batchRepo = component "BatchRepository" "Port: batch assembly and status tracking." "Domain Port Interface" "Port"
                    insurerPort = component "InsurerPort" "Port: submitRealTime, submitBatch, checkBatchStatus, getCapabilities. Strategy pattern interface." "Domain Port Interface" "Port"
                    eventPublisher = component "EventPublisher" "Port: publish(EndorsementEvent). Observer pattern." "Domain Port Interface" "Port"
                    anomalyDetectionPort = component "AnomalyDetectionPort" "Port: detectAnomalies(employerId). Pluggable algorithm." "Domain Port Interface" "Port"
                    forecastPort = component "BalanceForecastPort" "Port: generateForecast(employerId, insurerId). Pluggable engine." "Domain Port Interface" "Port"
                    errorResolutionPort = component "ErrorResolutionPort" "Port: resolveError(endorsementId, errorMessage). Pluggable resolver." "Domain Port Interface" "Port"
                    processMiningPort = component "ProcessMiningPort" "Port: computeStpRate(insurerId). Pluggable analyzer." "Domain Port Interface" "Port"
                    batchOptimizerPort = component "BatchOptimizerPort" "Port: optimizeBatch(endorsements, availableBalance). Pluggable optimizer." "Domain Port Interface" "Port"
                    notificationPort = component "NotificationPort" "Port: sendNotification(type, payload). Pluggable channel." "Domain Port Interface" "Port"
                }

                // ── Infrastructure Layer (Driven Side) ───────────────────

                group "Infrastructure Layer" {
                    // Persistence Adapters
                    jpaEndorsementAdapter = component "JpaEndorsementRepositoryAdapter" "Implements EndorsementRepository. Uses EndorsementMapper ACL for domain<->entity translation." "Spring Data JPA" "Adapter,Persistence"
                    jpaEaAccountAdapter = component "JpaEAAccountRepositoryAdapter" "Implements EAAccountRepository. Composite key mapping." "Spring Data JPA" "Adapter,Persistence"
                    endorsementMapper = component "EndorsementMapper" "Anti-Corruption Layer: translates domain enums to JPA strings, JsonNode to column types. Only class that imports both domain and JPA types." "Java Mapper" "ACL"

                    // Insurer Adapters
                    insurerRouter = component "InsurerRouter" "Factory pattern: auto-discovers InsurerPort beans via Spring DI, resolves correct adapter at runtime from DB config." "Spring Component" "Adapter,Factory"
                    mockAdapter = component "MockInsurerAdapter" "JSON/REST, 100ms delay, synthetic refs. @CircuitBreaker + @Retry." "Spring Component" "Adapter,Insurer"
                    iciciAdapter = component "IciciLombardAdapter" "REST/JSON, RT only, 150ms. @CircuitBreaker(name='iciciLombard') with 50%/20-call/30s config." "Spring Component" "Adapter,Insurer"
                    nivaAdapter = component "NivaBupaAdapter" "CSV/SFTP, batch only. submitRealTime() throws UnsupportedOperationException." "Spring Component" "Adapter,Insurer"
                    bajajAdapter = component "BajajAllianzAdapter" "SOAP/XML, RT+batch, 250ms. @CircuitBreaker(name='bajajAllianz') with 40%/15-call/45s config." "Spring Component" "Adapter,Insurer"

                    // Intelligence Adapters
                    ruleAnomalyDetector = component "RuleBasedAnomalyDetector" "5 rules: Volume Spike, ADD/DELETE Cycling, Suspicious Timing, Unusual Premium, Dormancy Break. Score 0-1." "Spring Component" "Adapter,Intelligence"
                    ollamaAnomalyDetector = component "OllamaAugmentedAnomalyDetector" "@ConditionalOnProperty(ollama). Wraps rule-based, enriches scores > 0.7 with LLM explanations. @CircuitBreaker with rule-based fallback." "Spring AI + Ollama" "Adapter,Intelligence,AI"
                    statisticalForecast = component "StatisticalForecastEngine" "30-day projection with dual seasonality: day-of-week (Mon=1.2x) + monthly (Apr=1.4x)." "Apache Commons Math" "Adapter,Intelligence"
                    simulatedErrorResolver = component "SimulatedErrorResolver" "5 error patterns with confidence scoring. Auto-apply above 95%." "Spring Component" "Adapter,Intelligence"
                    ollamaErrorResolver = component "OllamaErrorResolver" "@ConditionalOnProperty(ollama). Handles ambiguous cases (<95% confidence) with LLM analysis. @CircuitBreaker with rule-based fallback." "Spring AI + Ollama" "Adapter,Intelligence,AI"
                    eventStreamAnalyzer = component "EventStreamAnalyzer" "STP rate computation, bottleneck identification from event stream." "Spring Component" "Adapter,Intelligence"
                    constraintBatchOptimizer = component "ConstraintBatchOptimizer" "Priority sequencing + balance-aware batch assembly. 0/1 Knapsack DP." "Spring Component" "Adapter,Intelligence"

                    // Messaging
                    kafkaPublisher = component "KafkaEventPublisher" "Implements EventPublisher. Serializes events to JSON, sends to endorsement-events topic with employerId partition key. acks=all." "Spring Kafka" "Adapter,Messaging"

                    // Cross-Cutting
                    securityConfig = component "SecurityConfig" "Stateless sessions (SessionCreationPolicy.STATELESS), CORS, CSRF disabled." "Spring Security" "CrossCutting"
                    mdcFilter = component "MdcRequestFilter" "Injects requestId, traceId, spanId, endorsementId, employerId into MDC. Cleans in finally block." "Spring Filter" "CrossCutting"
                    metricsConfig = component "MetricsConfig" "20+ custom Micrometer metrics: counters (created, confirmed, rejected, error), timers (insurer latency), gauges (11 status counts)." "Micrometer" "CrossCutting"
                    shedlockConfig = component "ShedLockConfig" "Distributed scheduler locking via JDBC. Prevents duplicate runs across instances." "ShedLock" "CrossCutting"
                }
            }

            // ── Data Stores ──────────────────────────────────────────────────

            database = container "PostgreSQL" "13 tables, 20 Flyway migrations. ACID for financial operations. Optimistic locking. JSONB for flexible employee data." "${POSTGRESQL}" "Database"
            redis = container "Redis" "Distributed cache (60s TTL). @Cacheable for InsurerRegistry. Session-free (stateless)." "${REDIS}" "Cache"
            kafka = container "Kafka" "4 topics, 88 partitions. employerId partition key for per-employer ordering. KRaft mode (no ZooKeeper)." "${KAFKA}" "MessageQueue"

            // ── Observability ────────────────────────────────────────────────

            prometheus = container "Prometheus" "Metrics scraping every 15s from /actuator/prometheus. 7-day retention." "Prometheus 2.50" "Monitoring"
            grafana = container "Grafana" "7 auto-provisioned dashboards: app overview, business metrics, infrastructure, multi-insurer, reconciliation, intelligence, scheduler." "Grafana 10.3" "Monitoring"
            jaeger = container "Jaeger" "Distributed tracing via OTLP. 100% sampling. Search by endorsementId for full request journey." "Jaeger 1.55" "Monitoring"
            elasticsearch = container "Elasticsearch" "Structured JSON log storage. Full-text search across all log fields." "Elasticsearch 8.12" "Monitoring"
            logstash = container "Logstash" "Log pipeline: TCP input from app, parsing, forwarding to Elasticsearch." "Logstash 8.12" "Monitoring"
            kibana = container "Kibana" "Log visualization: search, filter, dashboard over Elasticsearch indices." "Kibana 8.12" "Monitoring"

            // ── Container-level relationships ────────────────────────────────

            spa -> backend "Makes API calls" "HTTPS/JSON"
            backend -> database "Reads/writes endorsements, accounts, configs" "JDBC/SQL"
            backend -> redis "Caches insurer configurations" "Redis Protocol"
            backend -> kafka "Publishes domain events, consumes commands" "Kafka Protocol"
            backend -> prometheus "Exposes /actuator/prometheus" "HTTP"
            backend -> jaeger "Sends OTLP traces" "gRPC :4317"
            backend -> logstash "Sends structured JSON logs" "TCP :5000"
            prometheus -> grafana "Data source for dashboards" "PromQL"
            logstash -> elasticsearch "Forwards parsed logs" "HTTP :9200"
            elasticsearch -> kibana "Data source for log search" "HTTP :9200"
        }

        // ── External Relationships ───────────────────────────────────────────

        hrAdmin -> plumEndorsement.spa "Creates endorsements, tracks status, views EA balance" "HTTPS"
        financeTeam -> plumEndorsement.spa "Monitors EA balances, reviews forecasts" "HTTPS"
        opsTeam -> plumEndorsement.spa "Reviews anomalies, manages insurers, monitors health" "HTTPS"
        opsTeam -> plumEndorsement.grafana "Views dashboards and metrics" "HTTPS"
        opsTeam -> plumEndorsement.jaeger "Traces requests by endorsementId" "HTTPS"
        opsTeam -> plumEndorsement.kibana "Searches structured logs" "HTTPS"

        plumEndorsement.backend -> iciciLombard "Submits real-time endorsements" "REST/JSON" "Insurer,Sync"
        plumEndorsement.backend -> nivaBupa "Uploads batch CSV files" "CSV/SFTP" "Insurer,Async"
        plumEndorsement.backend -> bajajAllianz "Submits endorsements" "SOAP/XML" "Insurer,Sync"
        plumEndorsement.backend -> ollamaService "Sends anomalies/errors for LLM analysis" "HTTP/JSON :11434" "AI"

        // ── Component-level relationships (backend internals) ────────────────

        // Controllers → Handlers/Services
        plumEndorsement.backend.endorsementController -> plumEndorsement.backend.createEndorsementHandler "Delegates create" "Method Call"
        plumEndorsement.backend.endorsementController -> plumEndorsement.backend.processEndorsementHandler "Delegates submit/confirm/reject" "Method Call"
        plumEndorsement.backend.endorsementController -> plumEndorsement.backend.endorsementQueryHandler "Delegates get/list" "Method Call"
        plumEndorsement.backend.eaAccountController -> plumEndorsement.backend.eaAccountRepo "Looks up EA balance" "Port Interface"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.anomalyDetectionService "Delegates anomaly queries" "Method Call"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.balanceForecastService "Delegates forecast generation" "Method Call"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.errorResolutionService "Delegates error resolution" "Method Call"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.processMiningService "Delegates STP metrics" "Method Call"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.employerHealthScoreService "Delegates health scores" "Method Call"
        plumEndorsement.backend.intelligenceController -> plumEndorsement.backend.insurerBenchmarkService "Delegates benchmarks" "Method Call"
        plumEndorsement.backend.insurerConfigController -> plumEndorsement.backend.insurerRegistry "Manages insurer configs" "Method Call"
        plumEndorsement.backend.reconciliationController -> plumEndorsement.backend.reconciliationEngine "Delegates reconciliation" "Method Call"
        plumEndorsement.backend.auditLogController -> plumEndorsement.backend.auditLogService "Queries audit logs" "Method Call"

        // Handlers → Domain
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.endorsementModel "Creates & validates" "Domain Logic"
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.stateMachine "Transitions state" "Domain Service"
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.eaAccountModel "Reserves balance" "Domain Logic"
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.endorsementRepo "Saves endorsement" "Port Interface"
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.eaAccountRepo "Saves EA account" "Port Interface"
        plumEndorsement.backend.createEndorsementHandler -> plumEndorsement.backend.eventPublisher "Publishes Created event" "Port Interface"

        plumEndorsement.backend.processEndorsementHandler -> plumEndorsement.backend.insurerRouter "Resolves insurer adapter" "Factory Pattern"
        plumEndorsement.backend.processEndorsementHandler -> plumEndorsement.backend.stateMachine "Transitions state" "Domain Service"
        plumEndorsement.backend.processEndorsementHandler -> plumEndorsement.backend.endorsementRepo "Updates endorsement" "Port Interface"
        plumEndorsement.backend.processEndorsementHandler -> plumEndorsement.backend.eventPublisher "Publishes lifecycle events" "Port Interface"

        plumEndorsement.backend.endorsementQueryHandler -> plumEndorsement.backend.endorsementRepo "Reads endorsements" "Port Interface"

        // Services → Ports
        plumEndorsement.backend.anomalyDetectionService -> plumEndorsement.backend.anomalyDetectionPort "Delegates detection" "Port Interface"
        plumEndorsement.backend.balanceForecastService -> plumEndorsement.backend.forecastPort "Delegates forecasting" "Port Interface"
        plumEndorsement.backend.errorResolutionService -> plumEndorsement.backend.errorResolutionPort "Delegates resolution" "Port Interface"
        plumEndorsement.backend.processMiningService -> plumEndorsement.backend.processMiningPort "Delegates analysis" "Port Interface"

        // Schedulers → Services/Ports
        plumEndorsement.backend.batchAssemblyScheduler -> plumEndorsement.backend.batchOptimizerPort "Optimizes batch order" "Port Interface"
        plumEndorsement.backend.batchAssemblyScheduler -> plumEndorsement.backend.insurerRouter "Resolves batch adapter" "Factory Pattern"
        plumEndorsement.backend.anomalyDetectionScheduler -> plumEndorsement.backend.anomalyDetectionService "Triggers scan" "Method Call"
        plumEndorsement.backend.balanceForecastScheduler -> plumEndorsement.backend.balanceForecastService "Triggers forecast" "Method Call"
        plumEndorsement.backend.reconciliationScheduler -> plumEndorsement.backend.reconciliationEngine "Triggers reconciliation" "Method Call"
        plumEndorsement.backend.processMiningScheduler -> plumEndorsement.backend.processMiningService "Triggers analysis" "Method Call"

        // Router → Adapters
        plumEndorsement.backend.insurerRouter -> plumEndorsement.backend.insurerRegistry "Looks up config" "Cached"
        plumEndorsement.backend.insurerRouter -> plumEndorsement.backend.mockAdapter "Resolves MOCK" "Strategy"
        plumEndorsement.backend.insurerRouter -> plumEndorsement.backend.iciciAdapter "Resolves ICICI_LOMBARD" "Strategy"
        plumEndorsement.backend.insurerRouter -> plumEndorsement.backend.nivaAdapter "Resolves NIVA_BUPA" "Strategy"
        plumEndorsement.backend.insurerRouter -> plumEndorsement.backend.bajajAdapter "Resolves BAJAJ_ALLIANZ" "Strategy"

        // Intelligence Port → Adapters
        plumEndorsement.backend.ruleAnomalyDetector -> plumEndorsement.backend.anomalyDetectionPort "Implements" "Rule Engine"
        plumEndorsement.backend.ollamaAnomalyDetector -> plumEndorsement.backend.anomalyDetectionPort "Implements (Ollama profile)" "GenAI"
        plumEndorsement.backend.statisticalForecast -> plumEndorsement.backend.forecastPort "Implements" "Statistical"
        plumEndorsement.backend.simulatedErrorResolver -> plumEndorsement.backend.errorResolutionPort "Implements" "Pattern Matching"
        plumEndorsement.backend.ollamaErrorResolver -> plumEndorsement.backend.errorResolutionPort "Implements (Ollama profile)" "GenAI"
        plumEndorsement.backend.eventStreamAnalyzer -> plumEndorsement.backend.processMiningPort "Implements" "Event Analysis"
        plumEndorsement.backend.constraintBatchOptimizer -> plumEndorsement.backend.batchOptimizerPort "Implements" "DP Algorithm"

        // Infrastructure Adapters → External
        plumEndorsement.backend.jpaEndorsementAdapter -> plumEndorsement.database "Persists via Spring Data JPA" "JDBC"
        plumEndorsement.backend.jpaEaAccountAdapter -> plumEndorsement.database "Persists via Spring Data JPA" "JDBC"
        plumEndorsement.backend.kafkaPublisher -> plumEndorsement.kafka "Publishes to endorsement-events" "Kafka Producer"
        plumEndorsement.backend.ollamaAnomalyDetector -> ollamaService "Sends anomalies for LLM enrichment" "Spring AI ChatClient"
        plumEndorsement.backend.ollamaErrorResolver -> ollamaService "Sends errors for LLM analysis" "Spring AI ChatClient"
        plumEndorsement.backend.iciciAdapter -> iciciLombard "Submits endorsements" "REST/JSON"
        plumEndorsement.backend.nivaAdapter -> nivaBupa "Uploads CSV batches" "SFTP"
        plumEndorsement.backend.bajajAdapter -> bajajAllianz "Submits endorsements" "SOAP/XML"
        plumEndorsement.backend.insurerRegistry -> plumEndorsement.redis "Caches insurer configs" "Redis"

        // ACL
        plumEndorsement.backend.jpaEndorsementAdapter -> plumEndorsement.backend.endorsementMapper "Translates domain <-> entity" "Method Call"

        // ── Deployment: Docker Compose (Development) ─────────────────────────

        development = deploymentEnvironment "Development (Docker Compose)" {

            deploymentNode "Developer Machine" "" "Docker Compose" "DevMachine" {

                deploymentNode "Frontend Container" "" "Node.js" "DockerContainer" {
                    containerInstance plumEndorsement.spa
                }

                deploymentNode "Backend Container" "" "Eclipse Temurin 21 JRE" "DockerContainer" {
                    containerInstance plumEndorsement.backend
                }

                deploymentNode "PostgreSQL Container" "" "postgres:16-alpine" "DockerContainer" {
                    containerInstance plumEndorsement.database
                }

                deploymentNode "Redis Container" "" "redis:7-alpine" "DockerContainer" {
                    containerInstance plumEndorsement.redis
                }

                deploymentNode "Kafka Container" "" "apache/kafka:3.7.0 (KRaft)" "DockerContainer" {
                    containerInstance plumEndorsement.kafka
                }

                deploymentNode "Prometheus Container" "" "prom/prometheus:v2.50.1" "DockerContainer" {
                    containerInstance plumEndorsement.prometheus
                }

                deploymentNode "Grafana Container" "" "grafana/grafana:10.3.3" "DockerContainer" {
                    containerInstance plumEndorsement.grafana
                }

                deploymentNode "Jaeger Container" "" "jaegertracing/all-in-one:1.55" "DockerContainer" {
                    containerInstance plumEndorsement.jaeger
                }

                deploymentNode "Elasticsearch Container" "" "elasticsearch:8.12.2" "DockerContainer" {
                    containerInstance plumEndorsement.elasticsearch
                }

                deploymentNode "Logstash Container" "" "logstash:8.12.2" "DockerContainer" {
                    containerInstance plumEndorsement.logstash
                }

                deploymentNode "Kibana Container" "" "kibana:8.12.2" "DockerContainer" {
                    containerInstance plumEndorsement.kibana
                }
            }
        }

        // ── Deployment: Kubernetes (Production) ──────────────────────────────

        production = deploymentEnvironment "Production (Kubernetes)" {

            deploymentNode "Kubernetes Cluster" "" "Kubernetes" "K8sCluster" {

                deploymentNode "plum-endorsements Namespace" "" "Kubernetes Namespace" "K8sNamespace" {

                    deploymentNode "Backend Deployment" "" "Deployment + HPA (2-8 pods)" "K8sPod" 3 {
                        containerInstance plumEndorsement.backend
                    }

                    deploymentNode "PostgreSQL StatefulSet" "" "PVC-backed" "K8sPod" {
                        containerInstance plumEndorsement.database
                    }

                    deploymentNode "Redis Deployment" "" "ClusterIP Service" "K8sPod" {
                        containerInstance plumEndorsement.redis
                    }

                    deploymentNode "Kafka StatefulSet" "" "KRaft mode" "K8sPod" {
                        containerInstance plumEndorsement.kafka
                    }

                    deploymentNode "Observability Stack" "" "Monitoring" "K8sPod" {
                        containerInstance plumEndorsement.prometheus
                        containerInstance plumEndorsement.grafana
                        containerInstance plumEndorsement.jaeger
                    }

                    deploymentNode "ELK Stack" "" "Logging" "K8sPod" {
                        containerInstance plumEndorsement.elasticsearch
                        containerInstance plumEndorsement.logstash
                        containerInstance plumEndorsement.kibana
                    }

                    infrastructureNode "Ingress Controller" "Routes external traffic to backend service" "NGINX Ingress"
                    infrastructureNode "PodDisruptionBudget" "minAvailable: 1 — ensures availability during rolling updates" "Kubernetes PDB"
                    infrastructureNode "HorizontalPodAutoscaler" "2-8 replicas at 70% CPU threshold" "Kubernetes HPA"
                }
            }
        }
    }

    // ── Views ────────────────────────────────────────────────────────────────

    views {

        // ── Level 1: System Context ──────────────────────────────────────────

        systemContext plumEndorsement "L1_SystemContext" "Level 1 — Who uses the system and what it connects to." {
            include *
            autoLayout lr 350 200
        }

        // ── Level 2: Container Diagram ───────────────────────────────────────

        container plumEndorsement "L2_Containers" "Level 2 — Deployable units and their communication protocols." {
            include *
            autoLayout lr 300 150
        }

        // ── Level 3: Component Diagram (Backend) ─────────────────────────────

        component plumEndorsement.backend "L3_Components" "Level 3 — Hexagonal architecture: API Layer, Application Layer, Domain Core, Infrastructure Layer." {
            include *
            autoLayout lr 250 100
        }

        // ── Level 3: Filtered — API Layer Only ───────────────────────────────

        component plumEndorsement.backend "L3_APILayer" "API Layer — 6 Controllers, 27 REST endpoints, RFC 7807 error handling." {
            include element.tag==Controller
            include element.tag==CrossCutting
            include element.tag==Handler
            autoLayout lr 300 150
        }

        // ── Level 3: Filtered — Domain Core Only ─────────────────────────────

        component plumEndorsement.backend "L3_DomainCore" "Domain Core — Models, ports, domain services. Zero infrastructure imports." {
            include element.tag==DomainModel
            include element.tag==DomainService
            include element.tag==Port
            autoLayout lr 300 150
        }

        // ── Level 3: Filtered — Intelligence Adapters ────────────────────────

        component plumEndorsement.backend "L3_Intelligence" "Intelligence Layer — 5 rule-based adapters + 2 Ollama GenAI adapters." {
            include element.tag==Intelligence
            include element.tag==Port
            include plumEndorsement.backend.anomalyDetectionService
            include plumEndorsement.backend.balanceForecastService
            include plumEndorsement.backend.errorResolutionService
            include plumEndorsement.backend.processMiningService
            autoLayout lr 300 150
        }

        // ── Level 3: Filtered — Insurer Adapters ─────────────────────────────

        component plumEndorsement.backend "L3_InsurerAdapters" "Multi-Insurer Framework — Strategy pattern with per-insurer circuit breakers." {
            include element.tag==Insurer
            include element.tag==Factory
            include plumEndorsement.backend.insurerPort
            include plumEndorsement.backend.insurerRegistry
            include plumEndorsement.backend.processEndorsementHandler
            autoLayout lr 300 150
        }

        // ── Dynamic: Create Endorsement Flow ─────────────────────────────────

        dynamic plumEndorsement "Dyn_CreateEndorsement" "End-to-end endorsement creation: validate, dedup, debit EA, grant provisional coverage, publish event." {
            title "Create Endorsement Flow"
            hrAdmin -> plumEndorsement.spa "1. Fills endorsement form (type, employee, premium)"
            plumEndorsement.spa -> plumEndorsement.backend "2. POST /api/v1/endorsements"
            plumEndorsement.backend -> plumEndorsement.database "3. Idempotency check (SELECT by key)"
            plumEndorsement.backend -> plumEndorsement.database "4. Reserve EA balance + save endorsement"
            plumEndorsement.backend -> plumEndorsement.kafka "5. Publish EndorsementEvent.Created"
            plumEndorsement.backend -> plumEndorsement.spa "6. Return 201 + endorsement ID"
            autoLayout lr
        }

        // ── Dynamic: Submit to Insurer Flow ──────────────────────────────────

        dynamic plumEndorsement "Dyn_InsurerSubmission" "Insurer submission: router resolves adapter by Strategy pattern, circuit breaker protects the call." {
            title "Submit to Insurer (Real-Time Path)"
            plumEndorsement.spa -> plumEndorsement.backend "1. POST /api/v1/endorsements/{id}/submit"
            plumEndorsement.backend -> plumEndorsement.redis "2. Lookup insurer config (cached)"
            plumEndorsement.backend -> iciciLombard "3. Submit via resolved adapter (@CircuitBreaker + @Retry)"
            plumEndorsement.backend -> plumEndorsement.database "4. Update status to CONFIRMED, save insurer reference"
            plumEndorsement.backend -> plumEndorsement.kafka "5. Publish EndorsementEvent.Confirmed"
            plumEndorsement.backend -> plumEndorsement.spa "6. Return updated endorsement"
            autoLayout lr
        }

        // ── Dynamic: Batch Assembly Flow ─────────────────────────────────────

        dynamic plumEndorsement "Dyn_BatchAssembly" "Batch assembly: scheduled every 15 min, optimizes order (DELETEs first), submits to batch-only insurers." {
            title "Batch Assembly & Submission (Every 15 min)"
            plumEndorsement.backend -> plumEndorsement.database "1. Find QUEUED_FOR_BATCH endorsements"
            plumEndorsement.backend -> plumEndorsement.database "2. Group by insurer, optimize order (Knapsack DP)"
            plumEndorsement.backend -> nivaBupa "3. Upload CSV batch to Niva Bupa (SFTP)"
            plumEndorsement.backend -> plumEndorsement.database "4. Update to BATCH_SUBMITTED, save batch ref"
            plumEndorsement.backend -> plumEndorsement.kafka "5. Publish EndorsementEvent.BatchSubmitted"
            autoLayout lr
        }

        // ── Dynamic: Anomaly Detection Flow ──────────────────────────────────

        dynamic plumEndorsement "Dyn_AnomalyDetection" "Anomaly detection: 5 rules run every 5 min, Ollama enriches high-scoring anomalies with LLM narratives." {
            title "Anomaly Detection (Every 5 min)"
            plumEndorsement.backend -> plumEndorsement.database "1. Load recent endorsements per employer"
            plumEndorsement.backend -> plumEndorsement.database "2. Run 5 rules, compute scores (0-1)"
            plumEndorsement.backend -> ollamaService "3. Send scores > 0.7 to LLM for enrichment"
            plumEndorsement.backend -> plumEndorsement.database "4. Save anomaly with score + LLM explanation"
            plumEndorsement.backend -> plumEndorsement.kafka "5. Publish EndorsementEvent.AnomalyDetected"
            autoLayout lr
        }

        // ── Deployment: Docker Compose ────────────────────────────────────────

        deployment plumEndorsement development "Deploy_DockerCompose" "Development environment: Docker Compose with 9+ services, health checks, and volume mounts." {
            include *
            autoLayout lr 300 150
        }

        // ── Deployment: Kubernetes ────────────────────────────────────────────

        deployment plumEndorsement production "Deploy_Kubernetes" "Production environment: Kubernetes with HPA (2-8 pods), PDB, Ingress, and PVC-backed storage." {
            include *
            autoLayout lr 300 150
        }

        // ── Styles ───────────────────────────────────────────────────────────

        styles {
            // People
            element "User" {
                shape Person
                background #4338CA
                color #ffffff
                fontSize 22
            }

            // Software Systems
            element "Software System" {
                background #6366F1
                color #ffffff
                shape RoundedBox
                fontSize 22
            }
            element "External System" {
                background #64748B
                color #ffffff
                shape RoundedBox
            }
            element "Insurer" {
                background #0891B2
                color #ffffff
            }
            element "AI" {
                background #7C3AED
                color #ffffff
            }

            // Containers
            element "Container" {
                background #818CF8
                color #ffffff
            }
            element "WebBrowser" {
                shape WebBrowser
                background #3B82F6
                color #ffffff
            }
            element "SpringBoot" {
                background #4F46E5
                color #ffffff
                shape Hexagon
            }
            element "Database" {
                shape Cylinder
                background #0D9488
                color #ffffff
            }
            element "Cache" {
                shape Cylinder
                background #DC2626
                color #ffffff
            }
            element "MessageQueue" {
                shape Pipe
                background #D97706
                color #ffffff
            }
            element "Monitoring" {
                shape RoundedBox
                background #059669
                color #ffffff
            }

            // Components
            element "Controller" {
                shape RoundedBox
                background #3B82F6
                color #ffffff
            }
            element "Handler" {
                shape RoundedBox
                background #8B5CF6
                color #ffffff
            }
            element "AppService" {
                shape RoundedBox
                background #6366F1
                color #ffffff
            }
            element "Scheduler" {
                shape RoundedBox
                background #0891B2
                color #ffffff
            }
            element "DomainModel" {
                shape RoundedBox
                background #059669
                color #ffffff
            }
            element "DomainService" {
                shape Hexagon
                background #10B981
                color #ffffff
            }
            element "Port" {
                shape Component
                background #14B8A6
                color #ffffff
                border dashed
            }
            element "Adapter" {
                shape RoundedBox
                background #F59E0B
                color #000000
            }
            element "Intelligence" {
                background #A855F7
                color #ffffff
            }
            element "ACL" {
                shape Diamond
                background #EF4444
                color #ffffff
            }
            element "CrossCutting" {
                shape RoundedBox
                background #64748B
                color #ffffff
            }
            element "Factory" {
                shape Hexagon
                background #F97316
                color #ffffff
            }

            // Deployment
            element "DevMachine" {
                background #1E293B
                color #ffffff
            }
            element "DockerContainer" {
                background #1E40AF
                color #ffffff
            }
            element "K8sCluster" {
                background #1E293B
                color #ffffff
            }
            element "K8sNamespace" {
                background #312E81
                color #ffffff
            }
            element "K8sPod" {
                background #1E40AF
                color #ffffff
            }
            element "Infrastructure Node" {
                shape RoundedBox
                background #475569
                color #ffffff
            }

            // Relationships
            relationship "Relationship" {
                color #6B7280
                thickness 2
            }
            relationship "Insurer" {
                color #0891B2
                thickness 3
                style dashed
            }
            relationship "AI" {
                color #7C3AED
                thickness 2
                style dotted
            }
            relationship "Sync" {
                style solid
            }
            relationship "Async" {
                style dashed
            }
        }
    }
}
