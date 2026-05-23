package com.nitroj.sor.sim;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture boundary checks for simulator adapters and scenario fixtures.
 */
class SimulatorArchitectureArchUnitTest {
    private static final JavaClasses SIM_CLASSES = new ClassFileImporter()
            .importPackages("com.nitroj.sor.sim");

    @Test
    void simulatorsDoNotDependOnCoreOrLegacyImplementationPackages() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage("com.nitroj.sor.sim..")
                .and().resideOutsideOfPackage("com.nitroj.sor.sim.server..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.nitroj.sor.core..",
                        "com.nitroj.sor.server..",
                        "com.nitroj.sor.http..",
                        "com.nitroj.sor.transport..",
                        "com.nitroj.adaptive.quantum.sor..");

        rule.check(SIM_CLASSES);
    }

    @Test
    void simulatorAdaptersStayInAdapterPackage() {
        final ArchRule rule = classes()
                .that().haveSimpleNameStartingWith("Simulated")
                .and().haveSimpleNameNotContaining("Cluster")
                .and().resideOutsideOfPackage("com.nitroj.sor.sim.scenario..")
                .should().resideInAPackage("com.nitroj.sor.sim.adapters..");

        rule.check(SIM_CLASSES);
    }
}
