package com.nitroj.sor.optnative;

import java.util.Set;

/** Panama FFM linker for batch allocation native functions. */
public final class BatchAllocatorLinker extends PanamaLibraryLinker {
    public BatchAllocatorLinker() {
        super("batch", "libcuda_tactical_optimizer.a", Set.of("batch_allocate", "cuda_tactical_optimize"));
    }
}
