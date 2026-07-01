#!/usr/bin/env bash
#
# Sample-server launcher for the Phase 8 Adaptive Quantum SOR JVM.
#
# The script centralizes the JVM flags that make the framework's runtime shape
# reproducible outside Gradle. The launchable application entrypoint lives in
# the `sor-test-server` module.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

JAVA_BIN="${JAVA_BIN:-java}"
HEAP_SIZE="${ADAPTIVE_QUANTUM_SOR_HEAP_SIZE:-4g}"
GC_LOG_DIR="${ADAPTIVE_QUANTUM_SOR_GC_LOG_DIR:-$ROOT_DIR/build/logs}"

mkdir -p "$GC_LOG_DIR"

export JAVA_OPTS="-XX:+UseZGC -XX:+UseCompactObjectHeaders -XX:+AlwaysPreTouch -Xms${HEAP_SIZE} -Xmx${HEAP_SIZE} -XX:+UseTransparentHugePages -XX:+UseNUMA -Xlog:gc*:file=${GC_LOG_DIR}/gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M ${JAVA_OPTS:-}"

exec "$ROOT_DIR/gradlew" :sor-test-server:run --args="$*"
