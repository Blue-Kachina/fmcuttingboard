plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "dev.fmcuttingboard"
version = providers.gradleProperty("pluginVersion").orNull ?: "0.0.1"

java {
    // IMPORTANT: IntelliJ Platform 2024.3 requires plugins to be compiled to Java 17 bytecode
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

// Ensure targetCompatibility that the IntelliJ Gradle plugin verifies is set to 17
java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>().configureEach {
    // Enforce Java 17 bytecode level
    options.release.set(17)
}

dependencies {
    // JNA for Windows native clipboard fallback — rely on IDE-bundled JNA at runtime
    // Use compileOnly to avoid bundling conflicting versions inside the plugin
    compileOnly("net.java.dev.jna:jna:5.14.0")
    compileOnly("net.java.dev.jna:jna-platform:5.14.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // For IntelliJ Platform test environment which may require JUnit 4 classes
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
