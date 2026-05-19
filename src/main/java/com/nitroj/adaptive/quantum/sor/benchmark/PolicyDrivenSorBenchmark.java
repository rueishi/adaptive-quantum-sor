package com.nitroj.adaptive.quantum.sor.benchmark;

/**
 * Responsibility: expose the adaptive SOR benchmark entry point.
 *
 * <p>Role in system: Phase 1 benchmark coverage requires both adaptive and
 * static benchmark classes. The adaptive harness routes through a published
 * baseline policy so it exercises the L0 policy-driven execution path.</p>
 *
 * <p>Relationships: mirrors {@link StaticSorBenchmark} report behavior while
 * using {@link BenchmarkFixture#policyDrivenSorExecutioner()}.</p>
 *
 * <p>Lifecycle: invoked manually or by tests.</p>
 *
 * <p>Design intent: provide a stable benchmark contract without introducing
 * external JMH dependencies into the current Gradle build.</p>
 */
public final class PolicyDrivenSorBenchmark {
    /** Runs the adaptive benchmark contract and returns latency/allocation data. */
    public BenchmarkReport run(final int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        final BenchmarkFixture fixture = new BenchmarkFixture();
        final var executioner = fixture.policyDrivenSorExecutioner();
        final long beforeMemory = usedMemory();
        final long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            executioner.route(fixture.parentOrder, fixture.childOrderBuffer);
        }
        final long elapsed = Math.max(1L, System.nanoTime() - start);
        final long afterMemory = usedMemory();
        return new BenchmarkReport(iterations, elapsed / iterations, Math.max(0L, afterMemory - beforeMemory), 2_000L);
    }

    private static long usedMemory() {
        final Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
