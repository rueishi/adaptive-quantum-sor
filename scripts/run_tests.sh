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
      --tests 'com.nitroj.adaptive.quantum.sor.model.*' \
      --tests 'com.nitroj.adaptive.quantum.sor.state.*' \
      --tests 'com.nitroj.adaptive.quantum.sor.policy.*' \
      --tests 'com.nitroj.adaptive.quantum.sor.optimizer.*'
    ;;
  integration)
    run_gradle :sor-core:test \
      --tests 'com.nitroj.adaptive.quantum.sor.integration.*'
    run_gradle :sor-test-server:test \
      --tests 'com.nitroj.adaptive.quantum.sor.e2e.*' \
      --tests 'com.nitroj.adaptive.quantum.sor.api.*'
    ;;
  simulator)
    run_gradle :sor-test-server:test
    ;;
  scenario)
    run_gradle :sor-test-server:test \
      --tests 'com.nitroj.adaptive.quantum.sor.scenario.*' \
      --tests 'com.nitroj.adaptive.quantum.sor.e2e.SorEndToEndTest.replayableScenarioProducesEquivalentSummary' \
      --tests 'com.nitroj.adaptive.quantum.sor.e2e.SorEndToEndTest.scenarioLiquidityDisappearanceRoutesSafely'
    ;;
  policy)
    run_gradle :sor-core:test \
      --tests 'com.nitroj.adaptive.quantum.sor.policy.compile.DefaultPolicyCompilerTest' \
      --tests 'com.nitroj.adaptive.quantum.sor.policy.validation.DefaultPolicyValidatorTest' \
      --tests 'com.nitroj.adaptive.quantum.sor.policy.lint.DefaultPolicyLintTest'
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
