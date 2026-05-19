package com.nitroj.adaptive.quantum.sor.policy.validation;

import com.nitroj.adaptive.quantum.sor.policy.HotRouteBook;
import com.nitroj.adaptive.quantum.sor.policy.SorPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: validate Phase 1 compiled policy artifacts.
 *
 * <p>Role in system: protects publication from malformed route offsets, invalid
 * venue IDs, missing hashes, and empty route lists.</p>
 *
 * <p>Relationships: reads {@link SorPolicy} and {@link HotRouteBook}; its
 * report is consumed by publication gates.</p>
 *
 * <p>Lifecycle: stateless object reused for every policy validation attempt.</p>
 *
 * <p>Design intent: duplicate critical structural checks at the publication
 * boundary, even though constructors also validate many invariants.</p>
 */
public final class DefaultPolicyValidator implements PolicyValidator {
    private final int venueCount;

    public DefaultPolicyValidator(final int venueCount) {
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        this.venueCount = venueCount;
    }

    /**
     * Validates policy identity, hashes, route offsets, aligned arrays, and
     * route venue IDs.
     */
    @Override
    public PolicyValidationReport validate(final SorPolicy policy) {
        final List<String> errors = new ArrayList<>();
        if (policy == null) {
            errors.add("policy must not be null");
            return new PolicyValidationReport(errors);
        }
        if (policy.policyHash64 == 0L || policy.policyHashSha256 == null || policy.policyHashSha256.length != 32) {
            errors.add("policy hash must be present");
        }
        if (policy.hotRouteBook == null || policy.fullPolicyMatrix == null) {
            errors.add("policy route structures must be present");
            return new PolicyValidationReport(errors);
        }
        validateHotRouteBook(policy.hotRouteBook, errors);
        return new PolicyValidationReport(errors);
    }

    private void validateHotRouteBook(final HotRouteBook book, final List<String> errors) {
        final int expectedOffsets = book.instrumentCount * book.regimeCount * book.urgencyCount + 1;
        if (book.routeListOffset.length != expectedOffsets) {
            errors.add("routeListOffset length invalid");
        }
        if (book.routeListOffset.length > 0 && book.routeListOffset[0] != 0) {
            errors.add("routeListOffset must start at zero");
        }
        for (int i = 1; i < book.routeListOffset.length; i++) {
            if (book.routeListOffset[i] < book.routeListOffset[i - 1]) {
                errors.add("routeListOffset must be monotonic");
            }
            if (book.routeListOffset[i] == book.routeListOffset[i - 1]) {
                errors.add("route list must be non-empty");
            }
        }
        if (book.routeListOffset[book.routeListOffset.length - 1] != book.routeVenueId.length) {
            errors.add("routeListOffset sentinel invalid");
        }
        final int routeLength = book.routeVenueId.length;
        if (book.weightBps.length != routeLength || book.routeFlags.length != routeLength) {
            errors.add("hot route arrays must align");
        }
        for (short venueId : book.routeVenueId) {
            if (venueId < 0 || venueId >= venueCount) {
                errors.add("invalid venueId in route list");
                break;
            }
        }
    }
}
