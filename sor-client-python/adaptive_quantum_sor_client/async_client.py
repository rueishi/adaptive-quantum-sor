"""
Purpose: Provides the asynchronous aiohttp-based Python SDK client for SOR HTTP endpoints.
Usage: Use AsyncSorClient from async applications, notebooks, or tests that need non-blocking HTTP calls.
"""

from __future__ import annotations

import asyncio
from collections.abc import AsyncIterator, Callable
from typing import Any

import httpx

from .client import _TRANSIENT_STATUS, _parse_events
from .exceptions import SorClientHttpError, SorClientRetryExhaustedError
from .models import LifecycleEvent, OrderStatus


class AsyncSorClient:
    """Typed asynchronous client for notebooks and concurrent workflows."""

    def __init__(
        self,
        base_url: str,
        *,
        client: httpx.AsyncClient | None = None,
        timeout: float = 5.0,
        max_retries: int = 3,
        backoff_seconds: float = 0.05,
        sleep: Callable[[float], Any] = asyncio.sleep,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.client = client or httpx.AsyncClient()
        self.timeout = timeout
        self.max_retries = max_retries
        self.backoff_seconds = backoff_seconds
        self._sleep = sleep
        self._owns_client = client is None

    async def submit_parent_order(
        self,
        *,
        instrument_id: int,
        side: int,
        quantity: int,
        urgency_id: int,
        client_order_id: int | None = None,
    ) -> OrderStatus:
        """Submit a parent order asynchronously."""
        payload: dict[str, int] = {
            "instrumentId": instrument_id,
            "side": side,
            "quantity": quantity,
            "urgencyId": urgency_id,
        }
        if client_order_id is not None:
            payload["clientOrderId"] = client_order_id
        response = await self._request("POST", "/orders", json=payload)
        return OrderStatus.from_json(response.json())

    async def get_status(self, parent_order_id: int) -> OrderStatus:
        """Fetch parent-order status asynchronously."""
        response = await self._request("GET", f"/orders/{parent_order_id}")
        return OrderStatus.from_json(response.json())

    async def subscribe_events(self) -> AsyncIterator[LifecycleEvent]:
        """Read lifecycle events asynchronously."""
        response = await self._request("GET", "/events/stream")
        for event in _parse_events(response.text):
            yield event

    async def aclose(self) -> None:
        """Close the underlying httpx client if this SDK created it."""
        if self._owns_client:
            await self.client.aclose()

    async def _request(self, method: str, path: str, **kwargs: Any) -> httpx.Response:
        attempts = self.max_retries + 1
        for attempt in range(1, attempts + 1):
            response = await self.client.request(
                method, f"{self.base_url}{path}", timeout=self.timeout, **kwargs
            )
            if response.status_code not in _TRANSIENT_STATUS:
                if response.status_code >= 400:
                    raise SorClientHttpError(response.status_code, response.text)
                return response
            if attempt < attempts:
                slept = self._sleep(self.backoff_seconds * (2 ** (attempt - 1)))
                if hasattr(slept, "__await__"):
                    await slept
        raise SorClientRetryExhaustedError(attempts)
