package com.nitroj.sor.core.recovery;

import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.Persistence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies kill and restart recovery behavior for startup recovery and initial policy bootstrap.
 *
 * <p>Run with :sor-core:test to protect engine startup, persistence replay, and recovery tests.</p>
 */
class KillAndRestartRecoveryTest {
    @Test
    void coordinatorRecoversLastPolicyAndLifecycleSequenceFromPersistence() {
        final InMemoryRecoverablePersistence persistence = new InMemoryRecoverablePersistence();
        persistence.appendLifecycleEvent(new LifecycleEvent().set(1, 11, 111));
        persistence.appendLifecycleEvent(new LifecycleEvent().set(2, 22, 222));
        persistence.persistPolicySnapshot(new Handle(7), new byte[] {1, 2, 3, 4});

        final RecoveryReport report = new RecoveryCoordinator().recover(persistence);

        assertEquals(7, report.activePolicyVersion());
        assertEquals(4, report.snapshotBytes());
        assertEquals(2, report.lifecycleEvents());
    }

    private record Handle(long version) implements PolicyHandle {
        @Override public long hash64() { return version; }
        @Override public byte[] hashSha256() { return new byte[32]; }
        @Override public long createdEpochNanos() { return 0; }
        @Override public long effectiveFromEpochNanos() { return 0; }
    }

    private static final class InMemoryRecoverablePersistence implements Persistence {
        private final List<LifecycleEvent> events = new ArrayList<>();
        private long policyVersion;
        private byte[] snapshot = new byte[0];

        @Override public long appendLifecycleEvent(final LifecycleEvent event) {
            final long sequence = events.size() + 1L;
            event.sequenceNumber(sequence);
            events.add(new LifecycleEvent().set(event.eventType(), event.subjectId(), event.epochNanos()).sequenceNumber(sequence));
            return sequence;
        }

        @Override public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {
            policyVersion = handle.version();
            snapshot = Arrays.copyOf(snapshotBytes, snapshotBytes.length);
        }

        @Override public byte[] readPolicySnapshot(final long policyVersion) { return Arrays.copyOf(snapshot, snapshot.length); }
        @Override public long lastPersistedPolicyVersion() { return policyVersion; }
        @Override public Iterator<LifecycleEvent> replay(final long sinceSequence) {
            return events.stream().filter(event -> event.sequenceNumber() > sinceSequence).toList().iterator();
        }
    }
}
