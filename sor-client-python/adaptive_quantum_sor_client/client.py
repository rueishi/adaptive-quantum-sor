"""
Purpose: Provides the synchronous requests-based Python SDK client for SOR HTTP endpoints.
Usage: Use SorClient from scripts or applications that need blocking order, status, stats, policy, and event calls.
"""

from __future__ import annotations

import time
from collections.abc import Callable, Iterator
from typing import Any

import requests

from .exceptions import SorClientHttpError, SorClientRetryExhaustedError
from .models import LifecycleEvent, OrderStatus, PolicySnapshot

_TRANSIENT_STATUS = {429, 500, 502, 503, 504}


class SorClient:
    """Typed synchronous client for the HTTP control-plane API."""

    def __init__(
        self,
        base_url: str,
        *,
        session: requests.Session | None = None,
        timeout: float = 5.0,
        max_retries: int = 3,
        backoff_seconds: float = 0.05,
        sleep: Callable[[float], None] = time.sleep,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.session = session or requests.Session()
        self.timeout = timeout
        self.max_retries = max_retries
        self.backoff_seconds = backoff_seconds
        self._sleep = sleep

    def submit_parent_order(
        self,
        *,
        instrument_id: int,
        side: int,
        quantity: int,
        urgency_id: int,
        client_order_id: int | None = None,
    ) -> OrderStatus:
        """Submit a parent order and return the typed order status."""
        payload: dict[str, int] = {
            "instrumentId": instrument_id,
            "side": side,
            "quantity": quantity,
            "urgencyId": urgency_id,
        }
        if client_order_id is not None:
            payload["clientOrderId"] = client_order_id
        response = self._request("POST", "/orders", json=payload)
        return OrderStatus.from_json(response.json())

    def cancel(self, parent_order_id: int) -> None:
        """Cancel a parent order if the server exposes the cancel endpoint."""
        self._request("DELETE", f"/orders/{parent_order_id}")

    def get_status(self, parent_order_id: int) -> OrderStatus:
        """Fetch parent-order status by server-assigned identifier."""
        response = self._request("GET", f"/orders/{parent_order_id}")
        return OrderStatus.from_json(response.json())

    def get_policy(self) -> PolicySnapshot:
        """Fetch the active policy identity."""
        response = self._request("GET", "/policy/current")
        return PolicySnapshot.from_json(response.json())

    def subscribe_events(self) -> Iterator[LifecycleEvent]:
        """Read the current lifecycle event stream as typed events."""
        response = self._request("GET", "/events/stream")
        yield from _parse_events(response.text)

    def _request(self, method: str, path: str, **kwargs: Any) -> requests.Response:
        attempts = self.max_retries + 1
        for attempt in range(1, attempts + 1):
            response = self.session.request(method, f"{self.base_url}{path}", timeout=self.timeout, **kwargs)
            if response.status_code not in _TRANSIENT_STATUS:
                if response.status_code >= 400:
                    raise SorClientHttpError(response.status_code, response.text)
                return response
            if attempt < attempts:
                self._sleep(self.backoff_seconds * (2 ** (attempt - 1)))
        raise SorClientRetryExhaustedError(attempts)


def _parse_events(text: str) -> Iterator[LifecycleEvent]:
    for block in text.strip().split("\n\n"):
        if not block:
            continue
        fields: dict[str, str] = {}
        for line in block.splitlines():
            if ": " in line:
                key, value = line.split(": ", 1)
                fields[key] = value
        yield LifecycleEvent(
            event_id=int(fields.get("id", 0)),
            timestamp_nanos=int(fields.get("timestampNanos", 0)),
            component_id=int(fields.get("componentId", 0)),
            event_type=int(fields.get("eventType", 0)),
            correlation_id=int(fields.get("correlationId", 0)),
            message=fields.get("data", ""),
        )
