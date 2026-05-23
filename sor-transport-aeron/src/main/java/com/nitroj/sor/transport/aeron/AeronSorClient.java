package com.nitroj.sor.transport.aeron;

import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEvent;
import com.nitroj.sor.api.SorEventListener;
import com.nitroj.sor.api.VenueStatus;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/** Aeron-shaped SorEngine client over a channel URI. */
public final class AeronSorClient implements SorEngine {
    private final AeronChannel channel;
    private final List<SorEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile boolean connected;

    public AeronSorClient(final String channelUri) {
        this.channel = new AeronChannel(channelUri);
        reconnect();
    }

    @Override public void warmup(final int syntheticOrderCount) { server().engine().warmup(syntheticOrderCount); }
    @Override public boolean isReady() { return server().engine().isReady(); }

    @Override
    public long submitParentOrder(final ParentOrderRequest request) {
        final long id = server().submit(request);
        emit(new SorEvent.RouteDecided(id, activePolicy().version(), activePolicy().hash64(), 0, 0, request.quantity(), System.nanoTime()));
        return id;
    }

    @Override public void cancelParentOrder(final long parentOrderId) { server().engine().cancelParentOrder(parentOrderId); }
    @Override public Optional<OrderStatus> getOrderStatus(final long parentOrderId) { return server().engine().getOrderStatus(parentOrderId); }
    @Override public Registration registerListener(final SorEventListener listener) { listeners.add(listener); return Registration.of(() -> listeners.remove(listener)); }
    @Override public PolicyHandle activePolicy() { return server().engine().activePolicy(); }
    @Override public void close() {}

    public void reconnect() {
        final boolean wasConnected = connected;
        connected = InProcessAeronBroker.server(channel.uri()) != null;
        if (wasConnected && !connected) {
            emit(new SorEvent.SessionStatusChanged(0, VenueStatus.OPEN, VenueStatus.CLOSED, System.nanoTime()));
        } else if (!wasConnected && connected) {
            emit(new SorEvent.SessionStatusChanged(0, VenueStatus.CLOSED, VenueStatus.OPEN, System.nanoTime()));
        }
    }

    private AeronSorServer server() {
        final AeronSorServer server = InProcessAeronBroker.server(channel.uri());
        if (server == null || !server.running()) {
            reconnect();
            throw new IllegalStateException("Aeron server unavailable for " + channel.uri());
        }
        connected = true;
        return server;
    }

    private void emit(final SorEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
