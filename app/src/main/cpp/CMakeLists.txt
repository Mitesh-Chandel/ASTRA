cmake_minimum_required(VERSION 3.22)

project(astra_ai LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Prebuilt llama.cpp / ggml libraries are stored in the app JNI directory.
set(LLAMA_ANDROID_LIB_DIR "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/${ANDROID_ABI}")

# Keep the headers separate from the prebuilt libraries so their source and
# version can be managed locally without embedding machine-specific paths.
if (DEFINED ENV{LLAMA_CPP_ROOT} AND NOT "$ENV{LLAMA_CPP_ROOT}" STREQUAL "")
    set(LLAMA_CPP_ROOT "$ENV{LLAMA_CPP_ROOT}" CACHE PATH
        "Path to a llama.cpp checkout compatible with the bundled native libraries" FORCE)
else()
    set(LLAMA_CPP_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../../../llama.cpp" CACHE PATH
        "Path to a llama.cpp checkout compatible with the bundled native libraries")
endif()
set(LLAMA_CPP_INCLUDE_DIR "${LLAMA_CPP_ROOT}/include")
set(GGML_PUBLIC_INCLUDE_DIR "${LLAMA_CPP_ROOT}/ggml/include")

if (NOT EXISTS "${LLAMA_CPP_INCLUDE_DIR}/llama.h")
    message(FATAL_ERROR
        "llama.cpp headers not found. Set LLAMA_CPP_ROOT to a compatible llama.cpp checkout.")
endif()

if (NOT EXISTS "${GGML_PUBLIC_INCLUDE_DIR}/ggml.h")
    message(FATAL_ERROR
        "ggml headers not found under LLAMA_CPP_ROOT. Use a complete llama.cpp checkout.")
endif()

# Import the existing prebuilt libraries as CMake targets.
add_library(ggml SHARED IMPORTED)
set_target_properties(ggml PROPERTIES
    IMPORTED_LOCATION "${LLAMA_ANDROID_LIB_DIR}/libggml.so"
)

add_library(ggml_base SHARED IMPORTED)
set_target_properties(ggml_base PROPERTIES
    IMPORTED_LOCATION "${LLAMA_ANDROID_LIB_DIR}/libggml-base.so"
)

add_library(ggml_cpu SHARED IMPORTED)
set_target_properties(ggml_cpu PROPERTIES
    IMPORTED_LOCATION "${LLAMA_ANDROID_LIB_DIR}/libggml-cpu.so"
)

add_library(llama SHARED IMPORTED)
set_target_properties(llama PROPERTIES
    IMPORTED_LOCATION "${LLAMA_ANDROID_LIB_DIR}/libllama.so"
)

add_library(llama_common SHARED IMPORTED)
set_target_properties(llama_common PROPERTIES
    IMPORTED_LOCATION "${LLAMA_ANDROID_LIB_DIR}/libllama-common.so"
)

# JNI bridge library.
add_library(astra_ai SHARED
    astra_ai.cpp
)

target_include_directories(astra_ai
    PRIVATE
        ${LLAMA_CPP_INCLUDE_DIR}
        ${GGML_PUBLIC_INCLUDE_DIR}
)

target_link_libraries(astra_ai
    PRIVATE
        llama
        llama_common
        ggml
        ggml_cpu
        ggml_base
        android
        log
)
