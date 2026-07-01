package com.nitroj.sor.core.ml;

import com.nitroj.sor.core.model.ModelSignalState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * Responsibility: import validated Phase 4 prediction artifacts into Java model signals.
 *
 * <p>Role in system: converts generated Python predictions into
 * {@link ModelSignalState} only after metadata, checksum, schema, and signal
 * bounds have passed validation.</p>
 *
 * <p>Relationships: reads {@link ModelArtifactMetadata}, uses
 * {@link ModelSignalValidator}, and produces model signal state for later
 * optimizer input integration.</p>
 *
 * <p>Lifecycle: called when a new offline model artifact is ready for approval.</p>
 *
 * <p>Design intent: invalid artifacts return the previous approved signals so
 * corrupt model output cannot replace active optimizer inputs.</p>
 */
public final class ModelArtifactImporter {
    private final ModelSignalValidator validator;

    public ModelArtifactImporter() {
        this(new ModelSignalValidator());
    }

    public ModelArtifactImporter(final ModelSignalValidator validator) {
        if (validator == null) {
            throw new IllegalArgumentException("validator must not be null");
        }
        this.validator = validator;
    }

    public ImportResult importArtifact(
            final Path artifactDirectory,
            final String expectedFeatureSchemaVersion,
            final ModelSignalState previousApproved,
            final int instrumentCount,
            final int venueCount,
            final int regimeCount
    ) throws IOException {
        if (artifactDirectory == null || expectedFeatureSchemaVersion == null || previousApproved == null) {
            throw new IllegalArgumentException("artifactDirectory, expectedFeatureSchemaVersion, and previousApproved must not be null");
        }
        try {
            final ModelArtifactMetadata metadata = ModelArtifactMetadata.read(artifactDirectory.resolve("model_metadata.properties"));
            final Path predictions = artifactDirectory.resolve(metadata.predictionFile());
            final String checksum = sha256(predictions);
            final ModelSignalValidator.ValidationReport report =
                    validator.validateMetadata(metadata, expectedFeatureSchemaVersion, checksum);
            if (!report.valid()) {
                return ImportResult.rejected(previousApproved, metadata.modelVersion(), String.join("; ", report.errors()));
            }
            final ModelSignalState imported = readPredictions(predictions, metadata.modelVersion(), instrumentCount, venueCount, regimeCount);
            return ImportResult.accepted(imported, metadata.modelVersion());
        } catch (RuntimeException | IOException ex) {
            return ImportResult.rejected(previousApproved, previousApproved.modelVersion(), ex.getMessage());
        }
    }

    private ModelSignalState readPredictions(
            final Path predictions,
            final long modelVersion,
            final int instrumentCount,
            final int venueCount,
            final int regimeCount
    ) throws IOException {
        final List<String> lines = Files.readAllLines(predictions, StandardCharsets.UTF_8);
        if (lines.size() <= 1) {
            throw new IllegalArgumentException("predictions.csv has no rows");
        }
        if (!"instrument_id,venue_id,regime_id,venue_score_bps".equals(lines.get(0))) {
            throw new IllegalArgumentException("predictions.csv header mismatch");
        }
        final ModelSignalState state = new ModelSignalState(instrumentCount, venueCount, regimeCount);
        state.setModelVersion(modelVersion);
        for (int lineNo = 1; lineNo < lines.size(); lineNo++) {
            final String[] parts = lines.get(lineNo).split(",", -1);
            if (parts.length != 4) {
                throw new IllegalArgumentException("prediction row must have 4 columns at line " + (lineNo + 1));
            }
            final int instrumentId = Integer.parseInt(parts[0]);
            final int venueId = Integer.parseInt(parts[1]);
            final int regimeId = Integer.parseInt(parts[2]);
            final int signal = validator.validateSignalBps(parts[3]);
            state.setVenueScoreBps(instrumentId, venueId, regimeId, signal);
        }
        return state;
    }

    private static String sha256(final Path file) throws IOException {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(Files.readAllBytes(file));
            final StringBuilder builder = new StringBuilder(hash.length * 2);
            for (final byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record ImportResult(
            boolean accepted,
            ModelSignalState modelSignals,
            long modelSignalVersion,
            String message
    ) {
        public static ImportResult accepted(final ModelSignalState signals, final long version) {
            return new ImportResult(true, signals, version, "accepted");
        }

        public static ImportResult rejected(final ModelSignalState previous, final long version, final String message) {
            return new ImportResult(false, previous, version, message);
        }
    }
}
