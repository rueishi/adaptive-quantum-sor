package com.nitroj.adaptive.quantum.sor.scenario;

/** Wrapper request for running a live scenario after applying a reset mode. */
public record ScenarioRunRequest(
        ScenarioResetRequest resetRequest,
        ScenarioParentOrderIntent[] parentOrders,
        int maxRouteAttempts,
        long routeTimeoutMillis
) {
    public ScenarioRunRequest {
        if (resetRequest == null) {
            throw new IllegalArgumentException("resetRequest must not be null");
        }
        parentOrders = parentOrders == null ? new ScenarioParentOrderIntent[0] : parentOrders.clone();
        if (maxRouteAttempts <= 0) {
            throw new IllegalArgumentException("maxRouteAttempts must be positive");
        }
        if (routeTimeoutMillis <= 0) {
            throw new IllegalArgumentException("routeTimeoutMillis must be positive");
        }
    }

    public ScenarioRunRequest(final ScenarioResetRequest resetRequest, final ScenarioParentOrderIntent[] parentOrders) {
        this(resetRequest, parentOrders, 16, 1_000L);
    }

    public ScenarioRunRequest(final ScenarioResetRequest resetRequest) {
        this(resetRequest, new ScenarioParentOrderIntent[0], 16, 1_000L);
    }

    public static ScenarioRunRequest parse(final String body) {
        return new ScenarioRunRequest(
                ScenarioResetRequest.parse(body),
                parseParentOrders(body),
                Math.toIntExact(optionalLongValue(body, "maxRouteAttempts", 16L)),
                optionalLongValue(body, "routeTimeoutMillis", 1_000L)
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
                    intValue(object, "side"),
                    longValue(object, "quantity"),
                    intValue(object, "urgencyId")
            ));
            cursor = objectEnd + 1;
        }
        return orders.toArray(ScenarioParentOrderIntent[]::new);
    }

    private static int intValue(final String body, final String key) {
        return Math.toIntExact(longValue(body, key));
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
}
