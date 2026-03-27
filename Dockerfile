# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Android build environment
# JDK 17 + Android SDK (platform-35, build-tools-35.0.0, NDK 26.1, CMake 3.22.1)
# Supports Android 11–15 natively (minSdk=30, targetSdk=35 / API 30–35).
# Android 16 (API 36) devices run APKs built with targetSdk=35 via forward
# compatibility, so all Android 11–16 devices are covered.
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
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools/35.0.0:${PATH}"

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
        "platforms;android-35" \
        "build-tools;35.0.0" \
        "ndk;26.1.10909125" \
        "cmake;3.22.1"

# Create non-root user matching GitHub Actions runner (UID=1001, GID=121)
# This prevents permission issues when mounting the workspace into the container
RUN groupadd -g 121 runner && \
    useradd -m -u 1001 -g 121 -s /bin/bash runner

# Set up Gradle user home with correct ownership for non-root user
RUN mkdir -p /home/runner/.gradle && \
    chown -R runner:runner /home/runner/.gradle

ENV GRADLE_USER_HOME=/home/runner/.gradle

# Switch to non-root user
USER runner

WORKDIR /workspace
CMD ["/bin/bash"]
