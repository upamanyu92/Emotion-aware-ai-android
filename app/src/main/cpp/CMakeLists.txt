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

# When integrating llama.cpp, add its source files here:
# add_subdirectory(llama.cpp)
# target_link_libraries(llm_engine llama ${log-lib} android)
