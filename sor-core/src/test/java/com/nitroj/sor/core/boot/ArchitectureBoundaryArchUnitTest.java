package com.nitroj.sor.core.boot;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture boundary checks for the embedded engine package.
 */
class ArchitectureBoundaryArchUnitTest {
    private static final JavaClasses CORE_CLASSES = new ClassFileImporter()
            .importPackages("com.nitroj.sor.core");

    @Test
    void embeddedEngineDoesNotDependOnAssemblyTransportOrSimulatorPackages() {
        final ArchRule rule = noClasses()
                .that().resideInAPackage("com.nitroj.sor.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.nitroj.sor.sim..",
                        "com.nitroj.sor.http..",
                        "com.nitroj.sor.transport..",
                        "com.nitroj.adaptive.quantum.sor.api..",
                        "com.nitroj.adaptive.quantum.sor.scenario..");

        rule.check(CORE_CLASSES);
    }
}
