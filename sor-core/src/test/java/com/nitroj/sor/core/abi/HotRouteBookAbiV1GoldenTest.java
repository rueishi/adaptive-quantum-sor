package com.nitroj.sor.core.abi;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/** Verifies the committed golden ABI v1 binary remains byte-identical. */
class HotRouteBookAbiV1GoldenTest {
    @Test
    void goldenBinaryMatchesCurrentWriter() throws Exception {
        final byte[] golden = Files.readAllBytes(Path.of("sor-core/src/test/resources/abi/hot_route_book_v1_golden.bin"));
        assertArrayEquals(golden, HotRouteBookAbiTestFixtures.serialized());
        HotRouteBookAbiV1Reader.read(java.nio.ByteBuffer.wrap(golden));
    }
}
