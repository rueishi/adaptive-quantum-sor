"""
Purpose: Provides notebook-friendly live monitoring helpers for SOR HTTP stats and events.
Usage: Use it in Jupyter workflows to poll current stats and build live views.
"""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timezone
from html import escape
from threading import Event, Thread
from typing import Mapping

import pandas as pd
from IPython.display import HTML, clear_output, display

from .notebook_client import SorNotebookClient


DEFAULT_BASE_URL = "http://127.0.0.1:8080"

DASHBOARD_CSS = """
<style>
:root {
  --sor-ink: #172033;
  --sor-muted: #5d6678;
  --sor-line: #d8dee8;
  --sor-surface: #ffffff;
  --sor-soft: #f6f8fb;
  --sor-blue: #2563eb;
  --sor-teal: #0f766e;
  --sor-green: #047857;
  --sor-amber: #b45309;
  --sor-red: #b91c1c;
}
.sor-dashboard {font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; color: var(--sor-ink);}
.sor-hero {border: 1px solid var(--sor-line); border-radius: 8px; padding: 18px 20px; margin: 4px 0 16px 0; background: linear-gradient(180deg, #ffffff 0%, #f7fafc 100%);}
.sor-hero h1 {font-size: 24px; margin: 0 0 6px 0; letter-spacing: 0;}
.sor-hero p {margin: 0; color: var(--sor-muted); font-size: 14px;}
.sor-section {margin: 20px 0 8px 0;}
.sor-section h2 {font-size: 18px; margin: 0 0 4px 0; letter-spacing: 0;}
.sor-section p {margin: 0 0 8px 0; color: var(--sor-muted); font-size: 13px;}
.sor-kpi-grid {display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin: 10px 0 16px 0;}
.sor-kpi {border: 1px solid var(--sor-line); border-radius: 8px; padding: 12px 14px; background: var(--sor-surface);}
.sor-kpi-label {font-size: 12px; color: var(--sor-muted); margin-bottom: 5px;}
.sor-kpi-value {font-size: 22px; font-weight: 750; line-height: 1.15;}
.sor-badge {display: inline-block; border-radius: 999px; padding: 3px 9px; font-size: 12px; font-weight: 700;}
.sor-pass {background: #ecfdf5; color: var(--sor-green); border: 1px solid #a7f3d0;}
.sor-warn {background: #fffbeb; color: var(--sor-amber); border: 1px solid #fde68a;}
.sor-fail {background: #fef2f2; color: var(--sor-red); border: 1px solid #fecaca;}
.sor-note {border-left: 4px solid var(--sor-blue); background: #eff6ff; padding: 10px 12px; margin: 10px 0 16px 0; color: #1e3a8a;}
.sor-error {border-left-color: var(--sor-red); background: #fef2f2; color: #7f1d1d;}
.sor-grid-2 {display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 14px; margin: 8px 0 18px 0;}
.sor-chart {border: 1px solid var(--sor-line); border-radius: 8px; padding: 12px; background: var(--sor-surface);}
.sor-chart h3 {font-size: 14px; margin: 0 0 10px 0;}
.sor-bar-row {display: grid; grid-template-columns: 160px 1fr 90px; gap: 10px; align-items: center; margin: 8px 0;}
.sor-bar-label {font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; color: #374151; overflow-wrap: anywhere;}
.sor-bar-track {height: 12px; background: #eef2f7; border-radius: 3px; overflow: hidden;}
.sor-bar {height: 12px; background: var(--sor-blue);}
.sor-bar.alt {background: var(--sor-teal);}
.sor-bar.warn {background: var(--sor-amber);}
.sor-bar-value {text-align: right; font-variant-numeric: tabular-nums; color: var(--sor-ink); font-size: 12px;}
table.sor-dashboard-table {border-collapse: collapse; width: 100%; table-layout: auto; margin: 8px 0 18px 0; font-size: 13px;}
table.sor-dashboard-table th, table.sor-dashboard-table td {border: 1px solid var(--sor-line); padding: 7px 9px; vertical-align: top; white-space: normal; overflow-wrap: anywhere; word-break: break-word; max-width: 560px; text-align: left;}
table.sor-dashboard-table th {background: var(--sor-soft); color: var(--sor-ink); font-weight: 700;}
table.sor-dashboard-table tbody tr:nth-child(even) {background: #fcfdff;}
</style>
"""


@dataclass(frozen=True)
class LiveMonitorInputs:
    """Controls for the live dashboard."""

    base_url: str
    timeout_seconds: float
    refresh_interval_seconds: float
    top_rows: int


@dataclass(frozen=True)
class LiveSnapshot:
    """One fetched dashboard snapshot."""

    inputs: LiveMonitorInputs
    refreshed_at: datetime
    stats: pd.DataFrame
    policy: pd.DataFrame
    order_summary_frames: dict[str, pd.DataFrame]
    snapshot_error: str = ""
    order_summary_error: str = ""


class LiveStatsDashboard:
    """Widget-backed live SOR dashboard for Jupyter."""

    def __init__(self) -> None:
        pd.set_option("display.max_colwidth", None)
        pd.set_option("display.max_columns", None)
        pd.set_option("display.width", 0)
        self.widgets = _load_widgets()
        self.controls: dict[str, object] = {}
        self.output = None
        self.status_output = None
        self.stop_event = Event()
        self.worker: Thread | None = None

    def display(self) -> None:
        """Display controls and an initially empty dashboard surface."""

        display_styles()
        if self.widgets is None:
            display_note("ipywidgets is not installed. Install tools/python-research/requirements.txt to enable the live dashboard controls.")
            snapshot = fetch_snapshot(self.default_inputs())
            render_snapshot(snapshot)
            return

        widgets = self.widgets
        self.controls = {
            "base_url": widgets.Text(value=DEFAULT_BASE_URL, description="API URL", layout=widgets.Layout(width="420px")),
            "timeout_seconds": widgets.FloatSlider(value=5.0, min=1.0, max=30.0, step=0.5, description="Timeout", continuous_update=False),
            "refresh_interval_seconds": widgets.FloatSlider(value=2.0, min=0.5, max=30.0, step=0.5, description="Interval", continuous_update=False),
            "top_rows": widgets.IntSlider(value=8, min=3, max=20, step=1, description="Top rows", continuous_update=False),
        }
        start_button = widgets.Button(description="Start dashboard", button_style="success", tooltip="Start refreshing the dashboard.")
        reset_button = widgets.Button(description="Reset", button_style="warning", tooltip="Stop refreshing and clear the dashboard output.")
        stop_button = widgets.Button(description="Stop", button_style="danger", tooltip="Stop refreshing the dashboard.")
        self.output = widgets.Output()
        self.status_output = widgets.Output()

        start_button.on_click(lambda _button: self.start())
        reset_button.on_click(lambda _button: self.reset())
        stop_button.on_click(lambda _button: self.stop())

        display(HTML("<div class='sor-dashboard sor-section'><h2>Dashboard Controls</h2><p>Start the live refresh loop, change the interval as needed, and stop or reset when finished.</p></div>"))
        display(widgets.VBox([
            widgets.HBox([self.controls["base_url"], self.controls["timeout_seconds"], self.controls["refresh_interval_seconds"], self.controls["top_rows"]]),
            widgets.HBox([start_button, reset_button, stop_button]),
            self.status_output,
            self.output,
        ]))

    def start(self) -> None:
        """Start periodic dashboard refresh."""

        self.stop()
        self.stop_event = Event()
        self._set_status("Dashboard running. Loading latest snapshot...")
        self._refresh_once()
        self._set_status("Dashboard running.")
        self.worker = Thread(target=self._refresh_loop, name="live-stats-dashboard", daemon=True)
        self.worker.start()

    def stop(self) -> None:
        """Stop periodic dashboard refresh."""

        self.stop_event.set()
        self._set_status("Dashboard stopped.")

    def reset(self) -> None:
        """Stop refreshing and clear the dashboard."""

        self.stop_event.set()
        if self.output is not None:
            with self.output:
                clear_output(wait=True)
        self._set_status("Dashboard reset.")

    def default_inputs(self) -> LiveMonitorInputs:
        """Return deterministic defaults for non-widget use."""

        return LiveMonitorInputs(DEFAULT_BASE_URL, 5.0, 2.0, 8)

    def read_inputs(self) -> LiveMonitorInputs:
        """Read current widget values."""

        if not self.controls:
            return self.default_inputs()
        return LiveMonitorInputs(
            base_url=str(self.controls["base_url"].value),
            timeout_seconds=float(self.controls["timeout_seconds"].value),
            refresh_interval_seconds=float(self.controls["refresh_interval_seconds"].value),
            top_rows=int(self.controls["top_rows"].value),
        )

    def _refresh_loop(self) -> None:
        while not self.stop_event.is_set():
            inputs = self.read_inputs()
            self._refresh_once(inputs)
            if self.stop_event.wait(max(0.1, inputs.refresh_interval_seconds)):
                break

    def _refresh_once(self, inputs: LiveMonitorInputs | None = None) -> None:
        inputs = inputs or self.read_inputs()
        try:
            snapshot = fetch_snapshot(inputs)
            self._render_to_output(lambda: render_snapshot(snapshot))
        except Exception as exc:  # Keep notebook background refresh failures visible.
            self._render_to_output(lambda: display_note("Dashboard refresh failed: " + str(exc), tone="error"))

    def _render_to_output(self, render) -> None:
        if self.output is None:
            return
        with self.output:
            clear_output(wait=True)
            render()

    def _set_status(self, message: str) -> None:
        if self.status_output is None:
            return
        with self.status_output:
            clear_output(wait=True)
            display(HTML(f"<div class='sor-dashboard sor-note'>{escape(message)}</div>"))


def fetch_snapshot(inputs: LiveMonitorInputs) -> LiveSnapshot:
    """Fetch one live dashboard snapshot."""

    client = SorNotebookClient(base_url=inputs.base_url, timeout_seconds=inputs.timeout_seconds)
    stats = pd.DataFrame()
    policy = pd.DataFrame()
    order_summary_frames: dict[str, pd.DataFrame] = {}
    snapshot_errors: list[str] = []
    order_summary_error = ""
    try:
        stats = client.stats_dataframe()
    except Exception as exc:  # Notebook dashboard should report API issues inline.
        snapshot_errors.append("/stats/current: " + str(exc))
    try:
        policy = client.policy_dataframe()
    except Exception as exc:  # Notebook dashboard should report API issues inline.
        snapshot_errors.append("/policy/current: " + str(exc))
    try:
        order_summary_frames = client.order_summary_frames()
    except Exception as exc:  # Notebook dashboard should degrade gracefully.
        order_summary_error = str(exc)
    return LiveSnapshot(
        inputs=inputs,
        refreshed_at=datetime.now(timezone.utc),
        stats=stats,
        policy=policy,
        order_summary_frames=order_summary_frames,
        snapshot_error="; ".join(snapshot_errors),
        order_summary_error=order_summary_error,
    )


def render_snapshot(snapshot: LiveSnapshot) -> None:
    """Render a professional live dashboard snapshot."""

    display_styles()
    stats = snapshot.stats
    policy = snapshot.policy
    summary = snapshot.order_summary_frames
    totals = first_frame(summary, "totals")
    by_instrument = first_frame(summary, "byInstrument")
    by_venue = first_frame(summary, "byVenue")
    by_instrument_venue = first_frame(summary, "byInstrumentVenue")

    venue_count = first_value(stats, "venueCount")
    policy_version = first_value(policy, "policyVersion")
    parent_orders = first_value(totals, "parentOrderCount", default=first_value(stats, "parentOrderCount", "ordersAccepted"))
    child_orders = first_value(totals, "childOrderCount", default=first_value(stats, "childOrderCount", "ordersRouted"))
    filled_qty = first_value(totals, "filledQty")
    remaining_qty = first_value(totals, "remainingQty")
    notional_ticks = first_value(totals, "notionalTicks")
    run_tone = "pass" if int(venue_count or 0) > 0 else "warn"

    display_header(
        "Live SOR Dashboard",
        f"Streaming snapshot from {snapshot.inputs.base_url}",
        snapshot.refreshed_at.strftime("%Y-%m-%d %H:%M:%S"),
    )
    display(HTML(f"<div class='sor-dashboard'>{badge('Live refresh', run_tone)}</div>"))
    if snapshot.snapshot_error:
        display_note("Dashboard data is incomplete: " + snapshot.snapshot_error, tone="error")
    display_kpis([
        ("Policy version", policy_version),
        ("Venue count", venue_count),
        ("Parent orders", parent_orders),
        ("Child orders", child_orders),
        ("Filled qty", filled_qty),
        ("Remaining qty", remaining_qty),
        ("Notional ticks", notional_ticks),
    ])

    if snapshot.order_summary_error:
        display_note("Order-summary charts are unavailable: " + snapshot.order_summary_error, tone="error")

    display(HTML("<div class='sor-dashboard sor-grid-2'>"
                 + chart_html("Order Flow", [
                     ("Parent orders", parent_orders, "bar"),
                     ("Child orders", child_orders, "alt"),
                 ])
                 + chart_html("Fill Progress", [
                     ("Filled qty", filled_qty, "bar"),
                     ("Remaining qty", remaining_qty, "warn"),
                 ])
                 + "</div>"))

    if not by_venue.empty:
        display(HTML("<div class='sor-dashboard sor-grid-2'>"
                     + dataframe_bar_chart("Filled Quantity By Venue", by_venue, "venueName", "filledQty", snapshot.inputs.top_rows, "bar")
                     + dataframe_bar_chart("Notional By Venue", by_venue, "venueName", "notionalTicks", snapshot.inputs.top_rows, "alt")
                     + "</div>"))
    if not by_instrument.empty:
        display(HTML("<div class='sor-dashboard sor-grid-2'>"
                     + dataframe_bar_chart("Filled Quantity By Instrument", by_instrument, "instrumentSymbol", "filledQty", snapshot.inputs.top_rows, "bar")
                     + dataframe_bar_chart("Remaining Quantity By Instrument", by_instrument, "instrumentSymbol", "remainingQty", snapshot.inputs.top_rows, "warn")
                     + "</div>"))

    manifest = pd.DataFrame([{
        "baseUrl": snapshot.inputs.base_url,
        "timeoutSeconds": snapshot.inputs.timeout_seconds,
        "refreshIntervalSeconds": snapshot.inputs.refresh_interval_seconds,
        "topRows": snapshot.inputs.top_rows,
        "refreshedAtUtc": snapshot.refreshed_at.strftime("%Y-%m-%d %H:%M:%S"),
    }])
    display_table("Snapshot Manifest", manifest, "Controls and timestamp for this dashboard refresh.")
    display_table("Stats And Policy Snapshot", pd.concat([stats.add_prefix("stats."), policy.add_prefix("policy.")], axis=1), "Compact current engine and policy state.")
    if not totals.empty:
        display_table("Order Totals", totals, "Cumulative parent, child, fill, residual, and notional totals.")
    if not by_venue.empty:
        display_table("Venue Breakdown", top_frame(by_venue, "filledQty", snapshot.inputs.top_rows), "Top venues by filled quantity.")
    if not by_instrument.empty:
        display_table("Instrument Breakdown", top_frame(by_instrument, "filledQty", snapshot.inputs.top_rows), "Top instruments by filled quantity.")
    if not by_instrument_venue.empty:
        display_table("Instrument-Venue Routes", top_frame(by_instrument_venue, "filledQty", snapshot.inputs.top_rows), "Top instrument/venue combinations by filled quantity.")


def chart_html(title: str, rows: list[tuple[str, object, str]]) -> str:
    values = [abs(float_or_zero(value)) for _, value, _ in rows]
    scale = max(values + [1.0])
    bars = []
    for label, value, tone in rows:
        numeric = float_or_zero(value)
        width = round(abs(numeric) / scale * 100, 1)
        bars.append(
            f"<div class='sor-bar-row'><span class='sor-bar-label'>{escape(str(label))}</span>"
            f"<div class='sor-bar-track'><div class='sor-bar {escape(tone)}' style='width:{width}%'></div></div>"
            f"<span class='sor-bar-value'>{escape(format_number(numeric))}</span></div>"
        )
    return f"<div class='sor-chart'><h3>{escape(title)}</h3>{''.join(bars)}</div>"


def dataframe_bar_chart(
    title: str,
    frame: pd.DataFrame,
    label_column: str,
    value_column: str,
    limit: int,
    tone: str,
) -> str:
    if frame.empty or value_column not in frame.columns:
        return f"<div class='sor-chart'><h3>{escape(title)}</h3><div class='sor-note'>No data available.</div></div>"
    label = label_column if label_column in frame.columns else frame.columns[0]
    rows = top_frame(frame, value_column, limit)
    return chart_html(title, [(row[label], row[value_column], tone) for _, row in rows.iterrows()])


def top_frame(frame: pd.DataFrame, value_column: str, limit: int) -> pd.DataFrame:
    if frame.empty or value_column not in frame.columns:
        return frame
    copy = frame.copy()
    copy[value_column] = pd.to_numeric(copy[value_column], errors="coerce").fillna(0)
    return copy.sort_values(value_column, ascending=False).head(limit).reset_index(drop=True)


def first_frame(frames: Mapping[str, pd.DataFrame], name: str) -> pd.DataFrame:
    frame = frames.get(name)
    return frame if frame is not None else pd.DataFrame()


def first_value(frame: pd.DataFrame, *names: str, default: object = 0) -> object:
    for name in names:
        if frame is not None and name in frame.columns and len(frame) > 0:
            return frame.iloc[0][name]
    return default


def float_or_zero(value: object) -> float:
    try:
        if pd.isna(value):
            return 0.0
        return float(value)
    except (TypeError, ValueError):
        return 0.0


def format_number(value: object) -> str:
    numeric = float_or_zero(value)
    if numeric.is_integer():
        return str(int(numeric))
    return f"{numeric:.2f}"


def display_styles() -> None:
    display(HTML(DASHBOARD_CSS))


def display_header(title: str, subtitle: str, generated_at: str) -> None:
    display(HTML(f"""
    <div class="sor-dashboard sor-hero">
      <h1>{escape(title)}</h1>
      <p>{escape(subtitle)}</p>
      <p>Refreshed at {escape(generated_at)} UTC</p>
    </div>
    """))


def display_section(title: str, subtitle: str = "") -> None:
    subtitle_html = f"<p>{escape(subtitle)}</p>" if subtitle else ""
    display(HTML(f"<div class='sor-dashboard sor-section'><h2>{escape(title)}</h2>{subtitle_html}</div>"))


def display_note(text: str, *, tone: str = "note") -> None:
    css = "sor-error" if tone == "error" else ""
    display(HTML(f"<div class='sor-dashboard sor-note {css}'>{escape(str(text))}</div>"))


def display_kpis(cards: list[tuple[str, object]]) -> None:
    html = []
    for label, value in cards:
        html.append(
            "<div class='sor-kpi'>"
            f"<div class='sor-kpi-label'>{escape(str(label))}</div>"
            f"<div class='sor-kpi-value'>{escape(str(value))}</div>"
            "</div>"
        )
    display(HTML("<div class='sor-dashboard sor-kpi-grid'>" + "".join(html) + "</div>"))


def badge(label: str, tone: str) -> str:
    css = {"pass": "sor-pass", "warn": "sor-warn", "fail": "sor-fail"}.get(tone, "sor-warn")
    return f"<span class='sor-badge {css}'>{escape(str(label))}</span>"


def display_table(title: str, frame: pd.DataFrame | None, subtitle: str = "") -> None:
    display_section(title, subtitle)
    if frame is None or frame.empty:
        display_note("No rows were returned for this section.")
        return
    html = frame.to_html(index=False, escape=True)
    html = html.replace('class="dataframe"', 'class="sor-dashboard-table"')
    display(HTML("<div class='sor-dashboard'>" + html + "</div>"))


def _load_widgets():
    try:
        import ipywidgets as widgets
    except ModuleNotFoundError:
        return None
    return widgets
