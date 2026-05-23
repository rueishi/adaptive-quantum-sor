"""Dataclasses returned by the Adaptive Quantum SOR Python client."""

from __future__ import annotations

from dataclasses import asdict, dataclass
from typing import Any


@dataclass(frozen=True, slots=True)
class OrderStatus:
    """Parent-order status response from `/orders` and `/orders/{id}`."""

    parent_order_id: int
    filled_qty: int
    remaining_qty: int
    status: int
    child_order_count: int = 0

    @classmethod
    def from_json(cls, payload: dict[str, Any]) -> "OrderStatus":
        """Create an order status from the API's camel-case JSON payload."""
        return cls(
            parent_order_id=int(payload.get("parentOrderId", 0)),
            filled_qty=int(payload.get("filledQty", 0)),
            remaining_qty=int(payload.get("remainingQty", 0)),
            status=int(payload.get("status", 0)),
            child_order_count=int(payload.get("childOrderCount", 0)),
        )

    def to_dict(self) -> dict[str, Any]:
        """Return a notebook-friendly dictionary with API-compatible keys."""
        return {
            "parentOrderId": self.parent_order_id,
            "filledQty": self.filled_qty,
            "remainingQty": self.remaining_qty,
            "status": self.status,
            "childOrderCount": self.child_order_count,
        }


@dataclass(frozen=True, slots=True)
class PolicySnapshot:
    """Active policy identity returned by `/policy/current`."""

    policy_version: int
    policy_hash64: int

    @classmethod
    def from_json(cls, payload: dict[str, Any]) -> "PolicySnapshot":
        """Create a policy snapshot from the API response payload."""
        return cls(
            policy_version=int(payload.get("policyVersion", 0)),
            policy_hash64=int(payload.get("policyHash64", 0)),
        )

    def to_dict(self) -> dict[str, Any]:
        """Return a notebook-friendly dictionary."""
        return asdict(self)


@dataclass(frozen=True, slots=True)
class LifecycleEvent:
    """Lifecycle event parsed from the API's server-sent event stream."""

    event_id: int
    timestamp_nanos: int
    component_id: int
    event_type: int
    correlation_id: int
    message: str
