package com.nitroj.sor.api;

import java.util.Optional;

/**
 * Responsibility: public engine contract. The single artifact embedders and
 * out-of-process clients code against.
 *
 * <p>Role in system: implemented by `sor-core` for embedded mode and by the
 * future Aeron client for out-of-process mode. Integrators do not distinguish
 * between those implementations.</p>
 *
 * <p>Relationships: accepts {@link ParentOrderRequest}, emits {@link SorEvent}
 * through {@link SorEventListener}, and exposes opaque {@link PolicyHandle}
 * state.</p>
 *
 * <p>Lifecycle: created via {@link SorEngineBuilder}. {@link #warmup(int)}
 * must complete before production submissions. {@link #close()} drains and
 * releases implementation resources.</p>
 *
 * <p>Design intent: small surface, explicit warmup, no future-style hot-path
 * acknowledgement. Order acknowledgement flows through listeners.</p>
 */
public interface SorEngine extends AutoCloseable {
    /**
     * Runs synthetic parent orders through the routing path to drive JIT
     * compilation to steady-state code shape.
     *
     * <p>Control-plane method, not hot-path. Implementations may allocate and
     * block during warmup.</p>
     *
     * @param syntheticOrderCount typically 10,000 to 100,000
     */
    void warmup(int syntheticOrderCount);

    /**
     * Reports whether warmup has completed.
     *
     * <p>Control-plane method, not hot-path. Must return promptly and must not
     * perform routing work.</p>
     *
     * @return true after {@link #warmup(int)} has completed
     */
    boolean isReady();

    /**
     * Submits a parent order and returns the generated parent order ID
     * immediately. Acknowledgement, routing, and fills arrive asynchronously via
     * registered listeners.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Must return within
     * 1 microsecond on the integrator thread, dominated by ring-buffer offer.</p>
     *
     * @param request validated parent order request
     * @return positive engine-assigned parent order ID
     * @throws BackpressureException if the parent order ring is full
     */
    long submitParentOrder(ParentOrderRequest request);

    /**
     * Idempotently cancels a parent order by ID.
     *
     * <p>Hot-path method. Must not allocate. Must not block. Unknown or already
     * complete orders return silently.</p>
     *
     * @param parentOrderId engine-assigned parent order ID
     */
    void cancelParentOrder(long parentOrderId);

    /**
     * Reads the latest known status of a parent order.
     *
     * <p>Control-plane method, not hot-path. May briefly contend on a read lock;
     * callers must not poll this per route.</p>
     *
     * @param parentOrderId engine-assigned parent order ID
     * @return status if known
     */
    Optional<OrderStatus> getOrderStatus(long parentOrderId);

    /**
     * Registers a listener for engine events.
     *
     * <p>Control-plane method, not hot-path. Listener callbacks run on the
     * event-fanout thread, never on the hot routing thread.</p>
     *
     * @param listener listener to register
     * @return handle whose close method unregisters the listener
     */
    Registration registerListener(SorEventListener listener);

    /**
     * Returns an opaque handle identifying the active policy.
     *
     * <p>Control-plane method, not hot-path. Integrators may inspect identity
     * fields but cannot reach internal policy structures.</p>
     *
     * @return active policy handle
     */
    PolicyHandle activePolicy();

    /**
     * Initiates graceful shutdown. Drains in-flight orders up to the configured
     * timeout, flushes persistence, stops threads, and releases resources.
     *
     * <p>Control-plane method, not hot-path. May block while draining.</p>
     */
    @Override
    void close();
}
