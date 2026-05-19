"""Scenario catalog library for readable SOR scenario files."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Mapping, TextIO


REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SCENARIOS_DIR = REPO_ROOT / "scenarios"


@dataclass(frozen=True)
class Scenario:
    """User-readable scenario metadata loaded from a YAML scenario file."""

    path: Path
    scenario_id: str
    description: str
    category: str
    tags: tuple[str, ...]
    seed: str
    ticks: str
    parent_orders: tuple[dict[str, str], ...] = ()

    @property
    def relative_path(self) -> Path:
        """Return the path relative to the repository root when possible."""
        try:
            return self.path.relative_to(REPO_ROOT)
        except ValueError:
            return self.path


def load_scenarios(scenarios_dir: Path | str = DEFAULT_SCENARIOS_DIR) -> list[Scenario]:
    """Load all scenario YAML files from category subfolders."""
    root = Path(scenarios_dir)
    scenarios: list[Scenario] = []
    for path in sorted(root.glob("*/*.yaml")):
        values = parse_scenario_file(path)
        scenarios.append(
            Scenario(
                path=path,
                scenario_id=str(values.get("scenarioId", "")),
                description=str(values.get("description", "")),
                category=str(values.get("category", path.parent.name)),
                tags=tuple(str(values.get("tags", "")).split(",")) if values.get("tags") else (),
                seed=str(values.get("seed", "")),
                ticks=str(values.get("ticks", "")),
                parent_orders=tuple(values.get("parentOrders", [])),
            )
        )
    return scenarios


def parse_scenario_file(path: Path | str) -> dict[str, object]:
    """Parse the small YAML subset used by scenario metadata files."""
    scenario_path = Path(path)
    values: dict[str, object] = {}
    section = ""
    tags: list[str] = []
    parent_orders: list[dict[str, str]] = []
    current_parent_order: dict[str, str] | None = None
    for raw in scenario_path.read_text(encoding="utf-8").splitlines():
        line = raw.split("#", 1)[0].rstrip()
        stripped = line.strip()
        if not stripped:
            continue
        if not line.startswith(" ") and stripped.endswith(":"):
            section = stripped[:-1]
            current_parent_order = None
            continue
        if not line.startswith(" "):
            section = ""
            current_parent_order = None
        if section == "tags" and stripped.startswith("- "):
            tags.append(stripped[2:].strip())
            continue
        if section == "parentOrders" and stripped.startswith("- "):
            current_parent_order = {}
            parent_orders.append(current_parent_order)
            item = stripped[2:].strip()
            if ":" in item:
                key, value = item.split(":", 1)
                current_parent_order[key.strip()] = value.strip().strip('"')
            continue
        if section == "parentOrders" and current_parent_order is not None and ":" in stripped:
            key, value = stripped.split(":", 1)
            current_parent_order[key.strip()] = value.strip().strip('"')
            continue
        if section:
            continue
        if ":" in stripped:
            key, value = stripped.split(":", 1)
            values[key.strip()] = value.strip().strip('"')
    if tags:
        values["tags"] = ",".join(tags)
    if parent_orders:
        values["parentOrders"] = parent_orders
    return values


def search_scenarios(
    scenarios: Iterable[Scenario],
    *,
    tags: Iterable[str] = (),
    category: str | None = None,
    text: str | None = None,
) -> list[Scenario]:
    """Filter scenarios by tags, category, and free-text match."""
    rows = list(scenarios)
    if category:
        rows = [scenario for scenario in rows if scenario.category == category]
    for tag in tags:
        rows = [scenario for scenario in rows if tag in scenario.tags]
    if text:
        needle = text.lower()
        rows = [
            scenario
            for scenario in rows
            if needle in scenario.scenario_id.lower() or needle in scenario.description.lower()
        ]
    return rows


def find_scenario(scenarios: Iterable[Scenario], scenario_id: str) -> Scenario:
    """Find a scenario by scenario ID or file stem."""
    for scenario in scenarios:
        if scenario.scenario_id == scenario_id or scenario.path.stem == scenario_id:
            return scenario
    raise ValueError(f"scenario not found: {scenario_id}")


def parent_order_suggestions(scenario: Scenario) -> list[str]:
    """Return human-readable parent order suggestions for a scenario."""
    if scenario.parent_orders:
        return [format_parent_order(order) for order in scenario.parent_orders]

    tags = set(scenario.tags)
    category = scenario.category
    base: list[str] = []

    if category in {"liquidity", "risk-capacity"} or {"liquidity", "residual", "no-liquidity"} & tags:
        base.append("BUY  instrumentId=0 quantity=12000 urgency=HIGH   # stress residual/no-liquidity behavior")
        base.append("SELL instrumentId=1 quantity=6000  urgency=NORMAL # compare opposite-side depth")
    if category == "order-flow" or "urgency" in tags:
        base.append("BUY  instrumentId=0 quantity=15000 urgency=HIGH   # sweep-style urgent parent order")
        base.append("BUY  instrumentId=0 quantity=1500  urgency=LOW    # passive/low-urgency comparison")
    if category in {"venue-health", "feed", "venue-quality"} or {"outage", "stale-feed", "toxicity"} & tags:
        base.append("BUY  instrumentId=0 quantity=5000  urgency=NORMAL # observe venue health/feed guards")
        base.append("SELL instrumentId=0 quantity=5000  urgency=HIGH   # force faster venue outcome feedback")
    if category == "auction-session" or {"auction", "halt"} & tags:
        base.append("BUY  instrumentId=0 quantity=3000  urgency=HIGH   # submit during auction/volatile window")
        base.append("SELL instrumentId=1 quantity=3000  urgency=NORMAL # compare instrument/session availability")
    if category in {"optimizer-policy", "ml-dataset"} or {"optimizer", "ml", "dataset"} & tags:
        base.append("BUY  instrumentId=0 quantity=7000  urgency=NORMAL # generate optimizer/dataset lineage")
        base.append("SELL instrumentId=1 quantity=7000  urgency=HIGH   # diversify labels and route keys")
    if category == "live-reset" or "live-reset" in tags:
        base.append("BUY  instrumentId=0 quantity=2500  urgency=NORMAL # submit before/after reset mode comparison")
    if category in {"baseline", "regime", "multi-instrument"}:
        base.append("BUY  instrumentId=0 quantity=4000  urgency=NORMAL # baseline replay parent order")
        base.append("SELL instrumentId=1 quantity=4000  urgency=NORMAL # second instrument/regime comparison")
    if category == "failure-negative" or "negative" in tags:
        base.append("BUY  instrumentId=0 quantity=5000  urgency=HIGH   # observe fail-safe behavior")

    seen: set[str] = set()
    unique: list[str] = []
    for item in base:
        if item not in seen:
            unique.append(item)
            seen.add(item)
    return unique or ["BUY instrumentId=0 quantity=4000 urgency=NORMAL # generic deterministic smoke order"]


def format_parent_order(order: Mapping[str, str]) -> str:
    """Format one scenario-file parent order default for display."""
    side = order.get("side", "BUY")
    instrument = order.get("instrumentId", "0")
    quantity = order.get("quantity", "4000")
    urgency = order.get("urgency", order.get("urgencyId", "NORMAL"))
    at_tick = order.get("atTick", "0")
    submit_mode = order.get("submitMode", order.get("submitVia", "SIMULATED"))
    ref = order.get("clientOrderRef", "")
    suffix = f" ref={ref}" if ref else ""
    return f"{side} instrumentId={instrument} quantity={quantity} urgency={urgency} atTick={at_tick} submitMode={submit_mode}{suffix}"


def format_scenarios(scenarios: Iterable[Scenario]) -> str:
    """Format scenario rows for CLI and notebook display."""
    lines: list[str] = []
    for scenario in scenarios:
        tags = ",".join(scenario.tags)
        lines.append(f"{scenario.scenario_id:38} {scenario.category:18} {tags:42} {scenario.relative_path}")
        lines.append(f"  {scenario.description}")
    return "\n".join(lines)


def write_scenarios(scenarios: Iterable[Scenario], output: TextIO) -> None:
    """Write formatted scenario rows."""
    text = format_scenarios(scenarios)
    if text:
        output.write(text)
        output.write("\n")


def build_arg_parser() -> argparse.ArgumentParser:
    """Build the scenario catalog command parser."""
    parser = argparse.ArgumentParser(description="List/search SOR scenario catalog.")
    sub = parser.add_subparsers(dest="command", required=True)
    sub.add_parser("list", help="List every scenario with category, tags, and description.")
    search = sub.add_parser("search", help="Search scenarios by tag/category/text.")
    search.add_argument("--tag", action="append", default=[])
    search.add_argument("--category")
    search.add_argument("--text")
    suggest = sub.add_parser("suggest", help="Show scenario details and parent-order suggestions.")
    suggest.add_argument("scenario_id")
    return parser


def main(argv: list[str] | None = None, output: TextIO | None = None) -> int:
    """Run the scenario catalog command."""
    import sys

    out = output if output is not None else sys.stdout
    parser = build_arg_parser()
    args = parser.parse_args(argv)
    scenarios = load_scenarios()

    if args.command == "list":
        write_scenarios(scenarios, out)
        return 0
    if args.command == "search":
        write_scenarios(search_scenarios(scenarios, tags=args.tag, category=args.category, text=args.text), out)
        return 0
    if args.command == "suggest":
        try:
            scenario = find_scenario(scenarios, args.scenario_id)
        except ValueError as exc:
            parser.exit(2, f"{exc}\n")
        write_scenarios([scenario], out)
        out.write("\nSuggested parent order submissions:\n")
        for item in parent_order_suggestions(scenario):
            out.write(f"  - {item}\n")
        return 0
    parser.error(f"unsupported command: {args.command}")
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
