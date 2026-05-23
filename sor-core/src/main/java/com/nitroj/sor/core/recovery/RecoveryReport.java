package com.nitroj.sor.core.recovery;

/** Recovery summary for startup and tests. */
public record RecoveryReport(boolean emptyWal, long activePolicyVersion, int snapshotBytes, long lifecycleEvents) {
}
