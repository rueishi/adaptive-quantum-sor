package com.nitroj.sor.core.ml;

import com.nitroj.sor.core.model.ModelSignalState;
import com.nitroj.sor.core.stats.FillQualityStats;
import com.nitroj.sor.core.stats.RegimeState;
import com.nitroj.sor.core.stats.SlippageStats;
import com.nitroj.sor.core.stats.ToxicityStats;
import com.nitroj.sor.core.stats.VenueStatsState;

/**
 * Responsibility: define the Java Phase 1 model-signal generation contract.
 *
 * <p>Role in system: optimizers consume model signals without knowing whether
 * the producer is a deterministic stub, future batch model, or external
 * research pipeline.</p>
 *
 * <p>Relationships: {@link MlSignalModelStub} implements this interface and
 * writes {@link ModelSignalState} from feature stats.</p>
 *
 * <p>Lifecycle: implementations are warm-path components called after feature
 * aggregation and before optimizer cycles.</p>
 *
 * <p>Design intent: keep Python and real ML training outside the Phase 1
 * runtime while preserving an explicit model boundary.</p>
 */
public interface MlSignalModel {
    /**
     * Generates bounded deterministic signals from current stats.
     */
    ModelSignalState generate(
            VenueStatsState venueStats,
            FillQualityStats fillQualityStats,
            ToxicityStats toxicityStats,
            SlippageStats slippageStats,
            RegimeState regimeState
    );
}
