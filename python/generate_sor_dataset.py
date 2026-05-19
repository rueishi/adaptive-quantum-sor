#!/usr/bin/env python3
"""Generate deterministic scenario-rich SOR feature datasets."""

import argparse
import csv
from pathlib import Path

from adaptive_quantum_sor.schema import FEATURE_COLUMNS, FEATURE_SCHEMA_VERSION


REGIME_PROFILES = {
    0: {"name": "calm", "spread": 1, "qty": 180_000, "lat": 75_000, "fill": 8500, "tox": 160, "reject": 60, "slip": 2},
    1: {"name": "volatile", "spread": 5, "qty": 95_000, "lat": 105_000, "fill": 7300, "tox": 520, "reject": 220, "slip": 11},
    2: {"name": "toxic", "spread": 14, "qty": 55_000, "lat": 130_000, "fill": 6400, "tox": 1180, "reject": 380, "slip": 27},
    3: {"name": "stress", "spread": 26, "qty": 28_000, "lat": 155_000, "fill": 5200, "tox": 820, "reject": 850, "slip": 44},
}

VENUE_PROFILES = {
    0: {"name": "stable-lit", "lat": -18_000, "fill": 650, "tox": -70, "reject": -20, "slip": -2, "qty": 30_000, "spread": 0},
    1: {"name": "fast-thin", "lat": -35_000, "fill": 200, "tox": 120, "reject": 30, "slip": 0, "qty": -15_000, "spread": 0},
    2: {"name": "dark-variable", "lat": 20_000, "fill": -300, "tox": 240, "reject": 130, "slip": 5, "qty": -40_000, "spread": 2},
    3: {"name": "toxic", "lat": 55_000, "fill": -900, "tox": 920, "reject": 360, "slip": 16, "qty": -65_000, "spread": 5},
    4: {"name": "slow-outage", "lat": 145_000, "fill": -1550, "tox": 500, "reject": 1250, "slip": 31, "qty": -95_000, "spread": 9},
    5: {"name": "rebate-high-reject", "lat": 80_000, "fill": -650, "tox": 350, "reject": 1650, "slip": 9, "qty": -55_000, "spread": 1},
    6: {"name": "high-liquidity-slow", "lat": 210_000, "fill": 450, "tox": 90, "reject": 90, "slip": 7, "qty": 75_000, "spread": 3},
    7: {"name": "fragile-fast", "lat": -42_000, "fill": -50, "tox": 480, "reject": 520, "slip": 13, "qty": -25_000, "spread": 2},
}


def clamp(value, low=0, high=10_000):
    return max(low, min(high, int(value)))


def generate_rows(row_count, instrument_count=8, venue_count=8, regime_count=4):
    """Yield deterministic feature rows with balanced scenario coverage."""

    event_id = 100_000
    parent_order_id = 900_000
    cycle = 0
    while event_id - 100_000 < row_count:
        policy_version = 100 + cycle // 25
        for instrument_id in range(instrument_count):
            base = 50_000 + instrument_id * 37_500 + cycle * (3 + instrument_id)
            for regime_id in range(regime_count):
                rp = REGIME_PROFILES[regime_id]
                route_child_count = 2 + ((cycle + regime_id + instrument_id) % 5)
                residual_base = [0, 250, 750, 1_500][regime_id]
                for venue_id in range(venue_count):
                    if event_id - 100_000 >= row_count:
                        return
                    vp = VENUE_PROFILES[venue_id % len(VENUE_PROFILES)]
                    event_id += 1
                    if venue_id == 0:
                        parent_order_id += 1

                    shock = ((cycle * 17 + instrument_id * 11 + regime_id * 7 + venue_id * 5) % 23) - 11
                    news_shock = 1 if (cycle + instrument_id) % 37 == 0 else 0
                    outage = 1 if venue_id in (4, 5) and (cycle + regime_id + instrument_id) % 11 in (0, 1) else 0
                    liquidity_crunch = 1 if regime_id == 3 and cycle % 9 in (0, 1, 2) else 0

                    spread = max(1, rp["spread"] + vp["spread"] + (cycle + venue_id + regime_id) % 4 + news_shock * 8)
                    bid = base + shock - spread // 2
                    ask = bid + spread
                    qty_base = rp["qty"] + vp["qty"] - cycle % 50 * 900 - regime_id * 2_800 + instrument_id * 2_200
                    if liquidity_crunch:
                        qty_base //= 3
                    bid_qty = max(500, qty_base + ((cycle + venue_id) % 7) * 2_500)
                    ask_qty = max(500, qty_base - 5_000 + ((cycle + regime_id) % 5) * 2_000)
                    latency = max(5_000, rp["lat"] + vp["lat"] + cycle % 100 * 1_700 + instrument_id * 2_900 + outage * 300_000)
                    fill = clamp(rp["fill"] + vp["fill"] - cycle % 80 * 35 + instrument_id * 55 - regime_id * 80 - outage * 1_800)
                    toxicity = clamp(rp["tox"] + vp["tox"] + cycle % 90 * 24 + instrument_id * 37 + venue_id * 18 + news_shock * 1_400)
                    reject = clamp(rp["reject"] + vp["reject"] + cycle % 70 * 38 + regime_id * 55 + outage * 2_200)
                    slippage = clamp(rp["slip"] + vp["slip"] + cycle % 40 * 2 + instrument_id * 2 + regime_id * 5 + news_shock * 35)

                    quality = fill - toxicity - reject - slippage * 45
                    if outage:
                        label_fill = 0
                    elif quality > 6_700:
                        label_fill = 10_000
                    elif quality > 4_500:
                        label_fill = 5_000
                    elif quality > 3_300 and venue_id in (0, 6):
                        label_fill = 2_500
                    else:
                        label_fill = 0

                    stress = regime_id * 850 + venue_id * 520 + cycle % 100 * 95 + instrument_id * 130 + outage * 1_200
                    label_slippage = clamp(slippage + stress // 170)
                    label_toxicity = clamp(toxicity + stress // 8)
                    residual = residual_base + (0 if label_fill == 10_000 else 300 if label_fill == 5_000 else 700 if label_fill == 2_500 else 1_250)

                    yield {
                        "schema_version": FEATURE_SCHEMA_VERSION,
                        "event_id": event_id,
                        "parent_order_id": parent_order_id,
                        "policy_version": policy_version,
                        "instrument_id": instrument_id,
                        "venue_id": venue_id,
                        "regime_id": regime_id,
                        "bid_price_ticks": bid,
                        "ask_price_ticks": ask,
                        "spread_ticks": spread,
                        "bid_qty": bid_qty,
                        "ask_qty": ask_qty,
                        "latency_nanos": latency,
                        "fill_probability_bps": fill,
                        "toxicity_bps": toxicity,
                        "reject_rate_bps": reject,
                        "slippage_bps": slippage,
                        "route_child_order_count": route_child_count,
                        "route_residual_qty": residual,
                        "label_fill_bps": label_fill,
                        "label_slippage_bps": label_slippage,
                        "label_toxicity_bps": label_toxicity,
                        "label_regime_id": regime_id,
                    }
        cycle += 1


def write_dataset(output, rows):
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=FEATURE_COLUMNS)
        writer.writeheader()
        writer.writerows(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rows", type=int, default=100_000)
    parser.add_argument("--output", type=Path, default=Path("python/examples/sor_notebook_features_100k.csv"))
    args = parser.parse_args()
    if args.rows <= 0:
        raise SystemExit("--rows must be positive")
    write_dataset(args.output, generate_rows(args.rows))
    print(f"wrote {args.rows} rows to {args.output}")


if __name__ == "__main__":
    main()
