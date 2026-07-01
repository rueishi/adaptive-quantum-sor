package com.nitroj.sor.core.policy;

/**
 * Responsibility: represent one immutable published SOR policy snapshot.
 *
 * <p>Role in system: this is the object atomically published to the execution
 * layer. It ties policy identity, optimizer lineage, hot-route execution data,
 * and the diagnostic full matrix into one read-only snapshot.</p>
 *
 * <p>Relationships: owns a {@link HotRouteBook} for the executioner and a
 * {@link FullPolicyMatrix} for validation, audit, and replay.</p>
 *
 * <p>Lifecycle: constructed by the compiler after validation and then shared by
 * publication reference. Callers must not mutate the arrays reachable through
 * its child structures after publication.</p>
 *
 * <p>Design intent: final fields make policy identity explicit and easy to copy
 * into route audit events without hidden lazy computation.</p>
 */
public final class SorPolicy {
    public final long policyVersion;
    public final long createdAtEpochNanos;
    public final long effectiveFromEpochNanos;

    public final long policyHash64;
    public final byte[] policyHashSha256;

    public final long isingResultVersion;
    public final long cudaTuningVersion;

    public final int optimizerType;
    public final int policyState;

    public final HotRouteBook hotRouteBook;
    public final FullPolicyMatrix fullPolicyMatrix;

    /**
     * Creates a policy snapshot with populated identity and child structures.
     */
    public SorPolicy(
            final long policyVersion,
            final long createdAtEpochNanos,
            final long effectiveFromEpochNanos,
            final long policyHash64,
            final byte[] policyHashSha256,
            final long isingResultVersion,
            final long cudaTuningVersion,
            final int optimizerType,
            final int policyState,
            final HotRouteBook hotRouteBook,
            final FullPolicyMatrix fullPolicyMatrix
    ) {
        if (policyVersion <= 0) {
            throw new IllegalArgumentException("policyVersion must be positive");
        }
        if (createdAtEpochNanos <= 0 || effectiveFromEpochNanos <= 0) {
            throw new IllegalArgumentException("policy timestamps must be positive");
        }
        if (policyHashSha256 == null || policyHashSha256.length != 32) {
            throw new IllegalArgumentException("policyHashSha256 must contain 32 bytes");
        }
        if (hotRouteBook == null || fullPolicyMatrix == null) {
            throw new IllegalArgumentException("policy route structures must not be null");
        }
        this.policyVersion = policyVersion;
        this.createdAtEpochNanos = createdAtEpochNanos;
        this.effectiveFromEpochNanos = effectiveFromEpochNanos;
        this.policyHash64 = policyHash64;
        this.policyHashSha256 = policyHashSha256.clone();
        this.isingResultVersion = isingResultVersion;
        this.cudaTuningVersion = cudaTuningVersion;
        this.optimizerType = optimizerType;
        this.policyState = policyState;
        this.hotRouteBook = hotRouteBook;
        this.fullPolicyMatrix = fullPolicyMatrix;
    }
}
