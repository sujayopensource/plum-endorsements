package com.plum.endorsements.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

/**
 * Architecture fitness functions enforcing hexagonal (ports-and-adapters) layer boundaries.
 *
 * <p>Rules derived from CLAUDE.md CNP-2 (Stateless Processes), HFDP-4 (Adapter Pattern),
 * and HFDP-8 (Dependency Inversion Principle).
 *
 * <p>Key invariant: dependencies flow inward — domain core has ZERO infrastructure imports.
 * Infrastructure implements domain ports; handlers orchestrate via ports.
 */
@AnalyzeClasses(
        packages = "com.plum.endorsements",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class HexagonalArchitectureFitnessFunction {

    // ── Layer Dependency Rules ────────────────────────────────────────────

    /**
     * Frozen rule: baselines existing violations (ProcessMiningService accessing JPA entities,
     * InsurerRegistry importing application exceptions, ReconciliationEngine importing InsurerRouter).
     * New violations will still fail the build. Run without freeze() to see all current violations.
     */
    @ArchTest
    static final ArchRule hexagonal_layer_dependencies =
            freeze(layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("API").definedBy("..api..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Infrastructure").definedBy("..infrastructure..")
                    // API is the entry point — nothing accesses it
                    .whereLayer("API").mayNotBeAccessedByAnyLayer()
                    // Application is accessed only by API and Infrastructure (for config/DI wiring)
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("API", "Infrastructure")
                    // Domain is the core — accessed by Application and Infrastructure
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                    // Infrastructure may only be accessed by API (for config/filter beans)
                    .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("API"));

    // ── Domain Purity Rules ──────────────────────────────────────────────

    @ArchTest
    static final ArchRule domain_models_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure..",
                            "jakarta.persistence..",
                            "org.springframework.kafka..",
                            "org.springframework.data.jpa..",
                            "org.springframework.web.."
                    )
                    .as("Domain models must not depend on infrastructure packages " +
                            "(jakarta.persistence, kafka, JPA, web)")
                    .because("Domain models define business rules and must be technology-agnostic " +
                            "(HFDP-8: Dependency Inversion Principle)");

    @ArchTest
    static final ArchRule domain_models_must_not_depend_on_application =
            noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .as("Domain models must not depend on application layer")
                    .because("Domain is the innermost ring — it depends on nothing external");

    @ArchTest
    static final ArchRule domain_ports_must_not_depend_on_infrastructure =
            noClasses()
                    .that().resideInAPackage("..domain.port..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure..",
                            "jakarta.persistence..",
                            "org.springframework.kafka..",
                            "org.springframework.data.jpa.."
                    )
                    .as("Domain ports must not depend on infrastructure implementations")
                    .because("Ports define contracts; adapters implement them " +
                            "(HFDP-4: Adapter Pattern)");

    // ── Anti-Corruption Layer ────────────────────────────────────────────

    @ArchTest
    static final ArchRule handlers_must_not_import_jpa_entities =
            noClasses()
                    .that().resideInAPackage("..application.handler..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure.persistence.entity..")
                    .as("Handlers must not import JPA entities directly")
                    .because("Handlers work with domain models; JPA entities stay behind " +
                            "the anti-corruption layer (EndorsementMapper)");

    @ArchTest
    static final ArchRule handlers_must_not_import_kafka_classes =
            noClasses()
                    .that().resideInAPackage("..application.handler..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework.kafka..",
                            "org.apache.kafka.."
                    )
                    .as("Handlers must not import Kafka classes")
                    .because("Handlers publish events through EventPublisher port, " +
                            "not directly to Kafka (HFDP-2: Observer Pattern)");

    @ArchTest
    static final ArchRule controllers_must_not_import_jpa_entities =
            noClasses()
                    .that().resideInAPackage("..api.controller..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure.persistence.entity..")
                    .as("Controllers must not import JPA entities")
                    .because("Controllers use DTOs; JPA entities are infrastructure concerns");
}
