/**
 * @file
 * @brief Deterministic CUDA tactical optimizer scoring ABI.
 *
 * The MVP score is expressed in basis points and clamped to [0, 10000] so Java
 * and native tests can compare results deterministically even when CUDA
 * hardware is optional. A later build profile may replace this with a
 * production CUDA kernel while preserving the exported scoring contract.
 */

#pragma once

#include "tactical_optimizer_api.h"

/**
 * @brief Computes a bounded tactical venue score.
 *
 * @param venue_weight_bps policy venue weight in basis points
 * @param fill_probability_bps expected fill probability contribution in basis points
 * @param toxicity_penalty_bps toxicity penalty in basis points
 * @return clamp(venue_weight_bps + fill_probability_bps - toxicity_penalty_bps, 0, 10000)
 */
int sor_cuda_tactical_score(int venue_weight_bps, int fill_probability_bps, int toxicity_penalty_bps);
