/**
 * @file
 * @brief C-compatible tactical optimizer ABI consumed by the Java Panama bridge.
 *
 * The ABI accepts primitive byte buffers only; Java object graphs must never
 * cross this boundary. Status values must remain aligned with
 * com.nitroj.sor.optnative.TacticalOptimizerNativeStatus.
 */

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
/**
 * @brief Returns the supplied value for native symbol-resolution smoke tests.
 *
 * @param value diagnostic value to echo
 * @return the same value
 */
int sor_tactical_echo(int value);

/**
 * @brief Optimizes one tactical routing input buffer into an output buffer.
 *
 * The input and output buffers are caller-owned and must remain valid for the
 * duration of the call. Length arguments are byte counts. A non-positive
 * timeout returns SOR_NATIVE_TIMEOUT.
 *
 * @param input_buffer encoded tactical optimizer input buffer
 * @param input_length input buffer length in bytes
 * @param output_buffer caller-owned output buffer
 * @param output_length output buffer length in bytes
 * @param timeout_nanos positive per-call budget in nanoseconds
 * @return one of the SOR_NATIVE_* status codes
 */
int sor_tactical_optimize(const uint8_t* input_buffer,
                         int input_length,
                         uint8_t* output_buffer,
                         int output_length,
                         int64_t timeout_nanos);
}
