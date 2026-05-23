package com.nitroj.sor.api.spi;

/**
 * Responsibility: mutable reusable output for synchronous risk checks.
 *
 * <p>Role in system: risk providers populate this object instead of allocating
 * a result on the routing path.</p>
 *
 * <p>Relationships: output parameter for
 * {@link RiskProvider#check(RiskCheckRequest, RiskDecision)}.</p>
 *
 * <p>Lifecycle: owned and reused by the engine.</p>
 *
 * <p>Design intent: make allow/reject explicit while preserving zero allocation.</p>
 */
public final class RiskDecision {
    private boolean allowed;
    private int reasonCode;

    /**
     * Marks the decision as allowed.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public RiskDecision allow() {
        this.allowed = true;
        this.reasonCode = 0;
        return this;
    }

    /**
     * Marks the decision as rejected with a reason code.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public RiskDecision reject(final int reasonCode) {
        this.allowed = false;
        this.reasonCode = reasonCode;
        return this;
    }

    /**
     * Clears to a rejecting default.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    public RiskDecision clear() {
        return reject(0);
    }

    public boolean allowed() { return allowed; }
    public int reasonCode() { return reasonCode; }
}
