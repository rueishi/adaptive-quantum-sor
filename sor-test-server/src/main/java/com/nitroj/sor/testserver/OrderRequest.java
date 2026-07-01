package com.nitroj.sor.testserver;

import com.nitroj.sor.core.model.OrderIntent;
import com.nitroj.sor.core.model.Side;

/**
 * Responsibility: represent and parse API parent-order requests.
 *
 * <p>Role in system: HTTP handlers validate JSON-like request bodies before
 * creating hot-path {@link OrderIntent} objects for notebook/demo submissions.
 * The request is intentionally small: instrument id, side, quantity, and
 * urgency id are enough to exercise the local routing path.</p>
 *
 * <p>Relationships: used by {@link NotebookScenarioHttpServer} for
 * {@code POST /orders}. It converts accepted control-plane input into core
 * {@link OrderIntent} values while reusing the core {@link Side} validation
 * rules.</p>
 *
 * <p>Lifecycle: created per POST /orders request.</p>
 *
 * <p>Design intent: keep the test-server API dependency-free. The parser is
 * deliberately narrow and should not be treated as a general JSON framework;
 * production integrations should prefer typed client APIs or the supported
 * transport/control modules.</p>
 */
public record OrderRequest(int instrumentId, int side, long quantity, int urgencyId) {
    public OrderRequest {
        if (instrumentId < 0 || !Side.isValid(side) || quantity <= 0 || urgencyId < 0) {
            throw new IllegalArgumentException("invalid order request");
        }
    }

    /** Parses a simple JSON object containing instrumentId, side, quantity, urgencyId. */
    public static OrderRequest parse(final String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("request body must not be blank");
        }
        return new OrderRequest(
                intValue(body, "instrumentId"),
                sideValue(body, "side"),
                longValue(body, "quantity"),
                intValue(body, "urgencyId")
        );
    }

    public OrderIntent toIntent(final long parentOrderId, final long nowNanos) {
        return new OrderIntent(parentOrderId, instrumentId, side, quantity, urgencyId, nowNanos);
    }

    private static int intValue(final String body, final String key) {
        return Math.toIntExact(longValue(body, key));
    }

    private static int sideValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing value for: " + key);
        }
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        if (start < body.length() && body.charAt(start) == '"') {
            final int end = body.indexOf('"', start + 1);
            if (end < 0) {
                throw new IllegalArgumentException("unterminated side value");
            }
            return Side.parse(body.substring(start + 1, end));
        }
        int end = start;
        while (end < body.length() && (body.charAt(end) == '-' || Character.isDigit(body.charAt(end)))) {
            end++;
        }
        if (end == start) {
            throw new IllegalArgumentException("side must be BUY or SELL");
        }
        return Side.parse(body.substring(start, end));
    }

    private static long longValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        if (colon < 0) {
            throw new IllegalArgumentException("missing value for: " + key);
        }
        int end = colon + 1;
        while (end < body.length() && Character.isWhitespace(body.charAt(end))) {
            end++;
        }
        int valueEnd = end;
        while (valueEnd < body.length() && (body.charAt(valueEnd) == '-' || Character.isDigit(body.charAt(valueEnd)))) {
            valueEnd++;
        }
        if (valueEnd == end) {
            throw new IllegalArgumentException("field must be numeric: " + key);
        }
        return Long.parseLong(body.substring(end, valueEnd));
    }
}
