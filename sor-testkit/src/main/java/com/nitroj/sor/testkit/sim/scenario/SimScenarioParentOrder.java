package com.nitroj.sor.testkit.sim.scenario;

/**
 * Responsibility: explicit scenario parent-order schedule entry.
 *
 * <p>Role in system: mirrors the catalog {@code parentOrders[]} surface for
 * simulator-only replay and downstream migration code.</p>
 *
 * <p>Relationships: consumed by {@link SimulatedOrderInjector} when a scenario
 * asks for a fixed order at a specific tick.</p>
 *
 * <p>Lifecycle: immutable value created during scenario loading.</p>
 *
 * <p>Design intent: preserve YAML compatibility while using the public engine
 * submission path.</p>
 */
public record SimScenarioParentOrder(int atTick, int instrumentId, int side, long quantity, int urgencyId,
                                     long clientOrderRef) {
    public SimScenarioParentOrder {
        if (atTick < 0) {
            throw new IllegalArgumentException("atTick must be non-negative");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}
