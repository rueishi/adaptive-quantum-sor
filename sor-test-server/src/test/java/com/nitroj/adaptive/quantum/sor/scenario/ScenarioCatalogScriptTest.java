package com.nitroj.adaptive.quantum.sor.scenario;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verify the user-facing scenario catalog library and command.
 *
 * <p>Role in system: notebook users need a simple command to discover
 * scenarios, filter by tags, and see suggested parent order submissions.</p>
 *
 * <p>Relationships: executes {@code python -m adaptive_quantum_sor_research.scenario_catalog}
 * against the checked-in scenario metadata catalog.</p>
 *
 * <p>Lifecycle: run by Gradle/JUnit as lightweight CLI artifact coverage.</p>
 *
 * <p>Design intent: keep the catalog discoverable without requiring a running
 * API server or Jupyter kernel.</p>
 */
final class ScenarioCatalogScriptTest {
    @Test
    void listsSearchesAndSuggestsParentOrders() throws Exception {
        final Result list = run("list");
        final Result search = run("search", "--tag", "liquidity");
        final Result suggest = run("suggest", "zero-liquidity-safe-route");
        final Result library = runPythonLibraryImport();

        assertEquals(0, list.exitCode, list.stderr);
        assertTrue(list.stdout.contains("baseline-normal-open"));
        assertTrue(list.stdout.contains("normal-volatile-thin"));
        assertEquals(0, search.exitCode, search.stderr);
        assertTrue(search.stdout.contains("liquidity-disappearance"));
        assertEquals(0, suggest.exitCode, suggest.stderr);
        assertTrue(suggest.stdout.contains("Suggested parent order submissions"));
        assertTrue(suggest.stdout.contains("quantity=12000"));
        final Result baselineSuggest = run("suggest", "baseline-normal-open");
        assertEquals(0, baselineSuggest.exitCode, baselineSuggest.stderr);
        assertTrue(baselineSuggest.stdout.contains("baseline-normal-buy"));
        assertTrue(baselineSuggest.stdout.contains("submitMode=SIMULATED"));
        assertEquals(0, library.exitCode, library.stderr);
        assertTrue(library.stdout.contains("library-ok"));
    }

    private static Result run(final String... args) throws Exception {
        final String[] command = new String[args.length + 3];
        command[0] = "python3";
        command[1] = "-m";
        command[2] = "adaptive_quantum_sor_research.scenario_catalog";
        System.arraycopy(args, 0, command, 3, args.length);
        final ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(Path.of(".").toFile());
        builder.environment().put("PYTHONPATH", "tools/python-research");
        final Process process = builder.start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("scenario catalog command timed out");
        }
        return new Result(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private static Result runPythonLibraryImport() throws Exception {
        final ProcessBuilder builder = new ProcessBuilder(
                "python3",
                "-c",
                """
                from adaptive_quantum_sor_research import load_scenarios, search_scenarios, parent_order_suggestions
                scenarios = load_scenarios()
                liquidity = search_scenarios(scenarios, tags=["liquidity"])
                assert liquidity
                assert parent_order_suggestions(liquidity[0])
                print("library-ok", len(scenarios), len(liquidity))
                """
        );
        builder.directory(Path.of(".").toFile());
        builder.environment().put("PYTHONPATH", "tools/python-research");
        final Process process = builder.start();
        final boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError("scenario catalog library import timed out");
        }
        return new Result(
                process.exitValue(),
                new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8),
                new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }
}
