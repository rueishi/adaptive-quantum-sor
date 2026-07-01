package com.nitroj.sor.testkit.ml;

import com.nitroj.sor.core.audit.RouteAuditEvent;
import com.nitroj.sor.core.ml.FeatureSchema;
import com.nitroj.sor.core.ml.TrainingLabelBuilder;
import com.nitroj.sor.core.state.MarketBookState;
import com.nitroj.sor.core.stats.ExecutionOutcomeStore;
import com.nitroj.sor.core.stats.VenueStatsState;
import com.nitroj.sor.testkit.scenario.ScenarioSummary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: export Phase 4 training features to a versioned CSV dataset.
 *
 * <p>Role in system: bridges simulated execution/market/stat/audit data into
 * the Python research training path.</p>
 *
 * <p>Relationships: reads {@link ExecutionOutcomeStore}, {@link MarketBookState},
 * {@link VenueStatsState}, and {@link RouteAuditEvent}; writes schema-versioned
 * CSV files consumed by {@code tools/python-research/scripts/train_models.py}.</p>
 *
 * <p>Lifecycle: invoked after a simulation or test scenario has generated
 * outcomes and route audit evidence.</p>
 *
 * <p>Design intent: CSV keeps the Adaptive Quantum SOR transparent while the schema version
 * provides the compatibility guard needed by later model imports.</p>
 */
public final class FeatureDatasetExporter {
    private final TrainingLabelBuilder labelBuilder;

    public FeatureDatasetExporter() {
        this(new TrainingLabelBuilder());
    }

    public FeatureDatasetExporter(final TrainingLabelBuilder labelBuilder) {
        if (labelBuilder == null) {
            throw new IllegalArgumentException("labelBuilder must not be null");
        }
        this.labelBuilder = labelBuilder;
    }

    public Path export(
            final ExecutionOutcomeStore outcomes,
            final MarketBookState marketBooks,
            final VenueStatsState venueStats,
            final RouteAuditEvent auditEvent,
            final Path outputFile
    ) throws IOException {
        if (outcomes == null || marketBooks == null || venueStats == null || auditEvent == null || outputFile == null) {
            throw new IllegalArgumentException("export inputs must not be null");
        }
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        final List<String> lines = new ArrayList<>();
        lines.add(FeatureSchema.csvHeader());
        for (int i = 0; i < outcomes.size(); i++) {
            final int venueId = outcomes.venueId(i);
            final int instrumentId = 0;
            final int regimeId = 0;
            final int statsIndex = venueStats.idxIVR(instrumentId, venueId, regimeId);
            final long bid = marketBooks.bidPriceTicks(instrumentId, venueId);
            final long ask = marketBooks.askPriceTicks(instrumentId, venueId);
            final TrainingLabelBuilder.TrainingLabels labels = labelBuilder.labels(outcomes, i, regimeId);
            lines.add(String.join(",",
                    FeatureSchema.VERSION,
                    Long.toString(auditEvent.eventId),
                    Long.toString(auditEvent.parentOrderId),
                    Long.toString(auditEvent.policyVersion),
                    Integer.toString(instrumentId),
                    Integer.toString(venueId),
                    Integer.toString(regimeId),
                    Long.toString(bid),
                    Long.toString(ask),
                    Long.toString(Math.max(0L, ask - bid)),
                    Long.toString(marketBooks.bidQty(instrumentId, venueId)),
                    Long.toString(marketBooks.askQty(instrumentId, venueId)),
                    Integer.toString(outcomes.latencyNanos(i)),
                    Integer.toString(venueStats.fillProbabilityBps[statsIndex]),
                    Integer.toString(venueStats.toxicityBps[statsIndex]),
                    Integer.toString(venueStats.rejectRateBps[statsIndex]),
                    Integer.toString(outcomes.slippageBps(i)),
                    Integer.toString(auditEvent.childOrderCount),
                    Long.toString(auditEvent.residualQty),
                    Integer.toString(labels.fillBps()),
                    Integer.toString(labels.slippageBps()),
                    Integer.toString(labels.toxicityBps()),
                    Integer.toString(labels.regimeId())
            ));
        }
        Files.write(outputFile, lines, StandardCharsets.UTF_8);
        return outputFile;
    }

    /**
     * Writes compact scenario lineage metadata next to a generated dataset.
     *
     * <p>Core logic: the metadata is intentionally primitive/string backed so a
     * notebook or CI artifact can prove which scenario id, seed, tick count, and
     * deterministic summary checksum produced a feature file.</p>
     *
     * @param summary deterministic scenario summary
     * @param outputFile metadata file to write
     * @return written metadata path
     * @throws IOException when the file cannot be written
     */
    public Path exportScenarioLineage(final ScenarioSummary summary, final Path outputFile) throws IOException {
        if (summary == null || outputFile == null) {
            throw new IllegalArgumentException("summary and outputFile must not be null");
        }
        if (outputFile.getParent() != null) {
            Files.createDirectories(outputFile.getParent());
        }
        final List<String> lines = List.of(
                "scenarioId=" + summary.scenarioId(),
                "scenarioSeed=" + summary.seed(),
                "scenarioTicks=" + summary.ticksRun(),
                "bookChecksum=" + summary.bookChecksum(),
                "orderCount=" + summary.orderCount(),
                "outcomeCount=" + summary.outcomeCount()
        );
        Files.write(outputFile, lines, StandardCharsets.UTF_8);
        return outputFile;
    }
}
