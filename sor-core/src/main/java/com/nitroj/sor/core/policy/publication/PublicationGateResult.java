package com.nitroj.sor.core.policy.publication;

/**
 * Responsibility: represent publication gate outcome.
 *
 * <p>Role in system: publisher uses this result to decide whether a compiled
 * policy may atomically replace the active policy.</p>
 *
 * <p>Relationships: produced by {@link PublicationGate} and consumed by
 * {@link com.nitroj.sor.core.policy.PolicyPublisher}.</p>
 *
 * <p>Lifecycle: created per publication attempt.</p>
 *
 * <p>Design intent: fields mirror the authoritative spec and expose simple
 * counters for tests and lifecycle messages.</p>
 */
public final class PublicationGateResult {
    public boolean publishAllowed;
    public int failedGateCount;
    public String[] failedGates;
    public int expectedImprovementBps;
    public int maxVenueChanges;
    public int maxWeightChangeBps;
    public String adequacyStatus;
    public String[] adequacyMissingCategories;
    public String robustObjective;
    public int selectedCandidateId = -1;

    public static PublicationGateResult allowed(final int expectedImprovementBps, final int maxVenueChanges, final int maxWeightChangeBps) {
        final PublicationGateResult result = new PublicationGateResult();
        result.publishAllowed = true;
        result.failedGates = new String[0];
        result.expectedImprovementBps = expectedImprovementBps;
        result.maxVenueChanges = maxVenueChanges;
        result.maxWeightChangeBps = maxWeightChangeBps;
        result.adequacyStatus = "";
        result.adequacyMissingCategories = new String[0];
        result.robustObjective = "";
        return result;
    }

    public static PublicationGateResult rejected(final String gate) {
        final PublicationGateResult result = new PublicationGateResult();
        result.publishAllowed = false;
        result.failedGateCount = 1;
        result.failedGates = new String[]{gate};
        result.adequacyStatus = "";
        result.adequacyMissingCategories = new String[0];
        result.robustObjective = "";
        return result;
    }
}
