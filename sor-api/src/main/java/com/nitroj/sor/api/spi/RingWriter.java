package com.nitroj.sor.api.spi;

/**
 * Responsibility: non-blocking writer for outbound child-order rings.
 *
 * <p>Role in system: venue adapters expose one writer per venue so the engine
 * can hand off child orders without direct venue calls on the hot path.</p>
 *
 * <p>Relationships: accepts {@link ChildOrderRef} carriers from the engine.</p>
 *
 * <p>Lifecycle: returned by {@link VenueAdapter#childOrderRingWriter(int)} and
 * used repeatedly after warmup.</p>
 *
 * <p>Design intent: boolean backpressure is explicit and allocation-free.</p>
 */
@FunctionalInterface
public interface RingWriter {
    /**
     * Offers a child order to the ring.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Returns false
     * instead of throwing for expected full-ring backpressure.</p>
     *
     * @param childOrder child order carrier
     * @return true if accepted, false if full
     */
    boolean offer(ChildOrderRef childOrder);
}
