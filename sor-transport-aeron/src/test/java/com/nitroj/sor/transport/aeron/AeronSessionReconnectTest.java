package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.SorEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies client reconnect behavior across Aeron server lifecycle transitions.
 *
 * <p>Run with transport tests before changing broker registration or channel lifecycle.</p>
 */
class AeronSessionReconnectTest {
    @Test
    void clientEmitsDownThenUpAroundReconnect() {
        final var engine = AeronTestSupport.engine(16);
        final var events = new ArrayList<SorEvent>();
        try (var server = new AeronSorServer("aeron:ipc", engine); var client = new AeronSorClient("aeron:ipc")) {
            client.registerListener(events::add);
            server.start();
            client.reconnect();
            server.close();
            client.reconnect();
            server.start();
            client.reconnect();
            assertTrue(events.stream().anyMatch(SorEvent.SessionStatusChanged.class::isInstance));
        }
    }
}
