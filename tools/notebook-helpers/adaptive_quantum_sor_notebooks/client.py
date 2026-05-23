"""Notebook control-plane client for Adaptive Quantum SOR."""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Mapping
from urllib import error, request

from adaptive_quantum_sor_research.dataframe import _require_pandas
from adaptive_quantum_sor_research.schema import ORDER_COLUMNS


@dataclass(frozen=True)
class SorNotebookClient:
    """Small HTTP client designed for use from Jupyter notebooks."""

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

    def stats_dataframe(self):
        """Fetch `/stats/current` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json(request.Request(self._url("/stats/current"), method="GET"))])

    def policy_dataframe(self):
        """Fetch `/policy/current` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json(request.Request(self._url("/policy/current"), method="GET"))])

    def order_summary(self) -> dict:
        """Fetch cumulative order routing totals from `/orders/summary`."""

        return self._json(request.Request(self._url("/orders/summary"), method="GET"))

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
        """Reset live demo scenario state through `/scenario/reset`."""

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
        return self._json(req)

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
        """Run a live scenario through `/scenario/run`, optionally with parent orders."""

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
        return self._json(req)

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
        """Fetch `/scenario/summary` and return a one-row pandas DataFrame."""

        pd = _require_pandas()
        return pd.DataFrame([self._json(request.Request(self._url("/scenario/summary"), method="GET"))])

    def scenario_events_dataframe(self):
        """Fetch `/scenario/events` and return one parsed event per DataFrame row."""

        pd = _require_pandas()
        text = request.urlopen(
            request.Request(self._url("/scenario/events"), method="GET"),
            timeout=self.timeout_seconds,
        ).read().decode("utf-8")
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
        try:
            with request.urlopen(req, timeout=self.timeout_seconds) as response:
                body = response.read().decode("utf-8")
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise RuntimeError(f"SOR API request failed with HTTP {exc.code}: {detail}") from exc
        return json.loads(body or "{}")
