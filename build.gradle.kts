plugins {
    id("java")
    // Version is managed centrally in settings.gradle.kts -> pluginManagement.plugins
    id("org.jetbrains.intellij.platform")
    // Grammar-Kit for generating the lexer from JFlex (Phase 2.2)
    id("org.jetbrains.grammarkit")
    // Generates <change-notes> for plugin.xml from CHANGELOG.md
    id("org.jetbrains.changelog")
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
        // Instrumentation dependencies (e.g., @NotNull assertions) are now resolved
        // automatically by the plugin; instrumentationTools() was removed in 2.x.
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

// Read CHANGELOG.md and expose entries keyed by version (matches [x.y.z] headers)
changelog {
    version.set(providers.gradleProperty("pluginVersion"))
    repositoryUrl.set("https://github.com/Blue-Kachina/fmcuttingboard")
    // This project doesn't use Added/Changed/etc. grouping in every entry; don't require it.
    groups.empty()
}

tasks.patchPluginXml {
    // Set the minimum compatible IDE build.
    // 242 = IntelliJ Platform 2024.2 baseline, which is the first version running on Java 21.
    // Our plugin targets Java 21 bytecode, so 2024.2 is the earliest compatible IDE.
    sinceBuild.set("242")

    // Do NOT cap the upper build. Simply never calling untilBuild.set(...) omits the
    // until-build attribute entirely, allowing the plugin to load on newer IDEs (e.g., 25x)
    // unless there are API breaks. (Setting untilBuild.set("") used to be silently tolerated,
    // but the current Plugin Verifier rejects the resulting until-build="" as an invalid value.)

    // Pull <change-notes> from the CHANGELOG.md entry matching the current pluginVersion,
    // falling back to the Unreleased section if that version hasn't been given its own heading yet.
    changeNotes.set(providers.gradleProperty("pluginVersion").map { pluginVersion ->
        with(changelog) {
            renderItem(
                (getOrNull(pluginVersion) ?: getUnreleased())
                    .withHeader(false)
                    .withEmptySections(false),
                org.jetbrains.changelog.Changelog.OutputType.HTML,
            )
        }
    })
}

// Plugin Verifier and signing configuration for Marketplace publishing.
intellijPlatform {
    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        // Sourced from CI secrets / local env vars — never commit these values.
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
    }
}

tasks.runIde {
    // Provide reasonable default heap for early development
    jvmArgs = listOf(
        "-Xmx1g",
        // Workaround: disable the bundled Gradle plugin in the runIde sandbox to avoid
        // a startup crash observed in 2024.3 where GradleJvmSupportMatrix fails parsing
        // JavaVersion "25" (see resources/startup_runIde.log). This does NOT affect our
        // plugin’s functionality and only applies to the runIde task.
        // Remove this flag once the upstream issue is fixed to re-enable Gradle features.
        "-Didea.plugins.disabled=com.intellij.gradle"
    )
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

// Convenience task to build distributable plugin artifact
// Explicit Task type to satisfy Kotlin DSL type inference in some environments
tasks.register<Task>("releasePlugin") {
    group = "build"
    description = "Builds the plugin distribution zip for release (outputs to build/distributions)"
    dependsOn("buildPlugin")
}

// The IDE's searchable options builder attempts to materialize settings UIs headlessly
// and may crash for lightweight custom languages. It's not required for running or
// publishing the plugin, so disable it to keep CI and local builds stable.
// If you later add searchable options, remove this block.
tasks.named("buildSearchableOptions") {
    enabled = false
}

// ===== Phase 2.2: Lexer Generation & Integration =====
val generatedDir = layout.buildDirectory.dir("generated-src/grammarkit")

tasks.register<org.jetbrains.grammarkit.tasks.GenerateLexerTask>("generateFileMakerCalculationLexer") {
    description = "Generates the FileMaker Calculation JFlex lexer"
    sourceFile.set(file("src/main/java/dev/fmcuttingboard/language/filemaker-calculation.flex"))
    // GrammarKit 2022.3.2 expects targetDir (as String) + targetClass to derive targetFile
    targetDir.set("build/generated-src/grammarkit/dev/fmcuttingboard/language")
    targetClass.set("_FileMakerCalculationLexer")
    purgeOldFiles.set(true)
}

sourceSets {
    main {
        java {
            srcDir(generatedDir)
        }
        // No extra resources srcDir here: nothing in the shipped plugin reads anything from
        // the repo-root `resources/` folder at runtime. That folder is human reference material
        // (captured samples, a curated function list) and previously leaked a whole vendored
        // third-party repo into the plugin artifact via a blanket srcDir("resources").
    }
}

// Ensure lexer is generated before compilation
tasks.withType<JavaCompile>().configureEach {
    dependsOn("generateFileMakerCalculationLexer")
}
