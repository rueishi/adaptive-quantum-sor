from pathlib import Path


def test_notebook_replay() -> None:
    notebook = Path("../notebooks/submit_parent_order.ipynb").read_text()
    assert "from adaptive_quantum_sor_client import SorClient" in notebook
    assert "requests.post" not in notebook
