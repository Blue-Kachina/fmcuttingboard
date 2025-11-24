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
 * FormattingModelBuilder. Tabs include Indents, Spaces, Wrapping and Braces,
 * and Blank Lines. The preview uses the FileMakerCalculation file type for
 * syntax highlighting and formatting.
 */
public class FileMakerCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @Override
    public @NotNull Language getLanguage() {
        return FileMakerCalculationLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        switch (settingsType) {
            case INDENT_SETTINGS:
                // Tabs and Indents
                consumer.showStandardOptions(
                        "USE_TAB_CHARACTER",
                        "TAB_SIZE",
                        "INDENT_SIZE",
                        "CONTINUATION_INDENT_SIZE"
                );
                // Custom option for FileMaker specifics
                consumer.showCustomOption(
                        FileMakerCustomCodeStyleSettings.class,
                        "DO_NOT_INDENT_TOP_LET_VARIABLES",
                        "Do not indent top let variables",
                        null
                );
                break;
            case SPACING_SETTINGS:
                // Spaces: before parentheses and around operators
                consumer.showStandardOptions(
                        // Before parentheses
                        "SPACE_BEFORE_IF_PARENTHESES",
                        "SPACE_BEFORE_SWITCH_PARENTHESES", // closest to 'case' parentheses
                        "SPACE_BEFORE_WHILE_PARENTHESES",
                        // Around operators
                        "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                        "SPACE_AROUND_RELATIONAL_OPERATORS",
                        "SPACE_AROUND_ADDITIVE_OPERATORS",
                        "SPACE_AROUND_MULTIPLICATIVE_OPERATORS"
                );
                break;
            default:
                // Keep the other tabs default for now
                break;
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
            addSpacesTab(settings);
            addWrappingAndBracesTab(settings);
            addBlankLinesTab(settings);
        }

        @Override
        protected @NotNull FileType getFileType() {
            // Ensure preview uses our fmcalc file type
            return FileMakerCalculationFileType.INSTANCE;
        }
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return "Let(\n" +
               "  [ a = Abs(-3.14159); b = Round(Sin(1.2345); 3) ];\n" +
               "  If( a > b; \"x\"; \"y\" )\n" +
               ")";
    }

    @Override
    public @NotNull CommonCodeStyleSettings getDefaultCommonSettings() {
        // Provide safe defaults so the preview panel can render without relying on IDE defaults.
        CommonCodeStyleSettings settings = new CommonCodeStyleSettings(getLanguage());
        CommonCodeStyleSettings.IndentOptions indent = settings.initIndentOptions();
        // Project defaults as requested
        indent.USE_TAB_CHARACTER = true;
        indent.TAB_SIZE = 4;
        indent.INDENT_SIZE = 4;
        indent.CONTINUATION_INDENT_SIZE = 8;

        // Spaces - Around operators
        settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        settings.SPACE_AROUND_RELATIONAL_OPERATORS = true;
        settings.SPACE_AROUND_ADDITIVE_OPERATORS = true;
        settings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;

        // Spaces - Before parentheses
        settings.SPACE_BEFORE_IF_PARENTHESES = true;
        settings.SPACE_BEFORE_SWITCH_PARENTHESES = true; // closest mapping for 'case'
        settings.SPACE_BEFORE_WHILE_PARENTHESES = true;

        // Parentheses interior
        settings.SPACE_WITHIN_PARENTHESES = false;
        return settings;
    }

    // Not all platform versions declare this in superclass; keep without @Override for compatibility
    public @Nullable FileType getFileType() {
        // Required so the preview panel uses correct syntax highlighting and formatting
        return FileMakerCalculationFileType.INSTANCE;
    }
}
