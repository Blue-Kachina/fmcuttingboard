package dev.fmcuttingboard.language.format;

import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
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
}
