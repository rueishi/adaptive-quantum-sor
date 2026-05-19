package com.nitroj.adaptive.quantum.sor.policy.validation;

import java.util.List;

/**
 * Responsibility: summarize compiled policy validation results.
 *
 * <p>Role in system: publication gates consume this report to decide whether a
 * compiled policy artifact may become active.</p>
 *
 * <p>Relationships: returned by {@link PolicyValidator} implementations and
 * passed to publication gates.</p>
 *
 * <p>Lifecycle: immutable after construction.</p>
 *
 * <p>Design intent: simple error strings are sufficient for Phase 1 while
 * retaining a stable success contract.</p>
 */
public final class PolicyValidationReport {
    private final List<String> errors;

    public PolicyValidationReport(final List<String> errors) {
        if (errors == null) {
            throw new IllegalArgumentException("errors must not be null");
        }
        this.errors = List.copyOf(errors);
    }

    public static PolicyValidationReport validReport() {
        return new PolicyValidationReport(List.of());
    }

    public List<String> errors() {
        return errors;
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
