# ROADMAP

This roadmap outlines the development phases for the **FMCuttingBoard** JetBrains plugin.  
Each phase is broken down into small, AI-friendly tasks.

---

## Phase 0 — Project Skeleton & Infrastructure

### 0.1 — Repository & Build Setup

- [x] Initialize plugin project structure for JetBrains IDEs (IntelliJ Platform Plugin)
- [x] Configure Gradle/Maven build for the plugin
- [x] Set plugin metadata (name, ID, description, version, vendor)
- [x] Configure plugin compatibility with target IDE versions
- [x] Set up basic `.gitignore` for the project
- [x] Add initial `README.md` and `ROADMAP.md` to the repository

### 0.2 — Development Environment & CI

- [x] Configure plugin run/debug configuration in the IDE
- [x] Add basic unit test framework setup (e.g., JUnit)
- [x] Add minimal sample test to verify test pipeline
- [x] Set up CI pipeline (e.g., GitHub Actions) to build plugin on push
- [x] Configure CI to run tests on each commit
- [x] Add status badges (build, tests) to `README.md` (if applicable)

---

## Phase 1 — Base Plugin UI & Actions

### 1.1 — Plugin Registration

- [x] Create plugin entry point files and configuration (e.g., `plugin.xml`)
- [x] Define plugin display name and description per README
- [x] Register plugin actions group for the Tools menu

### 1.2 — Tools Menu Integration

- [x] Define a new submenu under **Tools** named `FMCuttingBoard`
- [x] Register **Convert FileMaker Clipboard To XML** action under the submenu
- [x] Register **Read Clipboard Into New XML File** action under the submenu
- [x] Register **Push Clipboard Into FileMaker** action under the submenu
- [x] Provide default keyboard shortcuts (optional) and ensure they are overridable
  - Defaults added in plugin.xml (overridable via Keymap):
    - Convert FileMaker Clipboard To XML: Ctrl+Alt+C, X
    - Read Clipboard Into New XML File: Ctrl+Alt+C, F
    - Push Clipboard Into FileMaker: Ctrl+Alt+C, P

### 1.3 — Action UI Wiring

- [x] Implement base action classes for each menu item
- [x] Wire actions to IDE’s `ActionManager` via `plugin.xml`
- [x] Implement placeholder execute handlers (e.g., showing “Not implemented yet” notifications)
- [x] Ensure actions appear correctly in the Tools menu and submenu
- [x] Implement basic logging of action invocations (for debugging)

---

## Phase 2 — Clipboard Access & FileMaker Clipboard Detection

### 2.1 — Clipboard Access Abstraction

- [x] Implement a small service/class to read from the system clipboard
- [x] Implement a method to write text into the system clipboard
- [x] Add error handling for clipboard access failures (e.g., locked, unsupported content)
- [x] Add unit tests (where possible) for clipboard read/write wrapper

### 2.2 — FileMaker Clipboard Content Detection

- [x] Research FileMaker clipboard format(s) relevant to this plugin
 - [x] Define a small interface/protocol for “FileMakerClipboardParser”
 - [x] Implement a first pass parser that:
  - [x] Detects whether clipboard content is likely FileMaker-related
  - [x] Extracts or normalizes the text representation that will become XML
 - [x] Implement basic heuristics for rejecting non-FileMaker content
 - [x] Add unit tests for detection and parsing with small sample payloads (sanitized or synthetic)

---

## Phase 3 — Convert FileMaker Clipboard To XML

### 3.1 — Core Conversion Logic

- [x] Design data model for internal representation of FileMaker clipboard snippet(s)
- [x] Implement a converter from raw FileMaker clipboard data to XML string (fmxmlsnippet-like)
- [x] Handle common element types (e.g., fields, script steps, layouts) as needed by the plugin
- [x] Add error/exception handling for unexpected clipboard structures
- [x] Add unit tests for conversion with known sample inputs and expected XML outputs

### 3.2 — Action Implementation

- [x] Integrate clipboard reader in **Convert FileMaker Clipboard To XML** action
- [x] Invoke FileMaker clipboard detection/parsing when action is executed
- [x] If content recognized, convert to XML and replace clipboard with XML string
- [x] If content not recognized, show user-friendly notification explaining why
- [x] Add logging for success/failure outcomes of the action
- [ ] Add regression tests (as far as possible) covering user-facing behavior

---

## Phase 4 — Read Clipboard Into New XML File

### 4.1 — `.fmCuttingBoard` Directory Management

- [x] Implement utility for locating the current project root directory
- [x] Implement a helper to ensure `.fmCuttingBoard` directory exists under project root
- [x] Implement creation of `.fmCuttingBoard` directory if missing
- [x] Implement creation of `.fmCuttingBoard/.gitignore` containing only `*` when directory is created
- [x] Add tests for directory and `.gitignore` creation logic

### 4.2 — Timestamped XML File Creation

- [x] Implement a function to generate a timestamped filename (e.g., epoch-based) with `.xml` extension
- [x] Implement file creation inside `.fmCuttingBoard` using the generated filename
- [x] Implement robust error handling for file I/O failures
- [x] Add tests for filename format and file creation behavior (where feasible)

### 4.3 — Action Implementation

- [x] Wire **Read Clipboard Into New XML File** action to:
  - [x] Read current clipboard content
  - [x] Parse/detect FileMaker clipboard content
  - [x] Convert recognized content to XML if needed
  - [x] Write XML content to a new timestamped file in `.fmCuttingBoard`
- [x] Show notification on success, including created file path
- [x] Show descriptive notification if clipboard does not contain recognizable FileMaker content
- [x] Add logging for file creation and failures
- [x] Add integration tests (where feasible) for the end-to-end workflow

---

## Phase 5 — Push Clipboard Into FileMaker

### 5.1 — XML Parsing & Model

- [x] Define parser for the XML format generated or used by the plugin
- [x] Map XML elements back into an internal representation that can be transformed into FileMaker clipboard format
- [x] Implement validation for the expected XML structure (e.g., fmxmlsnippet)
- [x] Add tests verifying that valid/invalid XML cases are handled correctly

### 5.2 — Conversion to FileMaker Clipboard Format

- [x] Research required format for FileMaker clipboard payloads (data + metadata, if any)
- [x] Design converter from XML-based representation to FileMaker-compatible clipboard content
- [ ] Implement conversion logic for:
  - [x] Database fields snippets
  - [x] Script steps snippets
  - [ ] Other supported snippet types as needed
- [x] Add graceful error handling for unsupported snippet types or malformed XML
- [x] Add unit tests for each supported snippet type conversion

### 5.3 — Action Implementation & Context Awareness

- [ ] Implement **Push Clipboard Into FileMaker** action to:
  - [x] Read XML content from the currently active editor file (if applicable)
  - [x] Validate that the current file is an XML file
  - [x] Convert XML to FileMaker clipboard format
  - [x] Write FileMaker-formatted data to the system clipboard
- [ ] Ensure the action is only enabled when:
  - [x] A project is open
  - [x] An XML file is active in the editor (as per README)
- [x] Provide clear user notifications on success/failure
- [x] Add logging for conversion and clipboard write operations
- [ ] Add tests for enabling/disabling the action under different editor contexts

---

## Phase 6 — UX, Error Handling & Logging Enhancements

### 6.1 — Notifications & Messaging

- [ ] Standardize all user-facing messages and notifications
- [ ] Introduce helper utilities for showing notifications with consistent style
- [ ] Add detailed error messages for common failure scenarios
- [ ] Optionally add a “Show Details” link for advanced error info (e.g., logs)

### 6.2 — Logging & Diagnostics

- [ ] Introduce a logging facade or use platform logging consistently
- [ ] Add structured logs around:
  - [ ] Clipboard reads/writes
  - [ ] Conversion success/fail
  - [ ] File I/O operations
- [ ] Add a simple diagnostic mode (e.g., verbose logging flag) if needed
- [ ] Document where logs can be found for troubleshooting

---

## Phase 7 — Configuration & Extensibility

### 7.1 — Plugin Settings

- [ ] Add a settings/configuration UI page in IDE settings for the plugin
- [ ] Add options such as:
  - [ ] Custom base directory for saved XML files (default `.fmCuttingBoard`)
  - [ ] Custom filename format for new XML files
  - [ ] Toggle for automatic clipboard format detection nuances (if applicable)
- [ ] Persist settings using IDE’s settings mechanism
- [ ] Add tests (if supported) for settings persistence

### 7.2 — Advanced Behaviors (Optional)

- [ ] Provide optional preview dialog before writing to clipboard
- [ ] Add quick actions or context menu entries in Project View or Editor (if beneficial)

---

## Phase 8 — Testing, Hardening & Polish

### 8.1 — Extended Testing

- [ ] Expand unit test coverage for all critical components
- [ ] Add integration tests simulating full workflows:
  - [ ] Convert → Save XML → Push to Clipboard
- [ ] Perform manual testing across supported IDE versions
- [ ] Test behavior with large snippets and edge cases

### 8.2 — Performance & Robustness

- [ ] Profile any heavy parsing/conversion logic
- [ ] Optimize for typical payload sizes
- [ ] Ensure plugin fails gracefully without crashing the IDE
- [ ] Add timeouts or guards where necessary

### 8.3 — Documentation

- [ ] Update `README.md` with usage instructions, screenshots, and examples
- [ ] Add a short in-IDE help section or link to documentation
- [ ] Document known limitations and future ideas

---

## Phase 9 — Packaging & Release

### 9.1 — Build & Distribution

- [ ] Configure release build (versioning, changelog)
- [ ] Generate plugin artifact (e.g., `.zip` or `.jar`) suitable for distribution
- [ ] Test manual installation of the plugin in supported IDE(s)
- [ ] Validate that all actions and menus function correctly in a fresh environment

### 9.2 — Publication

- [ ] Prepare plugin listing text, description, and metadata for JetBrains Marketplace (or chosen distribution channel)
- [ ] Prepare icon and branding (if desired)
- [ ] Publish initial version
- [ ] Document installation steps in `README.md`

---

## Phase 10 — Feedback & Iteration

### 10.1 — Feedback Loop

- [ ] Add a simple “Provide Feedback” link somewhere in the plugin (about box, settings, etc.)
- [ ] Collect initial user feedback and bug reports
- [ ] Prioritize fixes and small quality-of-life improvements

### 10.2 — Future Enhancements (Backlog Seeding)

- [ ] Track feature requests for additional snippet types or formats
- [ ] Consider richer transformations or refactorings of fmxmlsnippet content
- [ ] Explore interoperability with other FileMaker tools and workflows
- [ ] Periodically review and update this roadmap as the project evolves
