package com.nitroj.adaptive.quantum.sor.scenario;

/** Route result for a parent order submitted with a live scenario run. */
public record ScenarioParentOrderResult(
        long parentOrderId,
        long filledQty,
        long remainingQty,
        int status,
        int childOrderCount,
        int routeAttempts,
        boolean timedOut,
        ScenarioChildFill[] fills
) {
    public ScenarioParentOrderResult {
        fills = fills == null ? new ScenarioChildFill[0] : fills.clone();
    }

    public ScenarioParentOrderResult(
            final long parentOrderId,
            final long filledQty,
            final long remainingQty,
            final int status,
            final int childOrderCount
    ) {
        this(parentOrderId, filledQty, remainingQty, status, childOrderCount, 0, false, new ScenarioChildFill[0]);
    }

    @Override
    public ScenarioChildFill[] fills() {
        return fills.clone();
    }

    public String toJson() {
        final String fillsJson = java.util.Arrays.stream(fills)
                .map(ScenarioChildFill::toJson)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return "{\"parentOrderId\":" + parentOrderId
                + ",\"filledQty\":" + filledQty
                + ",\"remainingQty\":" + remainingQty
                + ",\"status\":" + status
                + ",\"childOrderCount\":" + childOrderCount
                + ",\"routeAttempts\":" + routeAttempts
                + ",\"timedOut\":" + timedOut
                + ",\"fills\":" + fillsJson + "}";
    }
}
