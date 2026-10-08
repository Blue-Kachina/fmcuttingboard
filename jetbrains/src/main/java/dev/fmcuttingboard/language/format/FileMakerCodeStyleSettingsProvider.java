package dev.fmcuttingboard.language.format;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.TabbedLanguageCodeStylePanel;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.application.options.SmartIndentOptionsEditor;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationFileType;
import com.intellij.openapi.fileTypes.FileType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Code Style settings provider for FileMaker calculations.
 *
 * Implements a tabbed UI with a live preview backed by the registered
 * FormattingModelBuilder. Tabs: Tabs and Indents (plus "Do not indent top let
 * variables") and Wrapping (the right margin decides when a call breaks).
 */
public class FileMakerCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @Override
    public @NotNull Language getLanguage() {
        return FileMakerCalculationLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        // Only options the formatter uses (FmCalcFormatter). Spacing is fixed Claris style; see docs/fmcalc-formatting.md.
        switch (settingsType) {
            case INDENT_SETTINGS -> {
                consumer.showStandardOptions("USE_TAB_CHARACTER", "TAB_SIZE", "INDENT_SIZE");
                consumer.showCustomOption(
                        FileMakerCustomCodeStyleSettings.class,
                        "DO_NOT_INDENT_TOP_LET_VARIABLES",
                        "Do not indent top let variables",
                        null
                );
            }
            // "Hard wrap at": a call that fits within this width stays on one line
            case WRAPPING_AND_BRACES_SETTINGS -> consumer.showStandardOptions("RIGHT_MARGIN");
            default -> {
                // nothing else is configurable
            }
        }
    }

    /**
     * Ensure the standard "Tabs and Indents" tab is available by providing
     * a concrete indent options editor. Some platform versions require this
     * to render the tab for custom languages.
     */
    @Override
    public IndentOptionsEditor getIndentOptionsEditor() {
        return new SmartIndentOptionsEditor();
    }

    @Override
    public @NotNull com.intellij.psi.codeStyle.CustomCodeStyleSettings createCustomSettings(@NotNull CodeStyleSettings settings) {
        return new FileMakerCustomCodeStyleSettings(settings);
    }

    @Override
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings,
                                                             @NotNull CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, "FileMaker Calculation") {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                // Use a tabbed panel with preview, backed by our formatter
                return new FileMakerTabbedLanguageCodeStylePanel(getCurrentSettings(), settings);
            }
        };
    }

    /**
     * Tabbed code style panel with common tabs and live preview.
     */
    private static class FileMakerTabbedLanguageCodeStylePanel extends TabbedLanguageCodeStylePanel {
        public FileMakerTabbedLanguageCodeStylePanel(CodeStyleSettings currentSettings, CodeStyleSettings settings) {
            super(FileMakerCalculationLanguage.INSTANCE, currentSettings, settings);
        }

        @Override
        protected void initTabs(CodeStyleSettings settings) {
            addIndentOptionsTab(settings);
            addWrappingAndBracesTab(settings);
        }

        @Override
        protected @NotNull FileType getFileType() {
            // Ensure preview uses our fmcalc file type
            return FileMakerCalculationFileType.INSTANCE;
        }
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return "Let([a=Abs(-3.14159);b=Round(Sin(1.2345);3)];\n" +
               "Case(a>b;\"greater\";a<b;\"less\";\"equal\") & If(IsEmpty($note);\"\";\" (\" & $note & \")\"))";
    }

    @Override
    protected void customizeDefaults(@NotNull CommonCodeStyleSettings settings,
                                      @NotNull CommonCodeStyleSettings.IndentOptions indentOptions) {
        // Project defaults as requested
        indentOptions.USE_TAB_CHARACTER = true;
        indentOptions.TAB_SIZE = 4;
        indentOptions.INDENT_SIZE = 4;
    }

    // Not all platform versions declare this in superclass; keep without @Override for compatibility
    public @Nullable FileType getFileType() {
        // Required so the preview panel uses correct syntax highlighting and formatting
        return FileMakerCalculationFileType.INSTANCE;
    }
}
