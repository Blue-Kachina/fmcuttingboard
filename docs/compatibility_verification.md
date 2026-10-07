Here is the breakdown of the critical findings, warnings, and suggestions from your JetBrains Marketplace Plugin Verifier run for **FMCuttingBoard 1.0.5**:

---

### 1. Scheduled for Removal API Usages (1 Warning)

* **Count:** 1 usage detected.
* **Impact:** Scheduled for removal APIs will cause binary or source code breakage in upcoming IntelliJ Platform releases.

> *Note: Check your build logs/verifier details to identify the specific API line, as it is grouped under the verification summary.*

---

### 2. Deprecated API Usages (6 Warnings)

#### Deprecated Classes (2 usages)

* `EditorNotifications.Provider` (2 usages)
* *Alternative/Replacement:* Migrate to using `EditorNotificationProvider` (or the extension point `com.intellij.editorNotificationProvider`).



#### Deprecated Fields (2 usages)

* `FoldingDescriptor.EMPTY` (1 usage)
* `SyntaxHighlighterBase.EMPTY` (1 usage)
* *Alternative/Replacement:* Use standard empty arrays/collections or factory methods defined in their respective classes (e.g., `FoldingDescriptor.EMPTY_ARRAY` or standard array initializers).



#### Deprecated Methods (2 usages)

* `TextAttributesKey.createTextAttributesKey(...)` (1 usage)
* *Alternative/Replacement:* Check the parameter overload being passed; standard replacements usually use the overload taking `ExternalInfo` or `TextAttributes`.


* `LanguageCodeStyleSettingsProvider.getDefaultCommonSettings()` (1 usage)
* *Alternative/Replacement:* Configure common settings directly via language settings builders or override `customizeSettings(...)`.



---

### 3. Dependency & Installation Status

* **Status:** No errors or missing required plugin/module dependencies were reported during the verification runs.
* **Compatibility Verdict:** **Success / Compatible with warnings** across all tested build targets (including IntelliJ IDEA 2024.2.6 through 2026.2.1 EAP).

---

### 4. Marketplace Suggestions & Capabilities

* **Dynamic Plugin Unloading:**
* `Plugin can probably be enabled or disabled without IDE restart`
* *Action Item:* Ensure your plugin explicitly marks itself as dynamic in `plugin.xml` (or verified as dynamic) so users don't need to restart their IDE upon installation or update.