import subprocess
import sys


def test_no_mypy_errors() -> None:
    result = subprocess.run(
        [sys.executable, "-m", "mypy", "adaptive_quantum_sor_client"],
        check=False,
        capture_output=True,
        text=True,
    )
    assert result.returncode == 0, result.stdout + result.stderr
