Manual testing guidance across supported IDE versions

Scope
- This project targets IntelliJ-based IDEs per plugin.xml since API-level UI tests are out-of-scope for CI. Use this guide to validate core workflows manually.

Pre-requisites
- Build the plugin via Gradle: `gradlew buildPlugin` (or `gradlew releasePlugin`)
- Install the built plugin from the generated ZIP via Settings > Plugins > Gear icon > Install from Disk…

Environments to check
- IntelliJ IDEA Community 2024.3 or later
- (Optional) IntelliJ IDEA Ultimate 2024.3 or later

Checklist
1) New XML File From FM Clipboard
   - Copy a valid FileMaker fmxmlsnippet text (e.g., a <fmxmlsnippet> with a FieldDefinition or Script) to the system clipboard.
   - Invoke the action “FM: New XML File From FM Clipboard”.
   - Expected: A new timestamped XML file is created under .fmCuttingBoard at the project root and opened in the editor.
   - Expected: Notification indicates success and filepath.

2) Push Clipboard Into FileMaker (from active XML file)
   - Open a valid fmxmlsnippet XML file in the editor. Sample files are provided under resources/test-snippets:
     - ScriptSteps.xml (Script Workspace paste)
     - Fields.xml (Manage Database > Fields paste)
     - Tables.xml (Manage Database > Tables paste)
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
- Date of latest manual verification: 2025-11-22
- IDEs verified: IntelliJ IDEA Community 2024.3 (Windows)

Phase 1.5 — Windows End-to-End Clipboard Output
- Purpose: Validate that FileMaker recognizes our clipboard payload as an object paste (not plain text) for Script Steps, Fields, and Tables.
- Test Inputs: Use the sample XML files under resources/test-snippets.
- Procedure:
  1) Open one of the sample XML files.
  2) Run Tools > FMCuttingBoard > FM: Push Clipboard Into FileMaker.
  3) Switch to FileMaker and paste in the appropriate context.
  4) Observe behavior and document results in docs/Windows-EndToEnd-Test-Report.md.
- Notes:
  - Run the IDE with -Dfmcuttingboard.verbose=true to capture [CB-DIAG] logs of clipboard formats for correlation.
  - Repeat across FileMaker Pro 19/20/21 if available and record any version-specific differences.

Phase 1.2 — Clipboard Capture (Windows)
- Purpose: Capture FileMaker native clipboard formats and analyze encoding/newlines.
- Prereqs: Ensure you can copy Script Steps and Field/Table definitions in FileMaker.
- Steps:
  1) In FileMaker, copy a Script (or a selection of steps) so it’s on the system clipboard.
  2) In the IDE, run Tools > FMCuttingBoard > Diagnostics: Dump Clipboard Formats.
  3) Expected: IDE log shows lines prefixed with [CB-DUMP] for all formats, and [CB-ANALYZE] entries for every enumerated format (we now analyze ALL formats, not just FileMaker-specific ones).
  4) A report file is written to docs/FileMaker-Native-Clipboard-Analysis.md in your project (or to your home folder if no project is open).
  5) Repeat for Fields and Tables (copy from FileMaker’s Manage Database) and run the diagnostics action again.
  6) Validate the report contains BOM info, newline counts (CR/LF/CRLF), null terminator note, and a small fmxmlsnippet preview.

Notes
- If JNA is not available or you’re not on Windows, the diagnostics action will log a notice and skip native enumeration.
- The report appends new captures; delete the file to start fresh.
- Layouts and Layout Objects: Expect multiple flavors beyond the XML one. FileMaker emits Mac-XML2 (XML payload) and also large bitmap flavors like CF_DIB/CF_DIBV5 for previews. Only Mac-XML2 will decode to fmxmlsnippet; binary formats are expected and will not produce text previews.

Suggested FileMaker items to copy and analyze
- Script(s) and selections of Script Steps (Script Workspace)
- Field Definitions and Table Definitions (Manage Database)
- Custom Functions (Manage Custom Functions)
- Layout Objects (e.g., single field, button, group) and full Layouts if possible
- Value Lists (Manage Value Lists)
- Custom Menus and Menu Sets (Manage Custom Menus)
- Privilege Sets (Manage Security) — if copy is supported

Fresh environment validation
- Start the IDE with a fresh config directory (or use the Gradle `runIde` sandbox) to simulate a new user profile.
- Verify the Tools > FMCuttingBoard menu appears with all three actions.
- Confirm keyboard shortcuts are registered and do not conflict.
- Perform the full workflow using the checklist above. Expect no prior settings; defaults should be applied.
