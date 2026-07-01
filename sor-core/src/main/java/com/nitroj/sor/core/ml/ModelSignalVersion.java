package com.nitroj.sor.core.ml;

/**
 * Responsibility: identify deterministic model-signal versions.
 *
 * <p>Role in system: ML stubs stamp produced {@code ModelSignalState}
 * instances with a model version so optimizer lineage can explain which signal
 * generator produced a policy input.</p>
 *
 * <p>Relationships: used by {@link MlSignalModelStub} and copied into
 * policy optimization snapshots.</p>
 *
 * <p>Lifecycle: constants are stable across Phase 1 runs.</p>
 *
 * <p>Design intent: avoid magic numbers in tests and optimizer metadata.</p>
 */
public final class ModelSignalVersion {
    public static final long PHASE1_STUB_V1 = 1L;

    private ModelSignalVersion() {
    }
}
