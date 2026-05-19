#pragma once

#include <cstdint>

enum SorStrategicStatus : std::int32_t {
    SOR_STRATEGIC_OK = 0,
    SOR_STRATEGIC_INVALID_INPUT = 1,
    SOR_STRATEGIC_INVALID_RESULT = 2
};

struct SorStrategicQuboInput {
    std::int32_t venue_count;
    std::int32_t min_subset_size;
    std::int32_t max_subset_size;
    const std::int32_t* linear_coefficients;
    const std::int32_t* pair_coefficients;
};

struct SorStrategicQuboOutput {
    std::int32_t selected_count;
    std::int16_t selected_venue_ids[32];
    std::int64_t objective_energy;
};

extern "C" SorStrategicStatus sor_cudaq_strategic_optimize(
        const SorStrategicQuboInput* input,
        SorStrategicQuboOutput* output);
