package com.nitroj.sor.api.spi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Responsibility: verifies SPI public methods document hot-path or
 * control-plane behavior.
 *
 * <p>Role in system: SPI implementations are written by integrators, so the
 * API source must state allocation and blocking expectations.</p>
 *
 * <p>Relationships: extends the P8-03 documentation guard to
 * `com.nitroj.sor.api.spi`.</p>
 *
 * <p>Lifecycle: source-level test run by `:sor-api:test`.</p>
 *
 * <p>Design intent: make performance contracts inseparable from the Java API.</p>
 */
class SpiHotPathDocumentationTest {
    private static final Path SPI_SOURCE = Path.of("sor-api/src/main/java/com/nitroj/sor/api/spi");
    private static final List<Class<?>> TYPES = List.of(
            MarketDataSource.class, MarketDataListener.class, Quote.class,
            VenueAdapter.class, VenueAdapter.VenueAdapterCallback.class, RingWriter.class,
            ChildOrderRef.class, FillReport.class, RejectReport.class,
            RiskProvider.class, RiskCheckRequest.class, RiskDecision.class,
            Persistence.class, LifecycleEvent.class, Clock.class
    );

    /**
     * Confirms public SPI methods carry contract language.
     *
     * @throws IOException if source cannot be read
     */
    @Test
    void spiMethodsDocumentContracts() throws IOException {
        for (Class<?> type : TYPES) {
            final String source = Files.readString(sourcePath(type));
            for (Method method : type.getDeclaredMethods()) {
                if (Modifier.isPublic(method.getModifiers()) && !method.isSynthetic()
                        && (method.getParameterCount() > 0
                        || method.getName().equals("systemNano")
                        || method.getName().equals("nanoTime")
                        || method.getName().equals("epochNanos"))) {
                    assertTrue(hasContract(source, method.getName()),
                            () -> type.getName() + "#" + method.getName() + " lacks hot-path/control-plane docs");
                }
            }
        }
    }

    private static Path sourcePath(final Class<?> type) {
        Class<?> top = type;
        while (top.getEnclosingClass() != null) {
            top = top.getEnclosingClass();
        }
        return SPI_SOURCE.resolve(top.getSimpleName() + ".java");
    }

    private static boolean hasContract(final String source, final String methodName) {
        final int index = Math.max(source.indexOf(" " + methodName + "("), source.indexOf(" " + methodName + "(final"));
        if (index < 0) {
            return false;
        }
        final String nearby = source.substring(Math.max(0, index - 700), index);
        return nearby.contains("Hot-path method") || nearby.contains("Control-plane method");
    }
}
