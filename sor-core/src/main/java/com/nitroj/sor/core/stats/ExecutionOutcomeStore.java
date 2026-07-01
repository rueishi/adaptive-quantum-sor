package com.nitroj.sor.core.stats;

/**
 * Responsibility: retain bounded simulated venue execution outcomes.
 *
 * <p>Role in system: venue simulators append ACK, fill, reject, and cancel
 * outcomes here. Feature aggregation reads the store to derive rolling quality,
 * latency, health, slippage, and toxicity stats.</p>
 *
 * <p>Relationships: written by simulation, read by {@code FeatureAggregator}
 * and ML/optimizer stubs. Outcome venue IDs map to dense venue stats arrays.</p>
 *
 * <p>Lifecycle: allocated with a fixed capacity for a run or scenario and
 * reset between tests/scenarios when required.</p>
 *
 * <p>Design intent: primitive arrays keep Phase 1 deterministic and avoid
 * object allocation during simulated outcome generation.</p>
 */
public final class ExecutionOutcomeStore {
    public static final int ACK = 1;
    public static final int PARTIAL_FILL = 2;
    public static final int FULL_FILL = 3;
    public static final int REJECT = 4;
    public static final int CANCEL_ACK = 5;
    public static final int CANCEL_REJECT = 6;

    private final long[] childOrderIds;
    private final int[] venueIds;
    private final int[] outcomeTypes;
    private final long[] filledQty;
    private final int[] latencyNanos;
    private final int[] slippageBps;
    private final int[] toxicityBps;
    private int size;
    private long sequence;
    private int warningCount;

    public ExecutionOutcomeStore(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.childOrderIds = new long[capacity];
        this.venueIds = new int[capacity];
        this.outcomeTypes = new int[capacity];
        this.filledQty = new long[capacity];
        this.latencyNanos = new int[capacity];
        this.slippageBps = new int[capacity];
        this.toxicityBps = new int[capacity];
    }

    /**
     * Appends one validated simulated outcome and advances the store sequence.
     */
    public int append(
            final long childOrderId,
            final int venueId,
            final int outcomeType,
            final long filledQty,
            final int latencyNanos,
            final int slippageBps,
            final int toxicityBps
    ) {
        if (size == childOrderIds.length) {
            throw new IllegalStateException("execution outcome store is full");
        }
        if (childOrderId <= 0) {
            throw new IllegalArgumentException("childOrderId must be positive");
        }
        if (venueId < 0) {
            throw new IllegalArgumentException("venueId must be non-negative");
        }
        if (!isOutcomeType(outcomeType)) {
            throw new IllegalArgumentException("outcomeType must be known");
        }
        if (filledQty < 0) {
            throw new IllegalArgumentException("filledQty must be non-negative");
        }
        final int index = size++;
        childOrderIds[index] = childOrderId;
        venueIds[index] = venueId;
        outcomeTypes[index] = outcomeType;
        this.filledQty[index] = filledQty;
        this.latencyNanos[index] = clampNonNegative(latencyNanos);
        this.slippageBps[index] = clampBps(slippageBps);
        this.toxicityBps[index] = clampBps(toxicityBps);
        sequence++;
        return index;
    }

    /**
     * Records that an invalid raw outcome was ignored by a reader or simulator.
     */
    public void recordWarning() {
        warningCount++;
        sequence++;
    }

    public int size() {
        return size;
    }

    public long sequence() {
        return sequence;
    }

    public int warningCount() {
        return warningCount;
    }

    public long childOrderId(final int index) {
        checkIndex(index);
        return childOrderIds[index];
    }

    public int venueId(final int index) {
        checkIndex(index);
        return venueIds[index];
    }

    public int outcomeType(final int index) {
        checkIndex(index);
        return outcomeTypes[index];
    }

    public long filledQty(final int index) {
        checkIndex(index);
        return filledQty[index];
    }

    public int latencyNanos(final int index) {
        checkIndex(index);
        return latencyNanos[index];
    }

    public int slippageBps(final int index) {
        checkIndex(index);
        return slippageBps[index];
    }

    public int toxicityBps(final int index) {
        checkIndex(index);
        return toxicityBps[index];
    }

    public static boolean isFill(final int outcomeType) {
        return outcomeType == PARTIAL_FILL || outcomeType == FULL_FILL;
    }

    public static boolean isReject(final int outcomeType) {
        return outcomeType == REJECT || outcomeType == CANCEL_REJECT;
    }

    public static boolean isOutcomeType(final int outcomeType) {
        return outcomeType >= ACK && outcomeType <= CANCEL_REJECT;
    }

    private void checkIndex(final int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("outcome index out of range: " + index);
        }
    }

    static int clampBps(final int value) {
        return Math.max(0, Math.min(10_000, value));
    }

    private static int clampNonNegative(final int value) {
        return Math.max(0, value);
    }
}
