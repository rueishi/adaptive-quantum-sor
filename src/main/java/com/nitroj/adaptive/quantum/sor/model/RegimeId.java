package com.nitroj.adaptive.quantum.sor.model;

/**
 * Responsibility: define standard regime IDs used by Phase 1 routing inputs.
 *
 * <p>Role in system: the canonical route key includes a dense regime ID. These
 * constants provide named defaults for tests and future regime detection while
 * still allowing configured regime counts.</p>
 *
 * <p>Relationships: indexing utilities and route-policy structures use regime
 * IDs as array dimensions. Later feature aggregation code will map market
 * conditions onto these constants.</p>
 *
 * <p>Lifecycle: static constants are process-wide and immutable.</p>
 *
 * <p>Design intent: keeping IDs primitive avoids coupling early task cards to a
 * richer classification model that is out of scope until later phases.</p>
 */
public final class RegimeId {
    public static final int NORMAL = 0;
    public static final int VOLATILE = 1;
    public static final int THIN_BOOK = 2;
    public static final int STRESSED = 3;
    public static final int AUCTION = 4;

    private RegimeId() {
    }
}
