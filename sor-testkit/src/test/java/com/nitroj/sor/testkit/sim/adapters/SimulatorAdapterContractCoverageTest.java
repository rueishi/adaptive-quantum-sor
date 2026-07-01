package com.nitroj.sor.testkit.sim.adapters;

import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.RiskProvider;
import com.nitroj.sor.api.spi.VenueAdapter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies simulator implementations use the same public integration contracts as production adapters. */
final class SimulatorAdapterContractCoverageTest {
    @Test
    void simulatorRuntimeComponentsImplementPublicSorSpiDirectly() {
        assertTrue(MarketDataSource.class.isAssignableFrom(SimulatedMarketDataSource.class));
        assertTrue(VenueAdapter.class.isAssignableFrom(SimulatedVenueAdapter.class));
        assertTrue(RiskProvider.class.isAssignableFrom(SimulatedRiskProvider.class));
        assertTrue(Persistence.class.isAssignableFrom(InMemoryPersistence.class));
        assertTrue(Clock.class.isAssignableFrom(ManualClock.class));
    }

    @Test
    void simulatorSpiImplementationsLiveInAdaptersPackage() {
        assertEquals("com.nitroj.sor.testkit.sim.adapters", SimulatedMarketDataSource.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.adapters", SimulatedVenueAdapter.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.adapters", SimulatedRiskProvider.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.adapters", InMemoryPersistence.class.getPackageName());
        assertEquals("com.nitroj.sor.testkit.sim.adapters", ManualClock.class.getPackageName());
    }

    @Test
    void simulatorSourcesDoNotDeclareSimulatorOnlyInterfaceWrappers() throws IOException {
        try (var paths = Files.walk(Path.of("sor-testkit/src/main/java/com/nitroj/sor/testkit/sim"))) {
            final var offenders = paths
                    .filter(path -> path.getFileName().toString().matches("Sim[A-Z].*\\.java"))
                    .filter(path -> read(path).contains("interface "))
                    .toList();

            assertEquals(java.util.List.of(), offenders,
                    "simulator package must implement SOR public SPI contracts directly, not local wrapper interfaces");
        }
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
