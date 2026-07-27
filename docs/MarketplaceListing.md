# JetBrains Marketplace Listing

Name: FMCuttingBoard

Summary:
- Work with FileMaker clipboard content as XML. Convert to/from fmxmlsnippet and streamline your FileMaker workflows from any JetBrains IDE.

Description:
FMCuttingBoard helps developers manipulate FileMaker clipboard content in a structured way.
It adds a Tools > FMCuttingBoard menu with actions to:
- Convert FM Clipboard To XML Clipboard
- New XML File From FM Clipboard
- Push Clipboard Into FileMaker

Use cases:
- Inspect fmxmlsnippet payloads from FileMaker.
- Version-control your snippets as XML files in your project.
- Reconstruct clipboard content from XML and paste back into FileMaker (fields, scripts, etc.).

Features:
- Cross-platform clipboard access with fallbacks.
- Settings for output directory and filename pattern.
- Optional preview before writing to clipboard.
- Notifications and verbose logging for troubleshooting.

Screenshots:
- docs/screenshots/FmCuttingBoardScreenie.png — the `Tools > FMCuttingBoard` menu open over a generated fmxmlsnippet XML file (also shows the "FileMaker XML Detected" editor notification banner).
- The plugin's icon is used as the icon for the `Tools > FMCuttingBoard` menu item; no separate icon screenshot needed.
- docs/screenshots/FmCuttingBoard_fmcalc.png: a `.fmcalc` file showing FileMaker calculation-language syntax highlighting.

Installation:
- Install from disk using the ZIP built by Gradle, or via Marketplace once published.

Changelog:
- See CHANGELOG.md in the repository.

Vendor:
- Name: FMCuttingBoard
- Support: GitHub Issues
- Source: https://github.com/Blue-Kachina/fmcuttingboard

Privacy / Permissions:
- No network access required. Operates on clipboard and local files within the project.
- No telemetry or analytics collected. Nothing leaves the user's machine.
