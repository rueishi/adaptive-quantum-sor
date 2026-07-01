package com.nitroj.sor.codec.v1;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards against incompatible protocol v1 message changes.
 *
 * <p>Run with codec tests whenever fields, template IDs, or message semantics change.</p>
 */
class SorProtocolV1IncompatibleChangeBlockTest {
    @Test
    void schemaPolicyRequiresV2ForBreakingChanges() throws Exception {
        final String policy = Files.readString(Path.of("docs/integration/SBE_SCHEMA_POLICY.md"));
        final String schema = Files.readString(Path.of("sor-codec/src/main/resources/sbe/sor-protocol-v1.xml"));

        assertTrue(policy.contains("breaking change requires a new `sor-protocol-v2.xml`"));
        for (SorMessageType type : SorMessageType.values()) {
            assertTrue(schema.contains("name=\"" + type.name() + "\" id=\"" + type.templateId() + "\""));
        }
    }
}
