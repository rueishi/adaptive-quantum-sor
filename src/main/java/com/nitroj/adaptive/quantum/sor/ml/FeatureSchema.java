package com.nitroj.adaptive.quantum.sor.ml;

import java.util.List;

/**
 * Responsibility: define the versioned Phase 4 training feature schema.
 *
 * <p>Role in system: Java dataset export, Python training, and model artifact
 * import all use this schema version as their compatibility contract.</p>
 *
 * <p>Relationships: consumed by {@link FeatureDatasetExporter},
 * {@link TrainingLabelBuilder}, and later model artifact validation.</p>
 *
 * <p>Lifecycle: schema versions are append-only; incompatible column changes
 * must increment {@link #VERSION}.</p>
 *
 * <p>Design intent: explicit column order keeps the CSV research path simple
 * and deterministic without introducing a production feature store.</p>
 */
public final class FeatureSchema {
    public static final String VERSION = "feature-schema-v1";

    public static final List<String> COLUMNS = List.of(
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
            "label_regime_id"
    );

    private FeatureSchema() {
    }

    public static String csvHeader() {
        return String.join(",", COLUMNS);
    }
}
