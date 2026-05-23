package com.nitroj.sor.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the active policy handle is populated after construction/warmup. */
class ActivePolicyReflectsPublicationTest {
    @Test
    void activePolicyHasVersionAndDefensiveHash() {
        final var engine = EngineTestSupport.engine();
        engine.warmup(1);
        final var handle = engine.activePolicy();
        assertEquals(1, handle.version());
        final byte[] hash = handle.hashSha256();
        hash[0] ^= 1;
        assertNotEquals(hash[0], handle.hashSha256()[0]);
    }
}
