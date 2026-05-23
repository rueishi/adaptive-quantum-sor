package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable request passed to risk checks.
 *
 * <p>Role in system: the engine populates this before calling
 * {@link RiskProvider#check(RiskCheckRequest, RiskDecision)}.</p>
 *
 * <p>Relationships: paired with reusable {@link RiskDecision} output.</p>
 *
 * <p>Lifecycle: owned by the engine routing path and reused after warmup.</p>
 *
 * <p>Design intent: synchronous primitive risk input with no RPC or allocation.</p>
 */
public final class RiskCheckRequest {
    private long parentOrderId;
    private int instrumentId;
    private int side;
    private long quantity;

    /**
     * Updates every risk request field.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public RiskCheckRequest set(final long parentOrderId, final int instrumentId,
                                final int side, final long quantity) {
        this.parentOrderId = parentOrderId;
        this.instrumentId = instrumentId;
        this.side = side;
        this.quantity = quantity;
        return this;
    }

    /**
     * Clears all fields.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public RiskCheckRequest clear() { return set(0, 0, 0, 0); }

    public long parentOrderId() { return parentOrderId; }
    public int instrumentId() { return instrumentId; }
    public int side() { return side; }
    public long quantity() { return quantity; }
}
