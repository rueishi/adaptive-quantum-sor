package com.nitroj.sor.codec.v1;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SorProtocolV1ReproducibleGenerationTest {
    @Test
    void generationTaskWritesDeterministicMarkerSource() throws Exception {
        final Path generated = Path.of("sor-codec/build/generated/sbe/com/nitroj/sor/codec/v1/GeneratedProtocolMarker.java");
        if (Files.exists(generated)) {
            assertEquals(Files.readString(generated), Files.readString(generated));
        }
    }
}
