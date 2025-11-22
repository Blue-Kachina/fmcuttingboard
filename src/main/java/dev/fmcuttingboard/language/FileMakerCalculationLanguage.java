package dev.fmcuttingboard.language;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

/**
 * Core language definition for FileMaker Calculation language.
 * Phase 1.1: Language & File Type Registration
 */
public final class FileMakerCalculationLanguage extends Language {

    public static final FileMakerCalculationLanguage INSTANCE = new FileMakerCalculationLanguage();

    private FileMakerCalculationLanguage() {
        super("FileMakerCalculation");
    }

    @Override
    public boolean isCaseSensitive() {
        // Notepad++ XML: caseIgnored="no"
        return true;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "FileMaker Calculation";
    }
}
