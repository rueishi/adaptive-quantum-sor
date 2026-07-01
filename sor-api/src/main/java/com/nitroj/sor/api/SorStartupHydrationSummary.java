package com.nitroj.sor.api;

import java.util.Arrays;

/**
 * Responsibility: immutable evidence for a startup hydration attempt.
 *
 * <p>Role in system: reports accepted/rejected status, replay safety, source
 * snapshot IDs, counts, checksums, and failure reasons without exposing mutable
 * engine state.</p>
 *
 * <p>Relationships: returned by
 * {@link SorControlPlane#hydrate(SorStartupHydrationRequest)} and consumed by
 * diagnostics, HTTP control surfaces, and scenario evidence.</p>
 *
 * <p>Lifecycle: created once per hydration attempt and owned by the caller.</p>
 *
 * <p>Design intent: make startup readiness auditable and deterministic.</p>
 *
 * @param requestId hydration request identifier
 * @param accepted whether hydration committed or will be committed by engine
 * @param replaySafe whether resulting state is replay-safe
 * @param message operator-facing result message
 * @param orderSnapshotId order-state snapshot identifier
 * @param marketSnapshotId market-data snapshot identifier
 * @param orderAsOfSequence order-state source sequence
 * @param marketAsOfSequence market-data source sequence
 * @param parentCount number of parent records
 * @param childCount number of child records
 * @param marketCellCount number of market seed cells
 * @param orderChecksum deterministic order-state checksum
 * @param marketChecksum deterministic market-data checksum
 * @param failureReasons rejected or warning reasons
 */
public record SorStartupHydrationSummary(
        String requestId,
        boolean accepted,
        boolean replaySafe,
        String message,
        String orderSnapshotId,
        String marketSnapshotId,
        long orderAsOfSequence,
        long marketAsOfSequence,
        int parentCount,
        int childCount,
        int marketCellCount,
        long orderChecksum,
        long marketChecksum,
        String[] failureReasons
) {
    /**
     * Validates summary evidence and copies failure reasons.
     */
    public SorStartupHydrationSummary {
        if (requestId == null || requestId.isBlank() || message == null || message.isBlank()
                || orderSnapshotId == null || orderSnapshotId.isBlank()
                || marketSnapshotId == null || marketSnapshotId.isBlank()
                || orderAsOfSequence < 0 || marketAsOfSequence < 0 || parentCount < 0
                || childCount < 0 || marketCellCount < 0 || failureReasons == null) {
            throw new IllegalArgumentException("startup hydration summary inputs must be valid");
        }
        failureReasons = failureReasons.clone();
        for (String reason : failureReasons) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("startup hydration failure reasons must not be null or blank");
            }
        }
    }

    /**
     * Creates an accepted summary from a validated request.
     *
     * @param request startup hydration request
     * @param message result message
     * @return accepted summary
     */
    public static SorStartupHydrationSummary accepted(final SorStartupHydrationRequest request,
                                                      final String message) {
        return from(request, true, message, new String[0]);
    }

    /**
     * Creates a rejected summary from a validated request.
     *
     * @param request startup hydration request
     * @param message result message
     * @param failureReasons failure reasons
     * @return rejected summary
     */
    public static SorStartupHydrationSummary rejected(final SorStartupHydrationRequest request,
                                                      final String message,
                                                      final String[] failureReasons) {
        return from(request, false, message, failureReasons);
    }

    private static SorStartupHydrationSummary from(final SorStartupHydrationRequest request,
                                                   final boolean accepted,
                                                   final String message,
                                                   final String[] failureReasons) {
        if (request == null) {
            throw new IllegalArgumentException("startup hydration request must not be null");
        }
        final OrderStateSnapshot order = request.orderStateSnapshot();
        final MarketDataSeedSnapshot market = request.marketDataSnapshot();
        return new SorStartupHydrationSummary(
                request.requestId(),
                accepted,
                request.replaySafe(),
                message,
                order.snapshotId(),
                market.snapshotId(),
                order.asOfSequence(),
                market.asOfSequence(),
                order.parents().length,
                order.children().length,
                market.cells().length,
                order.checksum(),
                market.checksum(),
                failureReasons);
    }

    /**
     * Returns a defensive copy of failure reasons.
     *
     * @return failure reasons
     */
    @Override
    public String[] failureReasons() {
        return failureReasons.clone();
    }

    @Override
    public String toString() {
        return "SorStartupHydrationSummary{" + requestId + ',' + accepted + ',' + replaySafe + ','
                + message + ',' + orderSnapshotId + ',' + marketSnapshotId + ','
                + Arrays.toString(failureReasons) + '}';
    }
}
