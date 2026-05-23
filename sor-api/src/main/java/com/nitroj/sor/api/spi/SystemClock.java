package com.nitroj.sor.api.spi;

/**
 * Responsibility: package-private system clock implementation.
 *
 * <p>Role in system: backs {@link Clock#systemNano()} without exposing an
 * implementation type in the public API.</p>
 *
 * <p>Relationships: implements {@link Clock}; later engine code receives it
 * through the builder.</p>
 *
 * <p>Lifecycle: singleton enum-free instance.</p>
 *
 * <p>Design intent: isolate direct `System` time calls to one auditable class.</p>
 */
final class SystemClock implements Clock {
    static final SystemClock INSTANCE = new SystemClock();

    private SystemClock() {
    }

    /**
     * Delegates to {@link System#nanoTime()}.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    @Override
    public long nanoTime() {
        return System.nanoTime();
    }

    /**
     * Converts {@link System#currentTimeMillis()} to epoch nanoseconds.
     *
     * <p>Hot-path method. Must not allocate. Must not block.</p>
     */
    @Override
    public long epochNanos() {
        return System.currentTimeMillis() * 1_000_000L;
    }
}
