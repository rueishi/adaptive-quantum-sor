package com.nitroj.sor.core.boot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies that the committed production launcher carries the
 * JVM flags required by the P9-17 runtime profile.
 *
 * <p>Role in system: `scripts/run_engine.sh` is the operator-facing launch
 * path until P8-22 packages the standalone server. This test keeps the script
 * aligned with the JDK 25/ZGC/Compact Object Headers baseline.</p>
 *
 * <p>Relationships: complements Gradle's test JVM configuration. Gradle proves
 * the verification path; this script proves the production launch path.</p>
 *
 * <p>Lifecycle: reads the script as text during the unit test profile.</p>
 *
 * <p>Design intent: enforce the exact flags once so future edits cannot
 * silently drift the runtime away from the benchmark baseline.</p>
 */
class RunScriptFlagPresenceTest {
    private static final Path RUN_SCRIPT = Path.of("scripts/run_engine.sh");

    /**
     * Checks every required production JVM flag is present exactly once.
     *
     * @throws IOException if the launch script cannot be read
     */
    @Test
    void productionLauncherContainsRequiredJvmFlagsExactlyOnce() throws IOException {
        final String script = Files.readString(RUN_SCRIPT);

        assertContainsOnce(script, "-XX:+UseZGC");
        assertContainsOnce(script, "-XX:+UseCompactObjectHeaders");
        assertContainsOnce(script, "-XX:+AlwaysPreTouch");
        assertContainsOnce(script, "-XX:+UseTransparentHugePages");
        assertContainsOnce(script, "-XX:+UseNUMA");
        assertContainsOnce(script, "-Xlog:gc*");
        assertTrue(script.contains("-Xms${HEAP_SIZE}"), "script must set Xms from HEAP_SIZE");
        assertTrue(script.contains("-Xmx${HEAP_SIZE}"), "script must set Xmx from HEAP_SIZE");
    }

    /**
     * Counts literal occurrences to ensure a required flag is neither missing
     * nor duplicated.
     */
    private static void assertContainsOnce(final String text, final String token) {
        int count = 0;
        int index = text.indexOf(token);
        while (index >= 0) {
            count++;
            index = text.indexOf(token, index + token.length());
        }
        assertEquals(1, count, () -> "expected exactly one occurrence of " + token);
    }
}
