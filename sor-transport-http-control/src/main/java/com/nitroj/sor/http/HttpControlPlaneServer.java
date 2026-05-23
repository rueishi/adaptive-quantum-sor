package com.nitroj.sor.http;

import com.nitroj.sor.api.Observability;
import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.api.SorEngine;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** JDK HTTP control plane for research and ops endpoints. */
public final class HttpControlPlaneServer {
    private final SorEngine engine;
    private final Observability observability;
    private final HttpServer server;

    public HttpControlPlaneServer(final int port, final SorEngine engine, final Observability observability) throws IOException {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.observability = Objects.requireNonNull(observability, "observability must not be null");
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/healthz", this::healthz);
        server.createContext("/ready", this::ready);
        server.createContext("/metrics", this::metrics);
        server.createContext("/openapi.json", this::openapi);
        server.createContext("/orders", this::orders);
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
        send(exchange, 200, "{\"status\":\"alive\"}", "application/json");
    }

    private void ready(final HttpExchange exchange) throws IOException {
        if (engine.isReady()) {
            send(exchange, 200, "{\"ready\":true}", "application/json");
        } else {
            send(exchange, 503, "{\"ready\":false}", "application/json");
        }
    }

    private void metrics(final HttpExchange exchange) throws IOException {
        send(exchange, 200, observability.prometheusText(), "text/plain; version=0.0.4");
    }

    private void openapi(final HttpExchange exchange) throws IOException {
        send(exchange, 200, """
                {"openapi":"3.1.0","info":{"title":"Adaptive Quantum SOR HTTP Control Plane","version":"0.8.0"},"paths":{"/healthz":{"get":{"responses":{"200":{"description":"alive"}}}},"/ready":{"get":{"responses":{"200":{"description":"ready"},"503":{"description":"not ready"}}}},"/metrics":{"get":{"responses":{"200":{"description":"Prometheus metrics"}}}},"/openapi.json":{"get":{"responses":{"200":{"description":"OpenAPI document"}}}}}}
                """, "application/json");
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
}
