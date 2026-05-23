package com.nitroj.sor.api.spi;

import com.nitroj.sor.api.PolicyHandle;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Responsibility: verifies the persistence SPI sequencing, replay, and
 * write-once snapshot semantics.
 *
 * <p>Role in system: P8-04 defines the persistence contract before Chronicle
 * implements it in P8-11.</p>
 *
 * <p>Relationships: uses a fake in-memory implementation that conforms to
 * {@link Persistence}.</p>
 *
 * <p>Lifecycle: populated and replayed inside one unit test.</p>
 *
 * <p>Design intent: make sequence ownership and snapshot immutability explicit
 * for adapter authors.</p>
 */
class PersistenceContractTest {
    /**
     * Verifies event sequence assignment, replay ordering, and snapshot
     * write-once behavior.
     */
    @Test
    void persistenceContractIsExplicit() {
        final FakePersistence persistence = new FakePersistence();
        for (int i = 0; i < 100; i++) {
            assertEquals(i + 1, persistence.appendLifecycleEvent(new LifecycleEvent().set(1, i, i)));
        }

        final List<Long> replayed = new ArrayList<>();
        persistence.replay(50).forEachRemaining(event -> replayed.add(event.sequenceNumber()));
        assertEquals(50, replayed.size());
        assertEquals(51, replayed.get(0));
        assertEquals(100, replayed.get(49));

        final PolicyHandle handle = new TestPolicyHandle(1);
        persistence.persistPolicySnapshot(handle, new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, persistence.readPolicySnapshot(1));
        assertEquals(1, persistence.lastPersistedPolicyVersion());
        assertThrows(IllegalStateException.class, () -> persistence.persistPolicySnapshot(handle, new byte[] {9}));
    }

    private static final class FakePersistence implements Persistence {
        private final List<LifecycleEvent> events = new ArrayList<>();
        private final Map<Long, byte[]> snapshots = new HashMap<>();
        private long lastPolicyVersion;

        @Override
        public long appendLifecycleEvent(final LifecycleEvent event) {
            event.sequenceNumber(events.size() + 1L);
            events.add(event);
            return event.sequenceNumber();
        }

        @Override
        public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {
            if (snapshots.containsKey(handle.version())) {
                throw new IllegalStateException("snapshot already exists");
            }
            snapshots.put(handle.version(), snapshotBytes.clone());
            lastPolicyVersion = Math.max(lastPolicyVersion, handle.version());
        }

        @Override
        public byte[] readPolicySnapshot(final long policyVersion) {
            return snapshots.get(policyVersion).clone();
        }

        @Override
        public long lastPersistedPolicyVersion() {
            return lastPolicyVersion;
        }

        @Override
        public Iterator<LifecycleEvent> replay(final long sinceSequence) {
            return events.stream().filter(event -> event.sequenceNumber() > sinceSequence).iterator();
        }
    }

    private record TestPolicyHandle(long version) implements PolicyHandle {
        @Override public long hash64() { return 0; }
        @Override public byte[] hashSha256() { return new byte[32]; }
        @Override public long createdEpochNanos() { return 0; }
        @Override public long effectiveFromEpochNanos() { return 0; }
    }
}
