/**
 * @file
 * @brief Deterministic implementation of the tactical optimizer C ABI.
 *
 * This Phase 2 implementation validates ABI shape and timeout handling without
 * performing full routing optimization. It is compiled into the native
 * optimizer library and exercised by Panama bridge tests and native CTest
 * targets.
 */

#include "tactical_optimizer_api.h"

extern "C" int sor_tactical_echo(int value) {
    return value;
}

extern "C" int sor_tactical_optimize(const uint8_t* input_buffer,
                                      int input_length,
                                      uint8_t* output_buffer,
                                      int output_length,
                                      int64_t timeout_nanos) {
    if (input_buffer == nullptr || output_buffer == nullptr || input_length <= 0 || output_length <= 0) {
        return SOR_NATIVE_INVALID_INPUT;
    }
    if (timeout_nanos <= 0) {
        return SOR_NATIVE_TIMEOUT;
    }
    return SOR_NATIVE_OK;
}
