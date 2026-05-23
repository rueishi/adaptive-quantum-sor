package com.nitroj.sor.sim.adapters;

import com.nitroj.sor.sim.adapters.*;
import com.nitroj.sor.sim.scenario.*;
import com.nitroj.sor.sim.scenario.venues.*;

import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.spi.LifecycleEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryPersistenceContractTest {
    @Test
    void replaysEventsAfterRequestedSequence() {
        final InMemoryPersistence persistence = new InMemoryPersistence();
        for (int i = 0; i < 1000; i++) {
            persistence.appendLifecycleEvent(new LifecycleEvent().set(7, i, i * 10L));
        }

        final var replay = persistence.replay(995);
        int count = 0;
        long firstSubject = -1;
        while (replay.hasNext()) {
            final LifecycleEvent event = replay.next();
            if (count == 0) {
                firstSubject = event.subjectId();
            }
            count++;
        }

        assertEquals(5, count);
        assertEquals(995, firstSubject);
    }

    @Test
    void snapshotsAreWriteOnceAndDefensivelyCopied() {
        final InMemoryPersistence persistence = new InMemoryPersistence();
        final PolicyHandle handle = new TestPolicyHandle(3);
        final byte[] snapshot = {1, 2, 3};

        persistence.persistPolicySnapshot(handle, snapshot);
        snapshot[0] = 9;

        assertArrayEquals(new byte[] {1, 2, 3}, persistence.readPolicySnapshot(3));
        assertEquals(3, persistence.lastPersistedPolicyVersion());
        assertThrows(IllegalStateException.class, () -> persistence.persistPolicySnapshot(handle, new byte[] {4}));
    }

    private record TestPolicyHandle(long version) implements PolicyHandle {
        @Override public long hash64() { return 0; }
        @Override public byte[] hashSha256() { return Arrays.copyOf(new byte[32], 32); }
        @Override public long createdEpochNanos() { return 0; }
        @Override public long effectiveFromEpochNanos() { return 0; }
    }
}
