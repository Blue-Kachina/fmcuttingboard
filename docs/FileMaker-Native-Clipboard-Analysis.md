# FileMaker Native Clipboard Analysis — Findings Summary (Phase 1.2)

Generated: 2025-11-22

This document synthesizes observations from recent captures (see resources/captured_clipboard_formats.md) and the Diagnostics action output ([CB-DUMP]/[CB-ANALYZE]). It focuses on Windows clipboard formats used by FileMaker when copying different object types.

Key takeaways
- FileMaker writes multiple Windows clipboard formats for each copy. For structured objects it uses custom formats named Mac-XXXX along with standard text and, for layouts, image formats.
- For most Mac-XXXX formats, the payload begins with a 4-byte little-endian length prefix followed immediately by ASCII/UTF-8 fmxmlsnippet text. No UTF-8 BOM was observed and no trailing nulls were present.
- Line endings in fmxmlsnippet appear to be LF (\n) for larger snippets (notably Layouts via Mac-XML2). For smaller snippets, newline counts in sampled windows may be zero.
- Layout object copies include large binary bitmap flavors (e.g., CF_DIB/CF_DIBV5) in addition to the XML-bearing Mac-XML2 flavor. This is expected and explains “extra” entries in the dump.

Observed formats by object type (Windows)
- Scripts
  - Format: Mac-XMSC
  - Sample: size=440, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet XML visible shortly after a 4-byte LE length (e.g., B4 01 00 00 '<fmxmlsnippet…')
  - Newlines: not enough LFs counted in small sample; likely LF in practice
- Script Steps (selection)
  - Format: Mac-XMSS
  - Sample: size=131, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet '<Step …>' after 4-byte LE length (e.g., 7F 00 00 00 '<fmxmlsnippet…')
- Fields
  - Format: Mac-XMFD
  - Sample: size≈2412, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet '<Field …>' after 4-byte LE length (e.g., 68 09 00 00 '<fmxmlsnippet…')
- Tables
  - Format: Mac-XMTB
  - Sample: size≈4082, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet '<BaseTable …>' after 4-byte LE length (e.g., EE 0F 00 00 '<fmxmlsnippet…')
- Custom Functions
  - Format: Mac-XMFN
  - Sample: size≈217, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet '<CustomFunction …>' after 4-byte LE length (e.g., D5 00 00 00 '<fmxmlsnippet…')
- Value Lists
  - Format: Mac-XMVL
  - Sample: size≈187, BOM=none, endsWithNull=false
  - Payload: fmxmlsnippet '<ValueList …>' after 4-byte LE length
- Layout Objects (selection of 5)
  - Primary XML flavor: Mac-XML2 (very large)
    - Sample: size≈74 KB, BOM=none, endsWithNull=false
    - Newlines: LF observed in large numbers (e.g., LF=423 in sample window)
    - Payload: fmxmlsnippet '<LayoutObjectList …>' after initial 4 bytes (length prefix)
  - Additional binary flavors (non-XML):
    - CF_DIB (id=8) — size≈304,172, endsWithNull=true (typical for DIB blocks)
    - CF_DIBV5 (id=17) — size≈304,252, endsWithNull=false
    - These are expected for image-rich layout content and explain multiple [CB-DUMP] lines.

Encoding, BOM, terminators
- No UTF-8 BOM was present in the Mac-XXXX custom formats captured.
- Text appears to be UTF-8-compatible (ASCII subset shown) without BOM. The analyzer’s heuristic may label as “unknown” when only ASCII is observed, but the fmxmlsnippet content decodes as UTF-8.
- No null terminators at end-of-data were observed for Mac-XXXX flavors.
- For standard text formats (e.g., CF_UNICODETEXT), not part of the captures above, Windows typically expects UTF-16LE with a terminating null — this remains part of our plugin’s output for compatibility.

Implications for Phase 1.3 (Windows write path)
- For custom Mac-XXXX formats we should:
  - Write a 4-byte little-endian length prefix equal to the number of following XML bytes.
  - Write fmxmlsnippet payload as UTF-8 bytes without BOM.
  - Normalize newlines to LF (\n) or preserve original; current evidence supports LF for large snippets. Classic Mac CR (\r) does not appear in the observed data for these captures.
  - Do not append a trailing null terminator in the custom format block.
- For CF_UNICODETEXT (standard text flavor) we should continue writing UTF-16LE null-terminated text as today for fallback behavior.

Notes on the confusing Layout analysis
- Multiple entries for a single copy are normal: FileMaker advertises both XML (Mac-XML2) and bitmap/preview flavors (CF_DIB/CF_DIBV5). Only Mac-XML2 contains the fmxmlsnippet. The large binary blocks are image data and will not decode as text.

Next steps
- Update DefaultClipboardService.tryWindowsNativeWrite() to match the above layout for Mac-XXXX flavors (length prefix + UTF-8 without BOM, no trailing nulls; LF newlines).
- Ensure correct custom format selection based on fmxmlsnippet root elements (Scripts: Mac-XMSC or Steps: Mac-XMSS; Fields/Tables: Mac-XMFD/XMTB; Custom Functions: Mac-XMFN; Value Lists: Mac-XMVL; Layout Objects: Mac-XML2 — read-only for now unless explicitly supported).
- Validate with FileMaker by pasting into the appropriate contexts and iterating as needed.

Sources
- resources/captured_clipboard_formats.md (user-provided captures)
- IDE Diagnostics action output ([CB-DUMP]/[CB-ANALYZE])