"""
Purpose: Verifies test packaging behavior for the Python SDK or notebook compatibility layer.
Usage: Run with pytest to protect packaging, retries, typing, and HTTP client contracts.
"""

import subprocess
import sys
from pathlib import Path


def test_pypi_dry_run() -> None:
    subprocess.run([sys.executable, "-m", "build", "--no-isolation"], check=True)
    artifacts = [str(path) for path in Path("dist").glob("*")]
    assert artifacts
    subprocess.run([sys.executable, "-m", "twine", "check", *artifacts], check=True)
