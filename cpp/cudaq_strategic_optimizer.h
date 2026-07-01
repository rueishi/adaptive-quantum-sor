/**
 * @file
 * @brief C-callable CUDA-Q strategic optimizer ABI for venue subset selection.
 *
 * The ABI uses fixed integer fields and caller-owned arrays so Java native
 * bridges can pack QUBO inputs without sharing object ownership with C++.
 */

#pragma once

#include <cstdint>

/**
 * @brief Status values returned by sor_cudaq_strategic_optimize.
 */
enum SorStrategicStatus : std::int32_t {
    SOR_STRATEGIC_OK = 0,
    SOR_STRATEGIC_INVALID_INPUT = 1,
    SOR_STRATEGIC_INVALID_RESULT = 2
};

/**
 * @brief Caller-owned QUBO input for strategic venue subset selection.
 */
struct SorStrategicQuboInput {
    ///< Number of candidate venues. Current exhaustive implementation supports 1..30.
    std::int32_t venue_count;

    ///< Inclusive lower bound for selected venue count.
    std::int32_t min_subset_size;
    ///< Inclusive upper bound for selected venue count.
    std::int32_t max_subset_size;

    ///< Caller-owned array of venue_count linear coefficients. Required.
    const std::int32_t* linear_coefficients;

    ///< Optional caller-owned venue_count x venue_count row-major pair matrix.
    const std::int32_t* pair_coefficients;
};

/**
 * @brief Output buffer populated with the selected venue subset.
 */
struct SorStrategicQuboOutput {
    ///< Number of selected venues written to selected_venue_ids.
    std::int32_t selected_count;

    ///< Selected venue IDs in ascending venue order. Capacity is 32 entries.
    std::int16_t selected_venue_ids[32];

    ///< Objective energy for the selected subset. Lower energy is better.
    std::int64_t objective_energy;
};

/**
 * @brief Computes the minimum-energy subset satisfying the input size bounds.
 *
 * On success, output is fully overwritten. The deterministic tie-break chooses
 * the lowest bit-mask among equal-energy subsets, which gives reproducible
 * Java/native golden tests.
 *
 * @param input caller-owned QUBO input
 * @param output caller-owned output buffer populated on success
 * @return SOR_STRATEGIC_OK, SOR_STRATEGIC_INVALID_INPUT, or SOR_STRATEGIC_INVALID_RESULT
 */
extern "C" SorStrategicStatus sor_cudaq_strategic_optimize(
        const SorStrategicQuboInput* input,
        SorStrategicQuboOutput* output);
