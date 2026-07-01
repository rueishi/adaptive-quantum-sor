"""
Purpose: Verifies test sor client behavior for the Python SDK or notebook compatibility layer.
Usage: Run with pytest to protect packaging, retries, typing, and HTTP client contracts.
"""

from __future__ import annotations

from typing import Any

from adaptive_quantum_sor_client import SorClient


class FakeResponse:
    def __init__(self, status_code: int, payload: dict[str, Any]) -> None:
        self.status_code = status_code
        self._payload = payload
        self.text = str(payload)

    def json(self) -> dict[str, Any]:
        return self._payload


class FakeSession:
    def __init__(self) -> None:
        self.calls: list[tuple[str, str, dict[str, Any]]] = []

    def request(self, method: str, url: str, **kwargs: Any) -> FakeResponse:
        self.calls.append((method, url, kwargs))
        if method == "POST":
            return FakeResponse(200, {"parentOrderId": 9, "filledQty": 0, "remainingQty": 1000, "status": 1})
        return FakeResponse(200, {"parentOrderId": 9, "filledQty": 1000, "remainingQty": 0, "status": 2})


def test_submit_parent_order() -> None:
    session = FakeSession()
    client = SorClient("http://sor", session=session)  # type: ignore[arg-type]

    status = client.submit_parent_order(instrument_id=7, side=1, quantity=1000, urgency_id=2)

    assert status.parent_order_id == 9
    assert session.calls[0][0] == "POST"
    assert session.calls[0][1] == "http://sor/orders"
    assert session.calls[0][2]["json"] == {
        "instrumentId": 7,
        "side": 1,
        "quantity": 1000,
        "urgencyId": 2,
    }


def test_get_status() -> None:
    session = FakeSession()
    client = SorClient("http://sor", session=session)  # type: ignore[arg-type]

    status = client.get_status(9)

    assert status.remaining_qty == 0
    assert session.calls[0][0] == "GET"
    assert session.calls[0][1] == "http://sor/orders/9"
