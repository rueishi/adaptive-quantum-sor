package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Unit coverage for live scenario reset/run controls. */
final class ScenarioControlServiceTest {
    private static final SorConfig CONFIG = new SorConfig(2, 5, 3, 2, SorConfig.RuntimeMode.DEMO, true);

    @Test
    void resetModeRejectsUnknownValuesAndMarksAppendAsNotReplaySafe() {
        assertFalse(ScenarioResetMode.APPEND.replaySafe());
        assertTrue(ScenarioResetMode.PURGE_AND_REPOPULATE.replaySafe());
        assertEquals(ScenarioResetMode.ISOLATED, ScenarioResetMode.parse("isolated"));
        assertThrows(IllegalArgumentException.class, () -> ScenarioResetMode.parse("mystery"));
    }

    @Test
    void resetRequestRejectsMissingScenarioIdInvalidTicksAndInvalidResetMode() {
        assertThrows(IllegalArgumentException.class, () -> new ScenarioResetRequest("", 1L, 1, ScenarioResetMode.ISOLATED));
        assertThrows(IllegalArgumentException.class, () -> new ScenarioResetRequest("s", 1L, 0, ScenarioResetMode.ISOLATED));
        assertThrows(IllegalArgumentException.class, () -> ScenarioResetRequest.parse(
                "{\"scenarioId\":\"s\",\"seed\":1,\"ticks\":1,\"resetMode\":\"bad\"}"));
    }

    @Test
    void resetSummaryRecordsClearedKeptRepopulatedStateCategories() {
        final ScenarioResetSummary summary = new ScenarioResetSummary("s", ScenarioResetMode.PURGE_AND_REPOPULATE,
                true, new String[]{"orders"}, new String[]{"activePolicy"}, new String[]{"market"});

        assertTrue(summary.toJson().contains("orders"));
        assertTrue(summary.toJson().contains("activePolicy"));
        assertTrue(summary.toJson().contains("market"));
    }

    @Test
    void isolatedRunDoesNotMutateLiveContext() {
        final Fixture fixture = fixture();
        fixture.queue.offer(new com.nitroj.adaptive.quantum.sor.model.OrderIntent(1L, 0, Side.BUY, 100L, 0, 1L));

        final ScenarioRunResult result = fixture.service.run(new ScenarioRunRequest(
                new ScenarioResetRequest("isolated", 1L, 2, ScenarioResetMode.ISOLATED)));

        assertTrue(result.success());
        assertEquals(1, fixture.queue.size());
        assertTrue(result.replaySafe());
    }

    @Test
    void purgeAndRepopulateClearsStatsOrdersOutcomesAndRebuildsBaseline() {
        final Fixture fixture = fixture();
        fixture.queue.offer(new com.nitroj.adaptive.quantum.sor.model.OrderIntent(1L, 0, Side.BUY, 100L, 0, 1L));

        final ScenarioResetSummary summary = fixture.service.reset(
                new ScenarioResetRequest("purge", 1L, 2, ScenarioResetMode.PURGE_AND_REPOPULATE));

        assertEquals(0, fixture.queue.size());
        assertTrue(String.join(",", summary.clearedState()).contains("orders"));
        assertTrue(String.join(",", summary.repopulatedState()).contains("market"));
    }

    @Test
    void keepPolicyPurgeStatsRetainsActivePolicy() {
        final Fixture fixture = fixture();
        final long policyHash = fixture.publisher.activePolicy().policyHash64;

        final ScenarioResetSummary summary = fixture.service.reset(
                new ScenarioResetRequest("keep-policy", 1L, 2, ScenarioResetMode.KEEP_POLICY_PURGE_STATS));

        assertEquals(policyHash, fixture.publisher.activePolicy().policyHash64);
        assertTrue(String.join(",", summary.keptState()).contains("activePolicy"));
    }

    @Test
    void appendModePreservesStateAndMarksRunNotReplaySafe() {
        final Fixture fixture = fixture();
        fixture.queue.offer(new com.nitroj.adaptive.quantum.sor.model.OrderIntent(1L, 0, Side.BUY, 100L, 0, 1L));

        final ScenarioRunResult result = fixture.service.run(new ScenarioRunRequest(
                new ScenarioResetRequest("append", 1L, 2, ScenarioResetMode.APPEND)));

        assertTrue(result.success());
        assertFalse(result.replaySafe());
        assertEquals(1, fixture.queue.size());
    }

    private static Fixture fixture() {
        final ParentOrderIntentQueue queue = new ParentOrderIntentQueue(8);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(TestPolicyFixtures.policy(), PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);
        final ScenarioEngineContext context = new ScenarioEngineContext(CONFIG, queue, publisher, new InMemoryLifecycleEventStore(16, true));
        return new Fixture(queue, publisher, new ScenarioControlService(context));
    }

    private record Fixture(ParentOrderIntentQueue queue, PolicyPublisher publisher, ScenarioControlService service) {
    }
}
