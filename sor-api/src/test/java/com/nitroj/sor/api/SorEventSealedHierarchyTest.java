package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the event stream is modeled as a sealed hierarchy.
 *
 * <p>Role in system: gives integrators compiler-visible event exhaustiveness
 * once they switch over {@link SorEvent}.</p>
 *
 * <p>Relationships: checks the nested record event types declared by
 * {@link SorEvent}.</p>
 *
 * <p>Lifecycle: runs after API compilation.</p>
 *
 * <p>Design intent: make adding a new public event a deliberate API change.</p>
 */
class SorEventSealedHierarchyTest {
    /**
     * Confirms SorEvent is sealed and permits the documented P8-03 records.
     */
    @Test
    void sorEventIsSealedWithDocumentedSubtypes() {
        assertTrue(SorEvent.class.isSealed(), "SorEvent must be sealed");

        final Set<String> permitted = Set.of(SorEvent.class.getPermittedSubclasses()).stream()
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "RouteDecided",
                "ChildOrderEmitted",
                "Filled",
                "Rejected",
                "PolicyPublished",
                "SessionStatusChanged",
                "BackpressureRejected"
        ), permitted);
    }
}
