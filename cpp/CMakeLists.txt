# CMake build for the native tactical optimizer.
#
# Gradle invokes this file through nativeConfigure/nativeBuild/nativeTest so the
# Java and native verification paths are owned by one build entrypoint. CUDA is
# enabled when a compiler is available; C++ ABI/layout tests always run.
cmake_minimum_required(VERSION 3.24)
project(adaptive_quantum_sor_native LANGUAGES CXX)

include(CTest)
include(CheckLanguage)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(tactical_optimizer_api STATIC
    tactical_optimizer_api.cpp
)

target_include_directories(tactical_optimizer_api PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_library(cudaq_strategic_optimizer SHARED
    cudaq_strategic_optimizer.cpp
)
target_include_directories(cudaq_strategic_optimizer PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

add_executable(tactical_optimizer_api_test
    tests/tactical_optimizer_api_test.cpp
)
target_link_libraries(tactical_optimizer_api_test PRIVATE tactical_optimizer_api)
add_test(NAME tactical_optimizer_api_test COMMAND tactical_optimizer_api_test)

add_executable(tactical_optimizer_layout_test
    tests/tactical_optimizer_layout_test.cpp
)
target_include_directories(tactical_optimizer_layout_test PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
add_test(NAME tactical_optimizer_layout_test COMMAND tactical_optimizer_layout_test)

add_executable(cudaq_strategic_optimizer_test
    tests/cudaq_strategic_optimizer_test.cpp
)
target_link_libraries(cudaq_strategic_optimizer_test PRIVATE cudaq_strategic_optimizer)
add_test(NAME cudaq_strategic_optimizer_test COMMAND cudaq_strategic_optimizer_test)

check_language(CUDA)
if(CMAKE_CUDA_COMPILER)
    enable_language(CUDA)
    add_library(cuda_tactical_optimizer STATIC
        cuda_tactical_optimizer.cu
    )
    target_include_directories(cuda_tactical_optimizer PUBLIC ${CMAKE_CURRENT_SOURCE_DIR})

    add_executable(cuda_tactical_optimizer_test
        tests/cuda_tactical_optimizer_test.cpp
    )
    target_link_libraries(cuda_tactical_optimizer_test PRIVATE cuda_tactical_optimizer)
    add_test(NAME cuda_tactical_optimizer_test COMMAND cuda_tactical_optimizer_test)
else()
    message(STATUS "CUDA compiler not found; skipping CUDA tactical optimizer native test target")
endif()
