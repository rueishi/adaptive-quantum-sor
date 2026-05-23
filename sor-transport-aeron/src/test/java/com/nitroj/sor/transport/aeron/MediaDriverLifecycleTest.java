package com.nitroj.sor.transport.aeron;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaDriverLifecycleTest {
    @Test
    void embeddedMediaDriverDirectoryExistsWhileRunningAndIsRemovedOnClose() {
        final var server = new AeronSorServer("aeron:ipc", AeronTestSupport.engine(8));
        final Path dir = server.mediaDriverDir();
        server.start();
        assertTrue(Files.isDirectory(dir));
        server.close();
        assertFalse(Files.exists(dir));
    }
}
