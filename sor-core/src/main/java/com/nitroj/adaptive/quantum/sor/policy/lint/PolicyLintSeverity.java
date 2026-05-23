package com.nitroj.adaptive.quantum.sor.policy.lint;

/**
 * Responsibility: define lint issue severity levels.
 *
 * <p>Role in system: lint reports use severity to distinguish publication
 * blocking errors from warning-only policy concerns.</p>
 *
 * <p>Relationships: stored by {@link PolicyLintIssue} and summarized by
 * {@link PolicyLintReport}.</p>
 *
 * <p>Lifecycle: constants are stable for the process lifetime.</p>
 *
 * <p>Design intent: integer constants avoid enum allocation concerns in later
 * warm-path adjacent reporting while remaining explicit in code.</p>
 */
public final class PolicyLintSeverity {
    public static final int INFO = 0;
    public static final int WARNING = 1;
    public static final int ERROR = 2;

    private PolicyLintSeverity() {
    }
}
