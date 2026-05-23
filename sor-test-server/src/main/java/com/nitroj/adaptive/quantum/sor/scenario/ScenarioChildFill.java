package com.nitroj.adaptive.quantum.sor.scenario;

/** Venue-level fill evidence for a live scenario parent order. */
public record ScenarioChildFill(
        long childOrderId,
        long parentOrderId,
        int routeAttempt,
        int instrumentId,
        String instrumentSymbol,
        int venueId,
        String venueName,
        int side,
        String sideName,
        long childQty,
        long filledQty,
        long priceTicks,
        long notionalTicks,
        long policyVersion,
        long policyHash64
) {
    public String toJson() {
        return "{\"childOrderId\":" + childOrderId
                + ",\"parentOrderId\":" + parentOrderId
                + ",\"routeAttempt\":" + routeAttempt
                + ",\"instrumentId\":" + instrumentId
                + ",\"instrumentSymbol\":\"" + instrumentSymbol + "\""
                + ",\"venueId\":" + venueId
                + ",\"venueName\":\"" + venueName + "\""
                + ",\"side\":" + side
                + ",\"sideName\":\"" + sideName + "\""
                + ",\"childQty\":" + childQty
                + ",\"filledQty\":" + filledQty
                + ",\"priceTicks\":" + priceTicks
                + ",\"notionalTicks\":" + notionalTicks
                + ",\"policyVersion\":" + policyVersion
                + ",\"policyHash64\":" + policyHash64 + "}";
    }
}
