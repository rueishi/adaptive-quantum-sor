package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.metadata.VenueMetadata;

/**
 * Responsibility: generate deterministic venue metadata.
 *
 * <p>Role in system: replaces external venue master data during Phase 1.</p>
 *
 * <p>Relationships: delegates to {@link VenueMetadata#simulated(int, int)} for
 * dense venue names and support flags.</p>
 *
 * <p>Lifecycle: called during scenario setup or startup.</p>
 *
 * <p>Design intent: all generated venues are enabled and support every
 * generated instrument unless later scenarios override metadata.</p>
 */
public final class VenueMetadataSimulator {
    private final SorConfig config;

    public VenueMetadataSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    public VenueMetadata generate() {
        return VenueMetadata.simulated(config.instrumentCount(), config.venueCount());
    }
}
