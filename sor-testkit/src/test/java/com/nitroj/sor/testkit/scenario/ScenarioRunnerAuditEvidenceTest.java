package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.core.config.SorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies scenario replay audit evidence.
 *
 * <p>Role in system: proves a scenario run links reset summary,
 * engine-owned market-data checksum, route events, and final order state.</p>
 *
 * <p>Relationships: exercises {@link ScenarioRunner#runWithEvidence} and
 * legacy {@link ScenarioSummary} compatibility.</p>
 *
 * <p>Lifecycle: unit-level scenario replay test.</p>
 *
 * <p>Design intent: keep replay assertions deterministic while exposing richer
 * audit data than the summary alone.</p>
 */
final class ScenarioRunnerAuditEvidenceTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void runWithEvidenceLinksResetMarketRoutesAndFinalState() {
        final ScenarioSpec spec = spec(211L);

        final ScenarioAuditEvidence evidence = new ScenarioRunner().runWithEvidence(spec);

        assertEquals(SorResetMode.SCENARIO_REPLAY_RESET, evidence.resetSummary().mode());
        assertEquals(true, evidence.resetSummary().accepted());
        assertTrue(evidence.marketDataSnapshot().sequence() > 0);
        assertEquals(evidence.summary().routeCount(), evidence.routeDecidedEvents());
        assertEquals(evidence.summary().childOrderCount(), evidence.childOrderEvents());
        assertEquals(evidence.summary().rejectCount(), evidence.rejectEvents());
        assertEquals(evidence.summary().orderCount(), evidence.finalStateSummary().activeParentOrderCount());
        assertEquals(evidence.summary().childOrderCount(), evidence.finalStateSummary().activeChildOrderCount());
        assertTrue(evidence.lifecycleEventCount() >= 2L + evidence.routeDecidedEvents() + evidence.childOrderEvents());
    }

    @Test
    void runWithEvidenceIsReplayEquivalentForSameSeed() {
        final ScenarioSpec spec = spec(223L);
        final ScenarioRunner runner = new ScenarioRunner();

        final ScenarioAuditEvidence first = runner.runWithEvidence(spec);
        final ScenarioAuditEvidence second = runner.runWithEvidence(spec);

        assertEquals(first.summary(), second.summary());
        assertEquals(first.resetSummary().mode(), second.resetSummary().mode());
        assertEquals(first.marketDataSnapshot().checksum(), second.marketDataSnapshot().checksum());
        assertEquals(first.routeDecidedEvents(), second.routeDecidedEvents());
        assertEquals(first.childOrderEvents(), second.childOrderEvents());
        assertEquals(first.fillEvents(), second.fillEvents());
        assertEquals(first.rejectEvents(), second.rejectEvents());
        assertEquals(first.lifecycleEventCount(), second.lifecycleEventCount());
    }

    private static ScenarioSpec spec(final long seed) {
        return new ScenarioSpec("runner-audit", seed, 9, CONFIG,
                new ScenarioWindow[]{
                        new ScenarioWindow(0, 2, 0),
                        new ScenarioWindow(3, 5, 1),
                        new ScenarioWindow(6, 8, 2)
                },
                true,
                ScenarioSimulatorConfig.defaults());
    }
}
