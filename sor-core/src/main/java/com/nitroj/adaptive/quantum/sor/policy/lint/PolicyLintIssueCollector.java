package com.nitroj.adaptive.quantum.sor.policy.lint;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsibility: collect policy lint issues before producing a report.
 *
 * <p>Role in system: {@link DefaultPolicyLint} uses this mutable helper while
 * scanning candidate arrays, then freezes findings into a {@link PolicyLintReport}.</p>
 *
 * <p>Relationships: creates {@link PolicyLintIssue} records with consistent
 * severity/code/message fields.</p>
 *
 * <p>Lifecycle: allocated per lint run and discarded after report creation.</p>
 *
 * <p>Design intent: keeps issue bookkeeping out of the lint algorithm itself.</p>
 */
public final class PolicyLintIssueCollector {
    private final List<PolicyLintIssue> issues = new ArrayList<>();

    /** Adds an error issue. */
    public void error(final String code, final String message) {
        issues.add(new PolicyLintIssue(PolicyLintSeverity.ERROR, code, message));
    }

    /** Adds a warning issue. */
    public void warning(final String code, final String message) {
        issues.add(new PolicyLintIssue(PolicyLintSeverity.WARNING, code, message));
    }

    /** Builds an immutable lint report. */
    public PolicyLintReport report() {
        return new PolicyLintReport(issues);
    }
}
