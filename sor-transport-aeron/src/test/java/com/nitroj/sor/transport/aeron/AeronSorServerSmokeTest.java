package com.nitroj.sor.transport.aeron;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Smoke-tests Aeron server startup, submission, and shutdown behavior.
 *
 * <p>Run with transport tests after server lifecycle or media-driver changes.</p>
 */
class AeronSorServerSmokeTest {
    @Test
    void serverAcceptsSubmitParentOrderOverIpc() {
        final var engine = AeronTestSupport.engine(8);
        try (var server = new AeronSorServer("aeron:ipc", engine); var client = new AeronSorClient("aeron:ipc")) {
            server.start();
            assertEquals(1, client.submitParentOrder(AeronTestSupport.request()));
        }
    }
}
