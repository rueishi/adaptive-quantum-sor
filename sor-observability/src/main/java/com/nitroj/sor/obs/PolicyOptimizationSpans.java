package com.nitroj.sor.obs;

import java.util.ArrayList;
import java.util.List;

/** In-memory warm-path policy publication span recorder. */
public final class PolicyOptimizationSpans {
    private final List<PolicySpan> spans = new ArrayList<>();

    public synchronized void publish(final long policyVersion, final long policyHash64) {
        spans.add(new PolicySpan("policy.publish", policyVersion, policyHash64,
                List.of("policy.compile", "policy.validate", "policy.publish.commit")));
    }

    public synchronized List<PolicySpan> spans() {
        return List.copyOf(spans);
    }

    public record PolicySpan(String name, long policyVersion, long policyHash64, List<String> events) {
    }
}
