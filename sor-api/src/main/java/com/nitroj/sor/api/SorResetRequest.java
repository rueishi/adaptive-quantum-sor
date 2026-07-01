package com.nitroj.sor.api;

/**
 * Responsibility: immutable control-plane request for resetting engine state.
 *
 * <p>Role in system: carries reset mode, safety gates, and operator/scenario
 * reason text into {@link SorControlPlane#reset(SorResetRequest)}.</p>
 *
 * <p>Relationships: references {@link SorResetMode} and produces
 * {@link SorResetSummary} evidence.</p>
 *
 * <p>Lifecycle: created by scenario/recovery/control-plane code for one reset
 * attempt.</p>
 *
 * <p>Design intent: make destructive operations explicit and fail invalid
 * requests before state mutation.</p>
 *
 * @param mode reset mode
 * @param requireNoLiveOrders true to fail if working orders exist
 * @param reason human-readable reason for audit evidence
 */
public record SorResetRequest(SorResetMode mode, boolean requireNoLiveOrders, String reason) {
    /**
     * Validates reset intent before control-plane execution.
     */
    public SorResetRequest {
        if (mode == null) {
            throw new IllegalArgumentException("mode must not be null");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
    }
}
