package com.nitroj.sor.core.abi;

import com.nitroj.sor.core.policy.HotRouteBook;

import java.nio.ByteBuffer;

/**
 * Responsibility: shared deterministic fixtures for ABI v1 tests.
 *
 * <p>Role in system: every ABI test must exercise the same known route-book
 * shape used to generate the golden binary.</p>
 *
 * <p>Relationships: constructs `HotRouteBook` from the legacy policy package
 * and serializes it through {@link HotRouteBookAbiV1Writer}.</p>
 *
 * <p>Lifecycle: test-only static helpers.</p>
 *
 * <p>Design intent: centralize fixture bytes so golden, round-trip, and
 * corruption tests cannot drift apart.</p>
 */
final class HotRouteBookAbiTestFixtures {
    private HotRouteBookAbiTestFixtures() {
    }

    /**
     * Creates the deterministic 2 instruments x 3 venues x 1 regime x 1
     * urgency route book required by P8-05.
     */
    static HotRouteBook routeBook() {
        final int[] offsets = {0, 3, 6};
        final short[] venues = {0, 1, 2, 2, 1, 0};
        return new HotRouteBook(2, 1, 1, offsets, venues, new short[6],
                ints(1000, 2000, 7000, 4000, 3000, 3000),
                ints(10, 20, 30, 40, 50, 60),
                ints(1, 2, 3, 4, 5, 6),
                ints(9000, 8000, 7000, 6000, 5000, 4000),
                ints(7, 8, 9, 10, 11, 12),
                ints(9900, 9800, 9700, 9600, 9500, 9400),
                ints(1, 1, 2, 2, 3, 3),
                ints(15, 16, 17, 18, 19, 20),
                ints(21, 22, 23, 24, 25, 26),
                longs(10, 20, 30, 40, 50, 60),
                longs(100, 200, 300, 400, 500, 600),
                longs(1000, 2000, 3000, 4000, 5000, 6000),
                ints(2500, 2500, 2500, 2000, 2000, 2000));
    }

    /**
     * Serializes the shared fixture.
     */
    static byte[] serialized() {
        final HotRouteBook book = routeBook();
        final ByteBuffer buffer = ByteBuffer.allocate(HotRouteBookAbiV1Writer.requiredBytes(book));
        final int bytes = HotRouteBookAbiV1Writer.write(book, 42, 1234, 5678, buffer);
        final byte[] result = new byte[bytes];
        buffer.get(result);
        return result;
    }

    private static int[] ints(final int... values) {
        return values;
    }

    private static long[] longs(final long... values) {
        return values;
    }
}
