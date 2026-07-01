package com.nitroj.sor.codec.v1;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Provides reusable protocol fixtures for codec compatibility tests.
 *
 * <p>Use it from codec tests to keep round-trip, golden, and extension scenarios consistent.</p>
 */
final class ProtocolFixtures {
    private ProtocolFixtures() {}

    static Stream<SorProtocolMessage> messages() {
        return Arrays.stream(SorMessageType.values())
                .map(type -> new SorProtocolMessage(type,
                        new long[] {type.templateId(), type.templateId() * 10L, type.templateId() * 100L},
                        new int[] {type.templateId(), type.templateId() + 1},
                        type.name()));
    }

    static String hex(final byte[] bytes) {
        final StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            out.append(String.format("%02x", b));
        }
        return out.toString();
    }

    static byte[] unhex(final String hex) {
        final String clean = hex.replaceAll("\\s+", "");
        final byte[] bytes = new byte[clean.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }
}
