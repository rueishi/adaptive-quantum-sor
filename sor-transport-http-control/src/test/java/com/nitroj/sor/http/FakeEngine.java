package com.nitroj.sor.http;

import com.nitroj.sor.api.OrderStatus;
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.PolicyHandle;
import com.nitroj.sor.api.Registration;
import com.nitroj.sor.api.SorEngine;
import com.nitroj.sor.api.SorEventListener;

import java.util.Optional;

/**
 * Provides a lightweight in-memory SorEngine/SorControlPlane test double for HTTP control-plane tests.
 *
 * <p>Use it from HTTP transport tests instead of booting the full core engine.</p>
 */
final class FakeEngine implements SorEngine {
    boolean ready;

    @Override public void warmup(final int syntheticOrderCount) { ready = true; }
    @Override public boolean isReady() { return ready; }
    @Override public long submitParentOrder(final ParentOrderRequest request) { return 1; }
    @Override public void cancelParentOrder(final long parentOrderId) {}
    @Override public Optional<OrderStatus> getOrderStatus(final long parentOrderId) { return Optional.empty(); }
    @Override public Registration registerListener(final SorEventListener listener) { return Registration.of(() -> {}); }
    @Override public PolicyHandle activePolicy() { return null; }
    @Override public void close() {}
}
