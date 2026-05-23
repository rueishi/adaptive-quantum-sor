package com.nitroj.sor.sim.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CliFlagsTest {
    @Test
    void parsesEveryDocumentedFlag() {
        final var options = SimulatorServerApplication.Options.parse(new String[]{
                "--config=server.yaml",
                "--transport=aeron",
                "--aeron-channel=aeron:ipc",
                "--http-control-port=9191",
                "--warmup-orders=5000",
                "--heap-size=4g",
                "--metrics-port=9292"
        });

        assertEquals(java.nio.file.Path.of("server.yaml"), options.config());
        assertEquals("aeron", options.transport());
        assertEquals("aeron:ipc", options.aeronChannel());
        assertEquals(9191, options.httpControlPort());
        assertEquals(5000, options.warmupOrders());
        assertEquals("4g", options.heapSize());
        assertEquals(9292, options.metricsPort());
    }
}
