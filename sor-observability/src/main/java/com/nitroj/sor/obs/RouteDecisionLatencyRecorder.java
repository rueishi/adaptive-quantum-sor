package com.nitroj.sor.obs;

import java.util.Arrays;

/** Small deterministic histogram recorder for route decision latencies. */
public final class RouteDecisionLatencyRecorder {
    private final long[] values = new long[16_384];
    private int count;

    public synchronized void record(final long nanos) {
        if (count < values.length) {
            values[count++] = Math.max(0, nanos);
        }
    }

    public synchronized long count() {
        return count;
    }

    public synchronized long percentile(final double percentile) {
        if (count == 0) {
            return 0;
        }
        final long[] copy = Arrays.copyOf(values, count);
        Arrays.sort(copy);
        final int index = Math.min(copy.length - 1, (int) Math.ceil(percentile * copy.length) - 1);
        return copy[Math.max(0, index)];
    }
}
