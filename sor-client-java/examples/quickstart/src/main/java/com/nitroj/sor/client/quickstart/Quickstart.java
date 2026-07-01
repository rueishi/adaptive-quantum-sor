package com.nitroj.sor.client.quickstart;

import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.client.AeronSorClient;
import com.nitroj.sor.client.SorClientConfig;

/**
 * Demonstrates a minimal Java SDK quickstart for connecting to SOR and submitting an order.
 *
 * <p>Run it from the example project or read it as the first integration sample for Java clients.</p>
 */
public final class Quickstart {
    private Quickstart() {
    }

    public static void main(final String[] args) {
        try (AeronSorClient client = AeronSorClient.connect("aeron:ipc", SorClientConfig.defaults())) {
            client.submit(ParentOrderRequest.builder()
                    .instrumentId(0)
                    .side(Side.BUY)
                    .quantity(1)
                    .urgency(0)
                    .clientOrderId(42)
                    .build());
        } catch (IllegalStateException unavailable) {
            System.out.println("Start sor-test-server with Aeron IPC before running the quickstart.");
        }
    }
}
