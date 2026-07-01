package com.nitroj.sor.core.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify cross-phase documentation artifacts.
 *
 * <p>Role in system: covers X-TC-001 and X-TC-002 by pinning spec glossary,
 * architecture, and sequence-diagram terms required by the implementation
 * plan.</p>
 *
 * <p>Relationships: reads files under {@code docs/} as build-time
 * documentation checks.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: keep operator and developer documentation aligned with the
 * codebase after each phase adds new surfaces.</p>
 */
final class CrossPhaseDocumentationTest {
    @Test
    void glossaryAndArchitectureCoverRequiredTerms() throws IOException {
        final String glossary = Files.readString(Path.of("adaptive_quantum_sor_spec_v1.md"), StandardCharsets.UTF_8);
        final String architecture = read("architecture/ARCHITECTURE.md");

        for (final String term : new String[]{
                "HotRouteBook",
                "FullPolicyMatrix",
                "PolicyLint",
                "PolicyValidator",
                "PolicyCompiler",
                "routeKey",
                "StrategicVenueSubsetResult",
                "TacticalPolicyResult",
                "LifecycleEvent",
                "tools/python-research/adaptive_quantum_sor_research",
                "DataFrame",
                "model_metadata.properties"
        }) {
            assertContains(glossary + architecture, term);
        }
    }

    @Test
    void sequenceDiagramsCoverRequiredFlows() throws IOException {
        final String diagrams = read("architecture/SEQUENCE_DIAGRAMS.md");

        for (final String expected : new String[]{
                "```mermaid",
                "## Parent Order Routing",
                "## Policy Optimization Cycle",
                "## Cross-Parent Batch Venue Allocation",
                "## Robust Policy Selection",
                "## Policy Publication",
                "## Jupyter Order Submission",
                "## Live Jupyter Scenario Run With Explicit Reset And Parent Orders",
                "## Venue Behavior Outcome Loop",
                "BatchAllocationProblem",
                "BatchAllocationPlanStore",
                "BatchAllocationReport",
                "RobustPublicationGate",
                "ScenarioSweepEvaluator",
                "ScoreMatrixArtifactStore",
                "Scenario Catalog Library",
                "parentOrders[]",
                "ManyToOneRingBuffer",
                "PolicyDrivenSorExecutioner",
                "PolicyPublisher",
                "SorNotebookClient",
                "SorEngineImpl",
                "HttpControlPlaneServer",
                "SimulatedVenueAdapter",
                "ChildOrderRef"
        }) {
            assertContains(diagrams, expected);
        }
    }

    @Test
    void specEmbedsGoldenScenarioFlowDiagram() throws IOException {
        final String spec = Files.readString(Path.of("adaptive_quantum_sor_spec_v1.md"), StandardCharsets.UTF_8);
        final String drawio = Files.readString(Path.of("docs/architecture/diagrams/sequence_diagrams.drawio"), StandardCharsets.UTF_8);

        assertContains(spec, "Golden Sequence Diagrams");
        assertContains(spec, "docs/architecture/diagrams/sequence_diagrams.drawio");
        assertContains(spec, "docs/architecture/diagrams/sequence_parent_order_routing.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_policy_optimization_cycle.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_cross_parent_batch_allocation.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_robust_policy_selection.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_policy_publication.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_jupyter_order_submission.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_live_jupyter_scenario_parent_orders.png");
        assertContains(spec, "docs/architecture/diagrams/sequence_venue_behavior_outcome_loop.png");
        assertContains(drawio, "Cross-Parent Batch Venue Allocation");
        assertContains(drawio, "Robust Policy Selection");
        assertContains(drawio, "Live Jupyter Scenario Run With Explicit Reset And Parent Orders");
        assertContains(drawio, "Parent Order Routing");
        assertContains(drawio, "Policy Optimization Cycle");
        assertContains(drawio, "Policy Publication");
        assertContains(drawio, "Jupyter Order Submission");
        assertContains(drawio, "Venue Behavior Outcome Loop");
        for (final String png : new String[]{
                "docs/architecture/diagrams/sequence_parent_order_routing.png",
                "docs/architecture/diagrams/sequence_policy_optimization_cycle.png",
                "docs/architecture/diagrams/sequence_cross_parent_batch_allocation.png",
                "docs/architecture/diagrams/sequence_robust_policy_selection.png",
                "docs/architecture/diagrams/sequence_policy_publication.png",
                "docs/architecture/diagrams/sequence_jupyter_order_submission.png",
                "docs/architecture/diagrams/sequence_live_jupyter_scenario_parent_orders.png",
                "docs/architecture/diagrams/sequence_venue_behavior_outcome_loop.png"
        }) {
            assertTrue(Files.isRegularFile(Path.of(png)), () -> "rendered sequence PNG must exist: " + png);
        }
    }

    private static String read(final String filename) throws IOException {
        return Files.readString(Path.of("docs", filename), StandardCharsets.UTF_8);
    }

    private static void assertContains(final String text, final String expected) {
        assertTrue(text.contains(expected), () -> "documentation must contain: " + expected);
    }
}
