package com.nitroj.sor.core.policy;

import com.nitroj.sor.core.metadata.FeeScheduleSnapshot;
import com.nitroj.sor.core.metadata.InstrumentMetadata;
import com.nitroj.sor.core.metadata.OrderTypeCapabilityMatrix;
import com.nitroj.sor.core.metadata.VenueMetadata;
import com.nitroj.sor.core.model.ModelSignalState;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.optimizer.TacticalPolicyResult;
import com.nitroj.sor.core.optimizer.batch.BatchVenueAllocationPlan;
import com.nitroj.sor.core.risk.RiskLimitSnapshot;
import com.nitroj.sor.core.state.ChildOrderState;
import com.nitroj.sor.core.state.FeedHealthState;
import com.nitroj.sor.core.state.L2DepthBook;
import com.nitroj.sor.core.state.L3OrderBook;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.state.MarketSessionState;
import com.nitroj.sor.core.state.OutstandingChildOrderState;
import com.nitroj.sor.core.state.VenueBehaviorState;
import com.nitroj.sor.core.state.VenueSessionState;
import com.nitroj.sor.core.state.VenueThrottleState;
import com.nitroj.sor.core.stats.ExecutionOutcomeStore;
import com.nitroj.sor.core.stats.FillQualityStats;
import com.nitroj.sor.core.stats.LiquidityStabilityStats;
import com.nitroj.sor.core.stats.QueueStats;
import com.nitroj.sor.core.stats.RegimeState;
import com.nitroj.sor.core.stats.SlippageStats;
import com.nitroj.sor.core.stats.ToxicityStats;
import com.nitroj.sor.core.stats.VenueHealthStats;
import com.nitroj.sor.core.stats.VenueLatencyStats;
import com.nitroj.sor.core.stats.VenueStatsState;

/**
 * Responsibility: bundle one optimizer-cycle view of market, stats, metadata,
 * model signals, and policy lineage.
 *
 * <p>Role in system: warm-path optimizers consume this snapshot rather than
 * reading many mutable objects independently without lineage.</p>
 *
 * <p>Relationships: future builders populate the fields from simulator state,
 * feature aggregation state, risk/metadata snapshots, and the current policy.
 * ML, strategic, tactical, lint, and compiler layers read the same bundle.</p>
 *
 * <p>Lifecycle: created per optimizer cycle. The object is a container, not an
 * owner of the referenced state, and records sequence/version fields to make
 * eventual-consistency decisions auditable.</p>
 *
 * <p>Design intent: the class mirrors the authoritative spec and provides only
 * canonical index helpers, keeping construction policy in a later builder.</p>
 */
public final class PolicyOptimizationInput {
    public long inputSnapshotId;
    public long createdAtNanos;
    public long marketDataSnapshotSeq;
    public long venueStatsSnapshotSeq;
    public long executionOutcomeSnapshotSeq;
    public long modelSignalVersion;
    public long currentPolicyVersion;
    public String scenarioId;
    public long scenarioSeed;
    public int scenarioTicks;
    public int scenarioStartTickInclusive;
    public int scenarioEndTickInclusive;
    public boolean scenarioInputPublishable;

    public int instrumentCount;
    public int venueCount;
    public int regimeCount;
    public int urgencyCount;

    public MarketBookState marketBooks;
    public L2DepthBook l2DepthBook;
    public L3OrderBook l3OrderBook;
    public FeedHealthState feedHealthState;
    public MarketSessionState marketSessionState;
    public VenueBehaviorState venueBehaviorState;

    public VenueStatsState venueStats;
    public VenueLatencyStats latencyStats;
    public FillQualityStats fillQualityStats;
    public ToxicityStats toxicityStats;
    public SlippageStats slippageStats;
    public QueueStats queueStats;
    public LiquidityStabilityStats liquidityStats;
    public VenueHealthStats venueHealthStats;
    public VenueThrottleState venueThrottleState;
    public VenueSessionState venueSessionState;

    public ExecutionOutcomeStore executionOutcomes;
    public OutstandingChildOrderState outstandingChildOrders;
    public ChildOrderState childOrderState;

    public ModelSignalState modelSignals;

    public VenueMetadata venueMetadata;
    public InstrumentMetadata instrumentMetadata;
    public FeeScheduleSnapshot feeSchedule;
    public OrderTypeCapabilityMatrix orderTypeCapabilities;
    public RiskLimitSnapshot riskLimits;
    public RegimeState regimeState;

    public SorPolicy currentPolicy;
    public StrategicVenueSubsetResult latestStrategicSubset;
    public TacticalPolicyResult latestTacticalResult;
    public BatchVenueAllocationPlan latestBatchAllocationPlan;

    public PolicyOptimizationInput() {
    }

    /**
     * Computes instrument x venue index for arrays in this snapshot.
     */
    public int idxIV(final int instrumentId, final int venueId) {
        return instrumentId * venueCount + venueId;
    }

    /**
     * Computes instrument x venue x regime index for arrays in this snapshot.
     */
    public int idxIVR(final int instrumentId, final int venueId, final int regimeId) {
        return ((instrumentId * venueCount) + venueId) * regimeCount + regimeId;
    }

    /**
     * Computes instrument x venue x regime x urgency index for arrays in this snapshot.
     */
    public int idxIVRU(final int instrumentId, final int venueId, final int regimeId, final int urgencyId) {
        return (((instrumentId * venueCount) + venueId) * regimeCount + regimeId) * urgencyCount + urgencyId;
    }

    /**
     * Computes hot route key for instrument x regime x urgency lookup.
     */
    public int routeKey(final int instrumentId, final int regimeId, final int urgencyId) {
        return ((instrumentId * regimeCount) + regimeId) * urgencyCount + urgencyId;
    }
}
