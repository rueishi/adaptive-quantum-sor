/**
 * @file
 * @brief Tests the CUDA tactical optimizer native scoring contract.
 *
 * Run through CTest or the Gradle native build before changing tactical
 * optimizer GPU entry points. The test covers basis-point addition,
 * toxicity-penalty subtraction, lower clamping, and upper clamping.
 *
 * These unit tests intentionally use standard assert instead of an external test
 * framework so the Adaptive Quantum SOR native build remains dependency-free.
 */
#include "../cuda_tactical_optimizer.h"

#include <cassert>

int main() {
    assert(sor_cuda_tactical_score(1000, 9000, 0) == 10000);
    assert(sor_cuda_tactical_score(500, 5000, 1000) == 4500);
    assert(sor_cuda_tactical_score(0, 100, 500) == 0);
    assert(sor_cuda_tactical_score(9000, 9000, 0) == 10000);
    return 0;
}
