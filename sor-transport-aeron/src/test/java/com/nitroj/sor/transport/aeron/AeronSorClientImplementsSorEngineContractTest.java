package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.SorEngine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Aeron client preserves the public SorEngine contract.
 *
 * <p>Run with transport tests to ensure remote clients remain substitutable for embedded engines.</p>
 */
class AeronSorClientImplementsSorEngineContractTest {
    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"aeron:ipc", "aeron:udp?endpoint=localhost:40123"})
    void crossModeContractSubmitsAndReportsStatus(final String channel) {
        final SorEngine engine = AeronTestSupport.engine(16);
        try (var server = new AeronSorServer(channel, engine); var client = new AeronSorClient(channel)) {
            server.start();
            final long id = client.submitParentOrder(AeronTestSupport.request());
            assertTrue(client.isReady());
            assertTrue(client.getOrderStatus(id).isPresent());
        }
    }
}
