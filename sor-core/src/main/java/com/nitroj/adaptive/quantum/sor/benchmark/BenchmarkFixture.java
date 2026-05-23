package com.nitroj.adaptive.quantum.sor.benchmark;

import com.nitroj.adaptive.quantum.sor.config.SorConfig;
import com.nitroj.adaptive.quantum.sor.config.SorConfig.RuntimeMode;
import com.nitroj.adaptive.quantum.sor.execution.PolicyDrivenSorExecutioner;
import com.nitroj.adaptive.quantum.sor.execution.StaticSorExecutioner;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.model.ChildOrderBuffer;
import com.nitroj.adaptive.quantum.sor.model.OrderIntent;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.recovery.InitialPolicyBootstrap;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

import java.util.List;

/**
 * Responsibility: provide fixed benchmark inputs.
 *
 * <p>Role in system: benchmark classes use this fixture so latency/allocation
 * measurements run against deterministic market, fee, risk, and order inputs.</p>
 *
 * <p>Relationships: creates the static SOR benchmark dependencies. Adaptive
 * policy benchmarking publishes a deterministic baseline policy over the same
 * order and market shape.</p>
 *
 * <p>Lifecycle: created once per benchmark run.</p>
 *
 * <p>Design intent: avoid benchmark setup in measured methods.</p>
 */
public final class BenchmarkFixture {
    public final MarketBookState marketBookState = new MarketBookState(1, 3);
    public final VenueSessionState venueSessionState = new VenueSessionState(3);
    public final RiskLimitSnapshot riskLimitSnapshot = new RiskLimitSnapshot(1, 3);
    public final FeeScheduleSnapshot feeScheduleSnapshot = new FeeScheduleSnapshot(1, 3);
    public final OrderIntent parentOrder = new OrderIntent(1L, 0, Side.BUY, 100L, 0, 1L);
    public final ChildOrderBuffer childOrderBuffer = new ChildOrderBuffer(4);

    public BenchmarkFixture() {
        for (int venueId = 0; venueId < 3; venueId++) {
            marketBookState.updateTopOfBook(0, venueId, 100, 101 + venueId, 1_000, 1_000);
            venueSessionState.setStatus(venueId, com.nitroj.adaptive.quantum.sor.model.VenueStatus.OPEN);
            riskLimitSnapshot.setVenueLimits(0, venueId, 1_000_000L, 2_500);
            feeScheduleSnapshot.setFees(0, venueId, 0, venueId);
        }
        riskLimitSnapshot.setMaxChildQty(0, 1_000L);
    }

    public StaticSorExecutioner staticSorExecutioner() {
        return new StaticSorExecutioner(3, marketBookState, feeScheduleSnapshot, venueSessionState, riskLimitSnapshot);
    }

    public PolicyDrivenSorExecutioner policyDrivenSorExecutioner() {
        final SorConfig config = new SorConfig(1, 3, 1, 1, RuntimeMode.STRICT, true);
        final SorPolicy policy = new InitialPolicyBootstrap().bootstrap(config);
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(policy, PolicyValidationReport.validReport(), new PolicyLintReport(List.of()),
                new PolicyDiff(), 10, 1L);
        return new PolicyDrivenSorExecutioner(publisher, marketBookState, venueSessionState, riskLimitSnapshot);
    }
}
