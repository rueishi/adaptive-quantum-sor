package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.spi.Clock;

import java.util.concurrent.atomic.AtomicLong;

/** Deterministic simulator clock with explicit nanosecond advancement. */
public final class ManualClock implements Clock {
    private final AtomicLong now;

    public ManualClock() {
        this(0L);
    }

    public ManualClock(final long initialEpochNanos) {
        this.now = new AtomicLong(initialEpochNanos);
    }

    @Override
    public long nanoTime() {
        return now.get();
    }

    @Override
    public long epochNanos() {
        return now.get();
    }

    public long advanceNanos(final long nanos) {
        if (nanos < 0) {
            throw new IllegalArgumentException("nanos must be non-negative");
        }
        return now.addAndGet(nanos);
    }
}
