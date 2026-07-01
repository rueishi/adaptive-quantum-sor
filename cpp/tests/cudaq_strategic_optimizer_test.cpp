/**
 * @file
 * @brief Tests the CUDA-Q strategic optimizer native subset-selection contract.
 *
 * Run through CTest or the Gradle native build before changing strategic
 * optimizer ABI or QUBO selection logic. The test covers linear coefficient
 * selection, pairwise penalties, deterministic output order, and invalid input
 * handling.
 *
 * These unit tests intentionally use standard assert instead of an external test
 * framework so the Adaptive Quantum SOR native build remains dependency-free.
 */
#include "cudaq_strategic_optimizer.h"

#include <cassert>

int main() {
    const std::int32_t linear[] = {-10, -50, -20};
    SorStrategicQuboInput input{};
    input.venue_count = 3;
    input.min_subset_size = 1;
    input.max_subset_size = 2;
    input.linear_coefficients = linear;
    input.pair_coefficients = nullptr;
    SorStrategicQuboOutput output{};

    assert(sor_cudaq_strategic_optimize(&input, &output) == SOR_STRATEGIC_OK);
    assert(output.selected_count == 2);
    assert(output.selected_venue_ids[0] == 1);
    assert(output.selected_venue_ids[1] == 2);
    assert(output.objective_energy == -70);

    const std::int32_t interaction_linear[] = {-100, -90, -80};
    const std::int32_t pair[] = {
        0, 1000, 0,
        1000, 0, 0,
        0, 0, 0
    };
    input.venue_count = 3;
    input.min_subset_size = 1;
    input.max_subset_size = 2;
    input.linear_coefficients = interaction_linear;
    input.pair_coefficients = pair;

    assert(sor_cudaq_strategic_optimize(&input, &output) == SOR_STRATEGIC_OK);
    assert(output.selected_count == 2);
    assert(output.selected_venue_ids[0] == 0);
    assert(output.selected_venue_ids[1] == 2);
    assert(output.objective_energy == -180);

    input.venue_count = 0;
    assert(sor_cudaq_strategic_optimize(&input, &output) == SOR_STRATEGIC_INVALID_INPUT);
    assert(sor_cudaq_strategic_optimize(nullptr, &output) == SOR_STRATEGIC_INVALID_INPUT);
    return 0;
}
