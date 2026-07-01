"""
Purpose: Provides the SorNotebookClient used by Jupyter notebooks to call order, control, and scenario endpoints.
Usage: Use it from notebooks to submit orders, reset scenarios, run scenarios, and fetch summaries/events.
"""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Mapping
from urllib import error, request

from adaptive_quantum_sor_research.dataframe import _require_pandas
from adaptive_quantum_sor_research.schema import ORDER_COLUMNS


@dataclass(frozen=True)
class SorNotebookClient:
    """Small HTTP client designed for use from Jupyter notebooks.

    Order/status/control methods target the built-in user HTTP control module.
    Scenario methods target the test-server/demo scenario API and fail clearly
    when pointed at a server that does not expose `/scenario/*`.
    """

    base_url: str = "http://127.0.0.1:8080"
    timeout_seconds: float = 5.0

    def submit_order(self, order: Mapping[str, object]) -> dict:
        """Submit one parent order to the SOR control-plane API."""

        missing = [column for column in ORDER_COLUMNS if column not in order]
        if missing:
            raise ValueError("order missing fields: " + ",".join(missing))
        payload = json.dumps({column: self._order_value(column, order[column]) for column in ORDER_COLUMNS}).encode("utf-8")
        req = request.Request(
            self._url("/orders"),
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._json(req)

    def submit_orders_dataframe(self, frame):
        """Submit a pandas DataFrame of parent orders and return response rows."""

        pd = _require_pandas()
        missing = [column for column in ORDER_COLUMNS if column not in frame.columns]
        if missing:
            raise ValueError("orders DataFrame missing columns: " + ",".join(missing))
        responses = [self.submit_order(row) for row in frame[ORDER_COLUMNS].to_dict("records")]
        return pd.DataFrame(responses)

    def order_status(self, parent_order_id: int) -> dict:
        """Fetch one parent order status from `/orders/{parentOrderId}`."""

        return self._json(request.Request(self._url(f"/orders/{int(parent_order_id)}"), method="GET"))

    def health(self) -> dict:
        """Fetch built-in user HTTP liveness from `/healthz`."""

        return self._json(request.Request(self._url("/healthz"), method="GET"))

    def ready(self) -> dict:
        """Fetch built-in user HTTP readiness from `/ready`."""

        return self._json(request.Request(self._url("/ready"), method="GET"))

    def metrics_text(self) -> str:
        """Fetch Prometheus text from built-in user HTTP `/metrics`."""

        return self._text(request.Request(self._url("/metrics"), method="GET"))

    def control_state(self) -> dict:
        """Fetch built-in user HTTP engine state from `/control/state`."""

        return self._json(request.Request(self._url("/control/state"), method="GET"))

    def market_data_snapshot(self) -> dict:
        """Fetch built-in user HTTP market-data diagnostics from `/control/market-data`."""

        return self._json(request.Request(self._url("/control/market-data"), method="GET"))

    def reset_engine(
        self,
        mode: str = "APPEND",
        require_no_live_orders: bool = True,
        reason: str = "notebook control reset",
    ) -> dict:
        """Request a built-in user HTTP control-plane reset through `/control/reset`."""

        payload = json.dumps({
            "mode": mode,
            "requireNoLiveOrders": bool(require_no_live_orders),
            "reason": reason,
        }).encode("utf-8")
        req = request.Request(
            self._url("/control/reset"),
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._json(req)

    def stats_dataframe(self):
        """Fetch test-server/demo `/stats/current` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json_demo(request.Request(self._url("/stats/current"), method="GET"))])

    def policy_dataframe(self):
        """Fetch `/policy/current` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json(request.Request(self._url("/policy/current"), method="GET"))])

    def order_summary(self) -> dict:
        """Fetch test-server/demo cumulative order routing totals from `/orders/summary`."""

        return self._json_demo(request.Request(self._url("/orders/summary"), method="GET"))

    def order_summary_frames(self):
        """Fetch cumulative order routing totals as notebook-friendly DataFrames."""

        pd = _require_pandas()
        summary = self.order_summary()
        return {
            "totals": pd.DataFrame([summary.get("totals", {})]),
            "byInstrument": pd.DataFrame(summary.get("byInstrument", [])),
            "byVenue": pd.DataFrame(summary.get("byVenue", [])),
            "byInstrumentVenue": pd.DataFrame(summary.get("byInstrumentVenue", [])),
        }

    def reset_scenario(self, scenario_id: str, seed: int, ticks: int, reset_mode: str = "PURGE_AND_REPOPULATE") -> dict:
        """Reset test-server/demo scenario state through `/scenario/reset`."""

        payload = json.dumps({
            "scenarioId": scenario_id,
            "seed": int(seed),
            "ticks": int(ticks),
            "resetMode": reset_mode,
        }).encode("utf-8")
        req = request.Request(
            self._url("/scenario/reset"),
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._json_demo(req)

    def run_scenario(
        self,
        scenario_id: str,
        seed: int,
        ticks: int,
        reset_mode: str = "PURGE_AND_REPOPULATE",
        parent_orders: list[Mapping[str, object]] | None = None,
        max_route_attempts: int = 16,
        route_timeout_millis: int = 1000,
        simulator_generated_orders: bool | None = None,
    ) -> dict:
        """Run a test-server/demo scenario through `/scenario/run`, optionally with parent orders."""

        orders = parent_orders or []
        simulator_generated = len(orders) == 0 if simulator_generated_orders is None else bool(simulator_generated_orders)
        payload = json.dumps({
            "scenarioId": scenario_id,
            "seed": int(seed),
            "ticks": int(ticks),
            "resetMode": reset_mode,
            "maxRouteAttempts": int(max_route_attempts),
            "routeTimeoutMillis": int(route_timeout_millis),
            "simulatorGeneratedOrders": simulator_generated,
            "parentOrders": [self._scenario_parent_order_payload(order) for order in orders],
        }).encode("utf-8")
        req = request.Request(
            self._url("/scenario/run"),
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )
        return self._json_demo(req)

    @staticmethod
    def _scenario_parent_order_payload(order: Mapping[str, object]) -> dict:
        missing = [column for column in ORDER_COLUMNS if column not in order]
        if missing:
            raise ValueError("parent order missing fields: " + ",".join(missing))
        payload = {column: SorNotebookClient._order_value(column, order[column]) for column in ORDER_COLUMNS}
        payload["atTick"] = int(order.get("atTick", 0))
        payload["submitMode"] = str(order.get("submitMode", "SIMULATED"))
        payload["clientOrderRef"] = str(order.get("clientOrderRef", ""))
        return payload

    @staticmethod
    def _order_value(column: str, value: object) -> object:
        if column == "side" and isinstance(value, str):
            side = value.upper()
            if side not in {"BUY", "SELL"}:
                raise ValueError("side must be BUY, SELL, 1, or 2")
            return side
        return int(value)

    def scenario_summary_dataframe(self):
        """Fetch test-server/demo `/scenario/summary` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json_demo(request.Request(self._url("/scenario/summary"), method="GET"))])

    def scenario_events_dataframe(self):
        """Fetch test-server/demo `/scenario/events` and return one parsed event per DataFrame row."""

        pd = _require_pandas()
        text = self._text_demo(
            request.Request(self._url("/scenario/events"), method="GET"),
        )
        rows = []
        for sequence, raw_event in enumerate(text.split("\n\n"), start=1):
            if not raw_event.strip():
                continue
            row = {
                "sequence": sequence,
                "eventId": "",
                "timestampNanos": "",
                "componentId": "",
                "eventType": "",
                "correlationId": "",
                "event": "",
                "message": "",
            }
            for line in raw_event.splitlines():
                if line.startswith("id: "):
                    row["eventId"] = line[4:]
                elif line.startswith("event: "):
                    row["event"] = line[7:]
                elif line.startswith("timestampNanos: "):
                    row["timestampNanos"] = line[16:]
                elif line.startswith("componentId: "):
                    row["componentId"] = line[13:]
                elif line.startswith("eventType: "):
                    row["eventType"] = line[11:]
                elif line.startswith("correlationId: "):
                    row["correlationId"] = line[15:]
                elif line.startswith("data: "):
                    row["message"] = line[6:]
            if not row["eventId"]:
                row["eventId"] = sequence
            if not row["event"]:
                row["event"] = "lifecycle"
            rows.append(row)
        return pd.DataFrame(rows)

    def _url(self, path: str) -> str:
        return self.base_url.rstrip("/") + path

    def _json(self, req: request.Request) -> dict:
        return json.loads(self._text(req) or "{}")

    def _json_demo(self, req: request.Request) -> dict:
        try:
            return self._json(req)
        except RuntimeError as exc:
            raise RuntimeError(
                f"{exc}. This endpoint is part of the test-server/demo API, not the built-in user HTTP control module."
            ) from exc

    def _text(self, req: request.Request) -> str:
        try:
            with request.urlopen(req, timeout=self.timeout_seconds) as response:
                return response.read().decode("utf-8")
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"SOR API request failed with HTTP {exc.code}: {detail}") from exc

    def _text_demo(self, req: request.Request) -> str:
        try:
            return self._text(req)
        except RuntimeError as exc:
            raise RuntimeError(
                f"{exc}. This endpoint is part of the test-server/demo API, not the built-in user HTTP control module."
            ) from exc
