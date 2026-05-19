package com.nitroj.adaptive.quantum.sor.policy;

import com.nitroj.adaptive.quantum.sor.governance.PolicyChangeLedgerEntry;
import com.nitroj.adaptive.quantum.sor.governance.PolicyDiff;
import com.nitroj.adaptive.quantum.sor.governance.PolicySnapshotStore;
import com.nitroj.adaptive.quantum.sor.optimizer.StrategicVenueSubsetResult;
import com.nitroj.adaptive.quantum.sor.optimizer.TacticalPolicyResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify P1-TC-006 policy data structures.
 *
 * <p>Role in system: proves the policy containers use canonical indexing,
 * validate publication arrays, and preserve policy identity fields.</p>
 *
 * <p>Relationships: covers policy, optimizer result, and governance structures
 * that later compiler and publisher tasks consume.</p>
 *
 * <p>Lifecycle: executed by Gradle during unit-test validation for the policy
 * data-structure card.</p>
 *
 * <p>Design intent: tests focus on the task card's acceptance behaviors rather
 * than future compiler logic.</p>
 */
final class PolicyDataStructuresTest {
    @Test
    void routeListOffsetLengthFormulaAndRouteLookupWork() {
        final HotRouteBook book = routeBook();

        assertEquals(2 * 3 * 2 + 1, book.routeListOffset.length);
        assertEquals(8, book.routeKey(1, 1, 0));
        assertEquals(8, book.routeStart(1, 1, 0));
        assertEquals(9, book.routeEnd(1, 1, 0));
        assertEquals(8, book.routeVenueId[book.routeStart(1, 1, 0)]);
    }

    @Test
    void hotRouteBookRequiresAlignedArraysAndMonotonicOffsets() {
        final HotRouteBook book = routeBook();
        assertEquals(book.routeVenueId.length, book.weightBps.length);
        assertEquals(book.routeVenueId.length, book.maxParticipationBps.length);

        final int[] badOffsets = book.routeListOffset.clone();
        badOffsets[3] = badOffsets[2] - 1;
        assertEquals("routeListOffset must be monotonic", assertThrows(
                IllegalArgumentException.class,
                () -> new HotRouteBook(2, 3, 2, badOffsets, book.routeVenueId, book.routeFlags,
                        book.weightBps, book.latencyPenaltyNanos, book.toxicityPenaltyBps,
                        book.fillProbabilityBps, book.rejectPenaltyBps, book.queueSurvivalBps,
                        book.feePenaltyTicks, book.slippagePenaltyBps, book.marketImpactPenaltyBps,
                        book.minChildQty, book.maxChildQty, book.maxVenueNotional, book.maxParticipationBps)
        ).getMessage());

        assertEquals("weightBps length must align with routeVenueId", assertThrows(
                IllegalArgumentException.class,
                () -> new HotRouteBook(2, 3, 2, book.routeListOffset, book.routeVenueId, book.routeFlags,
                        new int[1], book.latencyPenaltyNanos, book.toxicityPenaltyBps,
                        book.fillProbabilityBps, book.rejectPenaltyBps, book.queueSurvivalBps,
                        book.feePenaltyTicks, book.slippagePenaltyBps, book.marketImpactPenaltyBps,
                        book.minChildQty, book.maxChildQty, book.maxVenueNotional, book.maxParticipationBps)
        ).getMessage());
    }

    @Test
    void policyIdentityFieldsArePopulatedAndHashIsDefensivelyCopied() {
        final byte[] sha = new byte[32];
        Arrays.fill(sha, (byte) 7);
        final SorPolicy policy = new SorPolicy(42L, 100L, 200L, 1234L, sha, 11L, 12L, 2, 1, routeBook(), fullMatrix());

        sha[0] = 99;

        assertEquals(42L, policy.policyVersion);
        assertEquals(100L, policy.createdAtEpochNanos);
        assertEquals(200L, policy.effectiveFromEpochNanos);
        assertEquals(1234L, policy.policyHash64);
        assertEquals(7, policy.policyHashSha256[0]);
        assertEquals(11L, policy.isingResultVersion);
        assertEquals(12L, policy.cudaTuningVersion);
        assertSame(policy.hotRouteBook, policy.hotRouteBook);
        assertNotNull(policy.fullPolicyMatrix);
    }

    @Test
    void policySnapshotStoreContractCanStoreAndLoadPolicy() {
        final SorPolicy policy = policy();
        final PolicySnapshotStore store = new PolicySnapshotStore() {
            private SorPolicy stored;

            @Override
            public void store(final SorPolicy policy) {
                stored = policy;
            }

            @Override
            public SorPolicy load(final long policyVersion) {
                return stored != null && stored.policyVersion == policyVersion ? stored : null;
            }
        };

        store.store(policy);

        assertSame(policy, store.load(policy.policyVersion));
        assertNull(store.load(999L));
    }

    @Test
    void fullMatrixAndMutableCandidateUseIvruLayout() {
        final FullPolicyMatrix matrix = fullMatrix();
        final MutablePolicyCandidate candidate = new MutablePolicyCandidate(2, 3, 2, 2);

        assertEquals(15, matrix.idxIVRU(1, 0, 1, 1));
        assertEquals(15, candidate.idxIVRU(1, 0, 1, 1));
        assertEquals(24, candidate.venueEligible.length);
        assertThrows(IndexOutOfBoundsException.class, () -> matrix.idxIVRU(2, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MutablePolicyCandidate(0, 1, 1, 1));
    }

    @Test
    void optimizationInputAndGovernanceStructuresExposeExpectedContracts() {
        final PolicyOptimizationInput input = new PolicyOptimizationInput();
        input.instrumentCount = 2;
        input.venueCount = 3;
        input.regimeCount = 2;
        input.urgencyCount = 2;

        assertEquals(5, input.idxIV(1, 2));
        assertEquals(11, input.idxIVR(1, 2, 1));
        assertEquals(23, input.idxIVRU(1, 2, 1, 1));
        assertEquals(7, input.routeKey(1, 1, 1));

        final StrategicVenueSubsetResult subset = new StrategicVenueSubsetResult();
        subset.instrumentCount = 2;
        subset.regimeCount = 2;
        subset.urgencyCount = 2;
        assertEquals(7, subset.routeKey(1, 1, 1));

        final TacticalPolicyResult tactical = new TacticalPolicyResult();
        tactical.version = 5L;
        assertEquals(5L, tactical.version);

        final PolicyDiff diff = new PolicyDiff();
        diff.maxWeightChangeBps = 250;
        final PolicyChangeLedgerEntry entry = new PolicyChangeLedgerEntry();
        entry.diff = diff;
        entry.published = true;
        assertTrue(entry.published);
        assertEquals(250, entry.diff.maxWeightChangeBps);
    }

    private static HotRouteBook routeBook() {
        final int routeCount = 12;
        final int[] offsets = new int[routeCount + 1];
        final short[] venues = new short[routeCount];
        for (int i = 0; i < routeCount; i++) {
            offsets[i] = i;
            venues[i] = (short) i;
        }
        offsets[routeCount] = routeCount;
        return new HotRouteBook(2, 3, 2, offsets, venues, new short[routeCount],
                filled(routeCount, 1_000), filled(routeCount, 10), filled(routeCount, 20),
                filled(routeCount, 9_000), filled(routeCount, 30), filled(routeCount, 8_000),
                filled(routeCount, 1), filled(routeCount, 5), filled(routeCount, 6),
                longFilled(routeCount, 1), longFilled(routeCount, 1_000), longFilled(routeCount, 10_000),
                filled(routeCount, 2_500));
    }

    private static FullPolicyMatrix fullMatrix() {
        final int length = 24;
        return new FullPolicyMatrix(2, 3, 2, 2, new boolean[length], new int[length], new int[length],
                new int[length], new int[length], new int[length], new int[length], new int[length],
                new int[length], new int[length], new int[length], new long[length], new long[length],
                new int[length], new short[length]);
    }

    private static SorPolicy policy() {
        return new SorPolicy(1L, 100L, 100L, 42L, new byte[32], 1L, 1L, 1, 1, routeBook(), fullMatrix());
    }

    private static int[] filled(final int length, final int value) {
        final int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    private static long[] longFilled(final int length, final long value) {
        final long[] result = new long[length];
        Arrays.fill(result, value);
        return result;
    }
}
