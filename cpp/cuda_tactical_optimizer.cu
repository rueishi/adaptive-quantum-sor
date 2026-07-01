/**
 * @file
 * @brief CPU-compatible implementation of the CUDA tactical scoring ABI.
 *
 * The current implementation is intentionally deterministic and CPU-compatible
 * so the native build can run in environments without a production CUDA
 * optimizer kernel.
 */

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
