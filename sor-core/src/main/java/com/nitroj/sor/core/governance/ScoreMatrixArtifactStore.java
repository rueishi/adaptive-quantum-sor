package com.nitroj.sor.core.governance;

import com.nitroj.sor.core.policy.robust.ScoreMatrix;

/**
 * Responsibility: persistence boundary for Phase 7 score-matrix evidence.
 */
public interface ScoreMatrixArtifactStore {
    String store(ScoreMatrix matrix);
}
