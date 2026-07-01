package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.testkit.sim.scenario.*;
import com.nitroj.sor.testkit.sim.scenario.venues.*;

import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.spi.LifecycleEvent;
import com.nitroj.sor.api.spi.Persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory persistence implementation for simulator and scenario tests. */
public final class InMemoryPersistence implements Persistence {
    private final ArrayList<LifecycleEvent> events = new ArrayList<>();
    private final Map<Long, byte[]> snapshots = new ConcurrentHashMap<>();
    private long lastPolicyVersion;

    @Override
    public synchronized long appendLifecycleEvent(final LifecycleEvent event) {
        final LifecycleEvent copy = new LifecycleEvent().set(event.eventType(), event.subjectId(), event.epochNanos());
        copy.sequenceNumber(events.size() + 1L);
        events.add(copy);
        event.sequenceNumber(copy.sequenceNumber());
        return copy.sequenceNumber();
    }

    @Override
    public void persistPolicySnapshot(final PolicyHandle handle, final byte[] snapshotBytes) {
        final byte[] previous = snapshots.putIfAbsent(handle.version(), Arrays.copyOf(snapshotBytes, snapshotBytes.length));
        if (previous != null) {
            throw new IllegalStateException("policy snapshot already exists for version " + handle.version());
        }
        lastPolicyVersion = Math.max(lastPolicyVersion, handle.version());
    }

    @Override
    public byte[] readPolicySnapshot(final long policyVersion) {
        final byte[] bytes = snapshots.get(policyVersion);
        return bytes == null ? new byte[0] : Arrays.copyOf(bytes, bytes.length);
    }

    @Override
    public long lastPersistedPolicyVersion() {
        return lastPolicyVersion;
    }

    @Override
    public synchronized Iterator<LifecycleEvent> replay(final long sinceSequence) {
        return events.stream()
                .filter(event -> event.sequenceNumber() > sinceSequence)
                .map(event -> new LifecycleEvent()
                        .sequenceNumber(event.sequenceNumber())
                        .set(event.eventType(), event.subjectId(), event.epochNanos()))
                .toList()
                .iterator();
    }
}
