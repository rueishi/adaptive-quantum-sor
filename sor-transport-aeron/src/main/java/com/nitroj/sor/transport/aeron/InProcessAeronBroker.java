package com.nitroj.sor.transport.aeron;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InProcessAeronBroker {
    private static final Map<String, AeronSorServer> SERVERS = new ConcurrentHashMap<>();

    private InProcessAeronBroker() {}

    static void register(final String channel, final AeronSorServer server) {
        SERVERS.put(channel, server);
    }

    static void unregister(final String channel, final AeronSorServer server) {
        SERVERS.remove(channel, server);
    }

    static AeronSorServer server(final String channel) {
        return SERVERS.get(channel);
    }
}
