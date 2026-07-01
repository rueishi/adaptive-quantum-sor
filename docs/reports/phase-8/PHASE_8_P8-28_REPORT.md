# P8-28 Completion Report

## Implemented Scope

P8-28 migrated `ScenarioRunner` into `sor-test-server` ownership and routed it
through `SorEngineBuilder` plus simulator adapters.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P8-SIM-019 ScenarioRunnerNewSimulatorPathTest, ScenarioApiNewSimulatorPathTest, NoLegacySimulatorProductionImportTest
P8-SIM-020 ScenarioSummaryCompatibilityTest, ScenarioRunnerTest
P8-SIM-021 scenario compatibility tests retained as golden evidence
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
./gradlew :sor-testkit:test --tests 'com.nitroj.sor.testkit.scenario.*'
./gradlew :sor-core:test --tests 'com.nitroj.sor.core.boot.NoLegacySimulatorProductionImportTest'
```
