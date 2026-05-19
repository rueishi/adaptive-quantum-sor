package com.nitroj.adaptive.quantum.sor.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: unit test {@link Indexing}.
 *
 * <p>Role in system: locks down canonical dense-array formulas used by all
 * state and policy structures.</p>
 *
 * <p>Relationships: independent utility test; downstream structures have their
 * own mirrored tests.</p>
 *
 * <p>Lifecycle: discovered by JUnit 5 during Gradle verification.</p>
 *
 * <p>Design intent: explicit numeric expectations catch formula drift better
 * than duplicating the implementation.</p>
 */
final class IndexingTest {

    @Test
    void formulasAndAccessorsMatchSpec() {
        final Indexing indexing = new Indexing(2, 3, 4, 5);

        assertEquals(5, indexing.idxIV(1, 2));
        assertEquals(23, indexing.idxIVR(1, 2, 3));
        assertEquals(119, indexing.idxIVRU(1, 2, 3, 4));
        assertEquals(39, indexing.routeKey(1, 3, 4));
        assertEquals(41, indexing.routeListOffsetLength());
        assertEquals(120, indexing.ivruLength());
        assertEquals(6, indexing.ivLength());
        assertEquals(2, indexing.instrumentCount());
        assertEquals(3, indexing.venueCount());
        assertEquals(4, indexing.regimeCount());
        assertEquals(5, indexing.urgencyCount());
    }

    @Test
    void minimumBoundaryIdsWork() {
        final Indexing indexing = new Indexing(1, 1, 1, 1);

        assertEquals(0, indexing.idxIVRU(0, 0, 0, 0));
        assertEquals(2, indexing.routeListOffsetLength());
    }

    @Test
    void invalidDimensionsAndIdsFailClearly() {
        final Indexing indexing = new Indexing(1, 1, 1, 1);

        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> indexing.idxIV(-1, 0)).getMessage().contains("instrumentId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> indexing.idxIV(0, 1)).getMessage().contains("venueId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> indexing.idxIVR(0, 0, 1)).getMessage().contains("regimeId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> indexing.idxIVRU(0, 0, 0, 1)).getMessage().contains("urgencyId"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> indexing.routeKey(0, -1, 0)).getMessage().contains("regimeId"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Indexing(0, 1, 1, 1)).getMessage().contains("instrumentCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Indexing(1, 0, 1, 1)).getMessage().contains("venueCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Indexing(1, 1, 0, 1)).getMessage().contains("regimeCount"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new Indexing(1, 1, 1, 0)).getMessage().contains("urgencyCount"));
    }
}
