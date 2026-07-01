package com.nitroj.sor.api;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the public API module exposes only the P8-03 engine
 * contract and keeps implementation dependencies out.
 *
 * <p>Role in system: this is the build guard for the `sor-api` zero-dependency
 * invariant.</p>
 *
 * <p>Relationships: inspects compiled API classes and fails if implementation
 * packages leak into the public API jar.</p>
 *
 * <p>Lifecycle: runs in `:sor-api:test` after API compilation.</p>
 *
 * <p>Design intent: keep the public framework surface stable and small before
 * later cards add SPI and implementation modules.</p>
 */
class SorApiZeroDependencyTest {
    /**
     * Confirms the expected public engine types are present.
     */
    @Test
    void publicEngineSurfaceContainsExpectedTypesOnlyForP8_03() {
        final Set<String> names = Set.of(
                SorEngine.class.getName(),
                SorEngineBuilder.class.getName(),
                ParentOrderRequest.class.getName(),
                OrderStatus.class.getName(),
                PolicyHandle.class.getName(),
                SorEvent.class.getName(),
                SorEventListener.class.getName(),
                Registration.class.getName(),
                Side.class.getName(),
                VenueStatus.class.getName()
        );

        assertTrue(names.contains("com.nitroj.sor.api.SorEngine"));
        assertTrue(names.contains("com.nitroj.sor.api.SorEngineBuilder"));
        assertTrue(names.contains("com.nitroj.sor.api.ParentOrderRequest"));
        assertTrue(names.contains("com.nitroj.sor.api.OrderStatus"));
        assertTrue(names.contains("com.nitroj.sor.api.PolicyHandle"));
        assertTrue(names.contains("com.nitroj.sor.api.SorEvent"));
        assertTrue(names.contains("com.nitroj.sor.api.SorEventListener"));
        assertTrue(names.contains("com.nitroj.sor.api.Registration"));
        assertTrue(names.contains("com.nitroj.sor.api.Side"));
        assertTrue(names.contains("com.nitroj.sor.api.VenueStatus"));
    }

    /**
     * Confirms compiled API classes do not depend on implementation packages.
     */
    @Test
    void apiClassesDoNotReferenceImplementationPackages() {
        final JavaClasses classes = new ClassFileImporter().importPackages("com.nitroj.sor.api");
        final Set<String> implementationPrefixes = Set.of(
                "com.nitroj.sor.core",
                "com.nitroj.sor.server",
                "com.nitroj.sor.sim",
                "com.nitroj.sor.optnative"
        );

        final boolean referencesImplementation = classes.stream()
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .anyMatch(dependency -> implementationPrefixes.stream()
                        .anyMatch(prefix -> dependency.getTargetClass().getPackageName().startsWith(prefix)));

        assertEquals(false, referencesImplementation, "sor-api must not reference implementation packages");
    }
}
