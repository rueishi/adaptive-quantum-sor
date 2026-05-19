package com.nitroj.adaptive.quantum.sor.scenario;

import com.nitroj.adaptive.quantum.sor.stats.RegimeState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: load the user-readable scenario YAML subset.
 *
 * <p>Role in system: converts files under {@code scenarios/} into validated
 * {@link ScenarioDefinition} instances without putting YAML parsing on any hot
 * path.</p>
 *
 * <p>Relationships: produces definitions that are converted to
 * {@link ScenarioSpec} and executed by {@link ScenarioRunner}.</p>
 *
 * <p>Lifecycle: stateless loader used by tests, notebooks, API tooling, or
 * future CLI commands.</p>
 *
 * <p>Design intent: support a deliberately small, readable YAML subset instead
 * of requiring users to understand Java constructors.</p>
 */
public final class ScenarioDefinitionLoader {
    /**
     * Loads a scenario metadata file from disk.
     */
    public ScenarioDefinition load(final Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (!Files.exists(path)) {
            throw new IOException("scenario file not found: " + path);
        }
        return loadString(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Parses a scenario YAML subset.
     *
     * <p>Supported keys are top-level scalar keys {@code scenarioId},
     * {@code description}, {@code seed}, {@code ticks},
     * {@code venueProfilesEnabled}, and a {@code windows} list whose items
     * contain {@code name}, {@code startTick}, {@code endTick}, and
     * {@code regime}. Additional sections such as {@code expected} are allowed
     * for user documentation and ignored by the runtime loader.</p>
     */
    public ScenarioDefinition loadString(final String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("scenario content must not be blank");
        }
        String scenarioId = "";
        String description = "";
        long seed = 0L;
        int ticks = -1;
        boolean venueProfilesEnabled = true;
        String section = "";
        final List<WindowBuilder> windows = new ArrayList<>();
        WindowBuilder currentWindow = null;

        for (String rawLine : content.split("\\R")) {
            final String lineWithoutComment = stripComment(rawLine);
            if (lineWithoutComment.isBlank()) {
                continue;
            }
            final String trimmed = lineWithoutComment.trim();
            if (!Character.isWhitespace(lineWithoutComment.charAt(0)) && trimmed.endsWith(":")) {
                section = trimmed.substring(0, trimmed.length() - 1);
                currentWindow = null;
                continue;
            }
            if (!Character.isWhitespace(lineWithoutComment.charAt(0))) {
                section = "";
                currentWindow = null;
            }
            if ("windows".equals(section) && trimmed.startsWith("- ")) {
                currentWindow = new WindowBuilder();
                windows.add(currentWindow);
                parseWindowProperty(currentWindow, trimmed.substring(2).trim());
                continue;
            }
            final int split = trimmed.indexOf(':');
            if (split < 1) {
                continue;
            }
            if ("windows".equals(section) && currentWindow != null) {
                parseWindowProperty(currentWindow, trimmed);
                continue;
            }
            if (!section.isEmpty()) {
                continue;
            }
            final String key = trimmed.substring(0, split).trim();
            final String value = unquote(trimmed.substring(split + 1).trim());
            switch (key) {
                case "scenarioId" -> scenarioId = value;
                case "description" -> description = value;
                case "seed" -> seed = parseLong("seed", value);
                case "ticks" -> ticks = Math.toIntExact(parseLong("ticks", value));
                case "venueProfilesEnabled" -> venueProfilesEnabled = parseBoolean(value);
                default -> {
                }
            }
        }

        final ScenarioWindow[] scenarioWindows = windows.stream()
                .map(WindowBuilder::build)
                .toArray(ScenarioWindow[]::new);
        return new ScenarioDefinition(scenarioId, description, seed, ticks, venueProfilesEnabled, scenarioWindows);
    }

    private static void parseWindowProperty(final WindowBuilder builder, final String property) {
        final int split = property.indexOf(':');
        if (split < 1) {
            return;
        }
        final String key = property.substring(0, split).trim();
        final String value = unquote(property.substring(split + 1).trim());
        switch (key) {
            case "name" -> builder.name = value;
            case "startTick" -> builder.startTick = Math.toIntExact(parseLong("startTick", value));
            case "endTick" -> builder.endTick = Math.toIntExact(parseLong("endTick", value));
            case "regime" -> builder.regimeId = regimeId(value);
            default -> {
            }
        }
    }

    private static int regimeId(final String value) {
        if ("NORMAL".equalsIgnoreCase(value)) {
            return RegimeState.NORMAL;
        }
        if ("VOLATILE".equalsIgnoreCase(value)) {
            return RegimeState.VOLATILE;
        }
        if ("THIN_BOOK".equalsIgnoreCase(value) || "THIN-BOOK".equalsIgnoreCase(value)) {
            return RegimeState.THIN_BOOK;
        }
        throw new IllegalArgumentException("regime must be NORMAL, VOLATILE, or THIN_BOOK");
    }

    private static String stripComment(final String line) {
        final int comment = line.indexOf('#');
        return comment < 0 ? line : line.substring(0, comment);
    }

    private static String unquote(final String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static long parseLong(final String key, final String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be an integer", ex);
        }
    }

    private static boolean parseBoolean(final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("venueProfilesEnabled must be true or false");
    }

    private static final class WindowBuilder {
        String name = "";
        int startTick = -1;
        int endTick = -1;
        int regimeId = -1;

        ScenarioWindow build() {
            if (name.isBlank()) {
                throw new IllegalArgumentException("window name must not be blank");
            }
            return new ScenarioWindow(startTick, endTick, regimeId);
        }
    }
}
