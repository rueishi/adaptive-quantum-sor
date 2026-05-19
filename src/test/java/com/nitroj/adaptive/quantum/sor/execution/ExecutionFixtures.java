package com.nitroj.adaptive.quantum.sor.execution;

import com.nitroj.adaptive.quantum.sor.TestPolicyFixtures;
import com.nitroj.adaptive.quantum.sor.governance.InMemoryPolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.metadata.FeeScheduleSnapshot;
import com.nitroj.adaptive.quantum.sor.model.Side;
import com.nitroj.adaptive.quantum.sor.policy.PolicyPublisher;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.publication.PublicationGate;
import com.nitroj.adaptive.quantum.sor.policy.validation.PolicyValidationReport;
import com.nitroj.adaptive.quantum.sor.risk.RiskLimitSnapshot;
import com.nitroj.adaptive.quantum.sor.state.MarketBookState;
import com.nitroj.adaptive.quantum.sor.state.VenueSessionState;

/**
 * Responsibility: provide execution-state fixtures for routing tests.
 *
 * <p>Role in system: adaptive, static, reslice, and metrics tests need
 * identical market/session/risk/policy setup.</p>
 *
 * <p>Relationships: uses prior policy fixture helpers and production state
 * structures.</p>
 *
 * <p>Lifecycle: static test helper only.</p>
 *
 * <p>Design intent: centralize small deterministic market setup so test bodies
 * stay focused on behavior.</p>
 */
public final class ExecutionFixtures {
    private ExecutionFixtures() {
    }

    public static Fixture fixture() {
        final MarketBookState market = new MarketBookState(1, TestPolicyFixtures.VENUES);
        market.updateTopOfBook(0, 0, 100, 101, 1_000, 1_000);
        market.updateTopOfBook(0, 1, 100, 102, 1_000, 500);
        market.updateTopOfBook(0, 2, 100, 103, 1_000, 0);
        final VenueSessionState sessions = new VenueSessionState(TestPolicyFixtures.VENUES);
        for (int venueId = 0; venueId < TestPolicyFixtures.VENUES; venueId++) {
            sessions.setStatus(venueId, com.nitroj.adaptive.quantum.sor.model.VenueStatus.OPEN);
        }
        final RiskLimitSnapshot risk = new RiskLimitSnapshot(1, TestPolicyFixtures.VENUES);
        risk.setMaxChildQty(0, 700L);
        for (int venueId = 0; venueId < TestPolicyFixtures.VENUES; venueId++) {
            risk.setVenueLimits(0, venueId, 1_000_000L, 2_500);
        }
        final SorPolicy policy = TestPolicyFixtures.policy();
        final PolicyPublisher publisher = new PolicyPublisher(PublicationGate.permissive(), new InMemoryPolicySnapshotStore());
        publisher.publish(policy, PolicyValidationReport.validReport(), TestPolicyFixtures.candidateBundle().lint(),
                new com.nitroj.adaptive.quantum.sor.governance.PolicyDiff(), 10, 1L);
        final FeeScheduleSnapshot fees = new FeeScheduleSnapshot(1, TestPolicyFixtures.VENUES);
        fees.setFees(0, 0, 0, 3);
        fees.setFees(0, 1, 0, 1);
        fees.setFees(0, 2, 0, 10);
        return new Fixture(market, sessions, risk, policy, publisher, fees);
    }

    public static com.nitroj.adaptive.quantum.sor.model.OrderIntent buy(final long qty) {
        return new com.nitroj.adaptive.quantum.sor.model.OrderIntent(1L, 0, Side.BUY, qty, 0, 1L);
    }

    public record Fixture(
            MarketBookState market,
            VenueSessionState sessions,
            RiskLimitSnapshot risk,
            SorPolicy policy,
            PolicyPublisher publisher,
            FeeScheduleSnapshot fees
    ) {
    }
}
