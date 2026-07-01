package com.nitroj.sor.core.ml;

import com.nitroj.sor.core.model.ModelSignalState;
import com.nitroj.sor.core.stats.FillQualityStats;
import com.nitroj.sor.core.stats.RegimeState;
import com.nitroj.sor.core.stats.SlippageStats;
import com.nitroj.sor.core.stats.ToxicityStats;
import com.nitroj.sor.core.stats.VenueStatsState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Responsibility: verify deterministic Phase 1 ML signal stub behavior.
 *
 * <p>Role in system: proves P1-TC-010 signal generation is bounded,
 * repeatable, versioned, and neutral when only default stats exist.</p>
 *
 * <p>Relationships: covers the {@link MlSignalModel} contract,
 * {@link MlSignalModelStub}, {@link ModelSignalVersion}, and
 * {@link ModelSignalState} public behavior.</p>
 *
 * <p>Lifecycle: executed as unit coverage for model-signal changes.</p>
 *
 * <p>Design intent: keep the ML boundary deterministic and Java-only in Phase 1.</p>
 */
final class MlSignalModelStubTest {
    @Test
    void signalsAreBoundedAndVersioned() {
        final Inputs inputs = new Inputs();
        inputs.fillStats.update(0, 20_000, 100L);
        inputs.toxicityStats.setToxicityBps(0, 20_000);
        inputs.slippageStats.setAvgSlippageBps(0, 20_000);
        inputs.venueStats.update(0, 0, 0, Integer.MAX_VALUE, 20_000, 20_000, 20_000, 0, 20_000);

        final ModelSignalState signals = new MlSignalModelStub(1, 1, 3).generate(
                inputs.venueStats, inputs.fillStats, inputs.toxicityStats, inputs.slippageStats, inputs.regimeState);

        assertEquals(ModelSignalVersion.PHASE1_STUB_V1, signals.modelVersion());
        assertTrue(signals.venueScoreBps(0, 0, 0) >= 0);
        assertTrue(signals.venueScoreBps(0, 0, 0) <= 10_000);
    }

    @Test
    void sameInputsProduceSameOutputs() {
        final Inputs inputs = new Inputs();
        inputs.fillStats.update(0, 8_000, 100L);
        inputs.toxicityStats.setToxicityBps(0, 100);
        inputs.slippageStats.setAvgSlippageBps(0, 50);
        inputs.venueStats.update(0, 0, 0, 1_000_000, 8_000, 100, 200, 0, 50);

        final MlSignalModelStub model = new MlSignalModelStub(1, 1, 3);
        final ModelSignalState first = model.generate(inputs.venueStats, inputs.fillStats, inputs.toxicityStats,
                inputs.slippageStats, inputs.regimeState);
        final ModelSignalState second = model.generate(inputs.venueStats, inputs.fillStats, inputs.toxicityStats,
                inputs.slippageStats, inputs.regimeState);

        assertEquals(first.venueScoreBps(0, 0, 0), second.venueScoreBps(0, 0, 0));
        assertEquals(first.modelVersion(), second.modelVersion());
    }

    @Test
    void insufficientDataProducesNeutralSignals() {
        final Inputs inputs = new Inputs();
        final ModelSignalState signals = new MlSignalModelStub(1, 1, 3).generate(
                inputs.venueStats, inputs.fillStats, inputs.toxicityStats, inputs.slippageStats, inputs.regimeState);

        assertEquals(5_000, signals.venueScoreBps(0, 0, 0));
        assertEquals(5_000, signals.venueScoreBps(0, 0, 1));
    }

    @Test
    void regimePenaltiesAndInvalidInputsAreHandled() {
        final Inputs inputs = new Inputs();
        inputs.regimeState.setRegime(0, RegimeState.THIN_BOOK);
        final ModelSignalState thin = new MlSignalModelStub(1, 1, 3).generate(
                inputs.venueStats, inputs.fillStats, inputs.toxicityStats, inputs.slippageStats, inputs.regimeState);
        assertEquals(4_500, thin.venueScoreBps(0, 0, 0));

        final ModelSignalState state = new ModelSignalState(1, 1, 1);
        state.setVenueScoreBps(0, 0, 0, 20_000);
        assertEquals(10_000, state.venueScoreBps(0, 0, 0));
        assertEquals("modelVersion must be non-negative", assertThrows(
                IllegalArgumentException.class,
                () -> state.setModelVersion(-1)
        ).getMessage());
        assertEquals("dimensions must be positive", assertThrows(
                IllegalArgumentException.class,
                () -> new MlSignalModelStub(0, 1, 1)
        ).getMessage());
        assertEquals("model inputs must not be null", assertThrows(
                IllegalArgumentException.class,
                () -> new MlSignalModelStub(1, 1, 1).generate(null, null, null, null, null)
        ).getMessage());
    }

    private static final class Inputs {
        final VenueStatsState venueStats = new VenueStatsState(1, 1, 3);
        final FillQualityStats fillStats = new FillQualityStats(1);
        final ToxicityStats toxicityStats = new ToxicityStats(1);
        final SlippageStats slippageStats = new SlippageStats(1);
        final RegimeState regimeState = new RegimeState(1);
    }
}
