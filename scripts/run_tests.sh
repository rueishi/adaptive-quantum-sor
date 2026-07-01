#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export JAVA_HOME

PROFILE="${1:-all}"

run_gradle() {
  "$ROOT_DIR/gradlew" "$@"
}

case "$PROFILE" in
  unit)
    run_gradle :sor-core:test \
      --tests 'com.nitroj.sor.core.model.*' \
      --tests 'com.nitroj.sor.core.state.*' \
      --tests 'com.nitroj.sor.core.policy.*' \
      --tests 'com.nitroj.sor.core.optimizer.*'
    ;;
  integration)
    run_gradle :sor-core:test \
      --tests 'com.nitroj.sor.core.integration.*'
    run_gradle :sor-test-server:test \
      --tests 'com.nitroj.sor.testserver.e2e.*' \
      --tests 'com.nitroj.sor.testserver.*'
    ;;
  simulator)
    run_gradle :sor-testkit:test \
      --tests 'com.nitroj.sor.testkit.sim.*' \
      --tests 'com.nitroj.sor.testkit.sim.adapters.*' \
      --tests 'com.nitroj.sor.testkit.sim.scenario.*'
    run_gradle :sor-test-server:test \
      --tests 'com.nitroj.sor.testserver.*'
    ;;
  scenario)
    run_gradle :sor-testkit:test \
      --tests 'com.nitroj.sor.testkit.scenario.*' \
      --tests 'com.nitroj.sor.testkit.ml.*' \
      --tests 'com.nitroj.sor.testkit.policy.robust.*'
    run_gradle :sor-test-server:test \
      --tests 'com.nitroj.sor.testserver.e2e.SorEndToEndTest.replayableScenarioProducesEquivalentSummary' \
      --tests 'com.nitroj.sor.testserver.e2e.SorEndToEndTest.scenarioLiquidityDisappearanceRoutesSafely'
    ;;
  policy)
    run_gradle :sor-core:test \
      --tests 'com.nitroj.sor.core.policy.compile.DefaultPolicyCompilerTest' \
      --tests 'com.nitroj.sor.core.policy.validation.DefaultPolicyValidatorTest' \
      --tests 'com.nitroj.sor.core.policy.lint.DefaultPolicyLintTest'
    ;;
  native)
    run_gradle :sor-core:nativeTest
    ;;
  all)
    run_gradle check
    ;;
  *)
    echo "unknown profile: $PROFILE" >&2
    echo "usage: scripts/run_tests.sh [unit|integration|simulator|scenario|policy|native|all]" >&2
    exit 2
    ;;
esac
