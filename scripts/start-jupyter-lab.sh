#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

API_PORT="${ADAPTIVE_QUANTUM_SOR_API_PORT:-8080}"
JUPYTER_PORT="${ADAPTIVE_QUANTUM_SOR_JUPYTER_PORT:-8888}"
WORKSPACE="${ADAPTIVE_QUANTUM_SOR_JUPYTER_WORKSPACE:-adaptive-quantum-sor}"
VENV_DIR="${ADAPTIVE_QUANTUM_SOR_NOTEBOOK_VENV:-$ROOT_DIR/.venv-notebook}"
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
export JAVA_HOME
export PYTHONPATH="$ROOT_DIR/python${PYTHONPATH:+:$PYTHONPATH}"

if [[ ! -x "$VENV_DIR/bin/python" ]]; then
  echo "Creating notebook Python virtual environment at ${VENV_DIR} ..."
  if ! python3 -m venv "$VENV_DIR"; then
    echo "Could not create a Python virtual environment." >&2
    echo "Install the venv package, then rerun this script." >&2
    echo "Example: sudo apt install python3-venv" >&2
    exit 1
  fi
fi

VENV_PYTHON="$VENV_DIR/bin/python"
VENV_JUPYTER="$VENV_DIR/bin/jupyter"

if [[ ! -x "$VENV_JUPYTER" ]] || ! "$VENV_PYTHON" - <<'PY' >/dev/null 2>&1
import pandas
PY
then
  echo "Installing notebook dependencies into ${VENV_DIR} ..."
  "$VENV_PYTHON" -m pip install --upgrade pip
  "$VENV_PYTHON" -m pip install -r "$ROOT_DIR/python/requirements.txt" jupyterlab
fi

API_PID=""
cleanup() {
  if [[ -n "$API_PID" ]] && kill -0 "$API_PID" >/dev/null 2>&1; then
    kill "$API_PID" >/dev/null 2>&1 || true
    wait "$API_PID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT INT TERM

echo "Building Java classes for the SOR engine API..."
"$ROOT_DIR/gradlew" -q classes

echo "Starting SOR engine API on http://127.0.0.1:${API_PORT} ..."
java -cp "$ROOT_DIR/build/classes/java/main:$ROOT_DIR/build/resources/main" \
  com.nitroj.adaptive.quantum.sor.AdaptiveQuantumSorApplication --api-port="$API_PORT" &
API_PID="$!"

echo "Waiting for API readiness..."
for _ in $(seq 1 40); do
  if "$VENV_PYTHON" - "$API_PORT" <<'PY' >/dev/null 2>&1
import sys
from urllib.request import urlopen
port = sys.argv[1]
with urlopen(f"http://127.0.0.1:{port}/stats/current", timeout=0.5) as response:
    raise SystemExit(0 if response.status == 200 else 1)
PY
  then
    break
  fi
  sleep 0.25
done

if ! kill -0 "$API_PID" >/dev/null 2>&1; then
  echo "SOR engine API failed to start." >&2
  exit 1
fi

echo "Opening JupyterLab workspace '${WORKSPACE}' with three notebook panels..."
echo "API base URL: http://127.0.0.1:${API_PORT}"
echo "Dataset: python/examples/sor_notebook_features_large.csv"
echo "Panels:"
echo "  1. notebooks/submit_parent_order.ipynb"
echo "  2. notebooks/live_stats_monitor.ipynb"
echo "  3. notebooks/scenario_runner.ipynb"

"$VENV_JUPYTER" lab \
  --port="$JUPYTER_PORT" \
  --NotebookApp.default_url="/lab/workspaces/${WORKSPACE}" \
  notebooks/submit_parent_order.ipynb \
  notebooks/live_stats_monitor.ipynb \
  notebooks/scenario_runner.ipynb
