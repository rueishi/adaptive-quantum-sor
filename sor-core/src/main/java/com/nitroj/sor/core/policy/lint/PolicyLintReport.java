package com.nitroj.sor.core.policy.lint;

import java.util.List;

/**
 * Responsibility: summarize policy lint results.
 *
 * <p>Role in system: policy compilation and publication use this report to
 * block candidates with errors and optionally allow warning-only candidates.</p>
 *
 * <p>Relationships: returned by {@link PolicyLint} implementations and built by
 * {@link PolicyLintIssueCollector}.</p>
 *
 * <p>Lifecycle: immutable after creation and safe to pass between warm-path
 * policy components.</p>
 *
 * <p>Design intent: precomputed error/warning counts make tests and publication
 * gates straightforward without repeatedly scanning issues.</p>
 */
public final class PolicyLintReport {
    private final List<PolicyLintIssue> issues;
    private final int errorCount;
    private final int warningCount;

    public PolicyLintReport(final List<PolicyLintIssue> issues) {
        if (issues == null) {
            throw new IllegalArgumentException("issues must not be null");
        }
        this.issues = List.copyOf(issues);
        int errors = 0;
        int warnings = 0;
        for (PolicyLintIssue issue : this.issues) {
            if (issue.severity() == PolicyLintSeverity.ERROR) {
                errors++;
            } else if (issue.severity() == PolicyLintSeverity.WARNING) {
                warnings++;
            }
        }
        this.errorCount = errors;
        this.warningCount = warnings;
    }

    public List<PolicyLintIssue> issues() {
        return issues;
    }

    public boolean hasErrors() {
        return errorCount > 0;
    }

    public boolean hasWarnings() {
        return warningCount > 0;
    }

    public int errorCount() {
        return errorCount;
    }

    public int warningCount() {
        return warningCount;
    }
}
