plugins {
    id("java")
    // Version is managed centrally in settings.gradle.kts -> pluginManagement.plugins
    id("org.jetbrains.intellij.platform")
}

group = "dev.fmcuttingboard"
version = providers.gradleProperty("pluginVersion").orNull ?: "0.0.1"

java {
    // IMPORTANT: IntelliJ Platform 2024.3 requires plugins to target Java 21 bytecode
    toolchain {
        languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(
            (providers.gradleProperty("javaVersion").orNull ?: "21").toInt()
        ))
    }
}

repositories {
    mavenCentral()
    // Repositories required by the IntelliJ Platform Gradle Plugin 2.x
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure IntelliJ Platform distribution via dependencies (2.x DSL)
dependencies {
    // IntelliJ Platform distribution
    intellijPlatform {
        create(
            providers.gradleProperty("platformType").orNull ?: "IC",
            providers.gradleProperty("platformVersion").orNull ?: "2024.3",
        )
        // Required for instrumentation (e.g., @NotNull assertions) during build
        instrumentationTools()
    }

    // JNA for Windows native clipboard fallback — rely on IDE-bundled JNA at runtime
    // Use compileOnly to avoid bundling conflicting versions inside the plugin
    compileOnly("net.java.dev.jna:jna:5.14.0")
    compileOnly("net.java.dev.jna:jna-platform:5.14.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // For IntelliJ Platform test environment which may require JUnit 4 classes
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    // Ensure JUnit Platform launcher is present on Gradle 9 test runtime
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.patchPluginXml {
    // 243 = IntelliJ Platform 2024.3 baseline
    sinceBuild.set("243")
}

tasks.runIde {
    // Provide reasonable default heap for early development
    jvmArgs = listOf("-Xmx1g")
}

// Ensure targetCompatibility that the IntelliJ Gradle plugin verifies is set to 21
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

tasks.withType<JavaCompile>().configureEach {
    // Enforce Java 21 bytecode level
    options.release.set(21)
}

tasks.test {
    useJUnitPlatform()
}
