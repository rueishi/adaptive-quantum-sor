package com.nitroj.adaptive.quantum.sor.ml;

import com.nitroj.adaptive.quantum.sor.model.ModelSignalState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 4 model artifact import and validation.
 *
 * <p>Role in system: covers P4-TC-003 by proving valid artifacts import,
 * checksum failures are rejected, schema mismatches are rejected, and NaN or
 * out-of-bound signals do not replace previous approved signals.</p>
 *
 * <p>Relationships: exercises {@link ModelArtifactImporter},
 * {@link ModelArtifactMetadata}, and {@link ModelSignalValidator}.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite after Python artifact
 * generation is available.</p>
 *
 * <p>Design intent: invalid artifacts return the exact previous signal object
 * so fail-safe behavior is simple to assert.</p>
 */
final class ModelArtifactImporterTest {
    @TempDir
    private Path tempDir;

    @Test
    void validArtifactImports() throws IOException {
        writeArtifact("feature-schema-v1", "6000");
        final ModelSignalState previous = previous();

        final ModelArtifactImporter.ImportResult result = new ModelArtifactImporter()
                .importArtifact(tempDir, FeatureSchema.VERSION, previous, 1, 2, 1);

        assertTrue(result.accepted());
        assertEquals(1L, result.modelSignalVersion());
        assertEquals(6_000, result.modelSignals().venueScoreBps(0, 1, 0));
        assertEquals(1L, result.modelSignals().modelVersion());
    }

    @Test
    void checksumFailureRejected() throws IOException {
        writeArtifact("feature-schema-v1", "6000");
        Files.writeString(tempDir.resolve("predictions.csv"),
                "instrument_id,venue_id,regime_id,venue_score_bps\n0,1,0,7000\n",
                StandardCharsets.UTF_8);
        final ModelSignalState previous = previous();

        final ModelArtifactImporter.ImportResult result = new ModelArtifactImporter()
                .importArtifact(tempDir, FeatureSchema.VERSION, previous, 1, 2, 1);

        assertFalse(result.accepted());
        assertSame(previous, result.modelSignals());
        assertTrue(result.message().contains("checksum failure"));
    }

    @Test
    void schemaMismatchRejected() throws IOException {
        writeArtifact("old-schema", "6000");
        final ModelSignalState previous = previous();

        final ModelArtifactImporter.ImportResult result = new ModelArtifactImporter()
                .importArtifact(tempDir, FeatureSchema.VERSION, previous, 1, 2, 1);

        assertFalse(result.accepted());
        assertSame(previous, result.modelSignals());
        assertTrue(result.message().contains("schema mismatch"));
    }

    @Test
    void nanAndOutOfBoundSignalRejected() throws IOException {
        writeArtifact("feature-schema-v1", "NaN");
        final ModelSignalState previous = previous();

        final ModelArtifactImporter.ImportResult nan = new ModelArtifactImporter()
                .importArtifact(tempDir, FeatureSchema.VERSION, previous, 1, 2, 1);
        assertFalse(nan.accepted());
        assertSame(previous, nan.modelSignals());
        assertTrue(nan.message().contains("venue_score_bps must be finite"));

        writeArtifact("feature-schema-v1", "10001");
        final ModelArtifactImporter.ImportResult outOfBound = new ModelArtifactImporter()
                .importArtifact(tempDir, FeatureSchema.VERSION, previous, 1, 2, 1);
        assertFalse(outOfBound.accepted());
        assertSame(previous, outOfBound.modelSignals());
        assertTrue(outOfBound.message().contains("venue_score_bps must be in [0,10000]"));
    }

    private void writeArtifact(final String schemaVersion, final String score) throws IOException {
        final Path predictions = tempDir.resolve("predictions.csv");
        Files.write(predictions, List.of(
                "instrument_id,venue_id,regime_id,venue_score_bps",
                "0,1,0," + score
        ), StandardCharsets.UTF_8);
        final String checksum = sha256(predictions);
        Files.writeString(tempDir.resolve("model_metadata.properties"),
                "modelVersion=1\n"
                        + "featureSchemaVersion=" + schemaVersion + "\n"
                        + "predictionFile=predictions.csv\n"
                        + "predictionChecksumSha256=" + checksum + "\n"
                        + "rowCount=1\n",
                StandardCharsets.UTF_8);
    }

    private static ModelSignalState previous() {
        final ModelSignalState previous = new ModelSignalState(1, 2, 1);
        previous.setModelVersion(99L);
        previous.setVenueScoreBps(0, 1, 0, 5_555);
        return previous;
    }

    private static String sha256(final Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
