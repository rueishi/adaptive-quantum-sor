# P8-25 Completion Report

## Implemented Scope

P8-25 completed simulator parity for venue outcomes: ACK, fill, reject,
toxicity, slippage, liquidity-dependent behavior, throttle, and session state.

## Acceptance Criteria Evidence

Implemented ACs:

```text
P8-SIM-015 SimulatedVenueAdapterLiquidityOutcomeTest, SimulatedVenueAdapterSessionThrottleTest, LegacyVenueBehaviorParityTest, LegacySessionThrottleParityTest
P8-SIM-020 ScenarioRunnerNewSimulatorPathTest, ScenarioSummaryCompatibilityTest
P8-SIM-021 legacy venue parity tests retained as golden compatibility tests
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
./gradlew :sor-testkit:test --tests 'com.nitroj.sor.testkit.sim.adapters.*Venue*'
```
