Windows End-to-End Clipboard Test Report (Phase 1.5)

Purpose
- Record results of pasting from the plugin into FileMaker on Windows across versions and snippet types.

How to use this document
- For each FileMaker version tested, fill the matrix below for each snippet type using the provided test XML files under resources/test-snippets.
- Steps per test
  1) Open the XML in the IDE.
  2) Run Tools > FMCuttingBoard > FM: Push Clipboard Into FileMaker.
  3) In FileMaker, paste in the appropriate context (Script Workspace, Manage Database > Fields/Tables, etc.).
  4) Note whether FileMaker recognizes the paste as an object (not plain text) and any messages.

Legend
- PASS: FileMaker recognizes and pastes the object appropriately
- TEXT: Paste comes in as plain text, not as an object
- FAIL: Error or nothing pasted; include details
- N/A: Not applicable in this version/context

Files under test (from resources/test-snippets)
- ScriptSteps.xml
- Fields.xml
- Tables.xml

Environment
- OS: Windows 10/11 (record exact build)
- IDE: IntelliJ IDEA (record edition and version)
- Plugin version: (from Help > About Plugins)
- Diagnostics: If possible, run with -Dfmcuttingboard.verbose=true and attach relevant [CB-DIAG] clipboard logs.

Results

FileMaker Pro 19
- Script Steps: 
  - Result: 
  - Notes:
- Fields: 
  - Result: 
  - Notes:
- Tables:
  - Result: 
  - Notes:

FileMaker Pro 20
- Script Steps: 
  - Result: 
  - Notes:
- Fields: 
  - Result: 
  - Notes:
- Tables:
  - Result: 
  - Notes:

FileMaker Pro 21
- Script Steps: 
  - Result: 
  - Notes:
- Fields: 
  - Result: 
  - Notes:
- Tables:
  - Result: 
  - Notes:

Version-specific observations
- 

Attachments
- Paste relevant IDE log snippets and screenshots where possible.
