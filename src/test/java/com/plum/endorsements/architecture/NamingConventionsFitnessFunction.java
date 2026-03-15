package com.plum.endorsements.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Architecture fitness functions enforcing naming conventions.
 *
 * <p>Rules derived from CLAUDE.md naming conventions table:
 * <ul>
 *   <li>Handler: {Action}Handler</li>
 *   <li>Controller: {Entity}Controller</li>
 *   <li>JPA entity: {Entity}Entity</li>
 *   <li>JPA adapter: Jpa{Entity}RepositoryAdapter or ...Adapter</li>
 *   <li>Insurer adapter: {InsurerName}Adapter</li>
 *   <li>Config: {Concern}Config</li>
 *   <li>Mapper: {Entity}Mapper</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.plum.endorsements",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class NamingConventionsFitnessFunction {

    // ── Handler Naming ───────────────────────────────────────────────────

    @ArchTest
    static final ArchRule handlers_should_be_named_handler =
            classes()
                    .that().resideInAPackage("..application.handler..")
                    .and().areAnnotatedWith(Service.class)
                    .should().haveSimpleNameEndingWith("Handler")
                    .as("Handler classes must end with 'Handler'")
                    .because("Naming convention: {Action}Handler " +
                            "(e.g., CreateEndorsementHandler, ProcessEndorsementHandler)");

    // ── Controller Naming ────────────────────────────────────────────────

    @ArchTest
    static final ArchRule controllers_should_be_named_controller =
            classes()
                    .that().resideInAPackage("..api.controller..")
                    .and().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .as("Controller classes must end with 'Controller'")
                    .because("Naming convention: {Entity}Controller " +
                            "(e.g., EndorsementController, IntelligenceController)");

    // ── JPA Entity Naming ────────────────────────────────────────────────

    @ArchTest
    static final ArchRule jpa_entities_should_be_named_entity =
            classes()
                    .that().resideInAPackage("..infrastructure.persistence.entity..")
                    .and().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().haveSimpleNameEndingWith("Entity")
                    .as("JPA entity classes must end with 'Entity'")
                    .because("Naming convention: {Entity}Entity " +
                            "(e.g., EndorsementEntity, EAAccountEntity)");

    // ── Adapter Naming ───────────────────────────────────────────────────

    @ArchTest
    static final ArchRule persistence_adapters_should_be_named_adapter =
            classes()
                    .that().resideInAPackage("..infrastructure.persistence.adapter..")
                    .should().haveSimpleNameEndingWith("Adapter")
                    .as("Persistence adapter classes must end with 'Adapter'")
                    .because("Naming convention: Jpa{Entity}RepositoryAdapter");

    @ArchTest
    static final ArchRule insurer_adapters_should_be_named_adapter =
            classes()
                    .that().resideInAPackage("..infrastructure.insurer..")
                    .and().haveSimpleNameContaining("Adapter")
                    .should().haveSimpleNameEndingWith("Adapter")
                    .as("Insurer adapter classes must end with 'Adapter'")
                    .because("Naming convention: {InsurerName}Adapter " +
                            "(e.g., IciciLombardAdapter, BajajAllianzAdapter)");

    // ── Mapper Naming ────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule mappers_should_be_named_mapper =
            classes()
                    .that().resideInAPackage("..infrastructure.persistence.mapper..")
                    .should().haveSimpleNameEndingWith("Mapper")
                    .as("Mapper classes must end with 'Mapper'")
                    .because("Naming convention: {Entity}Mapper " +
                            "(e.g., EndorsementMapper) — the anti-corruption layer");

    // ── Scheduler Naming ─────────────────────────────────────────────────

    @ArchTest
    static final ArchRule schedulers_should_be_named_scheduler =
            classes()
                    .that().resideInAPackage("..application.scheduler..")
                    .should().haveSimpleNameEndingWith("Scheduler")
                    .as("Scheduler classes must end with 'Scheduler'")
                    .because("Naming convention: {Concern}Scheduler " +
                            "(e.g., BatchAssemblyScheduler, AnomalyDetectionScheduler)");
}
