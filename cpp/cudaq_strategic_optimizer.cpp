#include "cudaq_strategic_optimizer.h"

#include <climits>

namespace {
int popcount(int mask) {
    int count = 0;
    while (mask != 0) {
        count += mask & 1;
        mask >>= 1;
    }
    return count;
}
}

extern "C" SorStrategicStatus sor_cudaq_strategic_optimize(
        const SorStrategicQuboInput* input,
        SorStrategicQuboOutput* output) {
    if (input == nullptr || output == nullptr || input->linear_coefficients == nullptr) {
        return SOR_STRATEGIC_INVALID_INPUT;
    }
    if (input->venue_count <= 0 || input->venue_count > 30
            || input->min_subset_size < 0
            || input->max_subset_size < input->min_subset_size
            || input->max_subset_size > input->venue_count) {
        return SOR_STRATEGIC_INVALID_INPUT;
    }

    long long best_energy = LLONG_MAX;
    int best_mask = 0;
    const int max_mask = 1 << input->venue_count;
    for (int mask = 0; mask < max_mask; ++mask) {
        const int selected = popcount(mask);
        if (selected < input->min_subset_size || selected > input->max_subset_size) {
            continue;
        }
        long long energy = 0;
        for (int venue = 0; venue < input->venue_count; ++venue) {
            if ((mask & (1 << venue)) != 0) {
                energy += input->linear_coefficients[venue];
            }
        }
        if (energy < best_energy || (energy == best_energy && mask < best_mask)) {
            best_energy = energy;
            best_mask = mask;
        }
    }

    if (best_energy == LLONG_MAX) {
        return SOR_STRATEGIC_INVALID_RESULT;
    }

    output->selected_count = 0;
    for (int venue = 0; venue < input->venue_count; ++venue) {
        if ((best_mask & (1 << venue)) != 0) {
            output->selected_venue_ids[output->selected_count++] = static_cast<std::int16_t>(venue);
        }
    }
    output->objective_energy = best_energy;
    return SOR_STRATEGIC_OK;
}
