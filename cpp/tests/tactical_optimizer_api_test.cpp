/**
 * @file
 * @brief Tests the tactical optimizer C ABI.
 *
 * Run through CTest or the Gradle native build before changing tactical
 * optimizer status codes or function signatures. The test covers symbol
 * reachability, successful validation, null/empty buffer rejection, and timeout
 * reporting.
 */
#include "../tactical_optimizer_api.h"

#include <cassert>
#include <cstdint>

int main() {
    uint8_t input[8] = {0};
    uint8_t output[8] = {0};

    assert(sor_tactical_echo(7) == 7);
    assert(sor_tactical_optimize(input, 8, output, 8, 1000) == SOR_NATIVE_OK);
    assert(sor_tactical_optimize(nullptr, 8, output, 8, 1000) == SOR_NATIVE_INVALID_INPUT);
    assert(sor_tactical_optimize(input, 0, output, 8, 1000) == SOR_NATIVE_INVALID_INPUT);
    assert(sor_tactical_optimize(input, 8, output, 8, 0) == SOR_NATIVE_TIMEOUT);
    return 0;
}
