package com.nitroj.sor.testkit.scenario;

/** Wrapper request for running a live scenario after applying a reset mode. */
public record ScenarioRunRequest(
        ScenarioResetRequest resetRequest,
        ScenarioParentOrderIntent[] parentOrders,
        int maxRouteAttempts,
        long routeTimeoutMillis,
        boolean simulatorGeneratedOrders
) {
    public ScenarioRunRequest {
        if (resetRequest == null) {
            throw new IllegalArgumentException("resetRequest must not be null");
        }
        parentOrders = parentOrders == null ? new ScenarioParentOrderIntent[0] : parentOrders.clone();
        if (parentOrders.length == 0 && !simulatorGeneratedOrders) {
            throw new IllegalArgumentException("parentOrders must not be empty unless simulatorGeneratedOrders is true");
        }
        for (ScenarioParentOrderIntent parentOrder : parentOrders) {
            if (parentOrder.atTick() >= resetRequest.ticks()) {
                throw new IllegalArgumentException("parent order atTick must be within scenario ticks");
            }
        }
        if (maxRouteAttempts <= 0) {
            throw new IllegalArgumentException("maxRouteAttempts must be positive");
        }
        if (routeTimeoutMillis <= 0) {
            throw new IllegalArgumentException("routeTimeoutMillis must be positive");
        }
    }

    public ScenarioRunRequest(final ScenarioResetRequest resetRequest, final ScenarioParentOrderIntent[] parentOrders) {
        this(resetRequest, parentOrders, 16, 1_000L, parentOrders == null || parentOrders.length == 0);
    }

    public ScenarioRunRequest(final ScenarioResetRequest resetRequest) {
        this(resetRequest, new ScenarioParentOrderIntent[0], 16, 1_000L, true);
    }

    public static ScenarioRunRequest parse(final String body) {
        return new ScenarioRunRequest(
                ScenarioResetRequest.parse(body),
                parseParentOrders(body),
                Math.toIntExact(optionalLongValue(body, "maxRouteAttempts", 16L)),
                optionalLongValue(body, "routeTimeoutMillis", 1_000L),
                optionalBooleanValue(body, "simulatorGeneratedOrders", false)
        );
    }

    @Override
    public ScenarioParentOrderIntent[] parentOrders() {
        return parentOrders.clone();
    }

    private static ScenarioParentOrderIntent[] parseParentOrders(final String body) {
        final String key = "\"parentOrders\"";
        final int keyIndex = body.indexOf(key);
        if (keyIndex < 0) {
            return new ScenarioParentOrderIntent[0];
        }
        final int arrayStart = body.indexOf('[', keyIndex + key.length());
        final int arrayEnd = body.indexOf(']', arrayStart + 1);
        if (arrayStart < 0 || arrayEnd < 0) {
            throw new IllegalArgumentException("parentOrders must be an array");
        }
        final String array = body.substring(arrayStart + 1, arrayEnd);
        if (array.isBlank()) {
            return new ScenarioParentOrderIntent[0];
        }
        final java.util.List<ScenarioParentOrderIntent> orders = new java.util.ArrayList<>();
        int cursor = 0;
        while (cursor < array.length()) {
            final int objectStart = array.indexOf('{', cursor);
            if (objectStart < 0) {
                break;
            }
            final int objectEnd = array.indexOf('}', objectStart + 1);
            if (objectEnd < 0) {
                throw new IllegalArgumentException("parentOrders contains an unterminated object");
            }
            final String object = array.substring(objectStart, objectEnd + 1);
            orders.add(new ScenarioParentOrderIntent(
                    intValue(object, "instrumentId"),
                    sideValue(object, "side"),
                    longValue(object, "quantity"),
                    intValue(object, "urgencyId"),
                    Math.toIntExact(optionalLongValue(object, "atTick", 0L)),
                    ScenarioParentOrderSubmitMode.parse(optionalStringValue(object, "submitMode", "SIMULATED")),
                    optionalStringValue(object, "clientOrderRef", "")
            ));
            cursor = objectEnd + 1;
        }
        return orders.toArray(ScenarioParentOrderIntent[]::new);
    }

    private static int intValue(final String body, final String key) {
        return Math.toIntExact(longValue(body, key));
    }

    private static int sideValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing parent order field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing parent order value for: " + key);
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (start < body.length() && body.charAt(start) == '"') {
            final int end = body.indexOf('"', start + 1);
            if (end < 0) {
                throw new IllegalArgumentException("unterminated parent order side value");
            }
            return com.nitroj.sor.core.model.Side.parse(body.substring(start + 1, end));
        }
        int end = start;
        while (end < body.length() && (body.charAt(end) == '-' || Character.isDigit(body.charAt(end)))) {
            end++;
        }
        if (end == start) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        return com.nitroj.sor.core.model.Side.parse(body.substring(start, end));
    }

    private static long longValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing parent order field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing parent order value for: " + key);
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < body.length() && (body.charAt(end) == '-' || Character.isDigit(body.charAt(end)))) {
            end++;
        }
        if (end == start) {
            throw new IllegalArgumentException("parent order field must be numeric: " + key);
        }
        return Long.parseLong(body.substring(start, end));
    }

    private static long optionalLongValue(final String body, final String key, final long defaultValue) {
        final String quoted = "\"" + key + "\"";
        if (body.indexOf(quoted) < 0) {
            return defaultValue;
        }
        return longValue(body, key);
    }

    private static boolean optionalBooleanValue(final String body, final String key, final boolean defaultValue) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            return defaultValue;
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing boolean value for: " + key);
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (body.startsWith("true", start)) {
            return true;
        }
        if (body.startsWith("false", start)) {
            return false;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }

    private static String optionalStringValue(final String body, final String key, final String defaultValue) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            return defaultValue;
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing string value for: " + key);
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (start >= body.length() || body.charAt(start) != '"') {
            throw new IllegalArgumentException(key + " must be a string");
        }
        final int end = body.indexOf('"', start + 1);
        if (end < 0) {
            throw new IllegalArgumentException("unterminated string value for: " + key);
        }
        return body.substring(start + 1, end);
    }
}
