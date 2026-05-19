// Deterministic CUDA tactical optimizer MVP header.
//
// A later build profile may compile this with nvcc. The Java Adaptive Quantum SOR tests exercise
// the same ABI through deterministic bridge modes so CUDA hardware is optional.
#pragma once

#include "tactical_optimizer_api.h"

int sor_cuda_tactical_score(int venue_weight_bps, int fill_probability_bps, int toxicity_penalty_bps);
