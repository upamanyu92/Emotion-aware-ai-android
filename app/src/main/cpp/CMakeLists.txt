cmake_minimum_required(VERSION 3.22.1)

project("llm_engine" VERSION 1.0.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for ARM
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+dotprod+fp16 -O3")
endif()

# Find Android log library
find_library(log-lib log)

# Build the shared JNI library
add_library(
    llm_engine
    SHARED
    llm_engine.cpp
)

# Link against Android log
target_link_libraries(
    llm_engine
    ${log-lib}
    android
)

# When integrating bitnet.cpp (Microsoft's 1-bit LLM runtime), add its
# source files here. bitnet.cpp is a fork of llama.cpp with optimised 1-bit
# kernels for ARM64 dotprod/i8mm.
#
# Step 1: clone bitnet.cpp into app/src/main/cpp/bitnet.cpp
#   git submodule add https://github.com/microsoft/BitNet.git app/src/main/cpp/bitnet.cpp
#
# Step 2: uncomment the two lines below and remove the stub add_library block:
# add_subdirectory(bitnet.cpp)
# target_link_libraries(llm_engine llama ${log-lib} android)
