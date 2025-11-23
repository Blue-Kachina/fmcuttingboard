package dev.fmcuttingboard.language.format;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal code style provider to expose common settings (indent, spaces around operators, etc.)
 * for FileMaker Calculation files. Formatting rules rely primarily on platform common settings.
 */
public class FileMakerCalculationCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @Override
    public @NotNull Language getLanguage() {
        return FileMakerCalculationLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        // Expose commonly useful groups
        if (settingsType == SettingsType.SPACING_SETTINGS) {
            consumer.showStandardOptions(
                    "SPACE_AROUND_ASSIGNMENT_OPERATORS",
                    "SPACE_AROUND_LOGICAL_OPERATORS",
                    "SPACE_AROUND_EQUALITY_OPERATORS",
                    "SPACE_AROUND_RELATIONAL_OPERATORS",
                    "SPACE_AROUND_ADDITIVE_OPERATORS",
                    "SPACE_AROUND_MULTIPLICATIVE_OPERATORS",
                    "SPACE_AFTER_COMMA",
                    "SPACE_BEFORE_COMMA"
            );
        } else if (settingsType == SettingsType.INDENT_SETTINGS) {
            consumer.showStandardOptions(
                    "INDENT_SIZE",
                    "CONTINUATION_INDENT_SIZE",
                    "TAB_SIZE",
                    "USE_TAB_CHARACTER"
            );
        }
    }

    @Override
    public CommonCodeStyleSettings getDefaultCommonSettings() {
        CommonCodeStyleSettings s = new CommonCodeStyleSettings(FileMakerCalculationLanguage.INSTANCE);
        CommonCodeStyleSettings.IndentOptions indent = s.initIndentOptions();
        indent.INDENT_SIZE = 2;
        indent.TAB_SIZE = 2;
        indent.CONTINUATION_INDENT_SIZE = 2;
        s.SPACE_AFTER_COMMA = true;
        s.SPACE_BEFORE_COMMA = false;
        // Prefer spaces around operators by default for readability
        s.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        s.SPACE_AROUND_LOGICAL_OPERATORS = true;
        s.SPACE_AROUND_EQUALITY_OPERATORS = true;
        s.SPACE_AROUND_RELATIONAL_OPERATORS = true;
        s.SPACE_AROUND_ADDITIVE_OPERATORS = true;
        s.SPACE_AROUND_MULTIPLICATIVE_OPERATORS = true;
        return s;
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return "Let(\n" +
               "  [ a = Abs(-3.14); b = Round(Sin(1.234); 3) ],\n" +
               "  If( a > b and not IsEmpty(Get(AccountName)); \"x\"; \"y\" )\n" +
               ")";
    }
}
