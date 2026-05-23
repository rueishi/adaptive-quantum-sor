package com.nitroj.sor.api;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Responsibility: idempotent handle returned when an event listener is
 * registered.
 *
 * <p>Role in system: callers close the handle to unregister listener state
 * without knowing the engine's listener collection implementation.</p>
 *
 * <p>Relationships: produced by {@link SorEngine#registerListener}; controls a
 * {@link SorEventListener}'s lifecycle.</p>
 *
 * <p>Lifecycle: created during listener registration and closed zero or more
 * times by the caller.</p>
 *
 * <p>Design intent: make deregistration safe to call from cleanup paths where
 * duplicate close calls are common.</p>
 */
public final class Registration implements AutoCloseable {
    private final Runnable onClose;
    private final AtomicBoolean closed = new AtomicBoolean();

    private Registration(final Runnable onClose) {
        this.onClose = Objects.requireNonNull(onClose, "onClose must not be null");
    }

    /**
     * Creates a registration handle that runs the supplied action once.
     *
     * <p>Control-plane method, not hot-path. This helper may allocate the
     * handle because listener registration is not part of routing.</p>
     *
     * @param onClose deregistration action
     * @return registration handle
     */
    public static Registration of(final Runnable onClose) {
        return new Registration(onClose);
    }

    /**
     * Unregisters once. Repeated calls are accepted and ignored.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            onClose.run();
        }
    }
}
