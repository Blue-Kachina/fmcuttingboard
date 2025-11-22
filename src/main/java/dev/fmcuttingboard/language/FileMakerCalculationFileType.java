package dev.fmcuttingboard.language;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
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
        // Temporary default icon until a dedicated one is added in a later phase
        return AllIcons.FileTypes.Any_type;
    }
}
