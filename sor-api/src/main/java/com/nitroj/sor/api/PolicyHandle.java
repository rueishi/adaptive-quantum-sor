package com.nitroj.sor.api;

/**
 * Responsibility: opaque handle to the currently active policy.
 *
 * <p>Role in system: integrators may compare handles for equality, read
 * identity fields, and pass handles to persistence, but they cannot reach the
 * internal `SorPolicy` or `HotRouteBook`.</p>
 *
 * <p>Relationships: returned by {@link SorEngine#activePolicy()} and emitted
 * indirectly by policy publication events.</p>
 *
 * <p>Lifecycle: created by engine implementations when policies are published
 * or recovered.</p>
 *
 * <p>Design intent: firewall policy internals from public API compatibility.</p>
 */
public interface PolicyHandle {
    /**
     * @return monotonically increasing policy version
     */
    long version();

    /**
     * @return fast 64-bit policy identity hash
     */
    long hash64();

    /**
     * @return defensive copy or immutable SHA-256 policy hash bytes
     */
    byte[] hashSha256();

    /**
     * @return policy creation timestamp in epoch nanoseconds
     */
    long createdEpochNanos();

    /**
     * @return timestamp from which this policy is effective
     */
    long effectiveFromEpochNanos();
}
