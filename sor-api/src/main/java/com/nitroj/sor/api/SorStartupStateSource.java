package com.nitroj.sor.api;

/**
 * Responsibility: source of startup order and market snapshots.
 *
 * <p>Role in system: lets OMS/EMS, recovery tooling, or deterministic testkit
 * code provide control-plane startup state before the engine accepts new parent
 * intent.</p>
 *
 * <p>Relationships: produces {@link OrderStateSnapshot} and
 * {@link MarketDataSeedSnapshot} values consumed by
 * {@link SorStartupHydrationRequest}.</p>
 *
 * <p>Lifecycle: called during startup, recovery, or explicit reset/repopulate
 * workflows, never on the per-order hot path.</p>
 *
 * <p>Design intent: make startup state ownership explicit while keeping
 * product OMS/EMS implementations outside the SOR library.</p>
 */
public interface SorStartupStateSource {
    /**
     * Returns the active parent/child order snapshot for the engine scope.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return order-state snapshot
     */
    OrderStateSnapshot initialOrderState();

    /**
     * Returns the initial venue-aware market-data snapshot for the engine
     * scope.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return market-data seed snapshot
     */
    MarketDataSeedSnapshot initialMarketData();
}
