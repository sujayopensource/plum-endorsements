package com.plum.endorsements.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture fitness functions enforcing event-driven patterns.
 *
 * <p>Rules derived from CLAUDE.md CNP-1 (Event-Driven Microservices) and
 * HFDP-2 (Observer Pattern).
 *
 * <p>Key invariants:
 * <ul>
 *   <li>Events live in domain.model (sealed interface)</li>
 *   <li>Handlers publish events through EventPublisher port, not directly to Kafka</li>
 *   <li>Infrastructure adapters never publish events autonomously</li>
 *   <li>Kafka classes confined to infrastructure.messaging</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.plum.endorsements",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class EventDrivenArchitectureFitnessFunction {

    // ── Kafka Encapsulation ──────────────────────────────────────────────

    @ArchTest
    static final ArchRule kafka_imports_confined_to_messaging_package =
            noClasses()
                    .that().resideOutsideOfPackages(
                            "..infrastructure.messaging..",
                            "..infrastructure.config.."
                    )
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.kafka..",
                            "org.apache.kafka.."
                    )
                    .as("Kafka imports must be confined to infrastructure.messaging and infrastructure.config")
                    .because("Kafka is an infrastructure concern; handlers use the EventPublisher port " +
                            "(CNP-1: Events published from handlers through ports, not adapters)");

    // ── Event Model Boundaries ───────────────────────────────────────────

    @ArchTest
    static final ArchRule event_classes_reside_in_domain_model =
            classes()
                    .that().haveSimpleNameEndingWith("Event")
                    .and().resideInAPackage("..domain..")
                    .should().resideInAPackage("..domain.model..")
                    .as("Event classes in domain must reside in domain.model package")
                    .because("Events are domain facts — part of the ubiquitous language " +
                            "(CNP-1: Sealed event hierarchy in domain.model)");

    // ── Port-Based Event Publishing ──────────────────────────────────────

    @ArchTest
    static final ArchRule domain_services_must_not_use_kafka_directly =
            noClasses()
                    .that().resideInAPackage("..domain.service..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.kafka..",
                            "org.apache.kafka..",
                            "..infrastructure.messaging.."
                    )
                    .as("Domain services must not use Kafka directly")
                    .because("Domain services are technology-agnostic; " +
                            "event publishing goes through the EventPublisher port");

    @ArchTest
    static final ArchRule schedulers_must_not_use_kafka_directly =
            noClasses()
                    .that().resideInAPackage("..application.scheduler..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.kafka..",
                            "org.apache.kafka.."
                    )
                    .as("Schedulers must not import Kafka classes directly")
                    .because("Schedulers orchestrate domain operations; " +
                            "events flow through handlers and the EventPublisher port");

    // ── Insurer Adapters Must Not Publish Events ─────────────────────────

    @ArchTest
    static final ArchRule insurer_adapters_must_not_depend_on_event_publisher =
            noClasses()
                    .that().resideInAPackage("..infrastructure.insurer..")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.plum.endorsements.domain.port.EventPublisher")
                    .as("Insurer adapters must not depend on EventPublisher")
                    .because("Infrastructure adapters must never publish events autonomously; " +
                            "events are published from handlers after domain operations succeed " +
                            "(CNP-1 Rule 4)");
}
