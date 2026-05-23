package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.lifecycle.InMemoryLifecycleEventStore;
import com.nitroj.adaptive.quantum.sor.model.ParentOrderIntentQueue;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;

/**
 * Responsibility: hold mutable live-demo dependencies for scenario controls.
 *
 * <p>Role in system: distinguishes live API state from isolated Gradle/JUnit
 * scenario contexts.</p>
 *
 * <p>Relationships: owned by {@link ScenarioControlService}; references the
 * live order queue, policy publisher, lifecycle store, and config.</p>
 *
 * <p>Lifecycle: created with the API server and reused for notebook requests.</p>
 *
 * <p>Design intent: make state reset boundaries explicit without adding global
 * mutable singletons.</p>
 */
public final class ScenarioEngineContext {
    public final SorConfig config;
    public final ParentOrderIntentQueue orderQueue;
    public final PolicyPublisher publisher;
    public final InMemoryLifecycleEventStore lifecycleStore;
    private ScenarioResetSummary lastResetSummary;
    private ScenarioSummary lastScenarioSummary;

    public ScenarioEngineContext(
            final SorConfig config,
            final ParentOrderIntentQueue orderQueue,
            final PolicyPublisher publisher,
            final InMemoryLifecycleEventStore lifecycleStore
    ) {
        if (config == null || orderQueue == null || publisher == null || lifecycleStore == null) {
            throw new IllegalArgumentException("scenario engine dependencies must not be null");
        }
        this.config = config;
        this.orderQueue = orderQueue;
        this.publisher = publisher;
        this.lifecycleStore = lifecycleStore;
    }

    public ScenarioResetSummary lastResetSummary() {
        return lastResetSummary;
    }

    public ScenarioSummary lastScenarioSummary() {
        return lastScenarioSummary;
    }

    void recordReset(final ScenarioResetSummary summary) {
        lastResetSummary = summary;
    }

    void recordScenario(final ScenarioSummary summary) {
        lastScenarioSummary = summary;
    }
}
