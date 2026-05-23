package com.nitroj.sor.optnative;

import java.util.Set;

/** Panama FFM linker for the strategic optimizer library. */
public final class StrategicOptimizerLinker extends PanamaLibraryLinker {
    public StrategicOptimizerLinker() {
        super("strategic", "libcudaq_strategic_optimizer.so", Set.of("cudaq_optimize_subset"));
    }
}
