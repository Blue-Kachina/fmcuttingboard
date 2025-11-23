package dev.fmcuttingboard.language.format;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal Code Style settings provider for FileMaker calculations.
 * Exposes the standard code style UI and a preview sample. Custom options
 * will be added in later iterations if needed.
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
}
