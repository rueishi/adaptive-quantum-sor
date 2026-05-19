package com.nitroj.adaptive.quantum.sor.optimizer.batch;

/** Stable status values for batch allocation backends. */
public enum BatchAllocationStatus {
    OK,
    BACKEND_UNAVAILABLE,
    TIMEOUT,
    INVALID_RESULT,
    NATIVE_FAILURE
}
