package com.nitroj.sor.api.spi;

/**
 * Responsibility: integrator-supplied venue connectivity boundary.
 *
 * <p>Role in system: exposes ring writers for outbound child orders and uses
 * callbacks to deliver fills or rejects back to the engine.</p>
 *
 * <p>Relationships: owns {@link RingWriter}, {@link FillReport}, and
 * {@link RejectReport} traffic for venues.</p>
 *
 * <p>Lifecycle: configured before engine build; callback is injected by the
 * engine before venue traffic starts.</p>
 *
 * <p>Design intent: prevent direct hot-path calls from routing into venue I/O.</p>
 */
public interface VenueAdapter {
    /**
     * Returns the non-blocking child-order writer for a venue.
     *
     * <p>Control-plane method, not hot-path. The returned writer's
     * {@link RingWriter#offer(ChildOrderRef)} method is hot-path.</p>
     *
     * @param venueId venue identifier
     * @return venue-specific ring writer
     */
    RingWriter childOrderRingWriter(int venueId);

    /**
     * Injects the engine callback used for inbound venue reports.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param callback engine-owned callback
     */
    void callback(VenueAdapterCallback callback);

    /**
     * Responsibility: engine-owned callback for venue reports.
     *
     * <p>Role in system: implemented by `sor-core`; adapters call it from their
     * own threads when fills or rejects arrive.</p>
     */
    interface VenueAdapterCallback {
        /**
         * Delivers a fill report.
         *
         * <p>Hot-path method. Must not allocate. Must not block.</p>
         */
        void deliverFill(FillReport report);

        /**
         * Delivers a reject report.
         *
         * <p>Hot-path method. Must not allocate. Must not block.</p>
         */
        void deliverReject(RejectReport report);
    }
}
