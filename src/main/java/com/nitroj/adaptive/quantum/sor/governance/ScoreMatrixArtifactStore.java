package com.nitroj.adaptive.quantum.sor.governance;

import com.nitroj.adaptive.quantum.sor.policy.robust.ScoreMatrix;

/**
 * Responsibility: persistence boundary for Phase 7 score-matrix evidence.
 */
public interface ScoreMatrixArtifactStore {
    String store(ScoreMatrix matrix);
}
