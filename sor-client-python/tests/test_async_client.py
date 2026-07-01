"""
Purpose: Verifies test async client behavior for the Python SDK or notebook compatibility layer.
Usage: Run with pytest to protect packaging, retries, typing, and HTTP client contracts.
"""

from __future__ import annotations

import httpx
import asyncio

from adaptive_quantum_sor_client import AsyncSorClient


def test_async_submit() -> None:
    asyncio.run(_run_async_submit())


async def _run_async_submit() -> None:
    seen: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["method"] = request.method
        seen["url"] = str(request.url)
        seen["json"] = request.read().decode()
        return httpx.Response(200, json={"parentOrderId": 12, "filledQty": 0, "remainingQty": 50, "status": 1})

    async_client = httpx.AsyncClient(transport=httpx.MockTransport(handler))
    client = AsyncSorClient("http://sor", client=async_client)

    status = await client.submit_parent_order(instrument_id=3, side=2, quantity=50, urgency_id=1)

    assert status.parent_order_id == 12
    assert seen["method"] == "POST"
    assert seen["url"] == "http://sor/orders"
    await async_client.aclose()
