# P8-23 Completion Report

## Implemented Scope

P8-23 completed simulator parity for catalog, fee schedule, session, throttle,
risk, and publication guard behavior in the new `com.nitroj.sor.sim` runtime.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P8-SIM-012 LegacyFeeScheduleParityTest
P8-SIM-013 LegacyCatalogParityTest
P8-SIM-017 LegacyRiskProviderParityTest
P8-SIM-018 LegacyPublicationGuardParityTest, SimulatedClusterControllerLifecycleEventParityTest
P8-SIM-021 fixed-seed parity tests retained as golden compatibility tests
```

Planned ACs:

```text
none
```

Failed ACs:

```text
none
```

## Validation Commands

```bash
./gradlew :sor-testkit:test --tests 'com.nitroj.sor.testkit.sim.scenario.*'
./gradlew :sor-testkit:test --tests 'com.nitroj.sor.testkit.sim.adapters.Legacy*ParityTest'
```
