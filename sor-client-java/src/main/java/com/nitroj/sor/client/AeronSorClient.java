package com.nitroj.sor.client;

import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEventListener;

import java.util.Objects;
import java.util.Optional;

/**
 * Public Java SDK client for Adaptive Quantum SOR over the Aeron transport.
 *
 * <p>The client delegates to the transport module while exposing a stable SDK
 * entry point, lifecycle methods, listener registration, order submission, and
 * shutdown behavior for integrators.</p>
 */
public final class AeronSorClient implements SorEngine {
    private final SorClientConfig config;
    private final com.nitroj.sor.transport.aeron.AeronSorClient delegate;

    private AeronSorClient(final String channelUri, final SorClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.delegate = new com.nitroj.sor.transport.aeron.AeronSorClient(channelUri);
    }

    /**
     * Connects to a SOR server listening on the supplied Aeron channel URI.
     *
     * @param channelUri Aeron URI such as {@code aeron:ipc}
     * @param config SDK connection and warmup settings
     * @return connected SDK client facade
     */
    public static AeronSorClient connect(final String channelUri, final SorClientConfig config) {
        return new AeronSorClient(channelUri, config);
    }

    /**
     * Runs SDK-configured synthetic warmup against the connected server.
     */
    public void warmup() {
        warmup(config.warmupOrderCount());
    }

    /**
     * Submits a parent order through the connected SOR server.
     *
     * @param request validated parent order request
     * @return server-assigned parent order identifier
     */
    public long submit(final ParentOrderRequest request) {
        return submitParentOrder(request);
    }

    /**
     * Rechecks the underlying transport session and emits a session event on
     * connection state transitions.
     */
    public void reconnect() {
        delegate.reconnect();
    }

    /**
     * Returns the immutable configuration used to create this client.
     *
     * @return client configuration
     */
    public SorClientConfig config() {
        return config;
    }

    @Override public void warmup(final int syntheticOrderCount) {
        delegate.warmup(syntheticOrderCount);
    }

    @Override public boolean isReady() {
        return delegate.isReady();
    }

    @Override public long submitParentOrder(final ParentOrderRequest request) {
        return delegate.submitParentOrder(request);
    }

    @Override public void cancelParentOrder(final long parentOrderId) {
        delegate.cancelParentOrder(parentOrderId);
    }

    @Override public Optional<OrderStatus> getOrderStatus(final long parentOrderId) {
        return delegate.getOrderStatus(parentOrderId);
    }

    @Override public Registration registerListener(final SorEventListener listener) {
        return delegate.registerListener(listener);
    }

    @Override public PolicyHandle activePolicy() {
        return delegate.activePolicy();
    }

    @Override public void close() {
        delegate.close();
    }
}
