package com.nitroj.sor.core.benchmark;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.core.intake.ParentOrderRing;
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

/** Regression benchmark for P8-08 parent-order ring offer latency. */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class RingBufferOfferLatencyBenchmark {
    @Benchmark
    public boolean offerThenDrain(final RingState state) {
        final boolean offered = state.ring.offer(state.nextId++, state.request, state.nextId);
        state.ring.drain(1);
        return offered;
    }

    @State(Scope.Thread)
    public static class RingState {
        ParentOrderRing ring;
        ParentOrderRequest request;
        long nextId;

        @Setup
        public void setup() {
            ring = new ParentOrderRing(16_384);
            request = ParentOrderRequest.builder().instrumentId(0).side(Side.BUY).quantity(1).urgency(0).build();
            nextId = 1;
        }
    }
}
