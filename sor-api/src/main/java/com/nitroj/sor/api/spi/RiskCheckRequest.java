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
     * Creates an empty reusable riskCheckRequest.
     */
    public RiskCheckRequest() {
    }

    /**
     * Updates every risk request field.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     *
     * @param parentOrderId engine-assigned parent order identifier
     * @param instrumentId instrument identifier
     * @param side side encoded by Side constants
     * @param quantity parent order quantity
     * @return this reusable request
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
     *
     * @return this reusable request
     */
    public RiskCheckRequest clear() { return set(0, 0, 0, 0); }

    /**
     * Returns the engine-assigned parent order identifier.
     *
     * @return engine-assigned parent order identifier
     */
    public long parentOrderId() { return parentOrderId; }
    /**
     * Returns the instrument identifier.
     *
     * @return instrument identifier
     */
    public int instrumentId() { return instrumentId; }
    /**
     * Returns the side encoded by Side constants.
     *
     * @return side encoded by Side constants
     */
    public int side() { return side; }
    /**
     * Returns the parent order quantity.
     *
     * @return parent order quantity
     */
    public long quantity() { return quantity; }
}
