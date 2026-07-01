package com.nitroj.sor.api;

import com.nitroj.sor.api.spi.Clock;
import com.nitroj.sor.api.spi.MarketDataSource;
import com.nitroj.sor.api.spi.Persistence;
import com.nitroj.sor.api.spi.RiskProvider;
import com.nitroj.sor.api.spi.VenueAdapter;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Responsibility: public construction surface for a {@link SorEngine}.
 *
 * <p>Role in system: replaces direct construction of demo-coupled runtime
 * classes. P8-03 introduces the strict builder shell; P8-04 adds typed SPI
 * wiring methods and P8-06 connects it to the engine implementation.</p>
 *
 * <p>Relationships: returns the public {@link SorEngine} interface and never
 * exposes `sor-core` implementation classes.</p>
 *
 * <p>Lifecycle: created once, configured, then {@link #build()} is called. The
 * builder is discarded after build.</p>
 *
 * <p>Design intent: make missing framework dependencies explicit instead of
 * silently creating fake defaults.</p>
 */
public final class SorEngineBuilder {
    private final Set<String> missing = new LinkedHashSet<>();
    private SorConfig config;
    private MarketDataSource marketData;
    private VenueAdapter venueAdapter;
    private RiskProvider riskProvider;
    private Persistence persistence;
    private Clock clock;
    private Observability observability = Observability.noop();

    private SorEngineBuilder() {
        missing.add("config");
        missing.add("marketData");
        missing.add("venueAdapter");
        missing.add("riskProvider");
        missing.add("persistence");
        missing.add("clock");
    }

    /**
     * Creates a new builder with all required framework dependencies unset.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return new builder
     */
    public static SorEngineBuilder create() {
        return new SorEngineBuilder();
    }

    /**
     * Supplies public engine configuration.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param config public engine configuration
     * @return this builder
     */
    public SorEngineBuilder config(final SorConfig config) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        missing.remove("config");
        return this;
    }

    /**
     * Supplies the market data source SPI implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param source market data source implementation
     * @return this builder
     */
    public SorEngineBuilder marketData(final MarketDataSource source) {
        this.marketData = Objects.requireNonNull(source, "marketData source must not be null");
        missing.remove("marketData");
        return this;
    }

    /**
     * Supplies the venue adapter SPI implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param adapter venue adapter implementation
     * @return this builder
     */
    public SorEngineBuilder venueAdapter(final VenueAdapter adapter) {
        this.venueAdapter = Objects.requireNonNull(adapter, "venueAdapter must not be null");
        missing.remove("venueAdapter");
        return this;
    }

    /**
     * Supplies the synchronous risk provider SPI implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param provider synchronous risk provider implementation
     * @return this builder
     */
    public SorEngineBuilder riskProvider(final RiskProvider provider) {
        this.riskProvider = Objects.requireNonNull(provider, "riskProvider must not be null");
        missing.remove("riskProvider");
        return this;
    }

    /**
     * Supplies the persistence SPI implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param persistence persistence implementation
     * @return this builder
     */
    public SorEngineBuilder persistence(final Persistence persistence) {
        this.persistence = Objects.requireNonNull(persistence, "persistence must not be null");
        missing.remove("persistence");
        return this;
    }

    /**
     * Supplies the clock SPI implementation.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @param clock clock implementation
     * @return this builder
     */
    public SorEngineBuilder clock(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        missing.remove("clock");
        return this;
    }

    /**
     * Supplies the lightweight observability bridge used by the engine.
     *
     * <p>Control-plane method, not hot-path. If omitted, the engine uses a
     * no-op implementation.</p>
     *
     * @param observability observability implementation
     * @return this builder
     */
    public SorEngineBuilder observability(final Observability observability) {
        this.observability = Objects.requireNonNull(observability, "observability must not be null");
        return this;
    }

    /**
     * Builds an engine. P8-03 has no implementation module dependency, so a
     * complete configuration is not yet constructible through this shell.
     *
     * <p>Control-plane method, not hot-path. Throws with every missing
     * dependency named so integrators can fix wiring deliberately.</p>
     *
     * @return never in P8-03; later cards return an engine implementation
     */
    public SorEngine build() {
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required SOR engine dependencies: " + String.join(", ", missing));
        }
        try {
            final Class<?> impl = Class.forName("com.nitroj.sor.core.SorEngineImpl");
            return (SorEngine) impl.getMethod("create", SorEngineBuilder.class).invoke(null, this);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("SorEngine implementation failed during construction", ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("SorEngine implementation is supplied by sor-core in P8-06", ex);
        }
    }

    /**
     * Returns the configured engine settings for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured engine settings
     */
    public SorConfig config() { return config; }

    /**
     * Returns the configured market data source for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured market data source
     */
    public MarketDataSource marketData() { return marketData; }

    /**
     * Returns the configured venue adapter for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured venue adapter
     */
    public VenueAdapter venueAdapter() { return venueAdapter; }

    /**
     * Returns the configured risk provider for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured risk provider
     */
    public RiskProvider riskProvider() { return riskProvider; }

    /**
     * Returns the configured persistence provider for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured persistence provider
     */
    public Persistence persistence() { return persistence; }

    /**
     * Returns the configured clock for implementation construction.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured clock
     */
    public Clock clock() { return clock; }

    /**
     * Returns the configured observability bridge.
     *
     * <p>Control-plane method, not hot-path.</p>
     *
     * @return configured observability bridge
     */
    public Observability observability() { return observability; }
}
