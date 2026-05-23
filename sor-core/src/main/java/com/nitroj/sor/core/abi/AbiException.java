package com.nitroj.sor.core.abi;

/**
 * Responsibility: reports validation failures while reading a binary ABI
 * snapshot.
 *
 * <p>Role in system: keeps ABI corruption, version, and truncation failures
 * explicit rather than leaking low-level buffer exceptions.</p>
 *
 * <p>Relationships: thrown by {@link HotRouteBookAbiV1Reader}.</p>
 *
 * <p>Lifecycle: created only on cold recovery/control paths.</p>
 *
 * <p>Design intent: carry a stable reason code for tests and future recovery
 * handling.</p>
 */
public final class AbiException extends RuntimeException {
    private final String reason;

    /**
     * Creates an ABI exception.
     *
     * @param reason stable reason code
     * @param message diagnostic message
     */
    public AbiException(final String reason, final String message) {
        super(message);
        this.reason = reason;
    }

    /**
     * Returns the stable ABI failure reason.
     */
    public String reason() {
        return reason;
    }
}
