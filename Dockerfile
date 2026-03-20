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

# Create non-root user matching GitHub Actions runner (UID=1001, GID=121)
# This prevents permission issues when mounting the workspace into the container
RUN groupadd -g 121 runner && \
    useradd -m -u 1001 -g 121 -s /bin/bash runner

# Pre-download Gradle distribution to avoid runtime download as non-root user
# This prevents permission issues when Gradle wrapper tries to download at runtime
RUN mkdir -p /tmp/gradle-init && \
    cd /tmp/gradle-init && \
    echo 'distributionUrl=https\\://services.gradle.org/distributions/gradle-8.7-bin.zip' > gradle-wrapper.properties && \
    curl -fsSL https://services.gradle.org/distributions/gradle-8.7-bin.zip -o gradle-8.7-bin.zip && \
    unzip -q gradle-8.7-bin.zip && \
    mkdir -p /home/runner/.gradle/wrapper/dists/gradle-8.7-bin && \
    mv gradle-8.7 /home/runner/.gradle/wrapper/dists/gradle-8.7-bin/ && \
    cd / && \
    rm -rf /tmp/gradle-init

# Set proper ownership for Gradle home
RUN chown -R runner:runner /home/runner/.gradle

# Set environment variable for Gradle user home
ENV GRADLE_USER_HOME=/home/runner/.gradle

# Switch to non-root user
USER runner

WORKDIR /workspace
CMD ["/bin/bash"]
