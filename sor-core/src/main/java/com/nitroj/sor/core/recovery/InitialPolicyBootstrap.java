package com.nitroj.sor.core.recovery;

import com.nitroj.sor.core.config.SorConfig;
import com.nitroj.sor.core.optimizer.StrategicVenueSubsetResult;
import com.nitroj.sor.core.optimizer.TacticalPolicyResult;
import com.nitroj.sor.core.policy.MutablePolicyCandidate;
import com.nitroj.sor.core.policy.SorPolicy;
import com.nitroj.sor.core.policy.compile.CompiledScoreConfig;
import com.nitroj.sor.core.policy.compile.DefaultPolicyCompiler;
import com.nitroj.sor.core.policy.lint.PolicyLintReport;

import java.util.List;

/**
 * Responsibility: regenerate a deterministic baseline policy during startup.
 *
 * <p>Role in system: restart recovery needs an active policy even when no
 * durable Phase 1 snapshot exists. This bootstrapper converts the reloaded
 * {@link SorConfig} dimensions into a small valid policy artifact.</p>
 *
 * <p>Relationships: used by {@link StartupRecoveryCoordinator}; produces the
 * same {@link SorPolicy} shape consumed by the publisher and executioner.</p>
 *
 * <p>Lifecycle: called during process startup or test recovery flows before
 * parent orders are allowed onto the adaptive route path.</p>
 *
 * <p>Design intent: keep recovery deterministic and dependency-light by using
 * the existing compiler contract with neutral, safe baseline parameters.</p>
 */
public class InitialPolicyBootstrap {
    private static final long BASELINE_VERSION = 1L;
    private static final long BASELINE_OPTIMIZER_RUN_ID = 1L;

    /**
     * Builds an initial policy from validated runtime dimensions.
     *
     * <p>Every instrument/regime/urgency route receives every configured venue
     * as eligible. Venue IDs are scored in ascending preference order, child
     * quantities are bounded, and participation is capped to conservative Phase
     * 1 values. The resulting candidate is compiled by the normal policy
     * compiler so route-book and full-matrix invariants match published
     * optimizer policies.</p>
     *
     * @param config reloaded startup configuration
     * @return compiled baseline policy ready for publication
     * @throws IllegalArgumentException when config is absent
     */
    public SorPolicy bootstrap(final SorConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(
                config.instrumentCount(),
                config.venueCount(),
                config.regimeCount(),
                config.urgencyCount()
        );
        final TacticalPolicyResult tactical = tactical(config);
        populateCandidate(candidate, tactical);
        final StrategicVenueSubsetResult strategic = strategic(config);
        return new DefaultPolicyCompiler(new CompiledScoreConfig(Math.min(4, config.venueCount()), 0, 1))
                .compile(candidate, strategic, tactical, new PolicyLintReport(List.of()));
    }

    private static void populateCandidate(final MutablePolicyCandidate candidate, final TacticalPolicyResult tactical) {
        for (int instrumentId = 0; instrumentId < candidate.instrumentCount; instrumentId++) {
            for (int venueId = 0; venueId < candidate.venueCount; venueId++) {
                for (int regimeId = 0; regimeId < candidate.regimeCount; regimeId++) {
                    for (int urgencyId = 0; urgencyId < candidate.urgencyCount; urgencyId++) {
                        final int idx = candidate.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        final int venuePreference = candidate.venueCount - venueId;
                        candidate.venueEligible[idx] = true;
                        candidate.venueWeightBps[idx] = 1_000 + venuePreference;
                        candidate.latencyPenaltyNanos[idx] = 0;
                        candidate.toxicityPenaltyBps[idx] = 0;
                        candidate.fillProbabilityBps[idx] = 9_000;
                        candidate.rejectPenaltyBps[idx] = 0;
                        candidate.minChildQty[idx] = 1L;
                        candidate.maxChildQty[idx] = 1_000L;
                        candidate.maxParticipationBps[idx] = 2_500;

                        tactical.venueWeightBps[idx] = candidate.venueWeightBps[idx];
                        tactical.latencyPenaltyNanos[idx] = candidate.latencyPenaltyNanos[idx];
                        tactical.toxicityPenaltyBps[idx] = candidate.toxicityPenaltyBps[idx];
                        tactical.fillProbabilityBps[idx] = candidate.fillProbabilityBps[idx];
                        tactical.rejectPenaltyBps[idx] = candidate.rejectPenaltyBps[idx];
                        tactical.queueSurvivalBps[idx] = 10_000;
                        tactical.slippagePenaltyBps[idx] = 0;
                        tactical.marketImpactPenaltyBps[idx] = 0;
                        tactical.minChildQty[idx] = candidate.minChildQty[idx];
                        tactical.maxChildQty[idx] = candidate.maxChildQty[idx];
                        tactical.maxParticipationBps[idx] = candidate.maxParticipationBps[idx];
                    }
                }
            }
        }
    }

    private static StrategicVenueSubsetResult strategic(final SorConfig config) {
        final StrategicVenueSubsetResult result = new StrategicVenueSubsetResult();
        result.version = BASELINE_VERSION;
        result.createdAtNanos = Math.max(1L, System.nanoTime());
        result.instrumentCount = config.instrumentCount();
        result.regimeCount = config.regimeCount();
        result.urgencyCount = config.urgencyCount();
        result.optimizerRunId = BASELINE_OPTIMIZER_RUN_ID;
        result.optimizerType = 0;
        final int routeCount = config.instrumentCount() * config.regimeCount() * config.urgencyCount();
        result.subsetOffset = new int[routeCount + 1];
        result.selectedVenueIds = new short[routeCount * config.venueCount()];
        int write = 0;
        for (int routeKey = 0; routeKey < routeCount; routeKey++) {
            result.subsetOffset[routeKey] = write;
            for (int venueId = 0; venueId < config.venueCount(); venueId++) {
                result.selectedVenueIds[write++] = (short) venueId;
            }
        }
        result.subsetOffset[routeCount] = write;
        return result;
    }

    private static TacticalPolicyResult tactical(final SorConfig config) {
        final TacticalPolicyResult result = new TacticalPolicyResult();
        result.version = BASELINE_VERSION;
        result.createdAtNanos = Math.max(1L, System.nanoTime());
        result.strategicSubsetVersion = BASELINE_VERSION;
        final int length = config.instrumentCount() * config.venueCount() * config.regimeCount() * config.urgencyCount();
        result.venueWeightBps = new int[length];
        result.latencyPenaltyNanos = new int[length];
        result.toxicityPenaltyBps = new int[length];
        result.fillProbabilityBps = new int[length];
        result.rejectPenaltyBps = new int[length];
        result.queueSurvivalBps = new int[length];
        result.slippagePenaltyBps = new int[length];
        result.marketImpactPenaltyBps = new int[length];
        result.minChildQty = new long[length];
        result.maxChildQty = new long[length];
        result.maxParticipationBps = new int[length];
        return result;
    }
}
