package com.nitroj.adaptive.quantum.sor.sim;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.state.ChildOrderState;
import com.nitroj.adaptive.quantum.sor.state.OutstandingChildOrderState;
import com.nitroj.adaptive.quantum.sor.stats.ExecutionOutcomeStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic venue behavior simulation.
 *
 * <p>Role in system: this is the direct unit-test surface for P5-TC-004 while
 * preserving the legacy baseline process API used by older simulator tests.</p>
 *
 * <p>Relationships: exercises {@link VenueBehaviorSimulator} writes into
 * {@link ExecutionOutcomeStore}, {@link OutstandingChildOrderState}, and
 * {@link ChildOrderState}.</p>
 *
 * <p>Lifecycle: executed by Gradle/JUnit with simulator tests.</p>
 *
 * <p>Design intent: keep profile initialization and seeded outcome replay
 * stable before scenario-driven tests use the richer market-state overload.</p>
 */
final class VenueBehaviorSimulatorTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void sameSeedProducesSameBaselineOutcomeSequence() {
        final ChildOrderBuffer firstOrders = orders();
        final ChildOrderBuffer secondOrders = orders();
        final ExecutionOutcomeStore firstOutcomes = new ExecutionOutcomeStore(8);
        final ExecutionOutcomeStore secondOutcomes = new ExecutionOutcomeStore(8);
        final VenueBehaviorSimulator first = new VenueBehaviorSimulator(CONFIG, 7L);
        final VenueBehaviorSimulator second = new VenueBehaviorSimulator(CONFIG, 7L);

        first.process(firstOrders, firstOutcomes, new OutstandingChildOrderState(2), new ChildOrderState(2));
        second.process(secondOrders, secondOutcomes, new OutstandingChildOrderState(2), new ChildOrderState(2));

        assertEquals(firstOutcomes.size(), secondOutcomes.size());
        for (int i = 0; i < firstOutcomes.size(); i++) {
            assertEquals(firstOutcomes.childOrderId(i), secondOutcomes.childOrderId(i));
            assertEquals(firstOutcomes.venueId(i), secondOutcomes.venueId(i));
            assertEquals(firstOutcomes.outcomeType(i), secondOutcomes.outcomeType(i));
            assertEquals(firstOutcomes.filledQty(i), secondOutcomes.filledQty(i));
            assertEquals(firstOutcomes.latencyNanos(i), secondOutcomes.latencyNanos(i));
            assertEquals(firstOutcomes.slippageBps(i), secondOutcomes.slippageBps(i));
            assertEquals(firstOutcomes.toxicityBps(i), secondOutcomes.toxicityBps(i));
        }
    }

    @Test
    void initializesDeterministicVenueProfiles() {
        final VenueBehaviorSimulator simulator = new VenueBehaviorSimulator(CONFIG, 9L);

        assertEquals(100, simulator.behaviorState().toxicityBps(0));
        assertEquals(200, simulator.behaviorState().toxicityBps(1));
        assertEquals(2_500, simulator.behaviorState().toxicityBps(2));
        assertEquals(300, simulator.behaviorState().toxicityBps(3));
        assertEquals(800, simulator.behaviorState().toxicityBps(4));
    }

    @Test
    void invalidInputsAreRejected() {
        final VenueBehaviorSimulator simulator = new VenueBehaviorSimulator(CONFIG, 11L);

        assertEquals("config must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new VenueBehaviorSimulator(null, 1L)
        ).getMessage());
        assertEquals("venue behavior inputs must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> simulator.process(null, new ExecutionOutcomeStore(2), new OutstandingChildOrderState(1), new ChildOrderState(1))
        ).getMessage());
    }

    private static ChildOrderBuffer orders() {
        final ChildOrderBuffer orders = new ChildOrderBuffer(2);
        orders.add(1L, 1L, 0, 0, Side.BUY, 1_000L, 1L, 1L);
        orders.add(2L, 1L, 0, 2, Side.SELL, 1_000L, 1L, 1L);
        return orders;
    }
}
