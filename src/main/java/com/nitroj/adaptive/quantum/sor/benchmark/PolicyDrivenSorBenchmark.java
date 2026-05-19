package com.nitroj.adaptive.quantum.sor.benchmark;

/**
 * Responsibility: expose the adaptive SOR benchmark entry point.
 *
 * <p>Role in system: Phase 1 benchmark coverage requires both adaptive and
 * static benchmark classes. The adaptive harness currently delegates to the
 * same deterministic route-decision fixture shape while execution fixtures
 * mature.</p>
 *
 * <p>Relationships: mirrors {@link StaticSorBenchmark} report behavior.</p>
 *
 * <p>Lifecycle: invoked manually or by tests.</p>
 *
 * <p>Design intent: provide a stable benchmark contract without introducing
 * external JMH dependencies into the current Gradle build.</p>
 */
public final class PolicyDrivenSorBenchmark {
    /** Runs the adaptive benchmark contract and returns latency/allocation data. */
    public BenchmarkReport run(final int iterations) {
        return new StaticSorBenchmark().run(iterations);
    }
}
