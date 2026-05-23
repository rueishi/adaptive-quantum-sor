package com.nitroj.sor.core.benchmark;

import com.nitroj.sor.api.Observability;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/** Allocation gate for the lightweight observability hot-path interface. */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class HotPathAllocationWithObservabilityBenchmark {
    @Benchmark
    public long observeRoute(final ObsState state) {
        state.observability.recordRouteDecisionLatency(state.next++);
        state.observability.recordParentOrderRingDepth((int) (state.next & 1023));
        return state.next;
    }

    @State(Scope.Thread)
    public static class ObsState {
        Observability observability;
        long next;

        @Setup
        public void setup() {
            observability = Observability.noop();
        }
    }
}
