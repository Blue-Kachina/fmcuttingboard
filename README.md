# FMCuttingBoard

<!-- CI Status Badges -->
![Build](https://github.com/Blue-Kachina/fmcuttingboard/actions/workflows/ci.yml/badge.svg)
![Tests](https://github.com/Blue-Kachina/fmcuttingboard/actions/workflows/ci.yml/badge.svg)
- A plugin for JetBrains IDEs
- When installed, the user will have a new menu item in their IDE's `Tools` menu
- The new menu item will be named the same as this plugin
- The new menu item will itself be a submenu:
  - Convert FileMaker Clipboard To XML 
  - Read Clipboard Into New XML File
  - Push Clipboard Into FileMaker (only enabled when XML file is currently active)

## Convert FileMaker Clipboard To XML
When this option is selected, we will attempt to read FileMaker content from the clipboard.
If we are successful in finding FileMaker content, then the clipboard will be replaced with it

## Read Clipboard Into New XML File
When this option is selected, we will attempt to read FileMaker content from the clipboard.  
If we are successful in finding FileMaker content, it will be written to file.
The file it's written to should be a file whose name is timestamped (perhaps using unix/epoch).
The file will be XML format.
That file will be stored within the project's `.fmCuttingBoard` directory (off of root).
In the event the directory doesn't exist at the time we need to create a file in it, then we will create the folder.
When creating the folder, we will always add a `.gitignore` to it as well.
The .gitigore will contain only a `*` character.

## Push Clipboard Into FileMaker
When this option is selected, we will attempt to populate the clipboard with content FileMaker will properly recognize.
In essence, this option is doing the opposite of what we're doing in `Read Clipboard Into New XML File`.
We're taking plain XML, and repackaging it into a new clipboard that we'll later be able to paste into FileMaker.
The trick will be in getting the format right.  
If the fmxmlsnippet represents database fields, then after running this command the user would be able to paste fields into FileMaker's Manage Database dialog.
If it contains an fmxmlsnippet of script steps, then we'd be able to paste Script Steps into FileMaker Script Workspace window, etc...

## Inspiration
Please review inspiration/GetFmClipboard.ps1.
It works similarly to how we will want the Convert FileMaker Clipboard To XML to work.


## Development

- Run the plugin in a sandbox IDE: `./gradlew runIde` (or `gradlew.bat runIde` on Windows)
- Run tests: `./gradlew test`
- The project uses JUnit 5 for unit tests and a GitHub Actions workflow to build and run tests on pushes/PRs.
