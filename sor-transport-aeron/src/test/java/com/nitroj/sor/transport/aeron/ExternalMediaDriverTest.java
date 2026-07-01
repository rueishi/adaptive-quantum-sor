package com.nitroj.sor.transport.aeron;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies behavior when an external media-driver directory is supplied.
 *
 * <p>Run with transport tests before changing driver ownership or filesystem lifecycle.</p>
 */
class ExternalMediaDriverTest {
    @TempDir Path dir;

    @Test
    void externalDriverDirectoryIsNotRemovedOnClose() {
        final var server = new AeronSorServer("aeron:ipc", AeronTestSupport.engine(8), dir);
        server.start();
        assertFalse(server.embeddedDriver());
        server.close();
        assertTrue(Files.isDirectory(dir));
    }
}
