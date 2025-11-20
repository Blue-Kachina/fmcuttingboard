plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "dev.fmcuttingboard"
version = providers.gradleProperty("pluginVersion").orNull ?: "0.0.1"

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(
            (providers.gradleProperty("javaVersion").orNull ?: "17").toInt()
        ))
    }
}

repositories {
    mavenCentral()
}

intellij {
    version.set(providers.gradleProperty("platformVersion").orNull ?: "2024.1")
    type.set(providers.gradleProperty("platformType").orNull ?: "IC")
}

tasks.patchPluginXml {
    sinceBuild.set("241")
}

tasks.runIde {
    // Provide reasonable default heap for early development
    jvmArgs = listOf("-Xmx1g")
}
