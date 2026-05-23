package com.nitroj.sor.api;

/**
 * Responsibility: callback interface for engine-emitted events.
 *
 * <p>Role in system: integrators register listeners through
 * {@link SorEngine#registerListener(SorEventListener)} to receive route,
 * lifecycle, fill, reject, and policy events.</p>
 *
 * <p>Relationships: consumes {@link SorEvent}; registration lifecycle is
 * controlled by {@link Registration}.</p>
 *
 * <p>Lifecycle: listener instances are supplied by integrators and invoked by
 * the engine event-fanout thread.</p>
 *
 * <p>Design intent: callbacks are explicitly off the hot routing thread, so
 * listeners may allocate and block.</p>
 */
@FunctionalInterface
public interface SorEventListener {
    /**
     * Called on the engine event-fanout thread, never on the hot routing
     * thread. Control-plane callback, not hot-path; listener implementations may
     * allocate and block.
     *
     * @param event event emitted by the engine
     */
    void onEvent(SorEvent event);
}
