package com.nitroj.sor.obs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyPublicationSpanTest {
    @Test
    void policyPublishCreatesOneWarmPathSpanWithAttributes() {
        final SorObservability observability = SorObservability.create();

        observability.recordPolicyPublished(7, 99, 123);

        final var span = observability.spans().spans().getFirst();
        assertEquals("policy.publish", span.name());
        assertEquals(7, span.policyVersion());
        assertEquals(99, span.policyHash64());
        assertTrue(span.events().contains("policy.validate"));
    }
}
