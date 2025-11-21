# FileMaker Clipboard Format — Working Notes (Phase 5.2)

These notes summarize the minimal viable payload format required for pasting content into FileMaker via the system clipboard, based on public behavior observations and experiments.

Key points:

- FileMaker accepts clipboard payloads containing an `fmxmlsnippet` XML block for many paste operations (e.g., script steps, fields). On Windows, placing the text of the `<fmxmlsnippet>...</fmxmlsnippet>` block onto the clipboard as UTF-8 text is typically sufficient for paste to work in target contexts.
- No additional wrapper or proprietary metadata is required for the basic scenarios covered by this plugin. Some environments may expose multiple clipboard formats simultaneously, but the XML text payload alone works for core workflows.
- Our "conversion to FileMaker clipboard" therefore focuses on validating and normalizing a provided fmxmlsnippet and returning the exact XML text to be placed onto the clipboard.

References and inspiration:

- `inspiration/GetFmClipboard.ps1` in this repository demonstrates extracting the fmxmlsnippet from the clipboard using standard Windows clipboard APIs and UTF-8 decoding.
- Community knowledge and prior tooling indicate fmxmlsnippet text suffices for pasting script steps or field definitions.

Assumptions for Phase 5.2:

- We support fmxmlsnippet payloads for Fields and Script Steps.
- Layouts and other snippet categories are not supported in this phase; attempting to convert those will raise a graceful error.
- The plugin will place the validated fmxmlsnippet text onto the clipboard (UTF-8) without additional formats.
