package com.nitroj.sor.codec.v1;

import java.util.Arrays;

/** Immutable protocol v1 fixture message. */
public record SorProtocolMessage(SorMessageType type, long[] longs, int[] ints, String text) {
    public SorProtocolMessage {
        longs = Arrays.copyOf(longs, longs.length);
        ints = Arrays.copyOf(ints, ints.length);
        text = text == null ? "" : text;
        if (text.length() > SorProtocolV1Codec.MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("variable-length string exceeds max length " + SorProtocolV1Codec.MAX_TEXT_LENGTH);
        }
    }

    @Override public long[] longs() { return Arrays.copyOf(longs, longs.length); }
    @Override public int[] ints() { return Arrays.copyOf(ints, ints.length); }
}
