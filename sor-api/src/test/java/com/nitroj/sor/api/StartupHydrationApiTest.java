package com.nitroj.sor.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies startup hydration request and summary DTO
 * contracts.
 *
 * <p>Role in system: protects P9-TC-018 control-plane hydration API behavior
 * before engine-side hydration is implemented.</p>
 *
 * <p>Relationships: covers {@link SorStartupHydrationRequest},
 * {@link SorStartupHydrationSummary}, and the default
 * {@link SorControlPlane#hydrate(SorStartupHydrationRequest)} method.</p>
 *
 * <p>Lifecycle: run by `:sor-api:test` whenever public API contracts change.</p>
 *
 * <p>Design intent: keep startup hydration typed, auditable, and separate from
 * hot-path order submission.</p>
 */
class StartupHydrationApiTest {
    @Test
    void hydrationRequestAndAcceptedSummaryPreserveSnapshotMetadata() {
        final SorStartupHydrationRequest request = request();

        final SorStartupHydrationSummary summary = SorStartupHydrationSummary.accepted(request, "hydrated");

        assertEquals("req-1", summary.requestId());
        assertEquals(true, summary.accepted());
        assertEquals(true, summary.replaySafe());
        assertEquals("orders-1", summary.orderSnapshotId());
        assertEquals("market-1", summary.marketSnapshotId());
        assertEquals(7, summary.orderAsOfSequence());
        assertEquals(9, summary.marketAsOfSequence());
        assertEquals(1, summary.parentCount());
        assertEquals(1, summary.childCount());
        assertEquals(1, summary.marketCellCount());
        assertEquals(request.orderStateSnapshot().checksum(), summary.orderChecksum());
        assertEquals(request.marketDataSnapshot().checksum(), summary.marketChecksum());
    }

    @Test
    void hydrationSummaryDefensivelyCopiesFailureReasons() {
        final String[] reasons = {"unsupported"};
        final SorStartupHydrationSummary summary = SorStartupHydrationSummary.rejected(
                request(), "rejected", reasons);

        reasons[0] = "mutated";
        final String[] read = summary.failureReasons();
        read[0] = "mutated-again";

        assertArrayEquals(new String[]{"unsupported"}, summary.failureReasons());
    }

    @Test
    void hydrationDtosValidateInvalidInputs() {
        final SorStartupHydrationRequest valid = request();

        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorStartupHydrationRequest(" ", "scope", valid.orderStateSnapshot(),
                        valid.marketDataSnapshot(), true, "reason")).getMessage().contains("startup hydration request"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorStartupHydrationSummary("req", true, true, "ok", "orders", "market",
                        -1, 0, 0, 0, 0, 0, 0, new String[0])).getMessage().contains("summary"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> new SorStartupHydrationSummary("req", true, true, "ok", "orders", "market",
                        0, 0, 0, 0, 0, 0, 0, new String[]{" "})).getMessage().contains("failure reasons"));
    }

    @Test
    void defaultControlPlaneHydrateRejectsUntilEngineImplementsIt() {
        final SorControlPlane controlPlane = new SorControlPlane() {
            @Override public SorResetSummary reset(final SorResetRequest request) {
                throw new UnsupportedOperationException();
            }
            @Override public SorStateSummary stateSummary() {
                throw new UnsupportedOperationException();
            }
            @Override public MarketDataSnapshotSummary marketDataSnapshot() {
                throw new UnsupportedOperationException();
            }
        };

        final SorStartupHydrationSummary summary = controlPlane.hydrate(request());

        assertEquals(false, summary.accepted());
        assertArrayEquals(new String[]{"unsupported"}, summary.failureReasons());
    }

    private static SorStartupHydrationRequest request() {
        return new SorStartupHydrationRequest("req-1", "all",
                new OrderStateSnapshot("orders-1", 7, 100,
                        new ParentOrderStateSnapshot[]{
                                new ParentOrderStateSnapshot(1, 2, Side.BUY, 101, 1000, 800,
                                        200, 300, OrderStatusCode.ROUTED, 120)
                        },
                        new ChildOrderStateSnapshot[]{
                                new ChildOrderStateSnapshot(11, 1, 2, 3, Side.BUY, 101,
                                        500, 300, 200, OrderStatusCode.ROUTED, 121)
                        }),
                new MarketDataSeedSnapshot("market-1", 9, 110,
                        new MarketDataSeedCell[]{
                                new MarketDataSeedCell(2, 3, 100, 101, 1_000, 900, 109)
                        }),
                true,
                "startup");
    }
}
