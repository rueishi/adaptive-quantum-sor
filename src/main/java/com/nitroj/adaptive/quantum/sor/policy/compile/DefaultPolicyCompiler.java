package com.nitroj.adaptive.quantum.sor.policy.compile;

import com.nitroj.adaptive.quantum.sor.governance.PolicyChangeLedgerEntry;
import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import com.nitroj.adaptive.quantum.sor.policy.FullPolicyMatrix;
import com.nitroj.adaptive.quantum.sor.policy.HotRouteBook;
import com.nitroj.adaptive.quantum.sor.policy.MutablePolicyCandidate;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;
import com.nitroj.adaptive.quantum.sor.policy.lint.PolicyLintReport;

import java.io.ByteArrayOutputStream;

/**
 * Responsibility: compile Phase 1 candidates into immutable policy artifacts.
 *
 * <p>Role in system: this class is the boundary between optimizer-produced
 * warm-path arrays and the read-only policy consumed by execution.</p>
 *
 * <p>Relationships: consumes {@link MutablePolicyCandidate},
 * {@link StrategicVenueSubsetResult}, {@link TacticalPolicyResult}, and emits
 * {@link SorPolicy}, {@link PolicyDiff}, and {@link PolicyChangeLedgerEntry}.</p>
 *
 * <p>Lifecycle: called once per candidate after linting. The compiler is
 * stateless except for the last diff/ledger references exposed for Phase 1
 * tests and simple publication wiring.</p>
 *
 * <p>Design intent: deterministic ranking by tactical weight and venue ID keeps
 * route lists stable while later richer scoring remains out of scope.</p>
 */
public final class DefaultPolicyCompiler implements PolicyCompiler {
    private final CompiledScoreConfig config;
    private final SorPolicy currentPolicy;
    private PolicyDiff lastDiff;
    private PolicyChangeLedgerEntry lastLedgerEntry;
    private long nextPolicyVersion = 1L;

    public DefaultPolicyCompiler(final CompiledScoreConfig config) {
        this(config, null);
    }

    public DefaultPolicyCompiler(final CompiledScoreConfig config, final SorPolicy currentPolicy) {
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        this.config = config;
        this.currentPolicy = currentPolicy;
    }

    /**
     * Compiles only when lint has no errors.
     */
    public SorPolicy compile(
            final MutablePolicyCandidate candidate,
            final StrategicVenueSubsetResult strategicResult,
            final TacticalPolicyResult tacticalResult,
            final PolicyLintReport lintReport
    ) {
        if (lintReport != null && lintReport.hasErrors()) {
            throw new IllegalArgumentException("cannot compile candidate with lint errors");
        }
        return compile(candidate, strategicResult, tacticalResult);
    }

    /**
     * Builds FullPolicyMatrix, capped HotRouteBook, hash, diff, and ledger entry.
     */
    @Override
    public SorPolicy compile(
            final MutablePolicyCandidate candidate,
            final StrategicVenueSubsetResult strategicResult,
            final TacticalPolicyResult tacticalResult
    ) {
        if (candidate == null || strategicResult == null || tacticalResult == null) {
            throw new IllegalArgumentException("candidate, strategicResult, and tacticalResult must not be null");
        }
        final FullPolicyMatrix matrix = fullMatrix(candidate, tacticalResult);
        final HotRouteBook hotRouteBook = hotRouteBook(candidate, tacticalResult);
        final byte[] canonical = canonicalBytes(candidate, hotRouteBook);
        final byte[] sha = PolicyHash.sha256(canonical);
        final long hash64 = PolicyHash.hash64(sha);
        final long version = currentPolicy == null ? nextPolicyVersion++ : currentPolicy.policyVersion + 1L;
        final long now = Math.max(1L, System.nanoTime());
        final SorPolicy policy = new SorPolicy(
                version,
                now,
                now,
                hash64,
                sha,
                strategicResult.version,
                tacticalResult.version,
                config.optimizerType,
                config.policyState,
                hotRouteBook,
                matrix
        );
        lastDiff = diff(currentPolicy, policy);
        lastLedgerEntry = ledger(currentPolicy, policy, lastDiff, strategicResult.optimizerRunId);
        return policy;
    }

    public PolicyDiff lastDiff() {
        return lastDiff;
    }

    public PolicyChangeLedgerEntry lastLedgerEntry() {
        return lastLedgerEntry;
    }

    private static FullPolicyMatrix fullMatrix(final MutablePolicyCandidate candidate, final TacticalPolicyResult tactical) {
        final int length = candidate.instrumentCount * candidate.venueCount * candidate.regimeCount * candidate.urgencyCount;
        final int[] rankScore = new int[length];
        for (int i = 0; i < length; i++) {
            rankScore[i] = tactical.venueWeightBps[i] + tactical.fillProbabilityBps[i]
                    - tactical.toxicityPenaltyBps[i] - tactical.rejectPenaltyBps[i];
        }
        return new FullPolicyMatrix(
                candidate.instrumentCount,
                candidate.venueCount,
                candidate.regimeCount,
                candidate.urgencyCount,
                candidate.venueEligible.clone(),
                rankScore,
                candidate.venueWeightBps.clone(),
                candidate.latencyPenaltyNanos.clone(),
                candidate.toxicityPenaltyBps.clone(),
                candidate.fillProbabilityBps.clone(),
                candidate.rejectPenaltyBps.clone(),
                tactical.queueSurvivalBps.clone(),
                new int[length],
                tactical.slippagePenaltyBps.clone(),
                tactical.marketImpactPenaltyBps.clone(),
                candidate.minChildQty.clone(),
                candidate.maxChildQty.clone(),
                candidate.maxParticipationBps.clone(),
                candidate.routeFlags.clone()
        );
    }

    private HotRouteBook hotRouteBook(final MutablePolicyCandidate candidate, final TacticalPolicyResult tactical) {
        final int routeCount = candidate.instrumentCount * candidate.regimeCount * candidate.urgencyCount;
        final int[] offsets = new int[routeCount + 1];
        final int maxRoutes = routeCount * Math.min(config.maxEligibleVenuesPerRoute, candidate.venueCount);
        final short[] venues = new short[maxRoutes];
        final short[] flags = new short[maxRoutes];
        final int[] weights = new int[maxRoutes];
        final int[] latency = new int[maxRoutes];
        final int[] toxicity = new int[maxRoutes];
        final int[] fill = new int[maxRoutes];
        final int[] reject = new int[maxRoutes];
        final int[] queue = new int[maxRoutes];
        final int[] fee = new int[maxRoutes];
        final int[] slippage = new int[maxRoutes];
        final int[] impact = new int[maxRoutes];
        final long[] minQty = new long[maxRoutes];
        final long[] maxQty = new long[maxRoutes];
        final long[] maxNotional = new long[maxRoutes];
        final int[] participation = new int[maxRoutes];
        int write = 0;
        for (int instrumentId = 0; instrumentId < candidate.instrumentCount; instrumentId++) {
            for (int regimeId = 0; regimeId < candidate.regimeCount; regimeId++) {
                for (int urgencyId = 0; urgencyId < candidate.urgencyCount; urgencyId++) {
                    final int routeKey = ((instrumentId * candidate.regimeCount) + regimeId) * candidate.urgencyCount + urgencyId;
                    offsets[routeKey] = write;
                    for (int rank = 0; rank < config.maxEligibleVenuesPerRoute; rank++) {
                        final int venueId = bestVenue(candidate, instrumentId, regimeId, urgencyId, venues, offsets[routeKey], write);
                        if (venueId < 0) {
                            break;
                        }
                        final int idx = candidate.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
                        venues[write] = (short) venueId;
                        flags[write] = candidate.routeFlags[idx];
                        weights[write] = candidate.venueWeightBps[idx];
                        latency[write] = candidate.latencyPenaltyNanos[idx];
                        toxicity[write] = candidate.toxicityPenaltyBps[idx];
                        fill[write] = candidate.fillProbabilityBps[idx];
                        reject[write] = candidate.rejectPenaltyBps[idx];
                        queue[write] = tactical.queueSurvivalBps[idx];
                        slippage[write] = tactical.slippagePenaltyBps[idx];
                        impact[write] = tactical.marketImpactPenaltyBps[idx];
                        minQty[write] = candidate.minChildQty[idx];
                        maxQty[write] = candidate.maxChildQty[idx];
                        maxNotional[write] = candidate.maxChildQty[idx] * 1_000L;
                        participation[write] = candidate.maxParticipationBps[idx];
                        write++;
                    }
                }
            }
        }
        offsets[routeCount] = write;
        return new HotRouteBook(candidate.instrumentCount, candidate.regimeCount, candidate.urgencyCount,
                trim(offsets, offsets.length), trim(venues, write), trim(flags, write), trim(weights, write),
                trim(latency, write), trim(toxicity, write), trim(fill, write), trim(reject, write),
                trim(queue, write), trim(fee, write), trim(slippage, write), trim(impact, write),
                trim(minQty, write), trim(maxQty, write), trim(maxNotional, write), trim(participation, write));
    }

    private static int bestVenue(
            final MutablePolicyCandidate candidate,
            final int instrumentId,
            final int regimeId,
            final int urgencyId,
            final short[] already,
            final int start,
            final int end
    ) {
        int bestVenue = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int venueId = 0; venueId < candidate.venueCount; venueId++) {
            if (contains(already, start, end, venueId)) {
                continue;
            }
            final int idx = candidate.idxIVRU(instrumentId, venueId, regimeId, urgencyId);
            if (!candidate.venueEligible[idx]) {
                continue;
            }
            final int score = candidate.venueWeightBps[idx] + candidate.fillProbabilityBps[idx]
                    - candidate.toxicityPenaltyBps[idx] - candidate.rejectPenaltyBps[idx];
            if (score > bestScore || (score == bestScore && venueId < bestVenue)) {
                bestScore = score;
                bestVenue = venueId;
            }
        }
        return bestVenue;
    }

    private static boolean contains(final short[] values, final int start, final int end, final int venueId) {
        for (int i = start; i < end; i++) {
            if (values[i] == venueId) {
                return true;
            }
        }
        return false;
    }

    private static byte[] canonicalBytes(final MutablePolicyCandidate candidate, final HotRouteBook book) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(candidate.instrumentCount);
        out.write(candidate.venueCount);
        out.write(candidate.regimeCount);
        out.write(candidate.urgencyCount);
        for (short venueId : book.routeVenueId) {
            out.write(venueId);
        }
        for (int weight : book.weightBps) {
            out.write(weight);
        }
        return out.toByteArray();
    }

    private static PolicyDiff diff(final SorPolicy previous, final SorPolicy current) {
        final PolicyDiff diff = new PolicyDiff();
        diff.previousPolicyVersion = previous == null ? 0L : previous.policyVersion;
        diff.newPolicyVersion = current.policyVersion;
        diff.changedRouteListCount = current.hotRouteBook.routeListOffset.length - 1;
        diff.changedWeightCount = current.hotRouteBook.weightBps.length;
        diff.changedPenaltyCount = current.hotRouteBook.toxicityPenaltyBps.length + current.hotRouteBook.rejectPenaltyBps.length;
        diff.addedVenueCount = current.hotRouteBook.routeVenueId.length;
        diff.maxWeightChangeBps = max(current.hotRouteBook.weightBps);
        return diff;
    }

    private static PolicyChangeLedgerEntry ledger(
            final SorPolicy previous,
            final SorPolicy current,
            final PolicyDiff diff,
            final long optimizerRunId
    ) {
        final PolicyChangeLedgerEntry entry = new PolicyChangeLedgerEntry();
        entry.ledgerEntryId = current.policyVersion;
        entry.timestampNanos = current.createdAtEpochNanos;
        entry.previousPolicyVersion = previous == null ? 0L : previous.policyVersion;
        entry.newPolicyVersion = current.policyVersion;
        entry.newPolicyHash64 = current.policyHash64;
        entry.optimizerRunId = optimizerRunId;
        entry.published = false;
        entry.reason = "COMPILED";
        entry.diff = diff;
        return entry;
    }

    private static int max(final int[] values) {
        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private static int[] trim(final int[] values, final int length) {
        return java.util.Arrays.copyOf(values, length);
    }

    private static short[] trim(final short[] values, final int length) {
        return java.util.Arrays.copyOf(values, length);
    }

    private static long[] trim(final long[] values, final int length) {
        return java.util.Arrays.copyOf(values, length);
    }
}
