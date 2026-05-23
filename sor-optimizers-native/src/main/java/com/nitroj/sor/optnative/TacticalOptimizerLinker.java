package com.nitroj.sor.optnative;

import java.util.Set;

/** Panama FFM linker for the tactical optimizer library. */
public final class TacticalOptimizerLinker extends PanamaLibraryLinker {
    public TacticalOptimizerLinker() {
        super("tactical", "libtactical_optimizer_api.a", Set.of("tactical_optimize", "tactical_layout_version"));
    }
}
