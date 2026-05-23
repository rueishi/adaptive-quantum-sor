# Phase 8 Completion Report

Date: 2026-05-21

## Readiness Summary

Phase 8 framework extraction is complete for the documented non-deferred scope.
The repository now contains the framework split around `sor-api`, `sor-core`,
simulator, transport, codec, observability, client, and deployment
modules while preserving Phase 1-7 behavior.

Simulator parity work from P8-23 through P8-30 has migrated the runtime path to
`sor-test-server`; the legacy `com.nitroj.adaptive.quantum.sor.sim` package has
now been deleted.

The obsolete `AdaptiveQuantumSorApplication`, `SorEngineRuntime`, and
`NotebookDemoApiLauncher` wrappers have also been removed. Launch ownership now
lives in `sor-test-server` through `SimulatorServerApplication`.

The completion gate in `adaptive_quantum_sor_spec_phase8.md` requires all 17
non-deferred card reports. Those reports are present under `docs/` as
`PHASE_8_P8-<card>_REPORT.md`. P8-12 remains deferred and does not gate Phase 8.

## Implemented Task Cards

```text
P8-01 JDK 25 Toolchain and ZGC Baseline
P8-02 Multi-project Gradle Layout
P8-03 sor-api: Engine Contract
P8-04 sor-api: SPI Interfaces
P8-05 HotRouteBook ABI v1
P8-06 SorEngineImpl and JIT Warmup Contract
P8-07 Simulator Rewrite Against the SPI
P8-08 Agrona Ring-buffer Order Intake
P8-09 sor-observability Module
P8-10 HTTP Control Plane Modernization
P8-13 SBE Wire Protocol
P8-14 Aeron Transport Server and Client
P8-15 Panama FFM Migration of Native Bridges
P8-20 sor-client-java SDK
P8-21 sor-client-python SDK
P8-22 sor-test-server Sample Deployment
```

Deferred / non-gating:

```text
P8-12 Postgres Downstream Tail Adapter
P8-11 Chronicle Queue Persistence Adapter
P8-16 through P8-19 Reference Adapter Modules
```

## Validation Commands

```bash
./gradlew test
./gradlew check
cd sor-client-python && pytest
```

## Planned ACs

```text
P8-SIM-012 through P8-SIM-024 for full legacy simulator parity, simulator integration interfaces, legacy mapping removal, and test-server ScenarioRunner migration
```

## Failed ACs

```text
none
```

## Known Limits

P8-12 remains deferred by specification. External publication to public Maven
Central and PyPI is represented by dry-run/package metadata tests rather than a
live registry push from this workspace.

P8-07 currently proves simulator SPI shape and transitional mapping coverage,
not full legacy behavior parity. P8-30 removes `@SimulatorMapping(legacy = "...")`
metadata and requires simulator runtime components to implement the public SOR
SPI contracts directly, the same contracts integrator-owned adapters implement.
Deletion of `com.nitroj.adaptive.quantum.sor.sim` was completed after P8-23
through P8-30 parity coverage and the explicit cleanup request.
