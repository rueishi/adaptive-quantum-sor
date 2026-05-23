package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegratorMeterRegistryHonoredTest {
    @Test
    void suppliedRegistryFacadeScrapesSuppliedBackend() {
        final SorObservability observability = SorObservability.create();
        final SorMeterRegistry registry = new SorMeterRegistry(observability);

        observability.recordBackpressureRejected(1);

        assertEquals(1, observability.backpressureRejections());
        assertTrue(registry.scrape().contains("sor_backpressure_rejections_total 1"));
    }
}
