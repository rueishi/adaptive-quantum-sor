package com.nitroj.sor.testkit.ml;

import com.nitroj.sor.core.audit.RouteAuditEvent;
import com.nitroj.sor.core.ml.FeatureSchema;
import com.nitroj.sor.core.ml.TrainingLabelBuilder;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.stats.ExecutionOutcomeStore;
import com.nitroj.sor.core.stats.VenueStatsState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify Phase 4 feature dataset export.
 *
 * <p>Role in system: covers P4-TC-001 by proving datasets are exported,
 * required columns are present, labels are bounded, and the schema version is
 * recorded.</p>
 *
 * <p>Relationships: exercises {@link FeatureDatasetExporter},
 * {@link FeatureSchema}, and {@link TrainingLabelBuilder}.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: tests use tiny deterministic simulator-like inputs so the
 * Python training path has a stable contract.</p>
 */
final class FeatureDatasetExporterTest {
    @TempDir
    private Path tempDir;

    @Test
    void datasetExportedWithRequiredColumnsAndSchemaVersion() throws IOException {
        final Path dataset = new FeatureDatasetExporter().export(
                outcomes(),
                market(),
                stats(),
                audit(),
                tempDir.resolve("training.csv")
        );

        final List<String> lines = Files.readAllLines(dataset);
        assertEquals(3, lines.size());
        assertEquals(FeatureSchema.csvHeader(), lines.get(0));
        assertTrue(lines.get(1).startsWith(FeatureSchema.VERSION + ",11,77,3,0,0,0,100,101,1"));
        assertTrue(lines.get(2).contains(",0,250,500,0"));
    }

    @Test
    void labelsBounded() {
        final TrainingLabelBuilder.TrainingLabels labels = new TrainingLabelBuilder()
                .labels(outcomes(), 0, -1);

        assertEquals(10_000, labels.fillBps());
        assertEquals(10_000, labels.slippageBps());
        assertEquals(0, labels.regimeId());
    }

    @Test
    void exporterRejectsMissingInputs() {
        assertEquals("export inputs must not be null", assertThrows(IllegalArgumentException.class,
                () -> new FeatureDatasetExporter().export(null, market(), stats(), audit(), tempDir.resolve("x.csv"))
        ).getMessage());
    }

    private static ExecutionOutcomeStore outcomes() {
        final ExecutionOutcomeStore outcomes = new ExecutionOutcomeStore(4);
        outcomes.append(1L, 0, ExecutionOutcomeStore.FULL_FILL, 100L, 1_000, 20_000, -10);
        outcomes.append(2L, 1, ExecutionOutcomeStore.REJECT, 0L, 2_000, 250, 500);
        return outcomes;
    }

    private static MarketBookState market() {
        final MarketBookState market = new MarketBookState(1, 2);
        market.updateTopOfBook(0, 0, 100L, 101L, 1_000L, 900L);
        market.updateTopOfBook(0, 1, 99L, 102L, 800L, 700L);
        return market;
    }

    private static VenueStatsState stats() {
        final VenueStatsState stats = new VenueStatsState(1, 2, 1);
        stats.update(0, 0, 0, 1_000, 8_000, 100, 50, 1, 20);
        stats.update(0, 1, 0, 2_000, 4_000, 500, 2_000, 3, 100);
        return stats;
    }

    private static RouteAuditEvent audit() {
        final RouteAuditEvent audit = new RouteAuditEvent();
        audit.set(11L, 12L, 77L, 3L, 99L, 2, 0L, 1);
        return audit;
    }
}
