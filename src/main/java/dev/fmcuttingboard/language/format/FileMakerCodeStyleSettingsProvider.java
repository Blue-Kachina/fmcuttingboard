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
        // Use default common settings; future work can expose custom options (Phase 5.3 details)
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
        // Conservative 2-space indentation, no tabs by default
        indent.INDENT_SIZE = 2;
        indent.CONTINUATION_INDENT_SIZE = 2;
        indent.TAB_SIZE = 2;
        settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        settings.SPACE_AROUND_LOGICAL_OPERATORS = true;
        settings.SPACE_AROUND_EQUALITY_OPERATORS = true;
        settings.SPACE_WITHIN_PARENTHESES = false;
        return settings;
    }

    // Not all platform versions declare this in superclass; keep without @Override for compatibility
    public @Nullable FileType getFileType() {
        // Required so the preview panel uses correct syntax highlighting and formatting
        return FileMakerCalculationFileType.INSTANCE;
    }
}
