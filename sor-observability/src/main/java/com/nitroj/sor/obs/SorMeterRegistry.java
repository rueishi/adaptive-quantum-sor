package com.nitroj.sor.obs;

import java.util.Set;

/** Lightweight registry facade used by tests and HTTP metrics exposition. */
public final class SorMeterRegistry {
    private final SorObservability observability;

    public SorMeterRegistry(final SorObservability observability) {
        this.observability = observability;
    }

    public Set<String> getMeters() {
        return observability.meterNames();
    }

    public String scrape() {
        return observability.prometheusText();
    }
}
