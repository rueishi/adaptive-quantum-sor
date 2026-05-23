package com.nitroj.sor.codec.v1;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Deterministic fixed-width protocol v1 encoder/decoder. */
public final class SorProtocolV1Codec {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_TEXT_LENGTH = 64;
    private static final int MAGIC = 0x534f5231;

    private SorProtocolV1Codec() {}

    public static byte[] encode(final SorProtocolMessage message) {
        final byte[] text = message.text().getBytes(StandardCharsets.UTF_8);
        if (text.length > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("variable-length string exceeds max length " + MAX_TEXT_LENGTH);
        }
        final long[] longs = message.longs();
        final int[] ints = message.ints();
        final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES * 6 + Long.BYTES * longs.length + Integer.BYTES * ints.length + text.length)
                .order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(MAGIC)
                .putInt(SCHEMA_VERSION)
                .putInt(message.type().templateId())
                .putInt(longs.length)
                .putInt(ints.length)
                .putInt(text.length);
        for (long value : longs) {
            buffer.putLong(value);
        }
        for (int value : ints) {
            buffer.putInt(value);
        }
        buffer.put(text);
        return buffer.array();
    }

    public static SorProtocolMessage decode(final byte[] bytes) {
        final ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (buffer.getInt() != MAGIC) {
            throw new IllegalArgumentException("bad protocol magic");
        }
        final int version = buffer.getInt();
        if (version < SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schema version " + version);
        }
        final SorMessageType type = SorMessageType.fromTemplateId(buffer.getInt());
        final long[] longs = new long[buffer.getInt()];
        final int[] ints = new int[buffer.getInt()];
        final int textLength = buffer.getInt();
        if (textLength > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("variable-length string exceeds max length " + MAX_TEXT_LENGTH);
        }
        for (int i = 0; i < longs.length; i++) {
            longs[i] = buffer.getLong();
        }
        for (int i = 0; i < ints.length; i++) {
            ints[i] = buffer.getInt();
        }
        final byte[] text = new byte[textLength];
        buffer.get(text);
        return new SorProtocolMessage(type, longs, ints, new String(text, StandardCharsets.UTF_8));
    }
}
