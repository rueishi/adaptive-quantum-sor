package com.nitroj.sor.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Verifies a complete builder returns a SorEngine instance. */
class SorEngineBuilderValidConfigurationTest {
    @Test
    void completeConfigurationBuildsEngine() {
        assertNotNull(EngineTestSupport.builder().build());
    }
}
