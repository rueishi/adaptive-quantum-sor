package com.nitroj.sor.testkit.scenario;

/**
 * Responsibility: validate a live scenario reset request.
 *
 * <p>Role in system: API and notebook clients use this request to explicitly
 * choose whether live state is purged, kept, isolated, or appended.</p>
 *
 * <p>Relationships: consumed by {@link ScenarioControlService} and converted
 * into a {@link ScenarioSpec} for scenario execution.</p>
 *
 * <p>Lifecycle: immutable per API request.</p>
 *
 * <p>Design intent: fail invalid live-control inputs before state mutation.</p>
 */
public record ScenarioResetRequest(String scenarioId, long seed, int ticks, ScenarioResetMode resetMode) {
    public ScenarioResetRequest {
        if (scenarioId == null || scenarioId.isBlank()) {
            throw new IllegalArgumentException("scenarioId must not be blank");
        }
        if (ticks <= 0) {
            throw new IllegalArgumentException("ticks must be positive");
        }
        if (resetMode == null) {
            throw new IllegalArgumentException("resetMode must not be null");
        }
    }

    public static ScenarioResetRequest parse(final String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("request body must not be blank");
        }
        return new ScenarioResetRequest(
                stringValue(body, "scenarioId"),
                longValue(body, "seed"),
                Math.toIntExact(longValue(body, "ticks")),
                ScenarioResetMode.parse(stringValue(body, "resetMode"))
        );
    }

    private static String stringValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        final int firstQuote = body.indexOf('"', colon + 1);
        final int secondQuote = body.indexOf('"', firstQuote + 1);
        if (colon < 0 || firstQuote < 0 || secondQuote < 0) {
            throw new IllegalArgumentException("field must be string: " + key);
        }
        return body.substring(firstQuote + 1, secondQuote);
    }

    private static long longValue(final String body, final String key) {
        final String quoted = "\"" + key + "\"";
        final int keyIndex = body.indexOf(quoted);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("missing field: " + key);
        }
        final int colon = body.indexOf(':', keyIndex + quoted.length());
        int start = colon + 1;
        while (start < body.length() && Character.isWhitespace(body.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < body.length() && (body.charAt(end) == '-' || Character.isDigit(body.charAt(end)))) {
            end++;
        }
        if (colon < 0 || end == start) {
            throw new IllegalArgumentException("field must be numeric: " + key);
        }
        return Long.parseLong(body.substring(start, end));
    }
}
