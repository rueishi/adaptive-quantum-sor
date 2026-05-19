// JNI wrapper for the Phase 2 tactical optimizer ABI.
//
// Java tests load the Gradle-built shared library and call these functions to
// prove the JVM can cross into native code rather than only exercising a Java
// simulation of the native bridge.
#include "tactical_optimizer_api.h"

#include <jni.h>

extern "C" JNIEXPORT jint JNICALL
Java_com_nitroj_adaptive_quantum_sor_nativebridge_JniTacticalOptimizerNativeBridge_echoNative(
        JNIEnv*,
        jobject,
        jint value) {
    return sor_tactical_echo(value);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_nitroj_adaptive_quantum_sor_nativebridge_JniTacticalOptimizerNativeBridge_optimizeStatusNative(
        JNIEnv* env,
        jobject,
        jobject input_buffer,
        jobject output_buffer,
        jlong timeout_nanos) {
    auto* input = static_cast<uint8_t*>(env->GetDirectBufferAddress(input_buffer));
    auto* output = static_cast<uint8_t*>(env->GetDirectBufferAddress(output_buffer));
    const auto input_length = static_cast<int>(env->GetDirectBufferCapacity(input_buffer));
    const auto output_length = static_cast<int>(env->GetDirectBufferCapacity(output_buffer));
    return sor_tactical_optimize(input, input_length, output, output_length, timeout_nanos);
}
