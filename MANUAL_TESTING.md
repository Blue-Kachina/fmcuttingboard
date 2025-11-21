Manual testing guidance across supported IDE versions

Scope
- This project targets IntelliJ-based IDEs per plugin.xml since API-level UI tests are out-of-scope for CI. Use this guide to validate core workflows manually.

Pre-requisites
- Build the plugin via Gradle: gradlew build
- Install the built plugin from the generated zip/jar via Settings > Plugins > Install from Disk.

Environments to check
- IntelliJ IDEA Community 2024.3 or later
- (Optional) IntelliJ IDEA Ultimate 2024.3 or later

Checklist
1) Read Clipboard Into New XML File
   - Copy a valid FileMaker fmxmlsnippet text (e.g., a <fmxmlsnippet> with a FieldDefinition or Script) to the system clipboard.
   - Invoke the action “FM: Read Clipboard Into New XML File”.
   - Expected: A new timestamped XML file is created under .fmCuttingBoard at the project root and opened in the editor.
   - Expected: Notification indicates success and filepath.

2) Push Clipboard Into FileMaker (from active XML file)
   - Open a valid fmxmlsnippet XML file in the editor.
   - Invoke the action “FM: Push Clipboard Into FileMaker”.
   - Expected: System clipboard receives the normalized fmxmlsnippet payload.
   - For unsupported types (e.g., pure Layouts), expect a warning/error notification.

3) Settings
   - Open Settings > Tools > FMCuttingBoard.
   - Change base directory name and filename pattern.
   - Run step (1) again and verify the file is created according to custom settings.

4) Edge cases
   - Empty clipboard: Expect informational notification; no file created.
   - Malformed XML in editor: Expect error notification on push.
   - Very large fmxmlsnippet (thousands of fields or steps): No UI freeze; operations complete successfully with notifications.

Notes
- Verbose logging can be enabled with JVM option: -Dfmcuttingboard.verbose=true
- On Windows/macOS, clipboard access may be restricted by OS policies; verify actions gracefully notify without crashing.

Recordkeeping
- Date of latest manual verification: 2025-11-21
- IDEs verified: IntelliJ IDEA Community 2024.3 (Windows)
