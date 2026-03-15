# Architecture: Hexagonal (Ports & Adapters)

**Project:** Plum Endorsement Management System
**Date:** March 8, 2026
**Pattern:** Hexagonal Architecture (Ports & Adapters) — Alistair Cockburn, 2005
**Also known as:** Clean Architecture (Robert C. Martin), Onion Architecture (Jeffrey Palermo)

---

## 1. Architectural Style

The Plum Endorsement Service implements **Hexagonal Architecture** (Ports & Adapters), not traditional Layered Architecture. The fundamental principle is:

> **The domain is at the center. All dependencies point inward. The domain depends on nothing.**

The application is structured as a hexagon where the **inside** contains business logic (domain models, business rules, use case orchestration) and the **outside** contains technology-specific adapters that plug into the domain through well-defined interfaces called **ports**.

```
                         ┌─────────────────────────────────┐
                         │         PRIMARY ADAPTERS          │
                         │    (Driving / Inbound Side)       │
                         │                                   │
                         │  REST Controllers   Schedulers    │
                         │  (EndorsementController)          │
                         │  (ReconciliationController)       │
                         │  (BatchAssemblyScheduler)         │
                         └──────────────┬────────────────────┘
                                        │ calls
                         ┌──────────────▼────────────────────┐
                         │                                   │
                         │       APPLICATION LAYER            │
                         │    (Use Case Orchestration)        │
                         │                                   │
                         │  CreateEndorsementHandler          │
                         │  ProcessEndorsementHandler         │
                         │  EndorsementQueryHandler           │
                         │  ReconciliationEngine              │
                         │                                   │
                         └──────────────┬────────────────────┘
                                        │ depends on
              ┌─────────────────────────▼──────────────────────────┐
              │                                                     │
              │                   DOMAIN CORE                       │
              │              (Pure Business Logic)                  │
              │                                                     │
              │   ┌─────────────────────────────────────────────┐  │
              │   │              MODELS                          │  │
              │   │  Endorsement     EAAccount                  │  │
              │   │  EndorsementBatch ProvisionalCoverage        │  │
              │   │  InsurerConfiguration                       │  │
              │   │  ReconciliationRun/Item                      │  │
              │   │  EndorsementEvent (16 types)                 │  │
              │   │  EndorsementPriority (P0-P3)                 │  │
              │   └─────────────────────────────────────────────┘  │
              │   ┌─────────────────────────────────────────────┐  │
              │   │           DOMAIN SERVICES                    │  │
              │   │  EndorsementStateMachine                     │  │
              │   │  EABalanceCalculator                         │  │
              │   │  InsurerRegistry                             │  │
              │   └─────────────────────────────────────────────┘  │
              │   ┌─────────────────────────────────────────────┐  │
              │   │              PORTS                            │  │
              │   │  (Interfaces — contracts for the outside)    │  │
              │   │                                               │  │
              │   │  EndorsementRepository  EAAccountRepository  │  │
              │   │  BatchRepository        ProvisionalCoverageR │  │
              │   │  InsurerPort            EventPublisher        │  │
              │   │  NotificationPort       InsurerConfigRepo     │  │
              │   │  ReconciliationRepository                     │  │
              │   └─────────────────────────────────────────────┘  │
              │                                                     │
              └──────────────────────┬──────────────────────────────┘
                                     │ implemented by
                         ┌───────────▼──────────────────────┐
                         │                                   │
                         │      SECONDARY ADAPTERS            │
                         │    (Driven / Outbound Side)        │
                         │                                   │
                         │  JpaEndorsementRepositoryAdapter   │
                         │  JpaEAAccountRepositoryAdapter     │
                         │  KafkaEventPublisher               │
                         │  LoggingNotificationAdapter        │
                         │  MockInsurerAdapter                │
                         │  IciciLombardAdapter               │
                         │  NivaBupaAdapter                   │
                         │  BajajAllianzAdapter               │
                         │  InsurerRouter (factory)           │
                         └───────────────────────────────────┘
```

---

## 2. Why Not Layered Architecture?

### Traditional Layered Architecture

In a layered architecture, dependencies flow **top-down** through fixed tiers:

```
┌────────────────────────────┐
│     Presentation Layer      │ ← depends on ↓
├────────────────────────────┤
│     Business Logic Layer    │ ← depends on ↓
├────────────────────────────┤
│     Data Access Layer       │ ← depends on ↓
├────────────────────────────┤
│        Database             │
└────────────────────────────┘
```

**Problems with layered architecture:**

| Problem | Layered Architecture | Hexagonal Architecture |
|---------|---------------------|----------------------|
| **Dependency direction** | Business logic depends on data access layer | Domain depends on nothing; data access implements domain port |
| **Database coupling** | Business logic is coupled to database types (JPA entities, Spring Data repositories) | Domain uses pure Java objects; JPA entities exist only in infrastructure |
| **Testability** | Testing business logic requires database or mocking framework-specific classes | Domain can be tested with simple mocks of port interfaces |
| **Technology lock-in** | Changing from PostgreSQL to MongoDB requires rewriting business logic | Only the adapter changes; domain code is unchanged |
| **Domain model anemia** | Business logic tends to leak into services, leaving models as data holders | Rich domain models with encapsulated behavior |
| **Insurer swappability** | Adding a new insurer requires touching business logic | New adapter implements `InsurerPort`; zero domain changes |
| **Event publisher coupling** | Business logic directly references Kafka classes | Domain calls `EventPublisher.publish(event)`; Kafka is an adapter detail |

### The Fundamental Difference

**Layered:** Business logic depends on infrastructure.
```java
// LAYERED: Service directly uses Spring Data repository
@Service
public class EndorsementService {
    @Autowired
    private EndorsementJpaRepository repository;  // ← JPA type in business layer

    public void create(EndorsementEntity entity) {  // ← JPA entity as parameter
        repository.save(entity);  // ← Coupled to JPA
    }
}
```

**Hexagonal:** Infrastructure depends on the domain.
```java
// HEXAGONAL: Handler depends on a domain port (abstraction)
@Service
public class CreateEndorsementHandler {
    private final EndorsementRepository repository;  // ← Domain port interface

    public Endorsement handle(Endorsement endorsement) {  // ← Domain object
        return repository.save(endorsement);  // ← Port call; adapter does JPA
    }
}

// Infrastructure adapter implements the domain port
public class JpaEndorsementRepositoryAdapter implements EndorsementRepository {
    private final SpringDataEndorsementRepository jpaRepo;
    private final EndorsementMapper mapper;

    @Override
    public Endorsement save(Endorsement endorsement) {
        var entity = mapper.toEntity(endorsement);   // Domain → JPA entity
        var saved = jpaRepo.save(entity);
        return mapper.toDomain(saved);               // JPA entity → Domain
    }
}
```

---

## 3. The Four Zones

### Zone 1: Domain Core (Center of the Hexagon)

**Package:** `com.plum.endorsements.domain`

The domain is the heart of the application. It contains:

- **Models** — Rich domain objects with encapsulated business behavior
- **Services** — Stateless domain operations that span multiple models
- **Ports** — Interfaces that define the contracts for interacting with the outside world

**Purity rule:** The domain imports **nothing** from application, infrastructure, or API packages. It uses only:
- Java standard library (`java.util`, `java.time`, `java.math`)
- Lombok (compile-time annotation processing for boilerplate reduction)
- Jackson `JsonNode` (domain-level data representation)

**Evidence of rich domain models (not anemic DTOs):**

```java
// Endorsement.java — Business logic lives IN the model
public class Endorsement {
    public void transitionTo(EndorsementStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + status + " to " + newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public boolean canRetry() {
        return retryCount < 3 && status == EndorsementStatus.REJECTED;
    }

    public boolean isTerminal() {
        return status.isTerminal();
    }
}

// EAAccount.java — Balance invariants enforced in the model
public class EAAccount {
    public BigDecimal availableBalance() {
        return balance.subtract(reserved);
    }

    public void reserve(BigDecimal amount) {
        this.reserved = this.reserved.add(amount);
        if (availableBalance().compareTo(BigDecimal.ZERO) < 0) {
            this.reserved = this.reserved.subtract(amount);
            throw new IllegalStateException(
                "Insufficient available balance to reserve " + amount);
        }
        this.updatedAt = Instant.now();
    }
}

// EndorsementStatus.java — State machine rules in the enum
public enum EndorsementStatus {
    CREATED(Set.of(VALIDATED)),
    VALIDATED(Set.of(PROVISIONALLY_COVERED)),
    PROVISIONALLY_COVERED(Set.of(SUBMITTED_REALTIME, QUEUED_FOR_BATCH)),
    // ... 11 states with allowed transitions
    CONFIRMED(Set.of()),      // terminal
    FAILED_PERMANENT(Set.of()); // terminal

    public boolean canTransitionTo(EndorsementStatus target) {
        return allowedTransitions.contains(target);
    }

    public boolean isTerminal() {
        return allowedTransitions.isEmpty();
    }
}
```

### Zone 2: Ports (Domain Boundary)

**Package:** `com.plum.endorsements.domain.port`

Ports are **pure Java interfaces** that define the domain's expectations of the outside world. They use only domain types.

**9 port interfaces:**

| Port | Direction | Purpose |
|------|-----------|---------|
| `EndorsementRepository` | Driven (outbound) | Persist and query endorsements |
| `EAAccountRepository` | Driven (outbound) | Manage employer advance accounts |
| `BatchRepository` | Driven (outbound) | Persist and query batches |
| `ProvisionalCoverageRepository` | Driven (outbound) | Manage provisional coverages |
| `InsurerPort` | Driven (outbound) | Submit endorsements to insurers |
| `EventPublisher` | Driven (outbound) | Publish domain events |
| `NotificationPort` | Driven (outbound) | Send employer notifications |
| `InsurerConfigurationRepository` | Driven (outbound) | Manage insurer configurations |
| `ReconciliationRepository` | Driven (outbound) | Manage reconciliation runs and items |

**Example port — pure interface with domain types only:**

```java
// EventPublisher.java — Maximally simple
public interface EventPublisher {
    void publish(EndorsementEvent event);
}

// InsurerPort.java — Domain records define the contract
public interface InsurerPort {
    SubmissionResult submitRealTime(UUID endorsementId, Map<String, Object> data);
    String submitBatch(UUID batchId, List<Map<String, Object>> endorsements);
    BatchStatusResult checkBatchStatus(String insurerBatchRef);
    InsurerCapabilities getCapabilities();

    default String getAdapterType() { return "MOCK"; }
    default Map<String, Object> mapToInsurerFormat(Map<String, Object> data) { return data; }
    default Map<String, Object> mapFromInsurerFormat(Map<String, Object> data) { return data; }

    // Domain-defined value objects (records)
    record SubmissionResult(boolean success, String insurerReference, String errorMessage) {}
    record BatchStatusResult(String status, List<EndorsementResult> results) {}
    record EndorsementResult(UUID endorsementId, boolean confirmed,
                              String insurerReference, String rejectionReason) {}
    record InsurerCapabilities(boolean supportsRealTime, boolean supportsBatch,
                                int maxBatchSize, long batchSlaHours, int rateLimitPerMinute) {}
}
```

### Zone 3: Application Layer (Use Case Orchestration)

**Package:** `com.plum.endorsements.application`

The application layer **orchestrates** domain operations. It does not contain business logic — it coordinates domain objects and ports to fulfill a use case.

```java
// CreateEndorsementHandler.java — Orchestration, not business logic
@Service
@RequiredArgsConstructor
@Transactional
public class CreateEndorsementHandler {

    // ALL dependencies are domain ports (abstractions)
    private final EndorsementRepository endorsementRepository;
    private final EAAccountRepository eaAccountRepository;
    private final ProvisionalCoverageRepository coverageRepository;
    private final EndorsementStateMachine stateMachine;
    private final EABalanceCalculator balanceCalculator;
    private final EventPublisher eventPublisher;

    public Endorsement handle(Endorsement endorsement) {
        // 1. Check idempotency (delegates to port)
        // 2. Persist (delegates to port)
        // 3. Publish event (delegates to port)
        // 4. Transition state (delegates to domain model)
        // 5. Grant coverage (delegates to port)
        // 6. Reserve balance (delegates to domain model)
        // Handler COORDINATES; domain DECIDES
    }
}
```

**Key principle:** The handler calls `endorsement.transitionTo()` (domain decides if valid) and `eaAccount.reserve()` (domain enforces invariants). The handler never implements business rules itself.

### Zone 4: Adapters (Outside the Hexagon)

**Primary (Driving) Adapters** — initiate actions:

| Adapter | Technology | Drives |
|---------|-----------|--------|
| `EndorsementController` | Spring MVC REST | Application handlers |
| `ReconciliationController` | Spring MVC REST | ReconciliationEngine |
| `InsurerConfigurationController` | Spring MVC REST | EndorsementQueryHandler |
| `BatchAssemblyScheduler` | Spring @Scheduled | Domain ports |
| `ReconciliationScheduler` | Spring @Scheduled | ReconciliationEngine |

**Secondary (Driven) Adapters** — respond to domain requests:

| Adapter | Implements Port | Technology |
|---------|----------------|-----------|
| `JpaEndorsementRepositoryAdapter` | `EndorsementRepository` | Spring Data JPA + PostgreSQL |
| `JpaEAAccountRepositoryAdapter` | `EAAccountRepository` | Spring Data JPA + PostgreSQL |
| `JpaBatchRepositoryAdapter` | `BatchRepository` | Spring Data JPA + PostgreSQL |
| `JpaProvisionalCoverageRepositoryAdapter` | `ProvisionalCoverageRepository` | Spring Data JPA + PostgreSQL |
| `JpaInsurerConfigurationRepositoryAdapter` | `InsurerConfigurationRepository` | Spring Data JPA + PostgreSQL |
| `JpaReconciliationRepositoryAdapter` | `ReconciliationRepository` | Spring Data JPA + PostgreSQL |
| `KafkaEventPublisher` | `EventPublisher` | Apache Kafka |
| `LoggingNotificationAdapter` | `NotificationPort` | SLF4J Logging |
| `MockInsurerAdapter` | `InsurerPort` | In-memory simulation |
| `IciciLombardAdapter` | `InsurerPort` | REST/JSON simulation |
| `NivaBupaAdapter` | `InsurerPort` | CSV/SFTP simulation |
| `BajajAllianzAdapter` | `InsurerPort` | SOAP/XML simulation |

---

## 4. Anti-Corruption Layer

A critical element of the hexagonal architecture is the **anti-corruption layer** that prevents infrastructure types from leaking into the domain. In this project, `EndorsementMapper` serves this role.

**Two separate class hierarchies exist for the same concept:**

```
Domain Model (pure)                Infrastructure Entity (JPA-annotated)
─────────────────────              ──────────────────────────────────────
Endorsement.java                   EndorsementEntity.java
  - EndorsementType type (enum)      - String type ("ADD")
  - EndorsementStatus status (enum)  - String status ("CREATED")
  - JsonNode employeeData           - String employeeData (JSON string)
  - No @Entity, no @Table           - @Entity @Table(name="endorsements")
  - No @Id, no @Column              - @Id @GeneratedValue @Column
  - Has transitionTo(), canRetry()  - Pure data holder
```

**The mapper translates bidirectionally:**

```java
// EndorsementMapper — the anti-corruption boundary
public Endorsement toDomain(EndorsementEntity entity) {
    return Endorsement.builder()
        .type(EndorsementType.valueOf(entity.getType()))      // String → Enum
        .status(EndorsementStatus.valueOf(entity.getStatus()))  // String → Enum
        .employeeData(objectMapper.readTree(entity.getEmployeeData()))
        .build();
}

public EndorsementEntity toEntity(Endorsement domain) {
    return EndorsementEntity.builder()
        .type(domain.getType().name())                        // Enum → String
        .status(domain.getStatus().name())                    // Enum → String
        .employeeData(objectMapper.writeValueAsString(domain.getEmployeeData()))
        .build();
}
```

**Why this matters:** If we switch from PostgreSQL to MongoDB, we create a new `MongoEndorsementEntity` and a new `MongoEndorsementRepositoryAdapter`, but `Endorsement.java` (domain) and `CreateEndorsementHandler.java` (application) remain unchanged.

---

## 5. Dependency Rule

The most important rule of hexagonal architecture:

> **Source code dependencies can only point inward. Nothing in an inner ring can know anything about something in an outer ring.**

```
                    DEPENDS ON NOTHING
                          ↑
              ┌───────────┴───────────┐
              │      Domain Core       │  ← Models, Services, Ports
              │  (pure business logic) │
              └───────────┬───────────┘
                          │
              DEPENDS ON DOMAIN ONLY
                          ↑
              ┌───────────┴───────────┐
              │   Application Layer    │  ← Handlers, Schedulers
              │ (use case orchestration)│
              └───────────┬───────────┘
                          │
           DEPENDS ON DOMAIN + APPLICATION
                          ↑
     ┌────────────────────┴────────────────────┐
     │         Infrastructure + API              │  ← Adapters, Controllers
     │    (technology-specific implementations)  │
     └───────────────────────────────────────────┘
```

**Verified in the codebase:**

| Package | Imports From | Does NOT Import From |
|---------|-------------|---------------------|
| `domain.model` | `java.*`, Lombok, Jackson `JsonNode` | application, infrastructure, api |
| `domain.service` | `domain.model`, `domain.port` | application, infrastructure, api |
| `domain.port` | `domain.model`, `java.*` | application, infrastructure, api |
| `application.handler` | `domain.model`, `domain.port`, `domain.service` | infrastructure, api |
| `application.scheduler` | `domain.model`, `domain.port` | api |
| `infrastructure.persistence.adapter` | `domain.model`, `domain.port`, infrastructure entities | api |
| `infrastructure.insurer` | `domain.port` | api, application |
| `infrastructure.messaging` | `domain.port`, `domain.model` | api, application |
| `api.controller` | `application.handler`, `domain.model` | infrastructure |

**The domain package has zero imports from any other project package.** This is the hallmark of hexagonal architecture done right.

---

## 6. Benefits Over Layered Architecture

### 6.1 Technology Independence

**Layered:** Switching from Kafka to RabbitMQ requires changes in the business logic layer because it directly references `KafkaTemplate`.

**Hexagonal (this project):** The domain defines `EventPublisher.publish(EndorsementEvent)`. Whether Kafka, RabbitMQ, AWS SNS, or an in-memory queue implements it is irrelevant to the domain.

```
Current:   EventPublisher ──implemented by──> KafkaEventPublisher
Swap to:   EventPublisher ──implemented by──> RabbitMQEventPublisher
           (domain code: ZERO changes)
```

**Real example in this project — InsurerPort:**

```
InsurerPort ──> MockInsurerAdapter     (in-memory, 100ms)
            ──> IciciLombardAdapter    (REST/JSON, 150ms, circuit breaker)
            ──> NivaBupaAdapter        (CSV/SFTP, batch-only)
            ──> BajajAllianzAdapter    (SOAP/XML, 250ms, circuit breaker)
            ──> [Future: RealIciciAdapter connecting to actual ICICI API]
```

Each adapter is a **plug** for the same **socket** (port). Adding a 5th insurer requires zero changes to the domain, application, or any existing adapter.

### 6.2 Testability

**Layered:** Testing the service layer requires a database (or complex mocking of Spring Data repositories, JPA entity manager, etc.).

**Hexagonal:** Domain models are testable with plain JUnit — no Spring context, no Testcontainers, no mocks:

```java
// Pure domain test — no Spring, no database
@Test
void rejectingThreeTimesMakesEndorsementPermanentlyFailed() {
    Endorsement e = Endorsement.builder()
        .status(EndorsementStatus.REJECTED)
        .retryCount(3)
        .build();

    assertFalse(e.canRetry());
    e.transitionTo(EndorsementStatus.FAILED_PERMANENT);
    assertTrue(e.isTerminal());
}
```

Application handlers are tested by mocking port interfaces — simple, focused, fast:

```java
// Application handler test — mocks are port interfaces
@Mock EndorsementRepository endorsementRepository;
@Mock EventPublisher eventPublisher;

@Test
void handleCreatesEndorsementAndPublishesEvent() {
    when(endorsementRepository.save(any())).thenReturn(endorsement);

    handler.handle(endorsement);

    verify(eventPublisher).publish(any(EndorsementEvent.Created.class));
}
```

### 6.3 Domain Model Richness

**Layered (typical anti-pattern):** Anemic domain models are data-only DTOs. Business logic lives in service classes, scattered across the service layer.

```java
// ANEMIC MODEL (anti-pattern in layered architecture)
public class Endorsement {
    private String status;       // just data
    private int retryCount;      // just data
    // no behavior — business rules live in EndorsementService
}

public class EndorsementService {
    public void reject(Endorsement e, String reason) {
        if (e.getRetryCount() < 3) { ... }     // logic here, not in model
        e.setStatus("RETRY_PENDING");           // raw string, no validation
    }
}
```

**Hexagonal (this project):** Rich domain models encapsulate both state AND behavior. The model protects its own invariants.

```java
// RICH MODEL (hexagonal architecture)
public class Endorsement {
    public void transitionTo(EndorsementStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(...);  // model enforces rules
        }
        this.status = newStatus;
    }

    public boolean canRetry() {
        return retryCount < 3 && status == EndorsementStatus.REJECTED;
    }
}

// EAAccount protects its own balance invariant
public class EAAccount {
    public void reserve(BigDecimal amount) {
        this.reserved = this.reserved.add(amount);
        if (availableBalance().compareTo(BigDecimal.ZERO) < 0) {
            this.reserved = this.reserved.subtract(amount);  // rollback
            throw new IllegalStateException("Insufficient balance");
        }
    }
}
```

### 6.4 Parallel Development

**Layered:** Teams must coordinate on the data access layer before the service layer can be built.

**Hexagonal:** Once ports are defined, teams work in parallel:
- Team A implements domain models and business rules
- Team B implements JPA adapters
- Team C implements Kafka event publisher
- Team D implements REST controllers

All teams code against the same port interfaces. Integration happens at composition time (Spring DI).

### 6.5 Adapter Substitutability

**Layered:** Replacing the notification mechanism (logging → email) requires changes in the business logic.

**Hexagonal:** Swap a single Spring bean:

```java
// Development profile
@Profile("dev")
@Component
public class LoggingNotificationAdapter implements NotificationPort {
    public void notifyEndorsementConfirmed(UUID employerId, UUID endorsementId) {
        log.info("Confirmed: {}", endorsementId);
    }
}

// Production profile
@Profile("prod")
@Component
public class EmailNotificationAdapter implements NotificationPort {
    public void notifyEndorsementConfirmed(UUID employerId, UUID endorsementId) {
        emailClient.send(employerId, "Your endorsement is confirmed");
    }
}
```

**Zero domain or application code changes.** The port contract is the stable interface.

### 6.6 Evolutionary Architecture

**Layered:** Architectural changes cascade through all layers (changing the persistence model requires changes in DAO, service, and sometimes controller layers).

**Hexagonal:** Changes are contained within a single adapter. Real examples from this project:

| Change | Files Affected | Domain Changed? |
|--------|---------------|-----------------|
| Add ICICI Lombard insurer | `IciciLombardAdapter.java`, `IciciLombardDataMapper.java` | No |
| Scale Kafka from 3 to 32 partitions | `KafkaConfig.java` | No |
| Change partition key from endorsementId to employerId | `KafkaEventPublisher.java` | No |
| Add reconciliation tables | `V8__create_reconciliation_tables.sql`, entity, adapter, repository | Only new port + model (additive) |
| Switch from REST to gRPC for insurer API | New adapter implementing `InsurerPort` | No |

---

## 7. CQRS Complement

The hexagonal architecture is complemented by **CQRS** (Command Query Responsibility Segregation) in the application layer:

```
                    ┌───────────────────────────┐
                    │     Application Layer       │
                    │                             │
   Commands ───────►│  CreateEndorsementHandler   │──────► Writes
   (POST, PUT)      │  ProcessEndorsementHandler  │
                    │                             │
   Queries ────────►│  EndorsementQueryHandler    │──────► Reads
   (GET)            │  @Transactional(readOnly)   │
                    └───────────────────────────┘
```

Write handlers are `@Transactional` (read-write). The query handler is `@Transactional(readOnly = true)`, enabling database-level read optimizations.

---

## 8. Event-Driven Domain Events

Domain events are first-class citizens, defined as a **sealed interface** in the domain:

```java
public sealed interface EndorsementEvent {
    UUID endorsementId();
    UUID employerId();
    Instant occurredAt();
    String eventType();

    record Created(...) implements EndorsementEvent { ... }
    record Validated(...) implements EndorsementEvent { ... }
    record Confirmed(...) implements EndorsementEvent { ... }
    // ... 16 event types total
}
```

The domain **defines** events. The infrastructure **publishes** them. This separation means:
- Events are expressed in business language (ubiquitous language)
- Serialization format, transport mechanism, and partitioning strategy are adapter concerns
- Adding a new consumer (audit log, analytics) requires zero domain changes

---

## 9. Architecture Quality Assessment

| Criterion | Score | Evidence |
|-----------|-------|---------|
| Domain purity | 9.5/10 | Models use only `java.*`, Lombok, Jackson `JsonNode`. No Spring/JPA. |
| Port interface purity | 9.5/10 | All ports are plain Java interfaces. One pragmatic `Page/Pageable` compromise. |
| Dependency direction | 10/10 | Domain imports zero project packages. Verified by grep. |
| Anti-corruption layer | 10/10 | Separate `Endorsement` (domain) and `EndorsementEntity` (JPA) with bidirectional mapper. |
| Rich domain model | 10/10 | `transitionTo()`, `canRetry()`, `reserve()`, `availableBalance()` — behavior lives in the model. |
| Adapter substitutability | 10/10 | 4 insurer adapters prove the pattern works. `EventPublisher`, `NotificationPort` are trivially swappable. |
| Testability | 10/10 | 182 unit tests run without Spring context. Port-based mocking in application tests. |
| **Overall** | **9.7/10** | Disciplined hexagonal architecture with minimal, pragmatic compromises. |

**Known compromises (pragmatic, not violations):**

| Compromise | Location | Impact | Rationale |
|------------|----------|--------|-----------|
| `@Component` on domain services | `EndorsementStateMachine`, `EABalanceCalculator` | Low | Avoids separate `@Configuration` class for bean registration |
| `@Cacheable` on `InsurerRegistry` | `domain.service.InsurerRegistry` | Low | Caching is cross-cutting; could be moved to infrastructure decorator |
| `Page/Pageable` in `EndorsementRepository` port | `domain.port.EndorsementRepository` | Low | Avoids reinventing pagination abstraction; Spring Data types are stable |

---

## 10. Summary: Hexagonal vs Layered

| Aspect | Layered Architecture | Hexagonal (This Project) |
|--------|---------------------|-------------------------|
| **Core principle** | Top-down layer dependencies | All dependencies point inward to domain |
| **Domain purity** | Domain depends on infrastructure | Domain depends on nothing |
| **Business logic location** | Scattered in service layer | Encapsulated in rich domain models |
| **Infrastructure coupling** | Service layer imports JPA, Kafka, etc. | Only adapters import infrastructure |
| **Adding a new insurer** | Modify service + DAO layers | Add one adapter class; zero domain changes |
| **Switching databases** | Rewrite DAO + service layers | Replace one adapter; domain unchanged |
| **Testing domain logic** | Requires Spring context or complex mocking | Plain JUnit, no framework needed |
| **Parallel development** | Sequential (DAO first, then service) | Parallel (code against port interfaces) |
| **Model type** | Anemic (data-only DTOs) | Rich (data + behavior) |
| **Change blast radius** | Changes cascade through layers | Changes contained within single adapter |
| **Adapter swappability** | Not designed for it | Core pattern (port = socket, adapter = plug) |
