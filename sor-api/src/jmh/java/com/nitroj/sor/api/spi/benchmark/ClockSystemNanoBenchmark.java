package com.nitroj.sor.api.spi.benchmark;

import com.nitroj.sor.api.spi.Clock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Responsibility: microbenchmarks the default API clock implementation.
 *
 * <p>Role in system: supports P8-04's requirement that clock access is cheap
 * enough for injected hot-path use.</p>
 *
 * <p>Relationships: benchmarks {@link Clock#systemNano()} without depending on
 * engine implementation modules.</p>
 *
 * <p>Lifecycle: run manually through `./gradlew :sor-api:jmh`.</p>
 *
 * <p>Design intent: keep time-source overhead visible before engine code is
 * migrated to universal clock injection.</p>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class ClockSystemNanoBenchmark {
    private final Clock clock = Clock.systemNano();

    /**
     * Measures monotonic nanosecond reads.
     */
    @Benchmark
    public long nanoTime() {
        return clock.nanoTime();
    }

    /**
     * Measures epoch nanosecond reads.
     */
    @Benchmark
    public long epochNanos() {
        return clock.epochNanos();
    }
}
