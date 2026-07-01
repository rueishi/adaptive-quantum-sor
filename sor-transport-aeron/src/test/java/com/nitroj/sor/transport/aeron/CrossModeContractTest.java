package com.nitroj.sor.transport.aeron;

import org.junit.jupiter.api.Test;

/**
 * Verifies behavior parity across embedded and Aeron transport modes.
 *
 * <p>Run with transport tests before changing client/server delegation semantics.</p>
 */
class CrossModeContractTest {
    @Test
    void namedCrossModeSuiteRuns() {
        new AeronSorClientImplementsSorEngineContractTest().crossModeContractSubmitsAndReportsStatus("aeron:ipc");
    }
}
