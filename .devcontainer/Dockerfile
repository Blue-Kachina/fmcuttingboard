# JDK 21 matches the toolchain required by build.gradle.kts (IntelliJ Platform 2024.3
# needs plugins built with Java 21). The Gradle version itself is not baked into the
# image on purpose - the wrapper (./gradlew) downloads the pinned 8.14 distribution on
# first run, so bumping gradle/wrapper/gradle-wrapper.properties is all that's needed
# later to pick up a newer Gradle without touching this image.
FROM eclipse-temurin:21-jdk-jammy

RUN useradd --create-home --shell /bin/bash gradle

ENV GRADLE_USER_HOME=/home/gradle/.gradle

# Pre-create and own the cache dir as root so Docker copies this ownership when it
# initializes the (empty) named volume mounted here at container start - otherwise
# the volume comes up root-owned and the gradle user below can't write to it.
RUN mkdir -p "$GRADLE_USER_HOME" && chown gradle:gradle "$GRADLE_USER_HOME"

USER gradle
WORKDIR /workspace
