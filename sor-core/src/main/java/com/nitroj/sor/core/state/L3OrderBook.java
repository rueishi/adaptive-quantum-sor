package com.nitroj.sor.core.state;

/**
 * Responsibility: provide a minimal shell for order-by-order book events.
 *
 * <p>Role in system: L3 simulation is optional in the spec. This class gives
 * later queue-model work a stable placeholder without implementing full order
 * matching in P1-TC-004.</p>
 *
 * <p>Relationships: future queue stats may read the latest event sequence and
 * count to derive simple queue-survival features.</p>
 *
 * <p>Lifecycle: allocated during startup if L3 simulation is enabled later.</p>
 *
 * <p>Design intent: storing only sequence and event count satisfies the shell
 * requirement while keeping matching-engine fidelity out of scope.</p>
 */
public final class L3OrderBook {
    private long sequence;
    private long eventCount;

    /**
     * Records that an L3 event was observed.
     *
     * @param sequence monotonically increasing event sequence
     */
    public void recordEvent(final long sequence) {
        if (sequence < this.sequence) {
            throw new IllegalArgumentException("sequence must not go backwards");
        }
        this.sequence = sequence;
        this.eventCount++;
    }

    public long sequence() {
        return sequence;
    }

    public long eventCount() {
        return eventCount;
    }
}
