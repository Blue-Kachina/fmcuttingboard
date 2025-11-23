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

    // Parser element types (Phase 4)
    public static final IElementType FUNCTION_CALL = new FileMakerCalculationElementType("FUNCTION_CALL");
    public static final IElementType ARG_LIST = new FileMakerCalculationElementType("ARG_LIST");
    public static final IElementType ARGUMENT = new FileMakerCalculationElementType("ARGUMENT");
    public static final IElementType PAREN_EXPRESSION = new FileMakerCalculationElementType("PAREN_EXPRESSION");
    public static final IElementType IDENTIFIER_EXPRESSION = new FileMakerCalculationElementType("IDENTIFIER_EXPRESSION");
    public static final IElementType LITERAL = new FileMakerCalculationElementType("LITERAL");
}
