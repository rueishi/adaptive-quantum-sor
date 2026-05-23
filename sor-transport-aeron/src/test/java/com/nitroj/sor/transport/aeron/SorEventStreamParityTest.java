package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.SorEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SorEventStreamParityTest {
    @Test
    void aeronAndEmbeddedEmitSameRouteEventSubtype() throws Exception {
        final var engine = AeronTestSupport.engine(16);
        final var embedded = new ArrayBlockingQueue<Class<?>>(1);
        final var aeron = new ArrayList<Class<?>>();
        engine.registerListener(event -> {
            if (event instanceof SorEvent.RouteDecided) {
                embedded.offer(event.getClass());
            }
        });
        try (var server = new AeronSorServer("aeron:ipc", engine); var client = new AeronSorClient("aeron:ipc")) {
            client.registerListener(event -> aeron.add(event.getClass()));
            server.start();
            client.submitParentOrder(AeronTestSupport.request());
        }
        assertEquals(embedded.poll(2, TimeUnit.SECONDS), aeron.getFirst());
    }
}
