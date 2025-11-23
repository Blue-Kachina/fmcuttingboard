package dev.fmcuttingboard.language.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import dev.fmcuttingboard.language.FileMakerCalculationFileType;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal PSI file for FileMaker Calculation language.
 * Provides a concrete PsiFile so we can wire a ParserDefinition and build a PSI tree.
 */
public class FileMakerCalculationFile extends PsiFileBase {

    public FileMakerCalculationFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, FileMakerCalculationLanguage.INSTANCE);
    }

    @Override
    public @NotNull FileType getFileType() {
        return FileMakerCalculationFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "FileMaker Calculation File";
    }
}
