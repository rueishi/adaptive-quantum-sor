package com.nitroj.sor.core.intake;

import com.nitroj.sor.api.ParentOrderRequest;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.ringbuffer.RingBufferDescriptor;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/** Agrona-backed parent-order intake ring with slot-count backpressure. */
public final class ParentOrderRing {
    private static final int MESSAGE_TYPE_ID = 1;
    private static final int MESSAGE_LENGTH = Long.BYTES * 4 + Integer.BYTES * 3;

    private final int capacity;
    private final AtomicInteger usedSlots = new AtomicInteger();
    private final ManyToOneRingBuffer ringBuffer;
    private final MutableDirectBuffer writeBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(MESSAGE_LENGTH));
    private final MessageHandler drainHandler = (msgTypeId, buffer, index, length) -> usedSlots.decrementAndGet();

    public ParentOrderRing(final int capacity) {
        if (Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("orderQueueCapacity must be a power-of-two for Agrona ring buffers: " + capacity);
        }
        this.capacity = capacity;
        final int byteCapacity = Math.max(4096, capacity * 128);
        this.ringBuffer = new ManyToOneRingBuffer(new UnsafeBuffer(
                ByteBuffer.allocateDirect(byteCapacity + RingBufferDescriptor.TRAILER_LENGTH)));
    }

    public ManyToOneRingBuffer ringBuffer() {
        return ringBuffer;
    }

    public int depth() {
        return usedSlots.get();
    }

    public boolean offer(final long parentOrderId, final ParentOrderRequest request, final long epochNanos) {
        int observed;
        do {
            observed = usedSlots.get();
            if (observed >= capacity) {
                return false;
            }
        } while (!usedSlots.compareAndSet(observed, observed + 1));

        synchronized (writeBuffer) {
            int offset = 0;
            writeBuffer.putLong(offset, parentOrderId); offset += Long.BYTES;
            writeBuffer.putInt(offset, request.instrumentId()); offset += Integer.BYTES;
            writeBuffer.putInt(offset, request.side()); offset += Integer.BYTES;
            writeBuffer.putLong(offset, request.quantity()); offset += Long.BYTES;
            writeBuffer.putInt(offset, request.urgencyId()); offset += Integer.BYTES;
            writeBuffer.putLong(offset, request.clientOrderId()); offset += Long.BYTES;
            writeBuffer.putLong(offset, epochNanos);
            if (!ringBuffer.write(MESSAGE_TYPE_ID, writeBuffer, 0, MESSAGE_LENGTH)) {
                usedSlots.decrementAndGet();
                return false;
            }
        }
        return true;
    }

    public int drain(final int limit) {
        final int read = ringBuffer.read(drainHandler, limit);
        return Math.max(0, read);
    }
}
