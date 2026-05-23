package com.nitroj.sor.core.recovery;

import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.Persistence;

import java.util.Iterator;

/** Minimal recovery coordinator that inspects persistence state on startup. */
public final class RecoveryCoordinator {
    public RecoveryReport recover(final Persistence persistence) {
        final long policyVersion = persistence.lastPersistedPolicyVersion();
        final byte[] snapshot = policyVersion == 0 ? new byte[0] : persistence.readPolicySnapshot(policyVersion);
        long lifecycleCount = 0;
        for (Iterator<LifecycleEvent> it = persistence.replay(0); it.hasNext(); ) {
            it.next();
            lifecycleCount++;
        }
        return new RecoveryReport(policyVersion == 0, policyVersion, snapshot.length, lifecycleCount);
    }
}
