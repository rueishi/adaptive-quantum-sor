package com.nitroj.sor.api;

/**
 * Responsibility: immutable control-plane request to hydrate startup state.
 *
 * <p>Role in system: carries OMS/EMS order state, market-data seed state,
 * scope, replay-safety intent, and audit reason into
 * {@link SorControlPlane#hydrate(SorStartupHydrationRequest)}.</p>
 *
 * <p>Relationships: contains {@link OrderStateSnapshot} and
 * {@link MarketDataSeedSnapshot}; produces {@link SorStartupHydrationSummary}
 * evidence.</p>
 *
 * <p>Lifecycle: created for one startup, recovery, or scenario
 * reset-and-repopulate attempt.</p>
 *
 * <p>Design intent: keep hydration separate from hot-path parent order
 * submission.</p>
 *
 * @param requestId unique hydration request identifier
 * @param scope human-readable engine or instrument/venue scope
 * @param orderStateSnapshot OMS/EMS order-state snapshot
 * @param marketDataSnapshot market-data seed snapshot
 * @param replaySafe whether the request is expected to be deterministic
 * @param reason audit reason
 */
public record SorStartupHydrationRequest(
        String requestId,
        String scope,
        OrderStateSnapshot orderStateSnapshot,
        MarketDataSeedSnapshot marketDataSnapshot,
        boolean replaySafe,
        String reason
) {
    /**
     * Validates required startup hydration inputs.
     */
    public SorStartupHydrationRequest {
        if (requestId == null || requestId.isBlank() || scope == null || scope.isBlank()
                || orderStateSnapshot == null || marketDataSnapshot == null
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("startup hydration request inputs must not be null or blank");
        }
    }
}
