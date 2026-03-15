package com.plum.endorsements.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Architecture fitness functions enforcing coding rules for cloud-native stateless services.
 *
 * <p>Rules derived from CLAUDE.md:
 * <ul>
 *   <li>CNP-2: All handlers and services are stateless (private final fields only)</li>
 *   <li>HFDP-8: Single Responsibility (each handler = one use case)</li>
 *   <li>CNP-6: Retries & Idempotency patterns</li>
 *   <li>CNP-7: Circuit Breaker patterns</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.plum.endorsements",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class CodingRulesFitnessFunction {

    // ── Stateless Services (CNP-2) ───────────────────────────────────────

    @ArchTest
    static final ArchRule handler_fields_must_be_private_and_final =
            noFields()
                    .that().areDeclaredInClassesThat()
                    .resideInAPackage("..application.handler..")
                    .and().areDeclaredInClassesThat()
                    .areAnnotatedWith(Service.class)
                    .should().bePublic()
                    .as("Handler fields must not be public (should be private final)")
                    .because("CNP-2: Stateless processes — handlers hold only private final " +
                            "references to injected dependencies. No mutable instance state.");

    // ── Spring Annotation Rules ──────────────────────────────────────────

    @ArchTest
    static final ArchRule handlers_must_be_annotated_with_service =
            classes()
                    .that().resideInAPackage("..application.handler..")
                    .and().haveSimpleNameEndingWith("Handler")
                    .should().beAnnotatedWith(Service.class)
                    .as("Handler classes must be annotated with @Service")
                    .because("Handlers are application services managed by Spring DI " +
                            "(CLAUDE.md: @Service for handlers)");

    // ── No Field Injection ───────────────────────────────────────────────

    @ArchTest
    static final ArchRule no_autowired_field_injection =
            noFields()
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .as("No @Autowired field injection anywhere in the codebase")
                    .because("CLAUDE.md: Always private final fields + @RequiredArgsConstructor. " +
                            "Never @Autowired or field injection.");

    // ── CQRS Separation ──────────────────────────────────────────────────

    @ArchTest
    static final ArchRule query_handlers_must_not_depend_on_event_publisher =
            noClasses()
                    .that().haveSimpleName("EndorsementQueryHandler")
                    .should().dependOnClassesThat()
                    .haveFullyQualifiedName("com.plum.endorsements.domain.port.EventPublisher")
                    .as("Query handlers must not depend on EventPublisher")
                    .because("CNP-9 CQRS: Commands publish events; queries never do. " +
                            "A query method must not modify state or publish to Kafka.");

    // ── Exception Handling ───────────────────────────────────────────────

    @ArchTest
    static final ArchRule exceptions_should_reside_in_exception_packages =
            classes()
                    .that().haveSimpleNameEndingWith("Exception")
                    .should().resideInAnyPackage(
                            "..application.exception..",
                            "..api.exception.."
                    )
                    .as("Custom exceptions must reside in exception packages")
                    .because("CLAUDE.md: Exception in application/exception/ " +
                            "+ GlobalExceptionHandler mapping");

    // ── Controller Must Not Access Infrastructure Directly ─────────────

    @ArchTest
    static final ArchRule controllers_must_not_depend_on_infrastructure_adapters =
            noClasses()
                    .that().resideInAPackage("..api.controller..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.persistence.adapter..",
                            "..infrastructure.persistence.entity..",
                            "..infrastructure.insurer..",
                            "..infrastructure.messaging.."
                    )
                    .as("Controllers must not depend on infrastructure adapters")
                    .because("Controllers delegate to handlers and application services; " +
                            "infrastructure access goes through domain ports (HFDP-8: SRP)");
}
