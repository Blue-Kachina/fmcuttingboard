# FMCuttingBoard

<!-- CI Status Badges -->
![Build](https://github.com/Blue-Kachina/fmcuttingboard/actions/workflows/ci.yml/badge.svg)
![Tests](https://github.com/Blue-Kachina/fmcuttingboard/actions/workflows/ci.yml/badge.svg)
- A plugin for JetBrains IDEs
- When installed, the user will have a new menu item in their IDE's `Tools` menu
- The new menu item will be named the same as this plugin
- The new menu item will itself be a submenu:
  - Convert FileMaker Clipboard To XML 
  - Read Clipboard Into New XML File
  - Push Clipboard Into FileMaker (only enabled when XML file is currently active)

## Convert FileMaker Clipboard To XML
When this option is selected, we will attempt to read FileMaker content from the clipboard.
If we are successful in finding FileMaker content, then the clipboard will be replaced with it

## Read Clipboard Into New XML File
When this option is selected, we will attempt to read FileMaker content from the clipboard.  
If we are successful in finding FileMaker content, it will be written to file.
The file it's written to should be a file whose name is timestamped (perhaps using unix/epoch).
The file will be XML format.
That file will be stored within the project's `.fmCuttingBoard` directory (off of root).
In the event the directory doesn't exist at the time we need to create a file in it, then we will create the folder.
When creating the folder, we will always add a `.gitignore` to it as well.
The .gitigore will contain only a `*` character.

## Push Clipboard Into FileMaker
When this option is selected, we will attempt to populate the clipboard with content FileMaker will properly recognize.
In essence, this option is doing the opposite of what we're doing in `Read Clipboard Into New XML File`.
We're taking plain XML, and repackaging it into a new clipboard that we'll later be able to paste into FileMaker.
The trick will be in getting the format right.  
If the fmxmlsnippet represents database fields, then after running this command the user would be able to paste fields into FileMaker's Manage Database dialog.
If it contains an fmxmlsnippet of script steps, then we'd be able to paste Script Steps into FileMaker Script Workspace window, etc...

## Inspiration
Please review inspiration/GetFmClipboard.ps1.
It works similarly to how we will want the Convert FileMaker Clipboard To XML to work.


## Development

- Run the plugin in a sandbox IDE: `./gradlew runIde` (or `gradlew.bat runIde` on Windows)
- Run tests: `./gradlew test`
- The project uses JUnit 5 for unit tests and a GitHub Actions workflow to build and run tests on pushes/PRs.

## IntelliJ IDEA Setup

You don’t need any custom build configuration beyond what’s already in the Gradle files. IntelliJ IDEA will import the project as a Gradle‑based IntelliJ Platform Plugin project and knows how to build, run, and test it.

Follow these steps:

1) Prerequisites
- Install IntelliJ IDEA Community or Ultimate (2024.3 or newer recommended).
- Ensure JDK 21 is installed (the project targets Java 21 via Gradle toolchains).

2) Open/Import the Project
- In IntelliJ IDEA: File > Open… and select the project root (the folder containing `settings.gradle.kts`).
- When prompted, choose “Open as Project”. IDEA will detect and import the Gradle project automatically.

3) Gradle JVM and Toolchain
- In Settings > Build, Execution, Deployment > Build Tools > Gradle, set “Gradle JVM” to a JDK 21 installation.
- The project declares a Java toolchain (21) and enables Foojay toolchain resolver, so Gradle can auto‑download a matching JDK if it isn’t installed. Keeping the Gradle JVM at 21 avoids IDE/Gradle mismatch issues.

4) IntelliJ Platform SDK Plugin
- The Gradle IntelliJ Plugin (`org.jetbrains.intellij`) will download the target IDE platform as needed. No manual SDK setup is required.

5) Running the Plugin from the IDE
- Open the Gradle tool window (View > Tool Windows > Gradle).
- Under Tasks > intellij, double‑click `runIde`.
  - This launches a sandbox IDE with the plugin installed, so you can test the actions under the Tools menu.
- Alternatively, create a Run Configuration: Run > Edit Configurations… > + > Gradle > select this project > Task: `runIde`.

6) Running Tests from the IDE
- Tests are standard JUnit 5 tests under `src/test/java`.
- You can run them in two ways:
  - Gradle: use the Gradle tool window and run the `test` task, or Run Configuration for the Gradle `test` task.
  - IntelliJ test runner: open a test class (e.g., `DefaultClipboardServiceTest`) and click the gutter run icon. Make sure the test runner uses the Gradle build system (recommended) or that your Project SDK is set to JDK 21.

7) Building the Plugin Artifact
- From a terminal: `./gradlew build` (or `gradlew.bat build` on Windows) runs compilation and tests.
- For distribution tasks (e.g., building a plugin ZIP), additional Gradle tasks from the IntelliJ Gradle plugin are available such as `buildPlugin` and `verifyPlugin`.

8) Common Troubleshooting
- If classes don’t resolve after opening the project, wait for indexing and the first Gradle sync to finish.
- If `runIde` fails due to JVM version problems, ensure both “Gradle JVM” and the Project SDK are set to JDK 21.
- If tests fail to discover, confirm “Use Gradle” is enabled for test running (Settings > Build, Execution, Deployment > Build Tools > Gradle > Run tests using: Gradle), or switch to “IntelliJ IDEA” if preferred.

That’s it—no extra IDEA build configuration is needed beyond the Gradle setup included in this repo.
