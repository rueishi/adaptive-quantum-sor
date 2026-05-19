package com.nitroj.adaptive.quantum.sor.policy.lint;

/**
 * Responsibility: represent one policy lint finding.
 *
 * <p>Role in system: lint reports contain these issues so publication and
 * lifecycle code can explain why a candidate is accepted, warned, or rejected.</p>
 *
 * <p>Relationships: produced by {@link PolicyLintIssueCollector} and consumed
 * by {@link PolicyLintReport}.</p>
 *
 * <p>Lifecycle: immutable after construction.</p>
 *
 * <p>Design intent: a compact record keeps code/message/severity tied together
 * without a heavier diagnostics framework.</p>
 */
public record PolicyLintIssue(int severity, String code, String message) {
    public PolicyLintIssue {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
