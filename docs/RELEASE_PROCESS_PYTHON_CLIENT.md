# Python Client Release Process

1. Confirm PyPI project ownership for `adaptive-quantum-sor-client`.
2. Install release tooling with `pip install -e sor-client-python[dev]`.
3. Run `pytest -v` and `mypy adaptive_quantum_sor_client`.
4. Build distributions with `python -m build`.
5. Validate metadata with `twine check dist/*`.
6. Upload to TestPyPI, smoke-test install from a clean virtualenv, then upload to PyPI.
7. Update notebooks to use the released SDK version and rerun notebook compatibility checks.
