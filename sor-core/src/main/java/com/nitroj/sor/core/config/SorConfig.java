package com.nitroj.sor.core.config;

/**
 * Responsibility: represent the validated Phase 1 scaffold configuration.
 *
 * <p>Role in system: every component introduced by P1-TC-001 through P1-TC-005
 * depends on these dimensions to size dense primitive arrays. The values are
 * immutable after parsing so startup failures happen before any hot-path state
 * is created.</p>
 *
 * <p>Relationships: {@link ConfigLoader} creates instances of this record. The
 * application entry point reads it to build a startup result. Later simulator,
 * metadata, and policy tasks can use it as the source of dense ID dimensions.</p>
 *
 * <p>Lifecycle: constructed once during application startup or directly inside
 * tests. Invalid dimensions and runtime modes are rejected at construction
 * time.</p>
 *
 * <p>Design intent: this record is intentionally small and explicit. It avoids
 * a generic map-based configuration surface so misspelled keys and invalid
 * dimensions can be caught deterministically.</p>
 */
public record SorConfig(
        int instrumentCount,
        int venueCount,
        int regimeCount,
        int urgencyCount,
        RuntimeMode runtimeMode,
        boolean strictUnknownKeys
) {

    /**
     * Creates and validates a scaffold configuration.
     *
     * @param instrumentCount number of dense instrument IDs
     * @param venueCount number of dense venue IDs
     * @param regimeCount number of dense regime IDs
     * @param urgencyCount number of dense urgency IDs
     * @param runtimeMode demo or strict runtime behavior
     * @param strictUnknownKeys whether config parsing rejects unknown keys
     */
    public SorConfig {
        requirePositive("instrumentCount", instrumentCount);
        requirePositive("venueCount", venueCount);
        requirePositive("regimeCount", regimeCount);
        requirePositive("urgencyCount", urgencyCount);
        if (runtimeMode == null) {
            throw new IllegalArgumentException("runtimeMode must not be null");
        }
    }

    /**
     * Builds the default configuration used by {@code adaptive-quantum-sor.yaml}.
     *
     * @return a validated demo-mode Adaptive Quantum SOR configuration
     */
    public static SorConfig defaults() {
        return new SorConfig(20, 100, 5, 4, RuntimeMode.DEMO, true);
    }

    private static void requirePositive(final String name, final int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    /**
     * Runtime behavior mode for the Adaptive Quantum SOR.
     *
     * <p>DEMO allows verbose operator-facing behavior in later tasks, while
     * STRICT is reserved for allocation and hot-path discipline checks.</p>
     */
    public enum RuntimeMode {
        DEMO,
        STRICT;

        /**
         * Parses a YAML scalar into a runtime mode.
         *
         * @param value mode text such as {@code demo} or {@code strict}
         * @return parsed runtime mode
         * @throws IllegalArgumentException when the mode is unknown
         */
        public static RuntimeMode parse(final String value) {
            if ("demo".equalsIgnoreCase(value)) {
                return DEMO;
            }
            if ("strict".equalsIgnoreCase(value)) {
                return STRICT;
            }
            throw new IllegalArgumentException("runtime.mode must be demo or strict");
        }
    }
}
