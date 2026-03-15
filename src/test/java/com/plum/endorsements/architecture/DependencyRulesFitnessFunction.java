package com.plum.endorsements.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture fitness functions enforcing dependency direction and isolation rules.
 *
 * <p>Rules derived from CLAUDE.md HFDP-8 Design Principles:
 * <ul>
 *   <li>Program to interfaces (handlers depend on ports, not adapters)</li>
 *   <li>Dependency Inversion (domain defines ports; infrastructure implements them)</li>
 *   <li>Encapsulate what varies (insurer logic in adapters, not handlers)</li>
 * </ul>
 */
@AnalyzeClasses(
        packages = "com.plum.endorsements",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class DependencyRulesFitnessFunction {

    // ── No Circular Dependencies Between Top-Level Packages ──────────────

    /**
     * Frozen rule: baselines existing cycle between domain.service.InsurerRegistry
     * importing application.exception.InsurerNotFoundException. New cycles will still fail.
     * TODO: Move InsurerNotFoundException to domain.exception to break the cycle.
     */
    @ArchTest
    static final ArchRule no_cycles_between_top_level_packages =
            freeze(slices()
                    .matching("com.plum.endorsements.(*)..")
                    .should().beFreeOfCycles()
                    .as("No circular dependencies between api, application, domain, and infrastructure")
                    .because("Circular dependencies create tight coupling and prevent independent deployment"));

    // ── Program to Interfaces ────────────────────────────────────────────

    @ArchTest
    static final ArchRule handlers_must_not_depend_on_jpa_adapters =
            noClasses()
                    .that().resideInAPackage("..application.handler..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure.persistence.adapter..")
                    .as("Handlers must depend on domain ports, not JPA adapter implementations")
                    .because("HFDP-8: Program to interfaces — inject EndorsementRepository, " +
                            "not JpaEndorsementRepositoryAdapter");

    @ArchTest
    static final ArchRule handlers_must_not_depend_on_insurer_adapters =
            noClasses()
                    .that().resideInAPackage("..application.handler..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.insurer.icici..",
                            "..infrastructure.insurer.bajaj..",
                            "..infrastructure.insurer.nivabupa.."
                    )
                    .as("Handlers must not depend on concrete insurer adapters")
                    .because("HFDP-1: Strategy Pattern — handlers use InsurerPort interface " +
                            "via InsurerRouter, never concrete adapters directly");

    // ── Adapter Isolation ────────────────────────────────────────────────

    @ArchTest
    static final ArchRule insurer_adapters_must_not_depend_on_each_other =
            noClasses()
                    .that().resideInAPackage("..infrastructure.insurer.icici..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.insurer.bajaj..",
                            "..infrastructure.insurer.nivabupa.."
                    )
                    .as("ICICI adapter must not depend on Bajaj or Niva Bupa adapters")
                    .because("Each insurer adapter is an independent Strategy implementation " +
                            "(HFDP-1: Strategy Pattern)");

    @ArchTest
    static final ArchRule bajaj_adapter_must_not_depend_on_other_adapters =
            noClasses()
                    .that().resideInAPackage("..infrastructure.insurer.bajaj..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.insurer.icici..",
                            "..infrastructure.insurer.nivabupa.."
                    )
                    .as("Bajaj adapter must not depend on ICICI or Niva Bupa adapters")
                    .because("Each insurer adapter is an independent Strategy implementation");

    @ArchTest
    static final ArchRule nivabupa_adapter_must_not_depend_on_other_adapters =
            noClasses()
                    .that().resideInAPackage("..infrastructure.insurer.nivabupa..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "..infrastructure.insurer.icici..",
                            "..infrastructure.insurer.bajaj.."
                    )
                    .as("Niva Bupa adapter must not depend on ICICI or Bajaj adapters")
                    .because("Each insurer adapter is an independent Strategy implementation");

    // ── Controllers Must Not Bypass Handlers ─────────────────────────────

    @ArchTest
    static final ArchRule controllers_must_not_access_repositories_directly =
            noClasses()
                    .that().resideInAPackage("..api.controller..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure.persistence..")
                    .as("Controllers must not access persistence layer directly")
                    .because("Controllers delegate to handlers; persistence is accessed through " +
                            "domain ports in the application layer (HFDP-8: SRP)");

    // ── No JPA Leakage Into Domain ───────────────────────────────────────

    @ArchTest
    static final ArchRule domain_must_not_use_jakarta_persistence =
            noClasses()
                    .that().resideInAPackage("..domain.model..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("jakarta.persistence..")
                    .as("Domain models must not use JPA annotations")
                    .because("Domain models are pure POJOs; JPA annotations belong on " +
                            "infrastructure entity classes (HFDP-8: Dependency Inversion)");
}
