package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.metadata.InstrumentMetadata;

/**
 * Responsibility: generate deterministic instrument metadata.
 *
 * <p>Role in system: replaces external instrument master data for Phase 1.</p>
 *
 * <p>Relationships: delegates to {@link InstrumentMetadata#simulated(int)} so
 * metadata layout remains consistent with prior task cards.</p>
 *
 * <p>Lifecycle: called during scenario setup.</p>
 *
 * <p>Design intent: all generated instruments are enabled and named by dense ID.</p>
 */
public final class InstrumentMetadataSimulator {
    private final SorConfig config;

    public InstrumentMetadataSimulator(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
    }

    public InstrumentMetadata generate() {
        return InstrumentMetadata.simulated(config.instrumentCount());
    }
}
