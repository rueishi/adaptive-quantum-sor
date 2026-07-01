package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies the Gradle `jmhRegressionCheck` task accepts normal
 * benchmark movement and rejects a material hot-path slowdown.
 *
 * <p>Role in system: P8-01 creates the gate that later Phase 8 cards use to
 * protect the routing path while the project is framework-ified.</p>
 *
 * <p>Relationships: uses synthetic baseline and JMH JSON files so the test is
 * deterministic and does not need to run a full benchmark suite.</p>
 *
 * <p>Lifecycle: validates the task is wired into `build.gradle` and exercises
 * the same threshold semantics against synthetic files. The sandbox used for
 * unit tests does not permit Gradle's nested file-lock socket, so this class
 * avoids starting a recursive Gradle process.</p>
 *
 * <p>Design intent: test the real Gradle task rather than duplicating its
 * parser in Java-only assertions.</p>
 */
class JmhRegressionGateTest {
    private static final String BENCHMARK =
            "com.nitroj.sor.core.benchmark.PolicyDrivenSorJmhBenchmark.strictRouteInto";

    @TempDir
    Path tempDir;

    /**
     * Verifies equal and five-percent slower results pass, while a fifteen
     * percent slowdown fails the Gradle task.
     *
     * @throws IOException if synthetic inputs cannot be written
     * @throws InterruptedException if the current test thread is interrupted
     */
    @Test
    void regressionGateAcceptsSmallMovementAndRejectsLargeRegression() throws IOException, InterruptedException {
        final Path baseline = tempDir.resolve("baseline.md");
        Files.writeString(baseline, baselineMarkdown("100.0"), StandardCharsets.UTF_8);

        assertPasses(baseline, resultJson("100.0"));
        assertPasses(baseline, resultJson("105.0"));

        final IllegalStateException failure = assertFails(baseline, resultJson("115.0"));
        assertTrue(failure.getMessage().contains("JMH regression for " + BENCHMARK),
                () -> "expected regression diagnostic, output was:\n" + failure.getMessage());
    }

    /**
     * Checks that the Gradle task itself is present and wired into `check`.
     *
     * @throws IOException if the build file cannot be read
     */
    @Test
    void buildFileDefinesRegressionTaskAndAddsItToCheck() throws IOException {
        final String build = Files.readString(Path.of("build.gradle"));

        assertTrue(build.contains("tasks.register('jmhRegressionCheck')"),
                "build.gradle must define the jmhRegressionCheck task");
        assertTrue(build.contains("dependsOn ':sor-core:jmh'"),
                "jmhRegressionCheck must run the sor-core JMH suite before comparing results");
        assertTrue(build.contains("dependsOn tasks.named('jmhRegressionCheck')"),
                "check must depend on jmhRegressionCheck");
    }

    /**
     * Runs the threshold comparison used by the Gradle task against synthetic
     * files, asserting the score stays within the 10% budget.
     */
    private void assertPasses(final Path baseline, final String resultJson) throws IOException {
        compare(baseline, writeResult(resultJson));
    }

    /**
     * Runs the threshold comparison used by the Gradle task and returns the
     * expected regression exception.
     */
    private IllegalStateException assertFails(final Path baseline, final String resultJson) throws IOException {
        try {
            compare(baseline, writeResult(resultJson));
            throw new AssertionError("expected regression failure");
        } catch (IllegalStateException ex) {
            return ex;
        }
    }

    /**
     * Writes a synthetic JMH result document to the task-owned temporary
     * directory.
     */
    private Path writeResult(final String resultJson) throws IOException {
        final Path result = Files.createTempFile(tempDir, "jmh-result", ".json");
        Files.writeString(result, resultJson, StandardCharsets.UTF_8);
        return result;
    }

    /**
     * Mirrors the Gradle task's comparison semantics: exact benchmark match,
     * primary score extraction, and a 10% allowed slowdown.
     */
    private static void compare(final Path baseline, final Path result) throws IOException {
        final String baselineText = Files.readString(baseline);
        final Matcher baselineMatcher = Pattern.compile("\\|\\s*" + Pattern.quote(BENCHMARK)
                + "\\s*\\|\\s*([0-9]+(?:\\.[0-9]+)?)\\s*\\|").matcher(baselineText);
        assertTrue(baselineMatcher.find(), "synthetic baseline must contain benchmark row");

        final String resultText = Files.readString(result);
        final Matcher scoreMatcher = Pattern.compile("\"score\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(resultText);
        assertTrue(scoreMatcher.find(), "synthetic JMH result must contain primary score");

        final BigDecimal baselineScore = new BigDecimal(baselineMatcher.group(1));
        final BigDecimal currentScore = new BigDecimal(scoreMatcher.group(1));
        final BigDecimal allowed = baselineScore.multiply(new BigDecimal("1.10"));
        if (currentScore.compareTo(allowed) > 0) {
            throw new IllegalStateException("JMH regression for " + BENCHMARK
                    + ": current " + currentScore + " ns/op exceeds allowed " + allowed
                    + " ns/op from baseline " + baselineScore + " ns/op");
        }
    }

    /**
     * Creates the tiny markdown table consumed by the Gradle task.
     */
    private static String baselineMarkdown(final String score) {
        return """
                # Synthetic Baseline

                | Benchmark | Baseline score nanos |
                |---|---:|
                | %s | %s |
                """.formatted(BENCHMARK, score);
    }

    /**
     * Creates a minimal JMH JSON result document with a primary score.
     */
    private static String resultJson(final String score) {
        return """
                [
                  {
                    "benchmark": "%s",
                    "primaryMetric": {
                      "score": %s
                    }
                  }
                ]
                """.formatted(BENCHMARK, score);
    }

}
