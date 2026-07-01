package com.nitroj.sor.http;

import com.nitroj.sor.api.MarketDataSnapshotSummary;
import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEventListener;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies explicit HTTP control-plane endpoints.
 *
 * <p>Role in system: proves out-of-process reset and diagnostics use
 * non-hot-path `/control/*` routes.</p>
 *
 * <p>Relationships: tests {@link HttpControlPlaneServer} against a tiny
 * {@link SorEngine}/{@link SorControlPlane} fake.</p>
 *
 * <p>Lifecycle: starts an ephemeral localhost server per test.</p>
 *
 * <p>Design intent: keep transport boundary behavior independent from
 * `sor-core` implementation details.</p>
 */
class HttpControlPlaneServerTest {
    @Test
    void exposesExplicitControlPlaneEndpoints() throws Exception {
        final FakeEngine engine = new FakeEngine();
        final HttpControlPlaneServer server = new HttpControlPlaneServer(0, engine, Observability.noop());
        server.start();
        try {
            final HttpClient client = HttpClient.newHttpClient();

            final String openapi = get(client, server, "/openapi.json");
            assertTrue(openapi.contains("/control/state"));
            assertTrue(openapi.contains("/control/market-data"));
            assertTrue(openapi.contains("/control/reset"));
            assertTrue(openapi.contains("/policy/current"));

            final String reset = post(client, server, "/control/reset",
                    "{\"mode\":\"CLEAR_MARKET_DATA\",\"requireNoLiveOrders\":false,\"reason\":\"test\"}");
            assertTrue(reset.contains("\"accepted\":true"));

            final String state = get(client, server, "/control/state");
            assertTrue(state.contains("\"activeParentOrderCount\":3"));
            assertTrue(state.contains("\"lastResetMode\":\"CLEAR_MARKET_DATA\""));

            final String market = get(client, server, "/control/market-data");
            assertTrue(market.contains("\"sequence\":7"));
            assertTrue(market.contains("\"populatedCellCount\":2"));

            final String policy = get(client, server, "/policy/current");
            assertTrue(policy.contains("\"policyVersion\":6"));
            assertTrue(policy.contains("\"hashSha256\":\"010203\""));
        } finally {
            server.stop();
        }
    }

    private static String get(final HttpClient client, final HttpControlPlaneServer server,
                              final String path) throws Exception {
        final HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + server.port() + path))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }

    private static String post(final HttpClient client, final HttpControlPlaneServer server,
                               final String path, final String body) throws Exception {
        final HttpResponse<String> response = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + server.port() + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }

    private static final class FakeEngine implements SorEngine, SorControlPlane {
        private SorResetSummary lastReset;

        @Override public void warmup(final int syntheticOrderCount) {}
        @Override public boolean isReady() { return true; }
        @Override public long submitParentOrder(final ParentOrderRequest request) { return 1; }
        @Override public void cancelParentOrder(final long parentOrderId) {}
        @Override public Optional<OrderStatus> getOrderStatus(final long parentOrderId) { return Optional.empty(); }
        @Override public Registration registerListener(final SorEventListener listener) { return Registration.of(() -> {}); }
        @Override public PolicyHandle activePolicy() { return new FakePolicyHandle(); }
        @Override public void close() {}

        @Override
        public SorResetSummary reset(final SorResetRequest request) {
            lastReset = new SorResetSummary(request.mode(), true, request.mode() != SorResetMode.APPEND,
                    "accepted", new String[]{"marketBook"}, new String[]{"activePolicy"}, new String[]{});
            return lastReset;
        }

        @Override
        public SorStateSummary stateSummary() {
            return new SorStateSummary(3, 4, 5, 6, lastReset);
        }

        @Override
        public MarketDataSnapshotSummary marketDataSnapshot() {
            return new MarketDataSnapshotSummary(7, 8, 2, 9);
        }
    }

    private record FakePolicyHandle() implements PolicyHandle {
        @Override public long version() { return 6; }
        @Override public long hash64() { return 7; }
        @Override public byte[] hashSha256() { return new byte[]{1, 2, 3}; }
        @Override public long createdEpochNanos() { return 8; }
        @Override public long effectiveFromEpochNanos() { return 9; }
    }
}
