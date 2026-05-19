// Phase 2 deterministic native tactical optimizer API stub.
//
// The Gradle Adaptive Quantum SOR build does not compile this file yet; it documents the native
// side of the bridge and can be compiled by a later CMake/CUDA profile.
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
