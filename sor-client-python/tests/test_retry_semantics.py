"""
Purpose: Verifies test retry semantics behavior for the Python SDK or notebook compatibility layer.
Usage: Run with pytest to protect packaging, retries, typing, and HTTP client contracts.
"""

from __future__ import annotations

from typing import Any

import pytest

from adaptive_quantum_sor_client import SorClient, SorClientRetryExhaustedError


class SequenceSession:
    def __init__(self, statuses: list[int]) -> None:
        self.statuses = statuses
        self.calls = 0

    def request(self, method: str, url: str, **kwargs: Any) -> Any:
        self.calls += 1
        status = self.statuses[min(self.calls - 1, len(self.statuses) - 1)]
        return type(
            "Response",
            (),
            {
                "status_code": status,
                "text": str(status),
                "json": lambda self: {"parentOrderId": 4, "filledQty": 0, "remainingQty": 1, "status": 1},
            },
        )()


def test_retries_on_503() -> None:
    sleeps: list[float] = []
    session = SequenceSession([503, 503, 200])
    client = SorClient(
        "http://sor",
        session=session,  # type: ignore[arg-type]
        max_retries=3,
        backoff_seconds=0.1,
        sleep=sleeps.append,
    )

    status = client.submit_parent_order(instrument_id=1, side=1, quantity=1, urgency_id=0)

    assert status.parent_order_id == 4
    assert session.calls == 3
    assert sleeps == [0.1, 0.2]


def test_gives_up_after_max_retries() -> None:
    session = SequenceSession([503])
    client = SorClient("http://sor", session=session, max_retries=2, sleep=lambda _: None)  # type: ignore[arg-type]

    with pytest.raises(SorClientRetryExhaustedError) as raised:
        client.submit_parent_order(instrument_id=1, side=1, quantity=1, urgency_id=0)

    assert raised.value.attempts == 3
    assert session.calls == 3
