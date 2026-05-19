#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JAVA_HOME

PROFILE="${1:-manual}"

case "$PROFILE" in
  manual)
    "$ROOT_DIR/gradlew" test --tests 'com.nitroj.adaptive.quantum.sor.benchmark.BenchmarkHarnessTest'
    ;;
  comparison)
    "$ROOT_DIR/gradlew" test --tests 'com.nitroj.adaptive.quantum.sor.metrics.ComparisonRunnerTest'
    PYTHONPATH="$ROOT_DIR/python${PYTHONPATH:+:$PYTHONPATH}" python3 "$ROOT_DIR/python/compare_sor_dataset.py" \
      --dataset "${ADAPTIVE_QUANTUM_SOR_COMPARISON_DATASET:-$ROOT_DIR/python/examples/sor_notebook_features_100k.csv}" \
      --output "${ADAPTIVE_QUANTUM_SOR_COMPARISON_REPORT:-$ROOT_DIR/build/reports/benchmarks/sor-comparison-report.md}"
    ;;
  *)
    echo "unknown benchmark profile: $PROFILE" >&2
    echo "usage: scripts/run_benchmarks.sh [manual|comparison]" >&2
    exit 2
    ;;
esac
