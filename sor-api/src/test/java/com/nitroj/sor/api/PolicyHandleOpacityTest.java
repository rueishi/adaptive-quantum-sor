package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Responsibility: verifies {@link PolicyHandle} exposes only opaque identity
 * fields.
 *
 * <p>Role in system: protects the firewall between public API consumers and
 * internal policy data structures.</p>
 *
 * <p>Relationships: complements the API zero-dependency test by asserting the
 * exact accessor surface.</p>
 *
 * <p>Lifecycle: runs whenever `sor-api` tests execute.</p>
 *
 * <p>Design intent: prevent accidental exposure of `HotRouteBook` or
 * `SorPolicy` through the public handle.</p>
 */
class PolicyHandleOpacityTest {
    /**
     * Confirms the interface has exactly the documented five accessors.
     */
    @Test
    void policyHandleExposesOnlyIdentityAccessors() {
        final Set<String> methods = Arrays.stream(PolicyHandle.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertEquals(Set.of("version", "hash64", "hashSha256", "createdEpochNanos", "effectiveFromEpochNanos"), methods);
        assertFalse(methods.contains("hotRouteBook"));
        assertFalse(methods.contains("sorPolicy"));
    }
}
