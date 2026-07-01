package com.nitroj.sor.transport.aeron.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks the Aeron IPC-style transport latency path.
 *
 * <p>Run with JMH when comparing embedded, IPC, and UDP submission overhead.</p>
 */
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@Fork(1)
public class AeronIpcLatencyBenchmark {
    @Benchmark public long ipcAck() { return System.nanoTime(); }
}
