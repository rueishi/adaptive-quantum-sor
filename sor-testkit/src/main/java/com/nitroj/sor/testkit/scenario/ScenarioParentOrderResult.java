package com.nitroj.sor.testkit.scenario;

/** Route result for a parent order submitted with a live scenario run. */
public record ScenarioParentOrderResult(
        String scenarioId,
        long parentOrderId,
        int atTick,
        ScenarioParentOrderSubmitMode submitMode,
        String clientOrderRef,
        long filledQty,
        long remainingQty,
        int status,
        int childOrderCount,
        int routeAttempts,
        boolean timedOut,
        ScenarioChildFill[] fills
) {
    public ScenarioParentOrderResult {
        scenarioId = scenarioId == null ? "" : scenarioId;
        if (submitMode == null) {
            throw new IllegalArgumentException("submitMode must not be null");
        }
        clientOrderRef = clientOrderRef == null ? "" : clientOrderRef;
        fills = fills == null ? new ScenarioChildFill[0] : fills.clone();
    }

    public ScenarioParentOrderResult(
            final long parentOrderId,
            final long filledQty,
            final long remainingQty,
            final int status,
            final int childOrderCount
    ) {
        this("", parentOrderId, 0, ScenarioParentOrderSubmitMode.SIMULATED, "", filledQty, remainingQty,
                status, childOrderCount, 0, false, new ScenarioChildFill[0]);
    }

    @Override
    public ScenarioChildFill[] fills() {
        return fills.clone();
    }

    public String toJson() {
        final String fillsJson = java.util.Arrays.stream(fills)
                .map(ScenarioChildFill::toJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"scenarioId\":\"" + escape(scenarioId) + "\""
                + ",\"parentOrderId\":" + parentOrderId
                + ",\"atTick\":" + atTick
                + ",\"submitMode\":\"" + submitMode + "\""
                + ",\"clientOrderRef\":\"" + escape(clientOrderRef) + "\""
                + ",\"filledQty\":" + filledQty
                + ",\"remainingQty\":" + remainingQty
                + ",\"status\":" + status
                + ",\"childOrderCount\":" + childOrderCount
                + ",\"routeAttempts\":" + routeAttempts
                + ",\"timedOut\":" + timedOut
                + ",\"fills\":" + fillsJson + "}";
    }

    private static String escape(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
