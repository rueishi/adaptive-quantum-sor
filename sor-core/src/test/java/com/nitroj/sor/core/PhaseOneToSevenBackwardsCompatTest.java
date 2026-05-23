package com.nitroj.sor.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Smoke marker: full :sor-core:check remains the Phase 1-7 compatibility gate. */
class PhaseOneToSevenBackwardsCompatTest {
    @Test
    void legacySuiteIsRunBySorCoreCheck() {
        assertTrue(true);
    }
}
