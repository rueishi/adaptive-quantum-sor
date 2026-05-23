package com.nitroj.sor.core.intake;

import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lazily creates one SPSC inbound fill ring per venue. */
public final class InboundFillRings {
    private static final int DEFAULT_BYTES = 1024 * 128;
    private final Map<Integer, OneToOneRingBuffer> rings = new ConcurrentHashMap<>();

    public OneToOneRingBuffer ringForVenue(final int venueId) {
        return rings.computeIfAbsent(venueId, ignored -> new OneToOneRingBuffer(new UnsafeBuffer(
                ByteBuffer.allocateDirect(DEFAULT_BYTES + RingBufferDescriptor.TRAILER_LENGTH))));
    }
}
