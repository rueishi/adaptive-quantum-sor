// Deterministic CUDA tactical optimizer MVP.
//
// This MVP scoring function is intentionally simple and bounded. Full cuOpt or
// custom kernel performance work is outside P2-TC-003.
#include "cuda_tactical_optimizer.h"

int sor_cuda_tactical_score(int venue_weight_bps, int fill_probability_bps, int toxicity_penalty_bps) {
    int score = venue_weight_bps + fill_probability_bps - toxicity_penalty_bps;
    if (score < 0) {
        return 0;
    }
    if (score > 10000) {
        return 10000;
    }
    return score;
}
