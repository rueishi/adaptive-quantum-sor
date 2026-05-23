package com.nitroj.sor.core;

import com.nitroj.sor.api.SorEngineBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies builder missing-dependency messages remain explicit. */
class SorEngineBuilderRejectionTest {
    @Test
    void incompleteConfigurationNamesMissingSpis() {
        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> SorEngineBuilder.create().build());
        assertTrue(ex.getMessage().contains("config"));
        assertTrue(ex.getMessage().contains("marketData"));
        assertTrue(ex.getMessage().contains("venueAdapter"));
        assertTrue(ex.getMessage().contains("riskProvider"));
        assertTrue(ex.getMessage().contains("persistence"));
        assertTrue(ex.getMessage().contains("clock"));
    }
}
