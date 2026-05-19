package com.nitroj.adaptive.quantum.sor.benchmark;

/**
 * Responsibility: run a simple static SOR latency/allocation benchmark.
 *
 * <p>Role in system: provides the Phase 1 manual benchmark harness before a
 * fuller CI profile is introduced.</p>
 *
 * <p>Relationships: uses {@link BenchmarkFixture} and static executioner.</p>
 *
 * <p>Lifecycle: invoked by tests, scripts, or manual Java runs.</p>
 *
 * <p>Design intent: dependency-free harness reports latency and allocation-like
 * memory delta without requiring JMH plugin setup in the current task.</p>
 */
public final class StaticSorBenchmark {
    /** Runs repeated static SOR route decisions and returns a report. */
    public BenchmarkReport run(final int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        final BenchmarkFixture fixture = new BenchmarkFixture();
        final var executioner = fixture.staticSorExecutioner();
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
