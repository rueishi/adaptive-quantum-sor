package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorControlPlane;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorResetMode;
import com.nitroj.sor.api.SorResetRequest;
import com.nitroj.sor.api.SorResetSummary;
import com.nitroj.sor.api.SorStateSummary;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** JDK HTTP control plane for research and ops endpoints. */
public final class HttpControlPlaneServer {
    private final SorEngine engine;
    private final SorControlPlane controlPlane;
    private final Observability observability;
    private final HttpServer server;

    public HttpControlPlaneServer(final int port, final SorEngine engine, final Observability observability) throws IOException {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.controlPlane = engine instanceof SorControlPlane sorControlPlane ? sorControlPlane : null;
        this.observability = Objects.requireNonNull(observability, "observability must not be null");
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/healthz", this::healthz);
        server.createContext("/ready", this::ready);
        server.createContext("/metrics", this::metrics);
        server.createContext("/openapi.json", this::openapi);
        server.createContext("/orders", this::orders);
        server.createContext("/policy/current", this::policyCurrent);
        server.createContext("/control/state", this::stateSummary);
        server.createContext("/control/market-data", this::marketDataSnapshot);
        server.createContext("/control/reset", this::reset);
    }

    public void start() {
        server.start();
        System.out.println("HTTP control plane on :" + port() + " — research and ops only; order flow uses Aeron");
    }

    public void stop() {
        server.stop(0);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    private void healthz(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        send(exchange, 200, "{\"status\":\"alive\"}", "application/json");
    }

    private void ready(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        if (engine.isReady()) {
            send(exchange, 200, "{\"ready\":true}", "application/json");
        } else {
            send(exchange, 503, "{\"ready\":false}", "application/json");
        }
    }

    private void metrics(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        send(exchange, 200, observability.prometheusText(), "text/plain; version=0.0.4");
    }

    private void openapi(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        send(exchange, 200, """
                {"openapi":"3.1.0","info":{"title":"Adaptive Quantum SOR HTTP Control Plane","version":"0.9.0"},"paths":{"/healthz":{"get":{"responses":{"200":{"description":"alive"}}}},"/ready":{"get":{"responses":{"200":{"description":"ready"},"503":{"description":"not ready"}}}},"/metrics":{"get":{"responses":{"200":{"description":"Prometheus metrics"}}}},"/openapi.json":{"get":{"responses":{"200":{"description":"OpenAPI document"}}}},"/orders":{"post":{"responses":{"200":{"description":"submitted parent order"},"400":{"description":"invalid order"}}}},"/orders/{id}":{"get":{"responses":{"200":{"description":"order status"},"404":{"description":"unknown order"}}}},"/policy/current":{"get":{"responses":{"200":{"description":"active policy identity"}}}},"/control/state":{"get":{"responses":{"200":{"description":"engine state summary"},"501":{"description":"control plane unavailable"}}}},"/control/market-data":{"get":{"responses":{"200":{"description":"market data snapshot"},"501":{"description":"control plane unavailable"}}}},"/control/reset":{"post":{"responses":{"200":{"description":"reset summary"},"501":{"description":"control plane unavailable"}}}}}}
                """, "application/json");
    }

    private void policyCurrent(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        final PolicyHandle policy = engine.activePolicy();
        if (policy == null) {
            send(exchange, 200, "{\"policyVersion\":0,\"policyHash64\":0,\"hashSha256\":\"\",\"createdEpochNanos\":0,\"effectiveFromEpochNanos\":0}", "application/json");
            return;
        }
        send(exchange, 200, "{\"policyVersion\":" + policy.version()
                + ",\"policyHash64\":" + policy.hash64()
                + ",\"hashSha256\":" + quote(hex(policy.hashSha256()))
                + ",\"createdEpochNanos\":" + policy.createdEpochNanos()
                + ",\"effectiveFromEpochNanos\":" + policy.effectiveFromEpochNanos() + "}", "application/json");
    }

    private void stateSummary(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        if (controlPlane == null) {
            send(exchange, 501, "{\"error\":\"control plane unavailable\"}", "application/json");
            return;
        }
        final SorStateSummary summary = controlPlane.stateSummary();
        final SorResetSummary reset = summary.lastResetSummary();
        send(exchange, 200, "{\"activeParentOrderCount\":" + summary.activeParentOrderCount()
                + ",\"activeChildOrderCount\":" + summary.activeChildOrderCount()
                + ",\"pendingChildQuantity\":" + summary.pendingChildQuantity()
                + ",\"activePolicyVersion\":" + summary.activePolicyVersion()
                + ",\"lastResetMode\":" + quote(reset == null ? "" : reset.mode().name())
                + ",\"lastResetAccepted\":" + (reset != null && reset.accepted()) + "}", "application/json");
    }

    private void marketDataSnapshot(final HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        if (controlPlane == null) {
            send(exchange, 501, "{\"error\":\"control plane unavailable\"}", "application/json");
            return;
        }
        final var summary = controlPlane.marketDataSnapshot();
        send(exchange, 200, "{\"sequence\":" + summary.sequence()
                + ",\"checksum\":" + summary.checksum()
                + ",\"populatedCellCount\":" + summary.populatedCellCount()
                + ",\"lastUpdateEpochNanos\":" + summary.lastUpdateEpochNanos() + "}", "application/json");
    }

    private void reset(final HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, "method not allowed", "text/plain");
            return;
        }
        if (controlPlane == null) {
            send(exchange, 501, "{\"error\":\"control plane unavailable\"}", "application/json");
            return;
        }
        final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        final SorResetMode mode = SorResetMode.valueOf(stringField(body, "mode", SorResetMode.APPEND.name()));
        final SorResetSummary summary = controlPlane.reset(new SorResetRequest(
                mode,
                booleanField(body, "requireNoLiveOrders", true),
                stringField(body, "reason", "http control reset")));
        send(exchange, 200, "{\"mode\":" + quote(summary.mode().name())
                + ",\"accepted\":" + summary.accepted()
                + ",\"replaySafe\":" + summary.replaySafe()
                + ",\"message\":" + quote(summary.message()) + "}", "application/json");
    }

    private void orders(final HttpExchange exchange) throws IOException {
        try {
            final String path = exchange.getRequestURI().getPath();
            if ("POST".equals(exchange.getRequestMethod()) && "/orders".equals(path)) {
                final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                final ParentOrderRequest request = ParentOrderRequest.builder()
                        .instrumentId(intField(body, "instrumentId", 0))
                        .side(intField(body, "side", Side.BUY))
                        .quantity(longField(body, "quantity", 1))
                        .urgency(intField(body, "urgencyId", 0))
                        .clientOrderId(longField(body, "clientOrderId", 0))
                        .build();
                final long id = engine.submitParentOrder(request);
                send(exchange, 200, statusJson(engine.getOrderStatus(id).orElse(null), id, request.quantity()), "application/json");
                return;
            }
            if ("GET".equals(exchange.getRequestMethod()) && path.startsWith("/orders/")) {
                final long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
                final var status = engine.getOrderStatus(id);
                send(exchange, status.isPresent() ? 200 : 404, statusJson(status.orElse(null), id, 0), "application/json");
                return;
            }
            send(exchange, 405, "method not allowed", "text/plain");
        } catch (RuntimeException ex) {
            send(exchange, 400, ex.getMessage(), "text/plain");
        }
    }

    private static void send(final HttpExchange exchange, final int status, final String body, final String contentType) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String statusJson(final OrderStatus status, final long id, final long fallbackQuantity) {
        if (status == null) {
            return "{\"parentOrderId\":" + id
                    + ",\"filledQty\":0,\"remainingQty\":" + fallbackQuantity
                    + ",\"status\":0,\"childOrderCount\":0}";
        }
        return "{\"parentOrderId\":" + status.parentOrderId()
                + ",\"filledQty\":" + status.filledQuantity()
                + ",\"remainingQty\":" + status.remainingQuantity()
                + ",\"status\":" + status.status().ordinal()
                + ",\"childOrderCount\":0}";
    }

    private static String quote(final String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String hex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >>> 4) & 0xf, 16));
            builder.append(Character.forDigit(value & 0xf, 16));
        }
        return builder.toString();
    }

    private static int intField(final String json, final String name, final int defaultValue) {
        return (int) longField(json, name, defaultValue);
    }

    private static long longField(final String json, final String name, final long defaultValue) {
        final String marker = "\"" + name + "\":";
        final int start = json.indexOf(marker);
        if (start < 0) {
            return defaultValue;
        }
        int cursor = start + marker.length();
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        int end = cursor;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return Long.parseLong(json.substring(cursor, end));
    }

    private static boolean booleanField(final String json, final String name, final boolean defaultValue) {
        final String marker = "\"" + name + "\":";
        final int start = json.indexOf(marker);
        if (start < 0) {
            return defaultValue;
        }
        int cursor = start + marker.length();
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        return json.startsWith("true", cursor);
    }

    private static String stringField(final String json, final String name, final String defaultValue) {
        final String marker = "\"" + name + "\":";
        final int start = json.indexOf(marker);
        if (start < 0) {
            return defaultValue;
        }
        int cursor = start + marker.length();
        while (cursor < json.length() && Character.isWhitespace(json.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= json.length() || json.charAt(cursor) != '"') {
            return defaultValue;
        }
        final int valueStart = cursor + 1;
        final int valueEnd = json.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return defaultValue;
        }
        return json.substring(valueStart, valueEnd);
    }
}
