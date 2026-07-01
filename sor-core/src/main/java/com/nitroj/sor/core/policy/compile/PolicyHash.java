package com.nitroj.sor.core.policy.compile;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Responsibility: compute deterministic policy hashes.
 *
 * <p>Role in system: compiled policies need stable identity for audit,
 * publication validation, and child-order stamping.</p>
 *
 * <p>Relationships: used by {@link DefaultPolicyCompiler} and later publisher
 * hash checks.</p>
 *
 * <p>Lifecycle: stateless utility called during compilation.</p>
 *
 * <p>Design intent: SHA-256 provides canonical bytes while the first eight
 * bytes form the compact 64-bit hash used by hot-path records.</p>
 */
public final class PolicyHash {
    private PolicyHash() {
    }

    /** Computes SHA-256 for canonical policy bytes. */
    public static byte[] sha256(final byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    /** Converts the first eight bytes of a SHA-256 hash into a non-zero long. */
    public static long hash64(final byte[] sha256) {
        if (sha256 == null || sha256.length < 8) {
            throw new IllegalArgumentException("sha256 must contain at least 8 bytes");
        }
        long value = 0L;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (sha256[i] & 0xFFL);
        }
        return value == 0L ? 1L : value;
    }
}
