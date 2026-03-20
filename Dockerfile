# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Android build environment
# JDK 17 + Android SDK (platform-34, build-tools-34.0.0, NDK 26.1, CMake 3.22.1)
#
# Build image:
#   docker build -t android-builder .
#
# Run a Gradle command inside the container:
#   docker run --rm -v "$PWD:/workspace" android-builder ./gradlew assembleDebug
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/34.0.0:${PATH}"

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
        curl \
        unzip \
        wget \
        git \
        build-essential \
        cmake \
        ninja-build \
        python3 \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android SDK command-line tools (cmdline-tools r12.0)
RUN mkdir -p "${ANDROID_HOME}/cmdline-tools" && \
    curl -fsSL -o /tmp/cmdline-tools.zip \
        "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" && \
    unzip -q /tmp/cmdline-tools.zip -d "${ANDROID_HOME}/cmdline-tools" && \
    mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager \
        "platform-tools" \
        "platforms;android-34" \
        "build-tools;34.0.0" \
        "ndk;26.1.10909125" \
        "cmake;3.22.1"

# Pre-warm Gradle wrapper cache directory
RUN mkdir -p /root/.gradle/wrapper/dists

WORKDIR /workspace
CMD ["/bin/bash"]
