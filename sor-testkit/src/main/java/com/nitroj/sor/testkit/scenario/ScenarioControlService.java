package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.core.lifecycle.LifecycleEvent;
import com.nitroj.sor.core.lifecycle.LifecycleEventType;

/**
 * Responsibility: execute live scenario reset and run requests.
 *
 * <p>Role in system: bridges notebook/API scenario controls to the deterministic
 * {@link ScenarioRunner} while keeping reset behavior explicit and audited.</p>
 *
 * <p>Relationships: mutates only {@link ScenarioEngineContext} for live modes;
 * isolated mode runs through a fresh scenario context and does not drain live
 * queues or alter policy state.</p>
 *
 * <p>Lifecycle: one service per API server.</p>
 *
 * <p>Design intent: failed reset/run operations return non-successful evidence
 * and do not publish partial state as valid.</p>
 */
public final class ScenarioControlService {
    private final ScenarioEngineContext context;
    private final ScenarioRunner runner;
    private long eventId = 10_000L;

    public ScenarioControlService(final ScenarioEngineContext context) {
        this(context, new ScenarioRunner());
    }

    public ScenarioControlService(final ScenarioEngineContext context, final ScenarioRunner runner) {
        if (context == null || runner == null) {
            throw new IllegalArgumentException("context and runner must not be null");
        }
        this.context = context;
        this.runner = runner;
    }

    public ScenarioResetSummary reset(final ScenarioResetRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        final ScenarioResetSummary summary;
        if (request.resetMode() == ScenarioResetMode.PURGE_AND_REPOPULATE) {
            drainOrders();
            summary = summary(request, new String[]{"orders", "stats", "outcomes", "market", "sessions", "throttles"},
                    new String[]{"activePolicy"}, new String[]{"market", "sessions", "throttles", "stats"});
        } else if (request.resetMode() == ScenarioResetMode.KEEP_POLICY_PURGE_STATS) {
            drainOrders();
            summary = summary(request, new String[]{"orders", "stats", "outcomes"},
                    new String[]{"activePolicy", "policySnapshotStore"}, new String[]{"stats"});
        } else if (request.resetMode() == ScenarioResetMode.ISOLATED) {
            summary = summary(request, new String[]{"isolatedOnly"}, new String[]{"liveOrders", "activePolicy", "stats"},
                    new String[]{"isolatedContext"});
        } else {
            summary = summary(request, new String[]{}, new String[]{"orders", "activePolicy", "stats", "outcomes"},
                    new String[]{"appendedScenario"});
        }
        context.recordReset(summary);
        context.lifecycleStore.append(new LifecycleEvent(nextEventId(), nextEventId(), 1,
                LifecycleEventType.SCENARIO_RESET, 0L,
                "scenario reset " + request.scenarioId() + " " + request.resetMode()));
        return summary;
    }

    public ScenarioRunResult run(final ScenarioRunRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        try {
            final ScenarioResetSummary reset = reset(request.resetRequest());
            final ScenarioSummary summary = runner.run(toSpec(request.resetRequest()));
            context.recordScenario(summary);
            context.lifecycleStore.append(new LifecycleEvent(nextEventId(), nextEventId(), 1,
                    LifecycleEventType.SCENARIO_RUN, 0L,
                    "scenario run " + summary.scenarioId() + " checksum " + summary.bookChecksum()));
            return new ScenarioRunResult(true, reset.replaySafe(), "scenario run complete", reset, summary);
        } catch (RuntimeException ex) {
            final ScenarioResetSummary failedReset = context.lastResetSummary() == null
                    ? summary(request.resetRequest(), new String[]{}, new String[]{"activePolicy"}, new String[]{})
                    : context.lastResetSummary();
            return new ScenarioRunResult(false, false, ex.getMessage(), failedReset, null);
        }
    }

    public ScenarioSummary lastSummary() {
        return context.lastScenarioSummary();
    }

    private ScenarioSpec toSpec(final ScenarioResetRequest request) {
        return new ScenarioSpec(request.scenarioId(), request.seed(), request.ticks(), context.config,
                new ScenarioWindow[]{new ScenarioWindow(0, request.ticks() - 1, 0)},
                true,
                ScenarioSimulatorConfig.defaults());
    }

    private ScenarioResetSummary summary(
            final ScenarioResetRequest request,
            final String[] cleared,
            final String[] kept,
            final String[] repopulated
    ) {
        return new ScenarioResetSummary(request.scenarioId(), request.resetMode(), request.resetMode().replaySafe(),
                cleared, kept, repopulated);
    }

    private void drainOrders() {
        while (context.orderQueue.poll() != null) {
            // Drain live demo queue explicitly during purge modes.
        }
    }

    private long nextEventId() {
        return eventId++;
    }
}
