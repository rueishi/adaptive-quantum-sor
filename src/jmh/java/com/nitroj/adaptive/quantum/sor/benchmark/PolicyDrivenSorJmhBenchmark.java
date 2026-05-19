package com.nitroj.adaptive.quantum.sor.benchmark;

import com.nitroj.adaptive.quantum.sor.audit.RouteAuditEvent;
import com.nitroj.adaptive.quantum.sor.execution.MutableRouteDecisionResult;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
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

/**
 * JMH evidence for the strict policy-driven routing path.
 *
 * <p>Run with:</p>
 *
 * <pre>
 * ./gradlew jmh -PjmhInclude=PolicyDrivenSorJmhBenchmark.strictRouteInto
 * </pre>
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class PolicyDrivenSorJmhBenchmark {
    private BenchmarkFixture fixture;
    private PolicyDrivenSorExecutioner executioner;
    private MutableRouteDecisionResult result;
    private RouteAuditEvent audit;

    @Setup
    public void setup() {
        fixture = new BenchmarkFixture();
        executioner = fixture.policyDrivenSorExecutioner();
        result = new MutableRouteDecisionResult();
        audit = new RouteAuditEvent();
    }

    @Benchmark
    public MutableRouteDecisionResult strictRouteInto() {
        return executioner.routeInto(fixture.parentOrder, fixture.childOrderBuffer, result, audit);
    }
}
