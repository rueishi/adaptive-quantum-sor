"""
Purpose: Verifies test notebook compat behavior for the Python SDK or notebook compatibility layer.
Usage: Run with pytest to protect packaging, retries, typing, and HTTP client contracts.
"""

from pathlib import Path


def test_notebook_replay() -> None:
    notebook = Path("../notebooks/submit_parent_order.ipynb").read_text()
    assert "from adaptive_quantum_sor_client import SorClient" in notebook
    assert "requests.post" not in notebook
