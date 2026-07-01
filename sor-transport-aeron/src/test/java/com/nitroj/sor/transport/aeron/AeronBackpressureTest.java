package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.BackpressureException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies Aeron transport behavior when the engine or broker cannot accept work.
 *
 * <p>Run with transport tests before changing non-blocking submission or retry semantics.</p>
 */
class AeronBackpressureTest {
    @Test
    void clientSeesEmbeddedBackpressureSemantics() {
        final var engine = AeronTestSupport.engine(1);
        try (var server = new AeronSorServer("aeron:ipc", engine); var client = new AeronSorClient("aeron:ipc")) {
            server.start();
            client.submitParentOrder(AeronTestSupport.request());
            assertThrows(BackpressureException.class, () -> client.submitParentOrder(AeronTestSupport.request()));
        }
    }
}
