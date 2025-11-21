pluginManagement {
    repositories {
        // Primary source for Gradle plugins, including org.jetbrains.intellij
        gradlePluginPortal()
        // Use the canonical Plugin Portal Maven endpoint explicitly to avoid mirror-related resolution issues
        maven("https://plugins.gradle.org/m2")
        // Keep Maven Central as a generic fallback (not used for Plugin Portal, but harmless here)
        mavenCentral()
    }
}

plugins {
    // Enable automatic JDK toolchain resolution/download via Foojay for Gradle toolchains
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "FMCuttingBoard"
