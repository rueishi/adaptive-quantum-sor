package com.nitroj.adaptive.quantum.sor.docs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify repeatable CI/local test profile scripts.
 *
 * <p>Role in system: covers X-TC-003 by ensuring unit, integration, simulator,
 * policy compile, native, and benchmark profiles are documented and wired to
 * concrete Gradle commands.</p>
 *
 * <p>Relationships: reads {@code scripts/run_tests.sh},
 * {@code scripts/run_benchmarks.sh}, and {@code docs/testing/CI_TEST_PROFILES.md}.</p>
 *
 * <p>Lifecycle: executed by Gradle with the JUnit suite.</p>
 *
 * <p>Design intent: profile scripts are operational documentation, so this test
 * keeps their key commands from drifting.</p>
 */
final class CiTestProfileTest {
    @Test
    void scriptsCoverRequiredProfiles() throws IOException {
        final String runTests = read("scripts/run_tests.sh");
        final String runBenchmarks = read("scripts/run_benchmarks.sh");
        final String docs = read("docs/testing/CI_TEST_PROFILES.md");
        final String spec = read("adaptive_quantum_sor_spec_v1.md");
        final String prompt = read(".prompt/RUN_X-TC-003.md");
        final String combined = runTests + runBenchmarks + docs + spec + prompt;

        for (final String expected : new String[]{
                "unit)",
                "integration)",
                "simulator)",
                "scenario)",
                "policy)",
                "native)",
                "all)",
                "BenchmarkHarnessTest",
                "ComparisonRunnerTest",
                "compare_sor_dataset.py",
                "generate_sor_dataset.py",
                "dataset-backed static-vs-adaptive comparison profile",
                "sor-comparison-report.md",
                ":sor-test-server:test",
                "DefaultPolicyCompilerTest",
                "DefaultPolicyValidatorTest",
                "DefaultPolicyLintTest",
                "nativeTest",
                "P1-BENCH-003"
        }) {
            assertTrue(combined.contains(expected), () -> "profile evidence must contain: " + expected);
        }
    }

    @Test
    void simulatorAndScenarioProfilesAreDocumentedSeparately() throws IOException {
        final String runTests = read("scripts/run_tests.sh");
        final String docs = read("docs/testing/CI_TEST_PROFILES.md");

        assertTrue(runTests.contains(":sor-test-server:test"));
        assertTrue(runTests.contains("--tests 'com.nitroj.adaptive.quantum.sor.scenario.*'"));
        assertTrue(runTests.contains("SorEndToEndTest.replayableScenarioProducesEquivalentSummary"));
        assertTrue(docs.contains("Simulator Deterministic Profile"));
        assertTrue(docs.contains("Scenario-Driven Simulation Profile"));
        assertTrue(docs.contains("replayable scenario tests also run from"));
    }

    private static String read(final String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}
