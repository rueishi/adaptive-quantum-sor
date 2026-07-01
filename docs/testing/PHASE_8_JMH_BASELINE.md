# Phase 8 JMH Baseline

This document began as the Phase 8 baseline for hot-path benchmark regression
checks. Phase 9 refreshes the runtime profile to keep later cards pinned to a
fixed JDK 25/ZGC/Compact Object Headers reference point instead of drifting
with local developer machines.

## Environment Metadata

| Field | Value |
|---|---|
| Phase | P9-17 refresh of P8-01 baseline |
| JDK | OpenJDK 25.0.2+10-Ubuntu-124.04 |
| Required toolchain | JDK 25 LTS |
| OS | Linux 6.6.87.2-microsoft-standard-WSL2 amd64 |
| Gradle | 9.4.1 |
| GC | Generational ZGC via `-XX:+UseZGC` |
| Object headers | Compact Object Headers via `-XX:+UseCompactObjectHeaders` |
| Heap | `-Xms2g -Xmx2g` for tests, `-Xms4g -Xmx4g` default launcher heap |
| Benchmark command | `./gradlew :sor-core:jmh -PjmhInclude=PolicyDrivenSorJmhBenchmark.strictRouteInto -PjmhWarmupIterations=1 -PjmhMeasurementIterations=1 -PjmhForks=1` |

## ZGC And Compact Object Headers

Compact Object Headers are enabled for the Phase 9 runtime profile. The
production launcher, Gradle test JVMs, and container metadata use
`-XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch`. The local JDK
25.0.2 runtime accepts these flags together and `-XX:+PrintFlagsFinal` must
report both `UseZGC=true` and `UseCompactObjectHeaders=true`.

## Benchmark Baseline

The `jmhRegressionCheck` Gradle task reads the benchmark name and baseline
score from this table. Scores are expressed in nanoseconds per operation; lower
is better.

| Benchmark | Baseline score nanos |
|---|---:|
| com.nitroj.sor.core.benchmark.PolicyDrivenSorJmhBenchmark.strictRouteInto | 70.304 |

## Hot-Path Allocation

The benchmark is run with JMH's GC profiler. The Phase 8 gate treats any
hot-path allocation growth as a regression to inspect before the next card
continues.

## Hot-Path Latency

The strict route benchmark is the current hot-path latency proxy until the
framework split introduces more granular `HotPathAllocationBenchmark` and
`HotPathLatencyBenchmark` classes.

## Optimizer Cycle Latency

The existing optimizer cycle benchmarks remain part of the broader benchmark
profile. They are recorded here as Phase 8 environment metadata even though
`P8-01` gates the strict route hot path first.
