package com.nitroj.sor.api;

/**
 * Responsibility: immutable public parent-order submission DTO.
 *
 * <p>Role in system: this is what an integrator passes to
 * {@link SorEngine#submitParentOrder(ParentOrderRequest)}. `sor-core`
 * translates it to its internal execution model.</p>
 *
 * <p>Relationships: validates primitive request fields using public API
 * constants such as {@link Side}.</p>
 *
 * <p>Lifecycle: built once, submitted once or reused by an integrator if they
 * deliberately preserve the same values.</p>
 *
 * <p>Design intent: reject invalid order intent at the API boundary with clear
 * messages before the hot routing path sees the request.</p>
 *
 * @param instrumentId non-negative instrument identifier
 * @param side public {@link Side} constant
 * @param quantity strictly positive parent quantity
 * @param urgencyId non-negative urgency bucket
 * @param clientOrderId integrator correlation identifier
 * @param arrivalEpochNanos zero for engine clock, otherwise non-negative epoch nanos
 */
public record ParentOrderRequest(
        int instrumentId,
        int side,
        long quantity,
        int urgencyId,
        long clientOrderId,
        long arrivalEpochNanos
) {
    /**
     * Validates the immutable parent-order request.
     */
    public ParentOrderRequest {
        if (instrumentId < 0) {
            throw new IllegalArgumentException("instrumentId must be non-negative");
        }
        if (!Side.isValid(side)) {
            throw new IllegalArgumentException("side must be Side.BUY or Side.SELL");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (urgencyId < 0) {
            throw new IllegalArgumentException("urgencyId must be non-negative");
        }
        if (arrivalEpochNanos < 0) {
            throw new IllegalArgumentException("arrivalEpochNanos must be non-negative");
        }
    }

    /**
     * Starts a builder for a validated immutable request.
     *
     * @return new builder with explicit defaults
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Responsibility: mutable construction helper for
     * {@link ParentOrderRequest}.
     *
     * <p>Role in system: lets integrators assemble an order in normal Java
     * style while centralizing validation in the final record constructor.</p>
     *
     * <p>Relationships: produces exactly one immutable DTO per
     * {@link #build()} call.</p>
     *
     * <p>Lifecycle: short-lived control-plane object, not retained by the
     * engine.</p>
     *
     * <p>Design intent: keep the public DTO immutable without forcing large
     * constructors into integrator code.</p>
     */
    public static final class Builder {
        private int instrumentId = -1;
        private int side;
        private long quantity;
        private int urgencyId;
        private long clientOrderId;
        private long arrivalEpochNanos;

        /**
         * Creates an empty request builder.
         */
        public Builder() {
        }

        /**
         * Sets the non-negative instrument identifier.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param id non-negative instrument identifier
         * @return this builder
         */
        public Builder instrumentId(final int id) {
            this.instrumentId = id;
            return this;
        }

        /**
         * Sets the side using {@link Side#BUY} or {@link Side#SELL}.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param side public side constant
         * @return this builder
         */
        public Builder side(final int side) {
            this.side = side;
            return this;
        }

        /**
         * Sets the strictly positive parent quantity.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param qty strictly positive parent quantity
         * @return this builder
         */
        public Builder quantity(final long qty) {
            this.quantity = qty;
            return this;
        }

        /**
         * Sets the non-negative urgency bucket.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param urgencyId non-negative urgency bucket
         * @return this builder
         */
        public Builder urgency(final int urgencyId) {
            this.urgencyId = urgencyId;
            return this;
        }

        /**
         * Sets the integrator-owned correlation identifier.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param id integrator-owned correlation identifier
         * @return this builder
         */
        public Builder clientOrderId(final long id) {
            this.clientOrderId = id;
            return this;
        }

        /**
         * Sets arrival epoch nanos; zero asks the engine to use its clock.
         *
         * <p>Control-plane method, not hot-path.</p>
         *
         * @param nanos arrival epoch nanoseconds, or zero for engine clock
         * @return this builder
         */
        public Builder arrivalEpochNanos(final long nanos) {
            this.arrivalEpochNanos = nanos;
            return this;
        }

        /**
         * Builds and validates the immutable request.
         *
         * <p>Control-plane method, not hot-path. Builders are integrator-side
         * construction helpers and may allocate the resulting DTO.</p>
         *
         * @return validated parent-order request
         */
        public ParentOrderRequest build() {
            return new ParentOrderRequest(instrumentId, side, quantity, urgencyId, clientOrderId, arrivalEpochNanos);
        }
    }
}
