package com.nitroj.sor.transport.aeron;

import org.junit.jupiter.api.Test;

class CrossModeContractTest {
    @Test
    void namedCrossModeSuiteRuns() {
        new AeronSorClientImplementsSorEngineContractTest().crossModeContractSubmitsAndReportsStatus("aeron:ipc");
    }
}
