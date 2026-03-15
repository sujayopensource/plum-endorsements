# Plum Endorsement Service — Claude Code Skills

These instructions encode the design principles from **Cloud Native Patterns** (Cornelia Davis, Manning) and **Head First Design Patterns** (Freeman & Robson, O'Reilly), grounded in the concrete patterns established in this codebase. Follow them whenever generating, modifying, or reviewing code in this repository.

Each section references the book chapter where the pattern originates, then shows **exactly how** it is implemented here with real file paths and code.

---

## Part A: Cloud Native Patterns (Cornelia Davis)

> "Cloud-native software is designed from the ground up to exploit the cloud platform — it embraces rapid change, large scale, and resilience as first-class concerns."

---

### CNP-1: Event-Driven Microservices (Ch. 4–5)

Davis describes three event-driven interaction styles: **event notification**, **event-carried state transfer**, and **event sourcing**. This codebase implements the first two.

#### How It Works Here

**Sealed event hierarchy** — `domain/model/EndorsementEvent.java`:
```java
public sealed interface EndorsementEvent {
    UUID endorsementId();
    UUID employerId();     // partition key for co-located processing
    Instant occurredAt();
    String eventType();

    record Created(UUID endorsementId, Instant occurredAt, UUID employerId,
                   UUID employeeId, EndorsementType type) implements EndorsementEvent { ... }
    record Confirmed(UUID endorsementId, Instant occurredAt, UUID employerId,
                     String insurerReference) implements EndorsementEvent { ... }
    // 22+ event record types
}
```

**Event-carried state transfer** — Each event record carries the contextual data consumers need (`amount`, `insurerReference`, `reason`, `anomalyScore`) so downstream consumers never need to query back to the source database.

**Publishing** — `infrastructure/messaging/KafkaEventPublisher.java`:
```java
String key = event.employerId() != null
        ? event.employerId().toString()      // partition by employer
        : event.endorsementId().toString();  // fallback
kafkaTemplate.send(TOPIC, key, payload);
```

**4 Kafka topics, 88 total partitions** — `infrastructure/messaging/KafkaConfig.java`:

| Topic | Partitions | Purpose |
|-------|-----------|---------|
| `endorsement-events` | 32 | Domain events (state changes, financial operations) |
| `endorsement-commands` | 32 | Command dispatch |
| `endorsement-notifications` | 8 | Notification fanout |
| `endorsement-reconciliation` | 16 | Reconciliation outcomes |

#### Rules When Writing New Code

1. **Events are facts in past tense.** Name them as things that have already happened: `Created`, `Confirmed`, `Rejected`, `EADebited`. Never `CreateEndorsement` or `ShouldConfirm`.
2. **Add new event types to the sealed interface.** Every new domain state change gets a `record` in `EndorsementEvent`. The sealed hierarchy ensures compile-time exhaustiveness.
3. **Carry sufficient state.** Include all data a consumer needs. A `Confirmed` event carries `insurerReference`; a `Rejected` event carries `reason`. Don't force consumers to call back.
4. **Publish from handlers, not adapters.** Events are published from `application/handler/` after domain operations succeed. Infrastructure adapters (`JpaEndorsementRepositoryAdapter`, `KafkaEventPublisher`) must never publish events autonomously.
5. **Partition by `employerId`.** All events for the same employer land on the same Kafka partition, guaranteeing per-employer ordering. Never change the partition key strategy without migration planning.
6. **`acks: all`** — Producer config requires all in-sync replicas to acknowledge. Do not weaken this for throughput.

---

### CNP-2: App Redundancy — Stateless Processes (Ch. 6)

Davis: "Stateless processes can be killed and restarted at will. They can be scaled horizontally by simply adding more instances." This is foundational to cloud-native scale-out.

#### How It Works Here

**Every handler and service is stateless** — All `@Service` and `@Component` classes hold only `private final` immutable references to injected dependencies. No mutable instance-level fields:

```java
// CreateEndorsementHandler.java — STATELESS
@Service
@RequiredArgsConstructor
@Transactional
public class CreateEndorsementHandler {
    private final EndorsementRepository endorsementRepository;    // immutable ref
    private final EAAccountRepository eaAccountRepository;        // immutable ref
    private final EventPublisher eventPublisher;                   // immutable ref
    // ... all final, all injected
}
```

**Stateless sessions** — `infrastructure/config/SecurityConfig.java`:
```java
.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
```

No HTTP session is ever created. Every request carries all needed context.

#### Rules When Writing New Code

1. **No mutable instance state.** Every field in a `@Service`, `@Component`, or `@RestController` must be `private final`. If you need working state, use method-local variables.
2. **No in-process singleton caches that aren't replaceable.** `InsurerRegistry` uses `@Cacheable` (Caffeine, 60s TTL) — this is acceptable because each instance builds its own cache and cache inconsistency between instances is tolerable for read-mostly configuration data. For shared mutable state, use Redis or the database.
3. **Thread-local cleanup.** MDC context (`endorsementId`, `employerId`, `traceId`) must be cleaned in a `finally` block. Leaked MDC keys poison subsequent requests on the same virtual thread.
4. **No file-system writes.** Don't write temp files, local logs, or state files. Everything goes to the database, Kafka, or external storage.

#### Missing Pattern — Distributed Scheduler Coordination

**Problem**: `BatchAssemblyScheduler`, `ReconciliationScheduler`, and 5 other `@Scheduled` beans have no distributed lock. Running 2+ instances causes duplicate batch submissions and reconciliation runs.

**Required**: When adding multi-instance support, integrate ShedLock:
```java
@Scheduled(cron = "${endorsement.batch.schedule-cron}")
@SchedulerLock(name = "batchAssembly", lockAtLeastFor = "PT1M", lockAtMostFor = "PT14M")
public void assembleAndSubmitBatches() { ... }
```

---

### CNP-3: Application Configuration (Ch. 7)

Davis advocates 12-Factor App style: "Store config in the environment." Configuration should be external to the artifact and vary between deployment stages without code changes.

#### How It Works Here

**Layered configuration** with environment-specific overrides:

| Profile | File | Used For |
|---------|------|----------|
| (default) | `application.yml` | Local development (`localhost`) |
| `test` | `application-test.yml` | Unit/integration tests (H2 or embedded) |
| `railway` | `application-railway.yml` | Cloud deployment (env vars: `${DATABASE_URL}`, `${REDIS_URL}`) |
| `json` | Activated via env var | Enables structured JSON logging for production |

**Environment variable injection** — `application-railway.yml`:
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
  data:
    redis:
      url: ${REDIS_URL}
  kafka:
    bootstrap-servers: ${KAFKA_URL:localhost:9092}
```

**Feature flags via `@Value`** — with safe defaults:
```java
@Value("${endorsement.intelligence.anomaly-detection.enabled:true}")
private boolean anomalyDetectionEnabled;

@Value("${endorsement.intelligence.batch-optimizer.enabled:true}")
private boolean optimizerEnabled;
```

**K8s ConfigMap** — `k8s/backend/configmap.yaml` externalises all connection strings.

#### Rules When Writing New Code

1. **Never hardcode connection strings, credentials, or URLs.** Use `${ENV_VAR:default}` syntax in YAML. The default should work for local development; environment variables override for staging/production.
2. **Use `@Value` with defaults for feature flags and tunables.** Pattern: `@Value("${endorsement.feature.name:sensible-default}")`. This lets operators change behaviour without redeployment.
3. **No `@ConfigurationProperties` in this codebase** — we use `@Value` injection. Keep it consistent. Only introduce `@ConfigurationProperties` if a feature requires binding a complex nested object.
4. **Cron schedules are configurable:** `@Scheduled(cron = "${endorsement.batch.schedule-cron}")`. Never hardcode cron expressions in annotations.
5. **Add new config to all relevant profiles.** If you add a new external dependency URL to `application.yml`, also add the `${ENV_VAR}` version to `application-railway.yml` and the K8s `configmap.yaml`.

---

### CNP-4: Application Lifecycle (Ch. 8)

Davis describes health endpoints, graceful shutdown, and deployment strategies (rolling, blue-green, canary) as essential to cloud-native lifecycle management.

#### How It Works Here

**Health endpoints** — Spring Actuator exposes `/actuator/health` with `show-details: always`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,circuitbreakers,retries
  endpoint:
    health:
      show-details: always
```

**Kubernetes probes** — `k8s/backend/deployment.yaml`:
```yaml
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30      # wait for Spring context to initialize
  periodSeconds: 10
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60      # longer delay; liveness failure = pod restart
  periodSeconds: 15
```

**Docker health check** — `Dockerfile`:
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:${PORT}/actuator/health || exit 1
```

**Container-aware JVM** — `Dockerfile`:
```dockerfile
ENTRYPOINT ["java",
  "-Dspring.threads.virtual.enabled=true",
  "-XX:+UseZGC",
  "-XX:MaxRAMPercentage=75.0",
  "-jar", "app.jar"]
```

#### Rules When Writing New Code

1. **Custom health indicators** — If you add a new infrastructure dependency (e.g., an external insurer API), implement a `HealthIndicator` so `/actuator/health` reports its status.
2. **Resource limits** — K8s deployment sets `memory: 768Mi`. JVM uses `MaxRAMPercentage=75.0` (576 MB heap). If your feature is memory-intensive, verify it fits within this budget.
3. **Startup order** — Services depend on Postgres, Kafka, Redis. The `docker-compose.yml` uses `depends_on` with `condition: service_healthy`. When adding a new infra dependency, add a health check and dependency declaration.

#### Missing Pattern — Graceful Shutdown

**Problem**: No `server.shutdown: graceful` is configured. When a pod is killed, in-flight requests and Kafka commits may be lost.

**Required**: When implementing, add:
```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

---

### CNP-5: Accessing Apps — Service Discovery & Routing (Ch. 9)

Davis describes how cloud-native apps discover and route to backend services. In this codebase, the pattern applies to **insurer routing** — the system must discover and route to the correct insurer adapter based on runtime configuration.

#### How It Works Here

**InsurerRouter** — `infrastructure/insurer/InsurerRouter.java`:
```java
@Component
public class InsurerRouter {
    private final InsurerRegistry insurerRegistry;
    private final Map<String, InsurerPort> adaptersByType;

    public InsurerRouter(InsurerRegistry registry, List<InsurerPort> adapters) {
        this.adaptersByType = adapters.stream()
            .collect(Collectors.toMap(InsurerPort::getAdapterType, Function.identity()));
    }

    public InsurerPort resolve(UUID insurerId) {
        InsurerConfiguration config = insurerRegistry.getConfiguration(insurerId);
        return adaptersByType.get(config.getAdapterType());
    }
}
```

**InsurerRegistry** — `domain/service/InsurerRegistry.java`:
```java
@Cacheable(value = "insurerConfigurations", key = "#insurerId")
public InsurerConfiguration getConfiguration(UUID insurerId) {
    return configurationRepository.findByInsurerId(insurerId)
            .orElseThrow(() -> new InsurerNotFoundException(insurerId));
}

@CacheEvict(value = {"insurerConfigurations", "insurerConfigurationsByCode"}, allEntries = true)
public InsurerConfiguration updateConfiguration(InsurerConfiguration config) {
    return configurationRepository.save(config);
}
```

**Resolution flow**: `Handler → InsurerRouter.resolve(insurerId) → InsurerRegistry (cached) → DB lookup → InsurerPort adapter`

**4 registered adapters**:

| Adapter Type | Class | Protocol | Capabilities |
|-------------|-------|----------|-------------|
| `MOCK` | `MockInsurerAdapter` | JSON/REST | RT + Batch |
| `ICICI_LOMBARD` | `IciciLombardAdapter` | REST/JSON | RT only |
| `NIVA_BUPA` | `NivaBupaAdapter` | CSV/SFTP | Batch only |
| `BAJAJ_ALLIANZ` | `BajajAllianzAdapter` | SOAP/XML | RT + Batch |

#### Rules When Writing New Code

1. **Add a new insurer = new adapter + DB row.** Create the adapter class implementing `InsurerPort` in `infrastructure/insurer/{name}/`, add a Flyway migration to insert the `insurer_configurations` row. The `InsurerRouter` auto-discovers new `InsurerPort` Spring beans.
2. **Never resolve adapters by `if/else` or `switch`.** Always go through `InsurerRouter.resolve(insurerId)`.
3. **Cache eviction on config change.** `InsurerRegistry.updateConfiguration()` uses `@CacheEvict`. If you add a config update path (e.g., admin API), route through this method.

---

### CNP-6: Interaction Redundancy — Retries & Idempotency (Ch. 10)

Davis: "Retries are the most basic form of interaction redundancy. But retries without idempotency are dangerous — they can cause duplicate side effects."

#### Retries — How It Works Here

**Resilience4j retry** with exponential backoff — `application.yml`:
```yaml
resilience4j:
  retry:
    instances:
      insurerSubmission:
        maxAttempts: 3
        waitDuration: 2s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
      iciciLombard:
        maxAttempts: 3
        waitDuration: 1s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
      bajajAllianz:
        maxAttempts: 5              # more retries — SOAP is flakier
        waitDuration: 3s
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
```

Applied via annotation — `MockInsurerAdapter.java`:
```java
@CircuitBreaker(name = "insurerSubmission", fallbackMethod = "submitRealTimeFallback")
@Retry(name = "insurerSubmission")
public SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> data) { ... }
```

#### Idempotency — How It Works Here

**Idempotency key check** — `CreateEndorsementHandler.java`:
```java
Optional<Endorsement> existing = endorsementRepository.findByIdempotencyKey(
        endorsement.getIdempotencyKey());
if (existing.isPresent()) {
    log.info("Duplicate endorsement detected for idempotency key: {}",
             endorsement.getIdempotencyKey());
    return existing.get();    // return existing, no side effects
}
```

Backed by unique database constraint on `idempotency_key`.

#### Domain-Level Retry — `Endorsement.java`:
```java
public boolean canRetry() {
    return retryCount < 3 && status == EndorsementStatus.REJECTED;
}

public void incrementRetry() {
    this.retryCount++;
    this.status = EndorsementStatus.RETRY_PENDING;
    this.updatedAt = Instant.now();
}
```

#### Rules When Writing New Code

1. **Every external call gets `@Retry` + `@CircuitBreaker`.** Any new adapter method that calls an external system must be annotated. Define a named instance in `application.yml` with appropriate backoff.
2. **Every create/mutate endpoint must be idempotent.** If it creates a resource, accept an idempotency key and check for duplicates before proceeding. If it mutates state, ensure the mutation is a no-op when re-applied (state machine transitions already handle this — `transitionTo()` throws if the transition is invalid, preventing double-application).
3. **Retries at the infrastructure layer; idempotency at the domain layer.** Resilience4j retries in adapters handle transient network failures. Idempotency keys in handlers prevent duplicate business operations.
4. **Never retry non-idempotent operations.** If an adapter method has side effects that can't be safely repeated (e.g., financial debit), either make it idempotent at the insurer API level or disable retry for that method.

---

### CNP-7: Fronting Services — Circuit Breakers (Ch. 11)

Davis: "A circuit breaker prevents cascading failures by failing fast when a downstream service is unhealthy, rather than waiting for timeouts."

#### How It Works Here

**Per-insurer circuit breakers** — `application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      insurerSubmission:             # default for mock
        slidingWindowSize: 10
        failureRateThreshold: 50     # open at 50% failure rate
        waitDurationInOpenState: 30s
      iciciLombard:
        slidingWindowSize: 20        # wider window — higher volume
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      bajajAllianz:
        slidingWindowSize: 15
        failureRateThreshold: 40     # more sensitive — SOAP is fragile
        waitDurationInOpenState: 45s  # longer wait — slower recovery
```

**Fallback methods** — every adapter has a typed fallback:
```java
private SubmissionResult submitRealTimeFallback(UUID endorsementId,
        Map<String, Object> data, Throwable t) {
    log.warn("Circuit breaker fallback for endorsement {}: {}", endorsementId, t.getMessage());
    return new SubmissionResult(false, null, "Insurer service unavailable: " + t.getMessage());
}
```

**Circuit breaker state exposed** via Actuator: `/actuator/circuitbreakers`

#### Rules When Writing New Code

1. **One circuit breaker instance per external service.** ICICI Lombard has its own instance (`iciciLombard`) tuned to its latency profile. Bajaj Allianz has its own (`bajajAllianz`) with different thresholds. Never share a circuit breaker across unrelated services.
2. **Always provide a fallback method.** The fallback must return the same type as the protected method and accept the same arguments plus a `Throwable`. Return a graceful degradation (e.g., `SubmissionResult(false, null, errorMessage)`) — never throw from a fallback.
3. **Tune from real data.** Current settings are defaults. After production deployment, tune `slidingWindowSize`, `failureRateThreshold`, and `waitDurationInOpenState` based on observed insurer latency percentiles.
4. **Monitor circuit breaker state.** `registerHealthIndicator: true` makes breaker state visible in `/actuator/health`. Alert when any breaker enters OPEN state.

---

### CNP-8: Troubleshooting — Distributed Tracing & Log Aggregation (Ch. 12)

Davis: "In a distributed system, understanding what happened requires correlating events across multiple services. Distributed tracing and structured logging provide this correlation."

#### Distributed Tracing — How It Works Here

**OpenTelemetry integration** — `build.gradle.kts`:
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("io.opentelemetry:opentelemetry-exporter-otlp")
```

**100% sampling + baggage propagation** — `application.yml`:
```yaml
tracing:
  sampling:
    probability: 1.0
  baggage:
    remote-fields: [endorsementId, employerId]
    correlation:
      fields: [endorsementId, employerId]
```

**MDC injection** — `infrastructure/config/MdcRequestFilter.java`:
```java
MDC.put("requestId", requestId);
var span = tracer.currentSpan();
if (span != null) {
    MDC.put("traceId", span.context().traceId());
    MDC.put("spanId", span.context().spanId());
}
```

#### Structured Logging — How It Works Here

**JSON logging for production** — `logback-spring.xml`:
```xml
<springProfile name="json">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>spanId</includeMdcKeyName>
        <includeMdcKeyName>requestId</includeMdcKeyName>
        <includeMdcKeyName>endorsementId</includeMdcKeyName>
        <includeMdcKeyName>employerId</includeMdcKeyName>
        <customFields>{"service":"endorsement-service"}</customFields>
    </encoder>
</springProfile>
```

**Human-readable for local dev**:
```
HH:mm:ss.SSS [thread] LEVEL logger [traceId=X spanId=X requestId=X endorsementId=X] - message
```

**Log pipeline**: App → Logstash (TCP:5000) → Elasticsearch → Kibana

#### Metrics — How It Works Here

**20+ custom metrics** in `infrastructure/config/MetricsConfig.java`:
- `endorsement.created` (Counter, tag: `type`)
- `endorsement.state.transition` (Counter, tags: `from`, `to`)
- `endorsement.insurer.submission.duration` (Timer, tags: `mode`, `result`)
- `endorsement.batch.size` (DistributionSummary)
- `endorsement.active.count` (Gauge, tag: `status` — 11 values)
- `endorsement.error` (Counter, tag: `type`)

**Request logging** — `infrastructure/config/RequestLoggingFilter.java`:
```java
log.info("HTTP {} {} -> {} ({}ms) [body={}B]",
    request.getMethod(), request.getRequestURI(), status, durationMs, contentSize);
```

#### Rules When Writing New Code

1. **Set MDC at handler entry, clear in `finally`.** Every handler method must:
   ```java
   try {
       MDC.put("endorsementId", id.toString());
       MDC.put("employerId", endorsement.getEmployerId().toString());
       // ... business logic
   } finally {
       MDC.remove("endorsementId");
       MDC.remove("employerId");
   }
   ```
2. **Record metrics for every operation.** Use `Timer.Sample` for durations, `Counter` for events, `Gauge` for current state. Every error handler in `GlobalExceptionHandler` increments `endorsement.error` with a `type` tag.
3. **Log at the right level.** `INFO` for business events (endorsement created, batch submitted). `WARN` for recoverable issues (retry triggered, cache miss). `ERROR` for unexpected failures. Never `DEBUG` in production-path code without a guard.
4. **Include correlation context in log messages.** Format: `"action description for endorsementId={}, employerId={}"`. Let MDC add the traceId/spanId automatically.
5. **Metric naming convention**: `endorsement.{domain}.{action}` with tags for dimensions. Tags over separate metric names — e.g., `endorsement.error` with `type=not_found` rather than `endorsement.error.not_found`.

---

### CNP-9: Cloud-Native Data — CQRS & Eventual Consistency (Ch. 13)

Davis: "Cloud-native data patterns accept that data will be eventually consistent across services. CQRS separates read and write models to optimise each independently."

#### CQRS — How It Works Here

**Command handlers** (mutate state, publish events):
```java
@Service
@Transactional                              // read-write transaction
public class CreateEndorsementHandler { ... }

@Service
public class ProcessEndorsementHandler { ... }
```

**Query handler** (read-only, optimisable):
```java
@Service
@Transactional(readOnly = true)             // can route to read replicas
public class EndorsementQueryHandler {
    public Endorsement findById(UUID id) { ... }
    public Page<Endorsement> findByEmployerId(UUID employerId, Pageable pageable) { ... }
}
```

**Open-in-view disabled** — `application.yml`:
```yaml
jpa:
  open-in-view: false                       # no lazy loading in controllers
```

#### Rules When Writing New Code

1. **Commands and queries in separate handler classes.** `CreateEndorsementHandler` and `ProcessEndorsementHandler` are commands. `EndorsementQueryHandler` is queries. Don't add query methods to command handlers or vice versa.
2. **Query handlers use `@Transactional(readOnly = true)`.** This enables PostgreSQL read-replica routing and avoids accidental writes.
3. **Commands publish events; queries never do.** A query method must not modify state or publish to Kafka.
4. **Paginate unbounded queries.** All list methods accept `Pageable`. In schedulers, use cursor-based pagination or status-based filtering (`findByStatus(QUEUED_FOR_BATCH)`) to avoid loading millions of rows.

---

## Part B: Head First Design Patterns (Freeman & Robson)

> "Knowing concepts like abstraction, inheritance, and polymorphism doesn't make you a good OO designer. A design guru thinks about how to create flexible designs that are maintainable and can cope with change."

---

### HFDP-1: Strategy Pattern (Ch. 1) — Insurer Adapters

**Book principle**: "Define a family of algorithms, encapsulate each one, and make them interchangeable. Strategy lets the algorithm vary independently from clients that use it."

#### How It Works Here

**Strategy interface** — `domain/port/InsurerPort.java`:
```java
public interface InsurerPort {
    SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> data);
    BatchStatusResult submitBatch(UUID batchId, List<Map<String, Object>> endorsements);
    InsurerCapabilities getCapabilities();
    String getAdapterType();

    // Template method defaults (see HFDP-7)
    default Map<String, Object> mapToInsurerFormat(Map<String, Object> data) { return data; }
    default Map<String, Object> mapFromInsurerFormat(Map<String, Object> data) { return data; }
}
```

**Concrete strategies** — each encapsulates a different insurer's API protocol:

| Strategy | Protocol | Key Behaviour |
|----------|----------|--------------|
| `MockInsurerAdapter` | JSON | 100ms delay, synthetic refs |
| `IciciLombardAdapter` | REST+JSON | 150ms, `@CircuitBreaker(name="iciciLombard")` |
| `NivaBupaAdapter` | CSV/SFTP | Batch-only, `submitRealTime()` throws `UnsupportedOperationException` |
| `BajajAllianzAdapter` | SOAP/XML | 250ms, XML envelope generation |

**Context** — `ProcessEndorsementHandler.java`:
```java
InsurerPort adapter = insurerRouter.resolve(endorsement.getInsurerId());
if (adapter.getCapabilities().supportsRealTime()) {
    result = adapter.submitRealTime(endorsementId, endorsementData);
} else {
    // queue for batch
}
```

The handler doesn't know or care which adapter it's using. The strategy is selected at runtime by the factory (`InsurerRouter`).

#### When to Apply

Use Strategy when behaviour **varies by a runtime parameter** and you would otherwise write `if (insurerType == ICICI) { ... } else if (insurerType == BAJAJ) { ... }`. Instead: define an interface, implement variants, inject the right one.

**Do not**: Use `switch`/`if-else` on insurer type in handlers or controllers.

---

### HFDP-2: Observer Pattern (Ch. 2) — Domain Events via Kafka

**Book principle**: "Define a one-to-many dependency so that when one object changes state, all dependents are notified automatically."

#### How It Works Here

**Subject interface** — `domain/port/EventPublisher.java`:
```java
public interface EventPublisher {
    void publish(EndorsementEvent event);
}
```

**Concrete subject** — `infrastructure/messaging/KafkaEventPublisher.java`:
- Serialises the event to JSON
- Sends to Kafka topic with `employerId` as partition key
- Records success/failure metrics

**Observer** (downstream consumers) — receive events from Kafka topics. Currently `AuditLoggerConsumer` logs events. Future consumers: notification service, analytics pipeline, billing system.

#### When to Apply

Use Observer when a state change in one component needs to trigger reactions in other components **without tight coupling**. In this codebase, handlers publish events and don't know (or care) who consumes them.

**Do not**: Import consumer/notification classes into handlers. All cross-cutting reactions go through event publishing.

---

### HFDP-3: State Pattern (Ch. 10) — Endorsement Lifecycle

**Book principle**: "Allow an object to alter its behaviour when its internal state changes. The object will appear to change its class."

#### How It Works Here

**11-state lifecycle** — `domain/model/EndorsementStatus.java`:
```java
public enum EndorsementStatus {
    CREATED(EnumSet.of(LazyRef.VALIDATED)),
    VALIDATED(EnumSet.of(LazyRef.PROVISIONALLY_COVERED, LazyRef.SUBMITTED_TO_INSURER, ...)),
    PROVISIONALLY_COVERED(EnumSet.of(LazyRef.SUBMITTED_TO_INSURER, LazyRef.QUEUED_FOR_BATCH)),
    SUBMITTED_TO_INSURER(EnumSet.of(LazyRef.INSURER_PROCESSING, LazyRef.CONFIRMED, ...)),
    // ... 7 more states
    ;

    public boolean canTransitionTo(EndorsementStatus next) {
        return allowedTransitions.contains(LazyRef.valueOf(next.name()));
    }

    public boolean isTerminal() { return allowedTransitions.isEmpty(); }
    public boolean requiresInsurerAction() { ... }
}
```

**State machine service** — `domain/service/EndorsementStateMachine.java`:
```java
public Endorsement transition(Endorsement endorsement, EndorsementStatus newStatus) {
    endorsement.transitionTo(newStatus);  // validates & mutates
    return endorsement;
}
```

**Domain model encapsulation** — `domain/model/Endorsement.java`:
```java
public void transitionTo(EndorsementStatus newStatus) {
    if (!status.canTransitionTo(newStatus)) {
        throw new IllegalStateException("Cannot transition from " + status + " to " + newStatus);
    }
    this.status = newStatus;
    this.updatedAt = Instant.now();
}
```

#### When to Apply

Use State when an entity has a **lifecycle** with transition rules. Define valid transitions in the status enum; validate through the state machine. This codebase applies it to: `EndorsementStatus`, `BatchStatus`, `ReconciliationOutcome`, `ProvisionalCoverage` (`isActive`, `confirm`, `expire`).

**Do not**: Scatter transition logic across handlers. All transition rules live in the status enum. Handlers call `stateMachine.transition()`.

---

### HFDP-4: Adapter Pattern (Ch. 7) — Infrastructure Translations

**Book principle**: "Convert the interface of a class into another interface clients expect. Adapter lets classes work together that couldn't otherwise because of incompatible interfaces."

#### How It Works Here

The **entire infrastructure layer** is built on adapters. Each adapter translates between the domain's language and an infrastructure technology's language:

**JPA Repository Adapter** — `infrastructure/persistence/adapter/JpaEndorsementRepositoryAdapter.java`:
```java
@Component
@RequiredArgsConstructor
public class JpaEndorsementRepositoryAdapter implements EndorsementRepository {
    private final SpringDataEndorsementRepository springDataRepo;  // Spring Data
    private final EndorsementMapper mapper;                         // translator

    @Override
    public Endorsement save(Endorsement endorsement) {
        var entity = mapper.toEntity(endorsement);     // domain → JPA
        var saved = springDataRepo.save(entity);
        return mapper.toDomain(saved);                  // JPA → domain
    }
}
```

**Anti-Corruption Layer** — `infrastructure/persistence/mapper/EndorsementMapper.java`:
- `toDomain(EndorsementEntity)` — translates JPA strings to domain enums, JPA `@Entity` to domain model
- `toEntity(Endorsement)` — translates domain enums to JPA strings, `JsonNode` to JPA column types

The mapper is the **only place** that knows both types. Domain models never import JPA annotations; JPA entities never leak into handlers.

#### When to Apply

Every new infrastructure integration = new adapter implementing a domain port. The adapter's job: translate types, handle infrastructure errors, and shield the domain from technology details.

**Do not**: Let JPA entities, Kafka message types, or HTTP client response types appear in handler or domain code.

---

### HFDP-5: Factory Pattern (Ch. 4) — Adapter Resolution

**Book principle**: "Define an interface for creating objects, but let subclasses (or configuration) decide which class to instantiate."

#### How It Works Here

**InsurerRouter** — `infrastructure/insurer/InsurerRouter.java`:
```java
// Factory builds its product registry from Spring-discovered beans
public InsurerRouter(InsurerRegistry registry, List<InsurerPort> adapters) {
    this.adaptersByType = adapters.stream()
        .collect(Collectors.toMap(InsurerPort::getAdapterType, Function.identity()));
}

// Factory method — returns the right product for the input
public InsurerPort resolve(UUID insurerId) {
    InsurerConfiguration config = insurerRegistry.getConfiguration(insurerId);
    return adaptersByType.get(config.getAdapterType());
}
```

Spring's DI auto-discovers all `InsurerPort` beans and passes them as a `List<InsurerPort>`. The router indexes them by `getAdapterType()` and resolves the correct one at runtime via database configuration lookup.

**Adding a new product (insurer) requires zero factory changes.** Create the adapter class, add a DB config row, and Spring + the router handle the rest.

#### When to Apply

Use Factory when code needs to instantiate one of several possible implementations, and the choice is determined by runtime data (config, request parameters). Never `new` a strategy directly in a handler.

---

### HFDP-6: Template Method (Ch. 8) — Port Default Methods

**Book principle**: "Define the skeleton of an algorithm in a method, deferring some steps to subclasses."

#### How It Works Here

**InsurerPort default methods** — `domain/port/InsurerPort.java`:
```java
default String getAdapterType() { return "MOCK"; }
default Map<String, Object> mapToInsurerFormat(Map<String, Object> data) { return data; }
default Map<String, Object> mapFromInsurerFormat(Map<String, Object> data) { return data; }
```

`MockInsurerAdapter` uses these defaults (pass-through). `IciciLombardAdapter` overrides `getAdapterType()` to return `"ICICI_LOMBARD"` and `mapToInsurerFormat()` to transform field names. `BajajAllianzAdapter` overrides to wrap data in XML envelopes.

#### When to Apply

When adding a new port method that most implementations handle identically, add it as a `default` method on the interface. Only concrete adapters that need different behaviour override it. This keeps the interface backward-compatible.

---

### HFDP-7: Builder Pattern (Ch. Appendix) — Domain Object Construction

#### How It Works Here

Lombok `@Builder` on all domain models with many fields:
```java
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Endorsement {
    private UUID id;
    private UUID employerId;
    private UUID employeeId;
    private UUID insurerId;
    private EndorsementType type;
    private BigDecimal premiumAmount;
    // ...
}
```

Java `record` for immutable value objects:
```java
record SubmissionResult(boolean success, String insurerReference, String errorMessage) {}
record BatchPlan(List<Endorsement> included, List<Endorsement> deferred, BigDecimal projectedBalance) {}
record BalanceForecast(BigDecimal requiredMinimum, BigDecimal shortfall, boolean topUpRequired) {}
```

**Use `@Builder` for mutable domain entities. Use `record` for immutable value objects, DTOs, events, and port results.**

---

### HFDP-8: Design Principles — The Foundation

Head First summarizes these as principles that underpin all patterns. Here's how each applies:

| Principle | How This Codebase Applies It | Violation to Watch For |
|-----------|------------------------------|----------------------|
| **Encapsulate what varies** | Insurer-specific logic encapsulated in adapter classes behind `InsurerPort` | Adding insurer-specific `if/else` in handlers |
| **Favour composition over inheritance** | Handlers compose ports via DI; zero inheritance hierarchies among services | Creating `AbstractEndorsementHandler` base classes |
| **Program to interfaces** | All handler dependencies are port interfaces (`EndorsementRepository`, `InsurerPort`, `EventPublisher`) | Injecting `JpaEndorsementRepositoryAdapter` directly |
| **Strive for loosely coupled designs** | Handlers know nothing about Kafka, JPA, or HTTP protocols | Importing `org.springframework.kafka.*` in a handler |
| **Open/Closed Principle** | New insurer = new adapter class + DB row. Zero changes to handlers, domain, or router. | Modifying `ProcessEndorsementHandler` to handle a new insurer |
| **Dependency Inversion** | Domain defines ports; infrastructure implements them. High-level modules don't depend on low-level modules. | Domain model importing `jakarta.persistence.*` |
| **Single Responsibility** | Each handler = one use case. Each adapter = one technology. Each domain model = its own invariants. | Handler method doing validation + persistence + notification + logging in 120 lines |
| **Hollywood Principle** ("Don't call us, we'll call you") | Framework calls handlers; handlers publish events; consumers react. No polling from handlers. | Handler polling for insurer response in a loop |

---

## Part C: Hexagonal Architecture — Where Both Books Meet

The architecture of this codebase sits at the intersection of both books:

- **Head First Design Patterns** provides the structural vocabulary: Strategy (adapters), State (lifecycle), Adapter (translations), Observer (events), Factory (routing).
- **Cloud Native Patterns** provides the operational vocabulary: circuit breakers (resilience), event-driven (Kafka), distributed tracing (OpenTelemetry), stateless processes (scale-out), externalised config (12-factor).

```
                         ┌──────────────────────────────────────────────┐
                         │              API Layer                       │
                         │   Controllers (REST) ← DTOs (records)        │
                         └──────────────────┬───────────────────────────┘
                                            │ depends on
                         ┌──────────────────▼───────────────────────────┐
                         │           Application Layer                  │
                         │   Handlers (CQRS) ← Schedulers (cron)        │
                         │   [Stateless, @Transactional, MDC, Metrics]  │
                         └──────────────────┬───────────────────────────┘
                                            │ depends on
           ┌────────────────────────────────▼────────────────────────────────┐
           │                        Domain Core                              │
           │   Models: Endorsement, EAAccount (Rich, behavioural)            │
           │   Ports: EndorsementRepository, InsurerPort, EventPublisher     │
           │   Services: EndorsementStateMachine, EABalanceCalculator        │
           │   Events: EndorsementEvent (sealed, 22+ types)                  │
           │   [ZERO infrastructure imports]                                 │
           └────────────────────────────────┬────────────────────────────────┘
                                            │ implemented by
  ┌─────────────────────────────────────────▼──────────────────────────────────────┐
  │                           Infrastructure Layer                                  │
  │                                                                                 │
  │   ┌─ JPA Adapters ─────────────────┐  ┌─ Insurer Adapters ──────────────────┐  │
  │   │ JpaEndorsementRepositoryAdapter │  │ MockInsurerAdapter (@CircuitBreaker) │  │
  │   │ EndorsementMapper (ACL)         │  │ IciciLombardAdapter (@Retry)         │  │
  │   └────────────────────────────────┘  │ NivaBupaAdapter (batch-only)         │  │
  │                                        │ BajajAllianzAdapter (SOAP/XML)       │  │
  │   ┌─ Messaging ─────────────────┐    │ InsurerRouter (Factory)              │  │
  │   │ KafkaEventPublisher          │    └──────────────────────────────────────┘  │
  │   │ KafkaConfig (4 topics, 88p)  │                                              │
  │   └──────────────────────────────┘    ┌─ Observability ──────────────────────┐  │
  │                                        │ MdcRequestFilter (traceId, spanId)   │  │
  │   ┌─ Config ─────────────────────┐    │ RequestLoggingFilter (HTTP timing)   │  │
  │   │ SecurityConfig (STATELESS)    │    │ MetricsConfig (20+ custom metrics)   │  │
  │   │ CacheConfig (Caffeine, 60s)   │    │ EndorsementGaugeRegistrar (11 gauges)│  │
  │   └──────────────────────────────┘    └──────────────────────────────────────┘  │
  └────────────────────────────────────────────────────────────────────────────────┘
```

---

## Part D: Quick Reference — Adding New Features

When implementing a new feature, this checklist maps each step to the pattern that governs it:

| Step | What | Pattern Source |
|------|------|---------------|
| 1 | Domain model in `domain/model/` — rich, with behaviour | HFDP: Encapsulate what varies |
| 2 | Port interface in `domain/port/` — pure abstraction | HFDP: Program to interfaces; DIP |
| 3 | Domain service in `domain/service/` if it spans aggregates | HFDP: SRP |
| 4 | Adapter in `infrastructure/` implementing the port | HFDP: Adapter pattern |
| 5 | `@CircuitBreaker` + `@Retry` on external calls | CNP Ch. 10–11: Interaction redundancy |
| 6 | `@Cacheable` for read-heavy config data | CNP Ch. 9: Reduce downstream pressure |
| 7 | Handler in `application/handler/` — stateless, `@Transactional` | CNP Ch. 6: Stateless processes |
| 8 | Idempotency check for create operations | CNP Ch. 10: Safe retries |
| 9 | Event record in `EndorsementEvent` sealed interface | CNP Ch. 4: Event-carried state transfer |
| 10 | Publish event from handler | HFDP: Observer pattern |
| 11 | MDC context (`endorsementId`, `employerId`) in handler | CNP Ch. 12: Correlation IDs |
| 12 | Metrics: Timer for duration, Counter for events/errors | CNP Ch. 12: Observable systems |
| 13 | Controller + DTO (`record`) in `api/` | HFDP: SRP (thin controller) |
| 14 | Exception in `application/exception/` + `GlobalExceptionHandler` mapping | CNP Ch. 12: Meaningful error responses |
| 15 | Flyway migration in `db/migration/V{n}__description.sql` | CNP Ch. 13: Schema evolution |
| 16 | Config in `application.yml` with `${ENV_VAR:default}` | CNP Ch. 7: Externalised config |
| 17 | Unit test with `@ExtendWith(MockitoExtension.class)`, AssertJ, AAA | Both: Testable design proves loose coupling |

### Test Conventions

- **Class**: `{ClassUnderTest}Test`
- **Method**: `{method}_{scenario}_{expectedBehaviour}` — e.g., `submitToInsurer_circuitBreakerOpen_shouldReturnFallback`
- **Framework**: JUnit 5 + Mockito + AssertJ
- **Mocking**: `@Mock` for ports, `@Spy` for domain services with real logic, `@InjectMocks` on class under test
- **Assertions**: AssertJ only (`assertThat(...).isEqualTo(...)`, `assertThatThrownBy(...)`)
- **Pattern**: Arrange-Act-Assert with clear section separation

### Naming Conventions

| Layer | Class Pattern | Example |
|-------|--------------|---------|
| Domain model | Bare noun | `Endorsement`, `EAAccount` |
| Domain port | `{Entity}Repository`, `{Concept}Port` | `EndorsementRepository`, `InsurerPort` |
| Domain service | Descriptive noun | `EndorsementStateMachine`, `EABalanceCalculator` |
| Handler | `{Action}Handler` | `CreateEndorsementHandler` |
| Controller | `{Entity}Controller` | `EndorsementController` |
| JPA entity | `{Entity}Entity` | `EndorsementEntity` |
| JPA adapter | `Jpa{Entity}RepositoryAdapter` | `JpaEndorsementRepositoryAdapter` |
| Insurer adapter | `{InsurerName}Adapter` | `IciciLombardAdapter` |
| DTO | `{Action}{Entity}Request`, `{Entity}Response` | `CreateEndorsementRequest` |
| Event | `EndorsementEvent.{PastTenseVerb}` | `EndorsementEvent.Confirmed` |
| Exception | `{Context}Exception` | `EndorsementNotFoundException` |
| Config | `{Concern}Config` | `KafkaConfig`, `SecurityConfig` |
| Mapper | `{Entity}Mapper` | `EndorsementMapper` |

### Dependency Injection

- Always `private final` fields + `@RequiredArgsConstructor`
- Never `@Autowired` or field injection
- `@Component` for domain services, mappers, adapters
- `@Service` for handlers
- `@RestController` for controllers
- `@Configuration` for Spring config
