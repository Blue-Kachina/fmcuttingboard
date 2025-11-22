# macOS Pasteboard Research (Phase 2.1)

Date: 2025-11-22

Summary
- Goal: Determine how to write fmxmlsnippet content so FileMaker for macOS recognizes it as structured objects (not plain text) when pasted.
- Constraint: No macOS environment available; this is a theoretical survey and plan for implementation.

Findings
- FileMaker on Windows uses custom clipboard formats named like "Mac-XMSS", "Mac-XMFD", "Mac-XMTB", etc. On macOS, the analog would be to publish custom pasteboard types (either legacy NSStringPboardType/NSPasteboardTypeString with additional custom types, or UTIs).
- macOS pasteboard APIs are accessible via AppKit (Objective‑C): NSPasteboard. Java integration generally requires JNI/JNA or an existing bridge.
- Likely types of interest on macOS:
  - Standard: public.utf8-plain-text, NSStringPboardType (legacy), public.utf16-external-plain-text
  - Custom (hypothesized): com.filemaker.fmxmlsnippet.* or the same names used as Windows custom format strings (e.g., "Mac-XMSS").
- Tools: pbpaste/pbcopy can work with certain types (via -Prefer) but are limited for custom types. Third-party clipboard viewers on macOS can help enumerate types.

API Options
1) JNI/JNA calling AppKit directly
   - NSPasteboard.generalPasteboard
   - clearContents
   - setData:forType: (NSData + NSString pasteboard type/UTI)
   - Pros: Full control over types.
   - Cons: Requires native stubs or a library; packaging complexity for an IntelliJ plugin.

2) Use AWT CopyPasteManager/Text flavors only (fallback)
   - Publish multiple flavors: String, text/plain, text/xml, application/xml, UTF‑16 streams.
   - Pros: No native code; works cross‑platform.
   - Cons: May not be sufficient for FileMaker to treat as object paste on macOS.

Planned Implementation Steps
- Phase 2.2: Introduce MacClipboardWriter scaffold that logs diagnostics and returns false (so the service falls back to multi‑flavor text). This sets up the integration point for future native NSPasteboard calls.
- Phase 2.3: Wire the writer into DefaultClipboardService.writeText() behind a macOS check with diagnostics.
- Future: Implement JNI/JNA path for NSPasteboard with content‑type mapping similar to Windows (Script/Steps/Fields/Tables/CFN/VL/LayoutObjects). Validate newline and BOM behaviors empirically on macOS.

Open Questions
- Exact pasteboard type names/UTIs FileMaker expects on macOS.
- Whether BOM is required/ignored for UTF‑8 payloads in custom types on macOS.
- Preferred newline style (LF likely) and whether FileMaker normalizes.

Next Actions
- Acquire macOS test environment and verify which pasteboard types appear after copying from FileMaker.
- Implement true native pasteboard write and iterate based on observed behavior.
