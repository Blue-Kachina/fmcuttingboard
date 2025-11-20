plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "dev.fmcuttingboard"
version = providers.gradleProperty("pluginVersion").orNull ?: "0.0.1"

java {
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(
            (providers.gradleProperty("javaVersion").orNull ?: "21").toInt()
        ))
    }
}

repositories {
    mavenCentral()
}

intellij {
    version.set(providers.gradleProperty("platformVersion").orNull ?: "2024.3")
    type.set(providers.gradleProperty("platformType").orNull ?: "IC")
}

tasks.patchPluginXml {
    // 243 = IntelliJ Platform 2024.3 baseline
    sinceBuild.set("243")
}

tasks.runIde {
    // Provide reasonable default heap for early development
    jvmArgs = listOf("-Xmx1g")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
