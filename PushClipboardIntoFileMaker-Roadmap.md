# PushClipboardIntoFileMaker Action — Roadmap to Full Compatibility

## Problem Statement

The `PushClipboardIntoFileMakerAction` currently writes fmxmlsnippet XML to the clipboard, but FileMaker does not recognize it as pasteable structured data in all scenarios. FileMaker requires specific clipboard formats (custom named formats on Windows like `Mac-XMSS` and `Mac-XMFD`, plus proper encoding/newline conventions) to identify content as objects rather than plain text.

**Goal**: Ensure all supported fmxmlsnippet types paste into FileMaker as structured objects, matching FileMaker's native copy behavior. Currently supported on Windows: Scripts, Script Steps, Fields, Tables, Custom Functions, Value Lists, and Layout Objects.

---

## Current State Analysis

### What Works
- ✅ Reading XML from editor
- ✅ Validating fmxmlsnippet structure (Scripts, Fields)
- ✅ Writing UTF-8 text to clipboard via `DefaultClipboardService`
- ✅ Windows JNA-based native clipboard writer with custom formats

### What's Missing
- ❌ macOS custom pasteboard support (equivalent to Windows Mac-* formats)
- ❌ Manual end-to-end testing across FileMaker versions (19/20/21) and documenting results
- ❌ Relationships/Graph and other less-common snippet types (TBD)
- ❌ Automated validation that output matches FileMaker's native clipboard (planned in Phase 4)

### Key Technical Issues
1. **Format detection logic incomplete**: `DefaultClipboardService.tryWindowsNativeWrite()` detects Scripts vs Fields but uses heuristics that may miss edge cases
2. **Mac-XMFD vs Mac-XMSS specificity**: Need to understand exactly which FileMaker contexts require which custom format
3. **Newline normalization**: Windows `\r\n` vs classic Mac `\r` — current code normalizes to `\r` but needs validation
4. **UTF-8 BOM requirement**: Custom formats currently add BOM; needs verification across FileMaker versions
5. **macOS support**: No custom pasteboard type registration yet

---

## Phased Roadmap

### **Phase 1: Windows Clipboard Format Validation & Enhancement**

**Goal**: Ensure Windows clipboard writes include all required formats with correct encoding for FileMaker to recognize content as objects.

#### 1.1 Verify Current Windows Custom Format Implementation
**Estimated Effort**: 2-3 hours

**Tasks**:
- [x] Review `DefaultClipboardService.tryWindowsNativeWrite()` line-by-line (src/main/java/dev/fmcuttingboard/clipboard/DefaultClipboardService.java:372-489)
- [x] Document exactly what's written: CF_UNICODETEXT (format 13), custom format ID, byte layouts
- [x] Verify BOM presence (UTF-8: `EF BB BF`, UTF-16LE: `FF FE`) in all written formats
- [x] Confirm null terminator placement for CF_UNICODETEXT and custom formats

**Output**: Document or inline comments describing current byte-level clipboard format

---

#### 1.2 Capture and Analyze FileMaker Native Clipboard Data
**Estimated Effort**: 3-4 hours

**Tasks**:
- [x] Use `ClipboardFormatsDumpAction` to capture format IDs/names when copying from FileMaker
- [x] Copy Scripts/Script Steps from FileMaker → Capture clipboard with diagnostic logging enabled
- [x] Copy Field/Table definitions from FileMaker → Capture clipboard
- [x] Copy Custom Functions from FileMaker → Capture clipboard
- [x] Compare byte-for-byte: BOM presence, newline style (`\r` vs `\n`), null terminators, UTF-16 vs UTF-8
- [x] Document findings: Which formats FileMaker writes, exact encoding, newline conventions

**Output**: Reference document with hex dumps and observations ("FileMaker-Native-Clipboard-Analysis.md")

---

#### 1.3 Align Plugin Output with FileMaker Native Format
**Estimated Effort**: 2-3 hours

**Tasks**:
- [x] Update `tryWindowsNativeWrite()` to match FileMaker's exact byte layout based on Phase 1.2 findings
- [x] Ensure correct custom format selection based on content flavor mapping
- [x] Validate UTF-8 BOM + null terminator for custom formats (Phase 1.2 shows no BOM, no NUL; diagnostics verify this)
- [x] Validate UTF-16LE + null terminator for CF_UNICODETEXT
- [x] Confirm newline normalization to classic Mac CR (`\r`) matches FileMaker (Phase 1.2 indicates LF `\n`; implementation normalized to LF accordingly)

**Output**: Updated `tryWindowsNativeWrite()` method in DefaultClipboardService.java:372-489

---

#### 1.4 Improve Content Type Detection
**Estimated Effort**: 2 hours

**Tasks**:
- [x] Review `tryWindowsNativeWrite()` heuristics at DefaultClipboardService.java:379-385
- [x] Enhance detection to handle:
  - Script (full): Look for `<Script` and map to `Mac-XMSC`
  - Script Steps: Look for `<Step` and map to `Mac-XMSS`
  - Tables: Look for `<BaseTable` and map to `Mac-XMTB` (checked before Field)
  - Fields: Look for `<Field` or `<FieldDefinition>` and map to `Mac-XMFD`
  - Layout Objects: Look for `<Layout`, `<LayoutObject`, `<ObjectList`, `<Object `, `<Part` and map to `Mac-XML2` (checked before Field)
  - Custom Functions: Look for `<CustomFunction` and map to `Mac-XMFN`
  - Value Lists: Look for `<ValueList` and map to `Mac-XMVL`
- [x] Add unit tests for each snippet type detection
- [x] Log detected type clearly in diagnostics

**Output**: Enhanced detection logic + unit tests in test/java/dev/fmcuttingboard/clipboard/

---

#### 1.5 Test Windows Clipboard Output End-to-End
**Estimated Effort**: 2-3 hours

**Tasks**:
- [x] Create test XML files for: Script Steps, Fields, Tables
  - Added under resources/test-snippets: ScriptSteps.xml, Fields.xml, Tables.xml (2025-11-22)
  - See MANUAL_TESTING.md (Phase 1.5 section) for usage instructions and docs/Windows-EndToEnd-Test-Report.md for recording results
- [x] Create test XML files for: Custom Functions, Value Lists, Layout Objects
  - Added under resources/test-snippets: CustomFunctions.xml, ValueLists.xml, LayoutObjects.xml (2025-11-22)
- [ ] For each test file:
  - [ ] Open in plugin → Run PushClipboardIntoFileMakerAction
  - [ ] Open FileMaker Pro
  - [ ] Attempt paste in appropriate context (Script Workspace, Manage Database, etc.)
  - [ ] Verify FileMaker recognizes as object (not plain text)
- [ ] Test with FileMaker Pro 19, 20, 21 if available
- [ ] Document any version-specific quirks

**Output**: Test report documenting success/failure by FileMaker version and snippet type

---

### **Phase 2: macOS Clipboard Format Support**

**Goal**: Add macOS-specific pasteboard type support so FileMaker on macOS recognizes our clipboard content.

#### 2.1 Research macOS Pasteboard APIs for Custom Types
**Estimated Effort**: 3-4 hours

**Tasks**:
- [x] Research NSPasteboard custom type registration (public.utf8-plain-text, custom UTIs)
- [x] Investigate JNA or JNI approach for calling NSPasteboard from Java
- [x] Review `MacClipboardReader.java` for read-side patterns
- [x] Determine if FileMaker uses custom UTI (e.g., `com.filemaker.fmxmlsnippet.script`, `com.filemaker.fmxmlsnippet.field`) or relies on standard text types
- [x] Capture FileMaker macOS clipboard using diagnostic tools (pbpaste, Clipboard Viewer apps) — theoretical notes only (no Mac available)

**Output**: Research document with API options and FileMaker pasteboard format observations

Commit message: "Phase 2.1 — Research macOS pasteboard APIs and FileMaker formats"

---

#### 2.2 Implement MacClipboardWriter with Custom Pasteboard Types
**Estimated Effort**: 4-6 hours

**Tasks**:
- [x] Create `MacClipboardWriter` class (parallel scaffold; diagnostics only until native path implemented)
- [ ] Use JNA to call NSPasteboard APIs:
  - `NSPasteboard.generalPasteboard()`
  - `clearContents()`
  - `setData:forType:` for custom types
- [ ] Write custom types based on snippet type:
  - Script Steps: Write to `Mac-XMSS` or appropriate UTI
  - Full Scripts: Write to `Mac-XMSC` or appropriate UTI
  - Fields: Write to `Mac-XMFD` or appropriate UTI
  - Tables: Write to `Mac-XMTB` or appropriate UTI
  - Custom Functions: Write to `Mac-XMFN` or appropriate UTI
  - Value Lists: Write to `Mac-XMVL` or appropriate UTI
  - Layout Objects: Write to `Mac-XML2` (and optionally legacy alias `Mac-XML`) or appropriate UTI
- [x] Write standard text types as fallback: `public.utf8-plain-text`, `NSStringPboardType` (via multi-flavor AWT fallback)
- [x] Ensure UTF-8/UTF-16 text flavors are exposed for macOS compatibility (stream flavors with BOM)
- [x] Normalize newlines handled consistently by existing path (LF for custom on Windows; macOS fallback leaves text unchanged)

**Output**: New `MacClipboardWriter.java` in src/main/java/dev/fmcuttingboard/clipboard/

Commit message: "Phase 2.2 — Add MacClipboardWriter scaffold with diagnostics and UTF-16 stream flavor example"

---

#### 2.3 Integrate MacClipboardWriter into DefaultClipboardService
**Estimated Effort**: 2 hours

**Tasks**:
- [x] Update `DefaultClipboardService.writeText()` (line 332-364) to detect macOS
- [x] Call `MacClipboardWriter.write()` if on macOS, similar to Windows native path
- [x] Fallback to AWT/CopyPasteManager if native write fails
- [x] Add diagnostics logging to trace which write path was used

**Output**: Updated DefaultClipboardService.java:332-364

Commit message: "Phase 2.3 — Integrate macOS writer path with fallback and diagnostics"

---

#### 2.4 Test macOS Clipboard Output End-to-End
**Estimated Effort**: 2-3 hours

**Tasks**:
- [ ] Test on macOS 12+ with FileMaker Pro 19+
- [ ] For Script Steps, Fields, Tables:
  - [ ] Run PushClipboardIntoFileMakerAction
  - [ ] Paste into FileMaker
  - [ ] Verify recognized as object, not plain text
- [ ] Compare pasteboard types written by plugin vs FileMaker native copy (use `pbpaste -Prefer <type>`)
- [ ] Document any discrepancies or version quirks

**Output**: macOS test report + any required adjustments to MacClipboardWriter

---

### **Phase 3: Expand Supported fmxmlsnippet Types**

**Goal**: Support all common fmxmlsnippet types beyond Scripts and Fields (Tables, Layouts, Custom Functions, Value Lists, etc.).

#### 3.1 Identify All fmxmlsnippet Element Types
**Estimated Effort**: 2 hours

**Tasks**:
- [ ] Review `ElementType` enum in `FmXmlParser.java` (if exists) or create it
- [ ] Document all known fmxmlsnippet types from FileMaker documentation and community resources:
  - Script Steps (`<Step`)
  - Fields (`<Field`, `<FieldDefinition`)
  - Base Tables (`<BaseTable`)
  - Layouts/Objects (`<Layout`, `<LayoutObjectList`, `<LayoutObject`, `<ObjectList`, `<Object`)
  - Custom Functions (`<CustomFunction`)
  - Value Lists (`<ValueList`)
  - Relationships/Graph (`<Relationship`)
- [ ] For each type, determine:
  - FileMaker paste context (where it can be pasted)
  - Clipboard format name (if custom, e.g., `Mac-XMLO` for layouts)
  - Any special encoding requirements

**Output**: Reference document "fmxmlsnippet-types-catalog.md"

---

#### 3.2 Update Content Type Detection for All Types
**Estimated Effort**: 3 hours

**Tasks**:
- [x] Enhance `tryWindowsNativeWrite()` detection logic to identify:
  - Layout Objects: `<Layout`, `<LayoutObject`, `<ObjectList`, `<Object`, `<Part` (checked before Fields)
  - Custom Functions: `<CustomFunction`
  - Value Lists: `<ValueList`
  - Base Tables: `<BaseTable` (checked before Field)
  - Full Scripts: `<Script` (checked before Steps)
  - Script Steps: `<Step`
  - Relationships: `<Relationship` (TBD)
- [x] Map each type to the appropriate Windows custom format:
  - Script (full) → `Mac-XMSC`
  - Script Steps → `Mac-XMSS`
  - Fields → `Mac-XMFD`
  - Tables → `Mac-XMTB`
  - Layout Objects → `Mac-XML2` (plus alias `Mac-XML`)
  - Custom Functions → `Mac-XMFN`
  - Value Lists → `Mac-XMVL`
  - Others → TBD based on research
- [x] Add unit tests for each detection case (see SnippetDetectionTest)

**Output**: Updated detection logic + comprehensive unit tests (Windows path complete for listed types)

---

#### 3.3 Research and Implement Custom Formats for Additional Types
**Estimated Effort**: 4-6 hours

**Tasks**:
- [x] For Layout Objects, Custom Functions, Value Lists (Windows):
  - [x] Capture/confirm custom clipboard format names via diagnostics
  - [x] Align byte structure (LE length prefix + UTF-8, no BOM, no trailing NUL; LF newlines)
  - [x] Implement write logic in `tryWindowsNativeWrite()`
- [ ] macOS implementation remains pending (`MacClipboardWriter` TBD in Phase 2)
- [ ] Test each type end-to-end manually:
  - [ ] Write to clipboard via plugin and paste into FileMaker in correct context
  - [ ] Verify object paste (not text)

**Output**: Enhanced `DefaultClipboardService` (Windows complete for listed types); macOS pending

---

#### 3.4 Update DefaultXmlToClipboardConverter to Allow All Types
**Estimated Effort**: 1 hour

**Tasks**:
- [x] Remove artificial restrictions in `DefaultXmlToClipboardConverter.java`
- [x] Previously rejected Layout-only snippets; now allowed (supports Layout Objects)
- [x] Validate XML structure and allow supported element types to proceed to clipboard write

**Output**: Updated DefaultXmlToClipboardConverter.java:22-40

---

### **Phase 4: Cross-Platform Testing and Validation**

**Goal**: Ensure robust behavior across Windows/macOS, multiple FileMaker versions, and all snippet types.

#### 4.1 Build Automated Test Suite
**Estimated Effort**: 4-5 hours

**Tasks**:
- [ ] Create integration test suite that:
  - [ ] Generates fmxmlsnippet XML programmatically for all types
  - [ ] Calls `PushClipboardIntoFileMakerAction.processXmlToClipboard()` (line 190)
  - [ ] Reads back clipboard via `DefaultClipboardService.readText()`
  - [ ] Validates clipboard contains expected formats (Windows: check custom format IDs; macOS: check pasteboard types)
- [ ] Use JNA/native APIs to query available clipboard formats after write
- [ ] Assert format names, byte counts, encoding markers (BOM presence)

**Output**: New test class `PushClipboardIntoFileMakerActionIntegrationTest.java`

---

#### 4.2 Manual Cross-Platform Testing Matrix
**Estimated Effort**: 6-8 hours

**Tasks**:
- [ ] Test matrix:
  - Platforms: Windows 10/11, macOS 12/13/14
  - FileMaker Versions: 19, 20, 21
  - Snippet Types: Scripts, Fields, Tables, Layouts, Custom Functions, Value Lists
- [ ] For each combination:
  - [ ] Push XML to clipboard via plugin
  - [ ] Paste into FileMaker in appropriate context
  - [ ] Document: Success/Failure, recognized as object or text, any errors
- [ ] Create spreadsheet tracking all test results

**Output**: Comprehensive test matrix spreadsheet + summary report

---

#### 4.3 Performance and Edge Case Testing
**Estimated Effort**: 3-4 hours

**Tasks**:
- [ ] Test large payloads (10KB, 100KB, 1MB XML)
- [ ] Test fmxmlsnippet with special characters (Unicode, emoji, XML entities)
- [ ] Test malformed XML (ensure graceful failure, not clipboard corruption)
- [ ] Test clipboard busy/locked scenarios (Windows OpenClipboard retries)
- [ ] Test clipboard write when FileMaker is not running (should succeed; verify with other apps)
- [ ] Test rapid successive writes (clipboard locking/unlocking)

**Output**: Edge case test report + fixes for any identified issues

---

#### 4.4 Documentation and User Guidance
**Estimated Effort**: 2-3 hours

**Tasks**:
- [ ] Update README with:
  - Supported fmxmlsnippet types
  - Platform-specific notes (Windows vs macOS)
  - Known limitations (if any)
- [ ] Add troubleshooting section:
  - "Paste as text instead of object" → Enable verbose logging, check clipboard formats
  - FileMaker version compatibility notes
- [ ] Document diagnostic logging usage (how to enable verbose mode)
- [ ] Add FAQ section for common issues

**Output**: Updated README.md and docs/Troubleshooting.md

---

### **Phase 5: Refinement and Polish**

**Goal**: Code cleanup, performance optimization, and final validation before considering the feature complete.

#### 5.1 Code Cleanup and Refactoring
**Estimated Effort**: 3-4 hours

**Tasks**:
- [ ] Extract clipboard format constants into dedicated class (e.g., `FileMakerClipboardFormats.java`)
  - Custom format names: `Mac-XMSC`, `Mac-XMSS`, `Mac-XMFD`, `Mac-XMTB`, `Mac-XMFN`, `Mac-XMVL`, `Mac-XML2`, etc.
  - Standard format IDs: `CF_UNICODETEXT = 13`, `CF_TEXT = 1`
- [ ] Reduce duplication between `WindowsClipboardReader`, `MacClipboardReader`, `DefaultClipboardService`
  - Extract shared encoding/decoding utilities into `ClipboardEncodingUtils.java`
  - Methods: `utf8BomNullTerminated()`, `utf16leNullTerminated()`, `stripNulls()`, `decodeBytesWithBomHeuristics()`
- [ ] Simplify `tryWindowsNativeWrite()` by extracting format-specific write logic into smaller methods
- [ ] Review all TODOs and FIXMEs in clipboard-related code, resolve or document as future work

**Output**: Refactored codebase with reduced duplication

---

#### 5.2 Performance Profiling
**Estimated Effort**: 2 hours

**Tasks**:
- [ ] Profile clipboard write path for 1KB, 10KB, 100KB payloads
- [ ] Measure time spent in:
  - XML validation (`FmXmlParser.parse()`)
  - Native clipboard APIs (`OpenClipboard`, `SetClipboardData`)
  - BOM/encoding transformations
- [ ] Identify bottlenecks (if any) and optimize
  - Consider caching format IDs after first registration
  - Minimize allocations in hot paths
- [ ] Set performance baseline: target <50ms for typical Script Steps snippet (<10KB)

**Output**: Performance report + optimizations (if needed)

---

#### 5.3 Security and Safety Review
**Estimated Effort**: 2 hours

**Tasks**:
- [ ] Review JNA native calls for potential issues:
  - Memory leaks (ensure `GlobalFree` called on failure paths in `globalAllocAndWrite()`)
  - Buffer overflows (validate `GlobalSize` before read)
  - Race conditions in clipboard open/close
- [ ] Validate all user inputs (XML content) are sanitized before clipboard write
  - No arbitrary code execution via XML injection (already safe: we write as text, not execute)
- [ ] Test behavior when JNA not available (fallback to AWT should work gracefully)
- [ ] Test behavior on locked-down systems (clipboard access restricted)

**Output**: Security audit checklist + any fixes

---

#### 5.4 Final Validation and Release Prep
**Estimated Effort**: 2-3 hours

**Tasks**:
- [ ] Run full test suite (unit + integration) on Windows and macOS
- [ ] Manually test 10 common workflows:
  - Copy Script from FileMaker → Edit in IDE → Push back to FileMaker
  - Copy Field → Edit → Push back
  - Create Script XML from scratch in IDE → Push to FileMaker
- [ ] Review all logging: ensure no sensitive data logged, appropriate log levels
- [ ] Prepare release notes:
  - Supported snippet types
  - Platform compatibility
  - Known issues (if any)
- [ ] Tag release and update version number

**Output**: Release-ready plugin + release notes

---

## Summary of Deliverables by Phase

| Phase                  | Deliverables                                                      | Estimated Total Effort  |
|------------------------|-------------------------------------------------------------------|-------------------------|
| **Phase 1: Windows**   | Updated `DefaultClipboardService`, test report, analysis docs; Windows support for Scripts, Script Steps, Fields, Tables, Custom Functions, Value Lists, Layout Objects | 14-18 hours |
| **Phase 2: macOS**     | `MacClipboardWriter`, integrated write path, macOS test report    | 11-15 hours             |
| **Phase 3: All Types** | Support for Layout Objects, Custom Functions, Value Lists (Windows), updated converter | 10-14 hours |
| **Phase 4: Testing**   | Automated tests, cross-platform matrix, edge cases, docs          | 15-20 hours             |
| **Phase 5: Polish**    | Refactored code, performance baseline, security review, release   | 9-11 hours              |
| **Total**              | Fully functional PushClipboardIntoFileMakerAction for all flavors | **59-78 hours**         |

---

## Recommended Execution Order

1. **Start with Phase 1.2** (Capture FileMaker native clipboard) — this is the foundation for all subsequent work
2. **Complete Phase 1** (Windows) before Phase 2 (macOS) — Windows is more complex due to custom format registration
3. **Iterate Phases 1-3 in parallel per snippet type** — e.g., get Scripts working end-to-end on Windows/macOS before moving to Fields
4. **Phase 4 (Testing) should run continuously** — add tests after each feature implementation, don't leave for the end
5. **Phase 5 (Polish) is final** — only start when all functionality is working

---

## Risk Mitigation

- **FileMaker version differences**: Test on at least 2 major versions (19 and 21) to catch breaking changes
- **Clipboard locking issues**: Implement retry logic (already present in `WindowsClipboardReader`, ensure in write path)
- **JNA availability**: Graceful fallback to AWT; document JNA as required dependency
- **macOS security**: Recent macOS versions require entitlements for pasteboard access; test on locked-down systems
- **Undocumented FileMaker formats**: If custom formats don't work, fallback to multi-flavor text write (current approach); document limitations

---

## Success Criteria

✅ **Done when**:
1. Script Steps, Fields, and Tables paste into FileMaker as objects (not text) on Windows and macOS
2. FileMaker versions 19-21 all recognize plugin clipboard output
3. Automated tests validate correct clipboard formats after write
4. Manual testing confirms 95%+ success rate across test matrix
5. Documentation clearly explains supported types and troubleshooting steps
6. Code is refactored, performant (<50ms for typical snippets), and secure
