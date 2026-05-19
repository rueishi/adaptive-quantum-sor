// Phase 2 native tactical optimizer ABI.
//
// Purpose:
// - Defines stable C-compatible status codes and function signatures for the
//   Java tactical optimizer bridge.
// - Uses primitive buffers only; Java object graphs must not cross this API.
// - Mirrors com.nitroj.adaptive.quantum.sor.nativebridge.TacticalOptimizerNativeStatus.
#pragma once

#include <stdint.h>

#define SOR_NATIVE_OK 0
#define SOR_NATIVE_LIBRARY_MISSING 1
#define SOR_NATIVE_INVALID_INPUT 2
#define SOR_NATIVE_GPU_UNAVAILABLE 3
#define SOR_NATIVE_TIMEOUT 4
#define SOR_NATIVE_OUTPUT_INVALID 5
#define SOR_NATIVE_FAILURE 6

extern "C" {
int sor_tactical_echo(int value);
int sor_tactical_optimize(const uint8_t* input_buffer,
                         int input_length,
                         uint8_t* output_buffer,
                         int output_length,
                         int64_t timeout_nanos);
}
