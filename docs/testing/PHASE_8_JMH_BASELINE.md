# Phase 8 JMH Baseline

This document is the Phase 8 baseline for hot-path benchmark regression
checks. It is intentionally committed before the framework refactor so later
cards compare against a fixed JDK 25/ZGC reference point instead of drifting
with local developer machines.

## Environment Metadata

| Field | Value |
|---|---|
| Phase | P8-01 |
| JDK | OpenJDK 25.0.2+10-Ubuntu-124.04 |
| Required toolchain | JDK 25 LTS |
| OS | Linux 6.6.87.2-microsoft-standard-WSL2 amd64 |
| Gradle | 9.4.1 |
| GC | Generational ZGC via `-XX:+UseZGC` |
| Heap | `-Xms2g -Xmx2g` for tests, `-Xms4g -Xmx4g` default launcher heap |
| Benchmark command | `./gradlew jmh -PjmhInclude=PolicyDrivenSorJmhBenchmark.strictRouteInto -PjmhWarmupIterations=1 -PjmhMeasurementIterations=1 -PjmhForks=1` |

## ZGC And Compact Object Headers

Compact Object Headers are deliberately not enabled for Phase 8. JDK 25 rejects
`-XX:+UseCompactObjectHeaders` when combined with ZGC, so the production
launcher and test JVM configuration use ZGC without compact headers. Revisit
this only when a future LTS supports both features together.

## Benchmark Baseline

The `jmhRegressionCheck` Gradle task reads the benchmark name and baseline
score from this table. Scores are expressed in nanoseconds per operation; lower
is better.

| Benchmark | Baseline score nanos |
|---|---:|
| com.nitroj.adaptive.quantum.sor.benchmark.PolicyDrivenSorJmhBenchmark.strictRouteInto | 70.304 |

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
