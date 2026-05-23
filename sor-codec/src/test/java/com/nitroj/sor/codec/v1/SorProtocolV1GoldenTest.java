package com.nitroj.sor.codec.v1;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SorProtocolV1GoldenTest {
    @ParameterizedTest
    @MethodSource("com.nitroj.sor.codec.v1.ProtocolFixtures#messages")
    void goldenMessageReencodesByteIdentical(final SorProtocolMessage message) throws Exception {
        final Path golden = Path.of("sor-codec/src/test/resources/sbe/golden/" + message.type().name() + ".bin");

        assertTrue(Files.isRegularFile(golden), "missing golden for " + message.type());
        final byte[] bytes = ProtocolFixtures.unhex(Files.readString(golden));
        assertArrayEquals(bytes, SorProtocolV1Codec.encode(SorProtocolV1Codec.decode(bytes)));
    }
}
