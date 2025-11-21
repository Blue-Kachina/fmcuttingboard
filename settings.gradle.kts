pluginManagement {
    repositories {
        // Primary source for Gradle plugins, including org.jetbrains.intellij
        gradlePluginPortal()
        // Use the canonical Plugin Portal Maven endpoint explicitly to avoid mirror-related resolution issues
        maven("https://plugins.gradle.org/m2")
        // Keep Maven Central as a generic fallback (not used for Plugin Portal, but harmless here)
        mavenCentral()
    }
    // Centralize plugin versions here so build scripts don't hardcode them
    plugins {
        // Use IntelliJ Platform Gradle Plugin 2.x — required for IntelliJ Platform 2024.2+
        id("org.jetbrains.intellij.platform") version "2.0.1"
    }
}

plugins {
    // Enable automatic JDK toolchain resolution/download via Foojay for Gradle toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "FMCuttingBoard"
