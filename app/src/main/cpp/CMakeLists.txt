cmake_minimum_required(VERSION 3.22.1)

project("llm_engine" VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for ARM
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+dotprod+fp16 -O3")
endif()

# ── llama.cpp integration ─────────────────────────────────────────────────────
# llama.cpp is the canonical GGUF inference library. All models referenced in
# LlmOption.kt (SmolLM2, TinyLlama, Phi-3, Mistral, etc.) are GGUF-based and
# supported out of the box. We pin a specific tag for reproducible builds.

include(FetchContent)

# Disable all unused sub-projects before FetchContent pulls the source so the
# variables are set in the cache before llama.cpp's own CMakeLists.txt reads them.
set(LLAMA_BUILD_EXAMPLES    OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS       OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER      OFF CACHE BOOL "" FORCE)
set(GGML_METAL              OFF CACHE BOOL "" FORCE)   # Apple-only, not for Android
set(GGML_CUDA               OFF CACHE BOOL "" FORCE)   # No CUDA on Android
set(GGML_VULKAN             OFF CACHE BOOL "" FORCE)   # Disable GPU for initial release
set(GGML_OPENMP             OFF CACHE BOOL "" FORCE)   # Avoid OpenMP NDK issues
set(BUILD_SHARED_LIBS       OFF CACHE BOOL "" FORCE)   # Static link into llm_engine.so

FetchContent_Declare(
    llama_cpp
    GIT_REPOSITORY https://github.com/ggerganov/llama.cpp.git
    GIT_TAG        b4170
    GIT_SHALLOW    TRUE
    GIT_PROGRESS   FALSE
)
FetchContent_MakeAvailable(llama_cpp)

# Find Android log library
find_library(log-lib log)

# Build the shared JNI library
add_library(
    llm_engine
    SHARED
    llm_engine.cpp
)

# llama.cpp root contains llama.h; FetchContent populates llama_cpp_SOURCE_DIR
target_include_directories(llm_engine PRIVATE ${llama_cpp_SOURCE_DIR})

# Link against llama (which pulls in ggml transitively), Android log, and android
target_link_libraries(
    llm_engine
    llama
    ${log-lib}
    android
)
