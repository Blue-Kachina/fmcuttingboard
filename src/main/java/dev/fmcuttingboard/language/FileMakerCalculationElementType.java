package dev.fmcuttingboard.language;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * PSI element types for the FileMaker Calculation language parser phase (reserved for future).
 * Present to satisfy Phase 2 scaffolding.
 */
public class FileMakerCalculationElementType extends IElementType {
    public FileMakerCalculationElementType(@NonNls @NotNull String debugName) {
        super(debugName, FileMakerCalculationLanguage.INSTANCE);
    }
}
