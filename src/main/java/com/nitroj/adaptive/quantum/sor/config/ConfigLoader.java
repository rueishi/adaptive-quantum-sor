package com.nitroj.adaptive.quantum.sor.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Responsibility: parse the small Phase 1 YAML configuration surface.
 *
 * <p>Role in system: configuration parsing is a startup-only control-plane
 * concern. The loader converts a human-editable YAML file into a validated
 * {@link SorConfig}; no hot-path component should parse YAML or hold generic
 * configuration maps.</p>
 *
 * <p>Relationships: {@link com.nitroj.adaptive.quantum.sor.AdaptiveQuantumSorApplication} uses this
 * loader during process startup. Tests use {@link #loadString(String)} to
 * exercise strict and invalid cases without filesystem setup.</p>
 *
 * <p>Lifecycle: instances are stateless and can be created when needed. Parsing
 * is deterministic and fails fast on missing files, unknown keys in strict
 * mode, malformed integer values, or invalid dimensions.</p>
 *
 * <p>Design intent: a tiny purpose-built parser is sufficient for the scaffold
 * because the task owns only a handful of scalar keys. A full YAML dependency
 * would add network and dependency-management risk before the project skeleton
 * is established.</p>
 */
public final class ConfigLoader {

    private static final String DEFAULT_RESOURCE = "adaptive-quantum-sor.yaml";

    /**
     * Loads the default classpath configuration.
     *
     * @return validated configuration from {@code src/main/resources/adaptive-quantum-sor.yaml}
     * @throws IOException if the resource is not available
     */
    public SorConfig loadDefault() throws IOException {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream input = classLoader.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (input == null) {
                throw new IOException("default config resource not found: " + DEFAULT_RESOURCE);
            }
            return loadString(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * Loads a configuration file from disk.
     *
     * @param path path to a YAML config file
     * @return validated configuration
     * @throws IOException if the path does not exist or cannot be read
     */
    public SorConfig load(final Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        if (!Files.exists(path)) {
            throw new IOException("config file not found: " + path);
        }
        return loadString(Files.readString(path, StandardCharsets.UTF_8));
    }

    /**
     * Parses YAML text into a validated {@link SorConfig}.
     *
     * <p>The parser supports the scalar structure owned by P1-TC-001:
     * {@code adaptiveQuantumSor.*}, {@code runtime.mode}, and
     * {@code config.strictUnknownKeys}. Unknown keys are collected during the
     * first pass and rejected after the strictness flag is known.</p>
     *
     * @param yaml YAML-like configuration text
     * @return validated configuration
     */
    public SorConfig loadString(final String yaml) {
        if (yaml == null || yaml.isBlank()) {
            throw new IllegalArgumentException("config content must not be blank");
        }

        int instruments = SorConfig.defaults().instrumentCount();
        int venues = SorConfig.defaults().venueCount();
        int regimes = SorConfig.defaults().regimeCount();
        int urgencies = SorConfig.defaults().urgencyCount();
        SorConfig.RuntimeMode mode = SorConfig.defaults().runtimeMode();
        boolean strictUnknownKeys = SorConfig.defaults().strictUnknownKeys();
        String section = "";
        final Set<String> unknownKeys = new HashSet<>();

        final String[] lines = yaml.split("\\R");
        for (String rawLine : lines) {
            final String withoutComment = stripComment(rawLine);
            if (withoutComment.isBlank()) {
                continue;
            }

            if (!Character.isWhitespace(withoutComment.charAt(0)) && withoutComment.endsWith(":")) {
                section = withoutComment.substring(0, withoutComment.length() - 1).trim();
                if (!isKnownSection(section)) {
                    unknownKeys.add(section);
                }
                continue;
            }

            final String line = withoutComment.trim();
            final int split = line.indexOf(':');
            if (split < 1) {
                throw new IllegalArgumentException("invalid config line: " + rawLine.trim());
            }
            final String key = line.substring(0, split).trim();
            final String value = line.substring(split + 1).trim();
            final String qualifiedKey = section + "." + key;

            switch (qualifiedKey) {
                case "adaptiveQuantumSor.instruments" -> instruments = parsePositiveInt(qualifiedKey, value);
                case "adaptiveQuantumSor.venues" -> venues = parsePositiveInt(qualifiedKey, value);
                case "adaptiveQuantumSor.regimes" -> regimes = parsePositiveInt(qualifiedKey, value);
                case "adaptiveQuantumSor.urgencies" -> urgencies = parsePositiveInt(qualifiedKey, value);
                case "runtime.mode" -> mode = SorConfig.RuntimeMode.parse(value);
                case "config.strictUnknownKeys" -> strictUnknownKeys = parseBoolean(qualifiedKey, value);
                default -> unknownKeys.add(qualifiedKey);
            }
        }

        if (strictUnknownKeys && !unknownKeys.isEmpty()) {
            throw new IllegalArgumentException("unknown config keys: " + unknownKeys);
        }

        return new SorConfig(instruments, venues, regimes, urgencies, mode, strictUnknownKeys);
    }

    private static boolean isKnownSection(final String section) {
        return "adaptiveQuantumSor".equals(section) || "runtime".equals(section) || "config".equals(section);
    }

    private static String stripComment(final String line) {
        final int commentIndex = line.indexOf('#');
        if (commentIndex < 0) {
            return line;
        }
        return line.substring(0, commentIndex);
    }

    private static int parsePositiveInt(final String key, final String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(key + " must be an integer", ex);
        }
    }

    private static boolean parseBoolean(final String key, final String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }
}
