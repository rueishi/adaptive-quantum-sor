// Unit tests for the Phase 2 tactical optimizer C ABI.
//
// These tests intentionally use standard assert instead of an external test
// framework so the Adaptive Quantum SOR native build remains dependency-free.
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
