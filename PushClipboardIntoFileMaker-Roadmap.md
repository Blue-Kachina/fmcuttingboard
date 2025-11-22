# PushClipboardIntoFileMaker Action — Roadmap to Full Compatibility

## Problem Statement

The `PushClipboardIntoFileMakerAction` currently writes fmxmlsnippet XML to the clipboard, but FileMaker does not recognize it as pasteable structured data in all scenarios. FileMaker requires specific clipboard formats (custom named formats on Windows like `Mac-XMSS` and `Mac-XMFD`, plus proper encoding/newline conventions) to identify content as objects rather than plain text.

**Goal**: Ensure all supported fmxmlsnippet types (Scripts, Fields, Tables) paste into FileMaker as structured objects, matching FileMaker's native copy behavior.

---

## Current State Analysis

### What Works
- ✅ Reading XML from editor
- ✅ Validating fmxmlsnippet structure (Scripts, Fields)
- ✅ Writing UTF-8 text to clipboard via `DefaultClipboardService`
- ✅ Windows JNA-based native clipboard writer with custom formats

### What's Missing
- ❌ FileMaker doesn't recognize our clipboard content as objects consistently
- ❌ Limited testing across FileMaker versions and contexts
- ❌ No macOS custom format support (`Mac-XMSS`, `Mac-XMFD`)
- ❌ Incomplete handling of all fmxmlsnippet types (Layouts, Tables, etc.)
- ❌ No automated validation that output matches FileMaker's native clipboard

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
- [ ] Document exactly what's written: CF_UNICODETEXT (format 13), custom format ID, byte layouts
- [ ] Verify BOM presence (UTF-8: `EF BB BF`, UTF-16LE: `FF FE`) in all written formats
- [ ] Confirm null terminator placement for CF_UNICODETEXT and custom formats

**Output**: Document or inline comments describing current byte-level clipboard format

---

#### 1.2 Capture and Analyze FileMaker Native Clipboard Data
**Estimated Effort**: 3-4 hours

**Tasks**:
- [ ] Use `ClipboardFormatsDumpAction` to capture format IDs/names when copying from FileMaker
- [ ] Copy Script Steps from FileMaker → Capture clipboard with diagnostic logging enabled
- [ ] Copy Field/Table definitions from FileMaker → Capture clipboard
- [ ] Use `WindowsClipboardReader` to read raw bytes from `Mac-XMSS` and `Mac-XMFD` formats
- [ ] Compare byte-for-byte: BOM presence, newline style (`\r` vs `\r\n`), null terminators, UTF-16 vs UTF-8
- [ ] Document findings: Which formats FileMaker writes, exact encoding, newline conventions

**Output**: Reference document with hex dumps and observations ("FileMaker-Native-Clipboard-Analysis.md")

---

#### 1.3 Align Plugin Output with FileMaker Native Format
**Estimated Effort**: 2-3 hours

**Tasks**:
- [ ] Update `tryWindowsNativeWrite()` to match FileMaker's exact byte layout based on Phase 1.2 findings
- [ ] Ensure correct custom format selection:
  - `Mac-XMSS` for Script Steps (`<Step` detected in XML)
  - `Mac-XMFD` for Fields/Tables (`<Field`, `<FieldDefinition`, or `<BaseTable` detected)
- [ ] Validate UTF-8 BOM + null terminator for custom formats
- [ ] Validate UTF-16LE + null terminator for CF_UNICODETEXT
- [ ] Confirm newline normalization to classic Mac CR (`\r`) matches FileMaker

**Output**: Updated `tryWindowsNativeWrite()` method in DefaultClipboardService.java:372-489

---

#### 1.4 Improve Content Type Detection
**Estimated Effort**: 2 hours

**Tasks**:
- [ ] Review `tryWindowsNativeWrite()` heuristics at DefaultClipboardService.java:379-385
- [ ] Enhance detection to handle:
  - Script Steps: Look for `<Step` (current)
  - Fields: Look for `<Field` or `<FieldDefinition>` (current)
  - Tables: Add detection for `<BaseTable` and use `Mac-XMFD`
  - Layouts: Identify layout snippets (to be supported later) and handle gracefully
- [ ] Add unit tests for each snippet type detection
- [ ] Log detected type clearly in diagnostics

**Output**: Enhanced detection logic + unit tests in test/java/dev/fmcuttingboard/clipboard/

---

#### 1.5 Test Windows Clipboard Output End-to-End
**Estimated Effort**: 2-3 hours

**Tasks**:
- [ ] Create test XML files for: Script Steps, Fields, Tables
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
- [ ] Research NSPasteboard custom type registration (public.utf8-plain-text, custom UTIs)
- [ ] Investigate JNA or JNI approach for calling NSPasteboard from Java
- [ ] Review `MacClipboardReader.java` for read-side patterns
- [ ] Determine if FileMaker uses custom UTI (e.g., `com.filemaker.fmxmlsnippet.script`, `com.filemaker.fmxmlsnippet.field`) or relies on standard text types
- [ ] Capture FileMaker macOS clipboard using diagnostic tools (pbpaste, Clipboard Viewer apps)

**Output**: Research document with API options and FileMaker pasteboard format observations

---

#### 2.2 Implement MacClipboardWriter with Custom Pasteboard Types
**Estimated Effort**: 4-6 hours

**Tasks**:
- [ ] Create `MacClipboardWriter` class (parallel to `WindowsClipboardReader`)
- [ ] Use JNA to call NSPasteboard APIs:
  - `NSPasteboard.generalPasteboard()`
  - `clearContents()`
  - `setData:forType:` for custom types
- [ ] Write custom types based on snippet type:
  - Script Steps: Write to `Mac-XMSS` or appropriate UTI
  - Fields/Tables: Write to `Mac-XMFD` or appropriate UTI
- [ ] Write standard text types as fallback: `public.utf8-plain-text`, `NSStringPboardType`
- [ ] Ensure UTF-8 encoding with BOM (if required by FileMaker on macOS)
- [ ] Normalize newlines to `\r` (classic Mac) to match Windows behavior

**Output**: New `MacClipboardWriter.java` in src/main/java/dev/fmcuttingboard/clipboard/

---

#### 2.3 Integrate MacClipboardWriter into DefaultClipboardService
**Estimated Effort**: 2 hours

**Tasks**:
- [ ] Update `DefaultClipboardService.writeText()` (line 332-364) to detect macOS
- [ ] Call `MacClipboardWriter.write()` if on macOS, similar to Windows native path
- [ ] Fallback to AWT/CopyPasteManager if native write fails
- [ ] Add diagnostics logging to trace which write path was used

**Output**: Updated DefaultClipboardService.java:332-364

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
  - Layouts (`<Layout`)
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
- [ ] Enhance `tryWindowsNativeWrite()` detection logic to identify:
  - Layouts: `<Layout`
  - Custom Functions: `<CustomFunction`
  - Value Lists: `<ValueList`
  - Relationships: `<Relationship`
  - Base Tables: `<BaseTable` (already partial support)
- [ ] Map each type to the appropriate Windows custom format:
  - Scripts → `Mac-XMSS`
  - Fields/Tables → `Mac-XMFD`
  - Layouts → `Mac-XMLO` (hypothesized, verify in Phase 3.3)
  - Others → TBD based on research
- [ ] Add unit tests for each detection case

**Output**: Updated detection logic + comprehensive unit tests

---

#### 3.3 Research and Implement Custom Formats for Additional Types
**Estimated Effort**: 4-6 hours

**Tasks**:
- [ ] For Layouts, Custom Functions, Value Lists, etc.:
  - [ ] Copy from FileMaker and capture custom clipboard format names via diagnostics
  - [ ] Analyze byte structure (BOM, newlines, encoding)
  - [ ] Implement write logic in `tryWindowsNativeWrite()` (Windows) and `MacClipboardWriter` (macOS)
- [ ] Test each type end-to-end:
  - [ ] Write to clipboard via plugin
  - [ ] Paste into FileMaker in correct context
  - [ ] Verify object paste (not text)

**Output**: Enhanced `DefaultClipboardService` and `MacClipboardWriter` supporting all types

---

#### 3.4 Update DefaultXmlToClipboardConverter to Allow All Types
**Estimated Effort**: 1 hour

**Tasks**:
- [ ] Remove artificial restrictions in `DefaultXmlToClipboardConverter.java:26-36`
- [ ] Currently rejects Layouts and unknown types; remove these blocks once clipboard formats are implemented
- [ ] Validate XML structure but allow all element types to proceed to clipboard write

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
  - Custom format names: `Mac-XMSS`, `Mac-XMFD`, `Mac-XMLO`, etc.
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
| **Phase 1: Windows**   | Updated `DefaultClipboardService`, test report, analysis docs     | 14-18 hours             |
| **Phase 2: macOS**     | `MacClipboardWriter`, integrated write path, macOS test report    | 11-15 hours             |
| **Phase 3: All Types** | Support for Layouts/Custom Functions/etc., updated converter      | 10-14 hours             |
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
