package com.nitroj.sor.sim.scenario;

/**
 * Responsibility: compact simulator-only dimensional configuration.
 *
 * <p>Role in system: replaces legacy {@code SorConfig} usage inside the new
 * simulator module. Simulator classes use this record to size dense arrays
 * without importing core runtime state or configuration classes.</p>
 *
 * <p>Relationships: catalogs, fee schedules, market data, venue behavior,
 * order injection, and scenario replay all share this configuration.</p>
 *
 * <p>Lifecycle: created at scenario setup and treated as immutable for the
 * duration of a simulator run.</p>
 *
 * <p>Design intent: keep simulator configuration explicit, primitive, and
 * independent from production class names so the simulator can behave like any
 * external integration.</p>
 */
public record SimConfig(int instrumentCount, int venueCount, int regimeCount, int urgencyCount) {
    public SimConfig {
        if (instrumentCount <= 0) {
            throw new IllegalArgumentException("instrumentCount must be positive");
        }
        if (venueCount <= 0) {
            throw new IllegalArgumentException("venueCount must be positive");
        }
        if (regimeCount <= 0) {
            throw new IllegalArgumentException("regimeCount must be positive");
        }
        if (urgencyCount <= 0) {
            throw new IllegalArgumentException("urgencyCount must be positive");
        }
    }

    /** Returns the default dimensions used by legacy scenario unit tests. */
    public static SimConfig defaults() {
        return new SimConfig(2, 5, 3, 2);
    }

    public void checkInstrument(final int instrumentId) {
        if (instrumentId < 0 || instrumentId >= instrumentCount) {
            throw new IndexOutOfBoundsException("instrumentId out of range: " + instrumentId);
        }
    }

    public void checkVenue(final int venueId) {
        if (venueId < 0 || venueId >= venueCount) {
            throw new IndexOutOfBoundsException("venueId out of range: " + venueId);
        }
    }

    public int cellIndex(final int instrumentId, final int venueId) {
        checkInstrument(instrumentId);
        checkVenue(venueId);
        return instrumentId * venueCount + venueId;
    }
}
