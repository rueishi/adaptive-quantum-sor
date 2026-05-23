package com.nitroj.sor.transport.aeron;

/** Validates supported Aeron channel URI shapes. */
public record AeronChannel(String uri) {
    public AeronChannel {
        if (!"aeron:ipc".equals(uri) && !uri.startsWith("aeron:udp?endpoint=")) {
            throw new IllegalArgumentException("unsupported Aeron channel URI: " + uri);
        }
    }

    public boolean ipc() {
        return "aeron:ipc".equals(uri);
    }
}
