# Fix Plugin Verifier deprecation warnings (docs/compatibility_verification.md)

## Context

The JetBrains Marketplace Plugin Verifier run for FMCuttingBoard 1.0.5 flagged 1 "scheduled for removal" API usage and 6 deprecated API usages (`docs/compatibility_verification.md`). These are warnings, not errors — the plugin is still "Success / Compatible" on IC 2024.2.6 through 2026.2.1 EAP — but the deprecated APIs will eventually break as newer IDE releases drop them, so they should be cleaned up while the plugin is still actively worked on.

I cross-checked the doc's summary against the actual verifier output on disk (`build/reports/pluginVerifier/IC-*/plugins/dev.bluekachina.fmcuttingboard/1.0.5/deprecated-usages.txt`) and against the real IntelliJ 2024.3 platform classes (via `javap` on the cached Gradle `ideaIC-2024.3-win` distribution), so the replacements below are confirmed to exist in the platform version this plugin targets (`sinceBuild=242`, `platformVersion=2024.3`, no `untilBuild` cap — see `build.gradle.kts:37,69`).

Note: the doc lists item 2 (`TextAttributesKey.createTextAttributesKey`) as "1 usage" — this is correct per the verifier (it groups all 7 call sites in the same static initializer as one usage entry), even though there are 7 actual call sites in source. All 7 need fixing.

One item in the doc (item 1, "Scheduled for Removal API Usages") wasn't identified by name in the doc — the real verifier report shows it's `FormattingModelBuilder.createModel(PsiElement, CodeStyleSettings)`, overridden in `FileMakerCalculationFormattingModelBuilder`, explicitly marked "This method will be removed in a future release."

Item 4 in the doc (dynamic plugin unloading) requires no code change — it's already satisfied (no `application-components`/`project-components`/`ApplicationComponent` legacy registrations exist), and there's no explicit "declare dynamic" attribute in current plugin.xml schema; the platform infers it automatically.

## Changes

### 1. `src/main/java/dev/fmcuttingboard/ui/FmXmlSnippetNotificationProvider.java` — migrate off `EditorNotifications.Provider`

Change the class to implement `com.intellij.ui.EditorNotificationProvider` instead of extending `EditorNotifications.Provider<EditorNotificationPanel>`. Confirmed via `javap` that the new interface is:

```java
public interface EditorNotificationProvider extends PossiblyDumbAware {
    Function<? super FileEditor, ? extends JComponent> collectNotificationData(Project project, VirtualFile file);
}
```

Replace `getKey()` + `createNotificationPanel(VirtualFile, FileEditor, Project)` with a single `collectNotificationData(Project, VirtualFile)` that does the existing file-type/text checks up front and returns a `Function<FileEditor, JComponent>` lambda that builds the panel (moving the `instanceof TextEditor` check inside the lambda, since the new API doesn't receive the `FileEditor` until inside that function). No `Key<EditorNotificationPanel>` is needed anymore.

`plugin.xml`'s `<editorNotificationProvider implementation="dev.fmcuttingboard.ui.FmXmlSnippetNotificationProvider"/>` registration (line 32) does not need to change — same extension point works for the new interface.

No existing tests reference this class (confirmed via grep of `src/test`).

### 2. `src/main/java/dev/fmcuttingboard/language/folding/FileMakerCalculationFoldingBuilder.java` — `FoldingDescriptor.EMPTY` → `FoldingDescriptor.EMPTY_ARRAY`

Line 63: `return FoldingDescriptor.EMPTY;` → `return FoldingDescriptor.EMPTY_ARRAY;`. Confirmed `EMPTY_ARRAY` exists on the class (same type, `FoldingDescriptor[]`).

### 3. `src/main/java/dev/fmcuttingboard/language/FileMakerCalculationSyntaxHighlighter.java` — two fixes

**a) `SyntaxHighlighterBase.EMPTY` → `TextAttributesKey.EMPTY_ARRAY`**
Line 87 (`return EMPTY;` inside `getTokenHighlights`) → `return TextAttributesKey.EMPTY_ARRAY;`. Confirmed this public static field exists on `TextAttributesKey`.

**b) `TextAttributesKey.createTextAttributesKey(String, TextAttributes)` → `createTextAttributesKey(String, TextAttributesKey)` with fallback keys**

Per user decision, switch all 7 keys to the fallback-key overload, mapping to the closest `DefaultLanguageHighlighterColors` constant (confirmed these all exist via `javap`). This means colors will now follow the user's IDE color scheme/theme instead of the current fixed hex values — an intentional tradeoff for a one-line-per-key fix instead of bundling a custom color-scheme XML.

Mapping:
| Current key | Fallback |
|---|---|
| `KEYWORD_CONTROL_FLOW` | `DefaultLanguageHighlighterColors.KEYWORD` |
| `KEYWORD_LOGICAL` | `DefaultLanguageHighlighterColors.KEYWORD` |
| `KEYWORD_TYPE` | `DefaultLanguageHighlighterColors.KEYWORD` |
| `FUNCTION` | `DefaultLanguageHighlighterColors.FUNCTION_CALL` |
| `COMMENT` | `DefaultLanguageHighlighterColors.LINE_COMMENT` |
| `STRING` | `DefaultLanguageHighlighterColors.STRING` |
| `NUMBER` | `DefaultLanguageHighlighterColors.NUMBER` |
| `OPERATOR` | `DefaultLanguageHighlighterColors.OPERATION_SIGN` |

Example:
```java
public static final TextAttributesKey KEYWORD_CONTROL_FLOW = TextAttributesKey.createTextAttributesKey(
        "FM_CALC_KEYWORD_CONTROL_FLOW", DefaultLanguageHighlighterColors.KEYWORD);
```
Drop the now-unused `TextAttributes`/`Color`/`Font` imports and the inline hex-color comments once all 7 are converted (`BAD_CHAR` stays as-is; it already uses `HighlighterColors.BAD_CHARACTER`).

### 4. `src/main/java/dev/fmcuttingboard/language/format/FileMakerCodeStyleSettingsProvider.java` — `getDefaultCommonSettings()` → `customizeDefaults(...)`

Confirmed via `javap` that `LanguageCodeStyleSettingsProvider` has a non-deprecated `protected void customizeDefaults(CommonCodeStyleSettings settings, CommonCodeStyleSettings.IndentOptions indentOptions)` designed exactly for this purpose (customize only the fields that differ from language defaults, instead of constructing the whole settings object).

Replace the current override (lines 132-149) with:
```java
@Override
protected void customizeDefaults(@NotNull CommonCodeStyleSettings settings,
                                  @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
    indentOptions.USE_TAB_CHARACTER = true;
    indentOptions.TAB_SIZE = 4;
    indentOptions.INDENT_SIZE = 4;
    indentOptions.CONTINUATION_INDENT_SIZE = 8;

    settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
    settings.SPACE_AROUND_RELATIONAL_OPERATORS = true;
    settings.SPACE_AROUND_ADDITIVE_OPERATORS = true;
    settings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;

    settings.SPACE_BEFORE_IF_PARENTHESES = true;
    settings.SPACE_BEFORE_SWITCH_PARENTHESES = true;
    settings.SPACE_BEFORE_WHILE_PARENTHESES = true;

    settings.SPACE_WITHIN_PARENTHESES = false;
}
```
Remove the `getDefaultCommonSettings()` override entirely (the base class's non-deprecated implementation will call `customizeDefaults` internally).

### 5. `src/main/java/dev/fmcuttingboard/language/format/FileMakerCalculationFormattingModelBuilder.java` — remove scheduled-for-removal override

Delete the legacy `createModel(@NotNull PsiElement element, @NotNull CodeStyleSettings settings)` override (lines 50-57), which only exists "for backwards compatibility with older IDEs." Since `sinceBuild=242` (2024.2) and the modern `createModel(FormattingContext)` overload (already implemented, lines 39-48) has existed since well before that baseline, the legacy override is unneeded and the interface's own default handles it.

### 6. No code change needed for dynamic-plugin-unloading suggestion (doc item 4) — already satisfied.

## Verification

1. `./gradlew build` — confirm the project still compiles cleanly with Java 21 / IC 2024.3 APIs.
2. `./gradlew test` — run existing unit tests (none currently reference the touched classes directly, but this catches any regressions in dependent code).
3. `./gradlew verifyPlugin` — re-run the Plugin Verifier and confirm `deprecated-usages.txt` under `build/reports/pluginVerifier/*/plugins/dev.bluekachina.fmcuttingboard/*/` is now empty (or at least no longer lists these 8 entries).
4. Manually sanity-check in a sandbox IDE (`./gradlew runIde`): open an XML file containing `<fmxmlsnippet` to confirm the notification banner still appears and the "Send To FileMaker Clipboard" button still works; open a `.fmcalc` file to confirm syntax highlighting, code folding, and Code Style settings (Settings → Editor → Code Style → FileMaker Calculation) still render correctly with the new theme-following colors.
