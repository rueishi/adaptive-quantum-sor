package com.nitroj.sor.testkit.sim.scenario;

import com.nitroj.sor.testkit.sim.adapters.*;

/**
 * Responsibility: orchestrate simulator components under a health guard.
 *
 * <p>Role in system: replaces the legacy supervisor/publication-guard pairing
 * with one simulator-local controller. It publishes ticks, polls venues, and
 * converts unexpected simulator task failures into observable health state.</p>
 *
 * <p>Relationships: owns {@link SimulatorHealthState}, writes lifecycle
 * evidence into {@link InMemoryPersistence}, and delegates market/venue work to
 * the concrete simulator integrations.</p>
 *
 * <p>Lifecycle: created per scenario runtime and retained until the run closes
 * or is reset.</p>
 *
 * <p>Design intent: centralize failure handling so policy/scenario publication
 * can be blocked after simulator failure without using core publication gates.</p>
 */
public final class SimulatedClusterController {
    private final ManualClock clock;
    private final SimulatedMarketDataSource marketData;
    private final SimulatedVenueAdapter venueAdapter;
    private final SimulatorHealthState healthState = new SimulatorHealthState();
    private final InMemoryPersistence persistence = new InMemoryPersistence();

    public SimulatedClusterController(final ManualClock clock, final SimulatedMarketDataSource marketData,
                                      final SimulatedVenueAdapter venueAdapter) {
        this.clock = clock;
        this.marketData = marketData;
        this.venueAdapter = venueAdapter;
    }

    public void publishTick(final int instrumentId, final long bid, final long ask) {
        marketData.publish(instrumentId, bid, ask, 100, 100, clock.epochNanos());
    }

    public int pollVenue(final int venueId, final int maxOrders) {
        return venueAdapter.pollVenue(venueId, maxOrders);
    }

    /** Runs one simulator task and records failure state without rethrowing. */
    public boolean runGuarded(final String simulatorName, final Runnable task) {
        if (simulatorName == null || simulatorName.isBlank()) {
            throw new IllegalArgumentException("simulatorName must not be blank");
        }
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        try {
            task.run();
            return true;
        } catch (RuntimeException ex) {
            final String message = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? ex.getClass().getSimpleName()
                    : ex.getMessage();
            healthState.markFailed(simulatorName, message, clock.epochNanos());
            persistence.appendLifecycleEvent(new com.nitroj.sor.api.spi.LifecycleEvent()
                    .set(1, healthState.failureCount(), clock.epochNanos()));
            return false;
        }
    }

    /** Returns whether simulator-derived publication is currently allowed. */
    public boolean publicationAllowed() {
        return healthState.healthy();
    }

    public SimulatorHealthState healthState() {
        return healthState;
    }

    public InMemoryPersistence persistence() {
        return persistence;
    }
}
