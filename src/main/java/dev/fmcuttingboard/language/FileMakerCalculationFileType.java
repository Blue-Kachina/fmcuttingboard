package dev.fmcuttingboard.language;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * File type for FileMaker Calculation language (.fmcalc)
 * Phase 1.1: Language & File Type Registration
 */
public final class FileMakerCalculationFileType extends LanguageFileType {

    public static final FileMakerCalculationFileType INSTANCE = new FileMakerCalculationFileType();

    private FileMakerCalculationFileType() {
        super(FileMakerCalculationLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "FileMaker Calculation";
    }

    @Override
    public @NotNull String getDescription() {
        return "FileMaker Calculation";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "fmcalc";
    }

    @Override
    public @Nullable Icon getIcon() {
        return IconLoader.getIcon("/icons/pluginIcon16.svg", FileMakerCalculationFileType.class);
    }
}
