"""Shared schema constants for notebook and training helpers."""

FEATURE_SCHEMA_VERSION = "feature-schema-v1"

FEATURE_COLUMNS = [
    "schema_version",
    "event_id",
    "parent_order_id",
    "policy_version",
    "instrument_id",
    "venue_id",
    "regime_id",
    "bid_price_ticks",
    "ask_price_ticks",
    "spread_ticks",
    "bid_qty",
    "ask_qty",
    "latency_nanos",
    "fill_probability_bps",
    "toxicity_bps",
    "reject_rate_bps",
    "slippage_bps",
    "route_child_order_count",
    "route_residual_qty",
    "label_fill_bps",
    "label_slippage_bps",
    "label_toxicity_bps",
    "label_regime_id",
]

PREDICTION_COLUMNS = [
    "instrument_id",
    "venue_id",
    "regime_id",
    "venue_score_bps",
]

ORDER_COLUMNS = [
    "instrumentId",
    "side",
    "quantity",
    "urgencyId",
]
