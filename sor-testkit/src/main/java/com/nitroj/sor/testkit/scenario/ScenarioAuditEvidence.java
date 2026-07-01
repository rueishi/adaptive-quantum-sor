package com.nitroj.sor.testkit.scenario;

import com.nitroj.sor.api.MarketDataSnapshotSummary;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;

/**
 * Responsibility: compact deterministic evidence for a completed scenario run.
 *
 * <p>Role in system: links reset evidence, engine-owned market-data checksum,
 * route/fill/reject counts, final order state, and lifecycle replay size.</p>
 *
 * <p>Relationships: returned by {@link ScenarioRunner#runWithEvidence} and
 * wraps the legacy-compatible {@link ScenarioSummary}.</p>
 *
 * <p>Lifecycle: immutable value created after scenario replay completes.</p>
 *
 * <p>Design intent: make scenario replay auditable without exposing simulator
 * internals or engine-owned mutable books.</p>
 */
public record ScenarioAuditEvidence(
        ScenarioSummary summary,
        SorResetSummary resetSummary,
        MarketDataSnapshotSummary marketDataSnapshot,
        SorStateSummary finalStateSummary,
        int routeDecidedEvents,
        int childOrderEvents,
        int fillEvents,
        int rejectEvents,
        long lifecycleEventCount
) {
    public ScenarioAuditEvidence {
        if (summary == null || resetSummary == null || marketDataSnapshot == null || finalStateSummary == null) {
            throw new IllegalArgumentException("scenario evidence summaries must not be null");
        }
        if (routeDecidedEvents < 0 || childOrderEvents < 0 || fillEvents < 0 || rejectEvents < 0
                || lifecycleEventCount < 0) {
            throw new IllegalArgumentException("scenario evidence counts must be non-negative");
        }
    }
}
