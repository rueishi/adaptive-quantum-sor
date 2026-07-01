package com.nitroj.sor.optnative;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies native symbol resolution behavior for Panama linkers.
 *
 * <p>Run when changing native library names, directories, or exported symbol lists.</p>
 */
class PanamaSymbolResolutionTest {
    @Test
    void expectedSymbolsResolve() {
        assertTrue(new TacticalOptimizerLinker().resolves("tactical_optimize"));
        assertTrue(new StrategicOptimizerLinker().resolves("cudaq_optimize_subset"));
        assertTrue(new BatchAllocatorLinker().resolves("batch_allocate"));
    }
}
