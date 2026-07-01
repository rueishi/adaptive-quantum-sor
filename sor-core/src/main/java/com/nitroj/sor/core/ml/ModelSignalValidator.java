package com.nitroj.sor.core.ml;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: validate imported model signal artifact compatibility and values.
 *
 * <p>Role in system: prevents corrupt Python artifacts, schema mismatches, NaN
 * text, or out-of-bound model signals from replacing approved Java signals.</p>
 *
 * <p>Relationships: used by {@link ModelArtifactImporter} after metadata and
 * prediction CSV parsing.</p>
 *
 * <p>Lifecycle: stateless validator created by the importer or tests.</p>
 *
 * <p>Design intent: reports collect all validation failures while callers keep
 * the prior approved model signal state on any invalid artifact.</p>
 */
public final class ModelSignalValidator {
    public ValidationReport validateMetadata(
            final ModelArtifactMetadata metadata,
            final String expectedFeatureSchemaVersion,
            final String actualChecksum
    ) {
        final List<String> errors = new ArrayList<>();
        if (metadata == null) {
            errors.add("metadata must not be null");
            return new ValidationReport(errors);
        }
        if (!metadata.featureSchemaVersion().equals(expectedFeatureSchemaVersion)) {
            errors.add("schema mismatch: expected " + expectedFeatureSchemaVersion
                    + " but was " + metadata.featureSchemaVersion());
        }
        if (!metadata.predictionChecksumSha256().equals(actualChecksum)) {
            errors.add("checksum failure for " + metadata.predictionFile());
        }
        return new ValidationReport(errors);
    }

    public int validateSignalBps(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("venue_score_bps must not be blank");
        }
        if ("NaN".equalsIgnoreCase(rawValue) || "Infinity".equalsIgnoreCase(rawValue)) {
            throw new IllegalArgumentException("venue_score_bps must be finite");
        }
        final int value = Integer.parseInt(rawValue);
        if (value < 0 || value > 10_000) {
            throw new IllegalArgumentException("venue_score_bps must be in [0,10000]");
        }
        return value;
    }

    public record ValidationReport(List<String> errors) {
        public ValidationReport {
            errors = List.copyOf(errors);
        }

        public boolean valid() {
            return errors.isEmpty();
        }
    }
}
