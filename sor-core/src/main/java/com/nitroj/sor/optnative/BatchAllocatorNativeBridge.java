package com.nitroj.sor.optnative;

import com.nitroj.adaptive.quantum.sor.optimizer.batch.BatchAllocationBackend;
import com.nitroj.adaptive.quantum.sor.optimizer.batch.BatchAllocationBackendResult;
import com.nitroj.adaptive.quantum.sor.optimizer.batch.BatchAllocationProblem;
import com.nitroj.adaptive.quantum.sor.optimizer.batch.BatchAllocationStatus;
import com.nitroj.adaptive.quantum.sor.optimizer.batch.BatchVenueAllocator;

/**
 * Responsibility: placeholder native/cuOpt boundary for Phase 6 batch allocation.
 *
 * <p>Role in system: mirrors existing native optimizer bridge patterns while
 * keeping Phase 6 safe before a real native batch allocator is added.</p>
 */
public final class BatchAllocatorNativeBridge implements BatchAllocationBackend {
    private final BatchVenueAllocator delegate;

    public BatchAllocatorNativeBridge() {
        this.delegate = null;
    }

    public BatchAllocatorNativeBridge(final BatchVenueAllocator delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

    public boolean backendAvailable() {
        return delegate != null;
    }

    @Override
    public BatchAllocationBackendResult allocate(final BatchAllocationProblem problem) {
        if (!backendAvailable()) {
            return BatchAllocationBackendResult.failure(BatchAllocationStatus.BACKEND_UNAVAILABLE, "backend unavailable");
        }
        return BatchAllocationBackendResult.success(delegate.allocate(problem));
    }
}
