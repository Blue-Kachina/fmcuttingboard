package dev.fmcuttingboard.language;

import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Token type definitions for the FileMaker Calculation language lexer.
 * These are returned by the generated JFlex lexer.
 */
public final class FileMakerCalculationTokenType {

    private FileMakerCalculationTokenType() {}

    public static final IElementType WHITE_SPACE = TokenType.WHITE_SPACE;
    public static final IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    // Generic groups
    public static final IElementType IDENTIFIER = token("IDENTIFIER");
    public static final IElementType NUMBER = token("NUMBER");
    public static final IElementType STRING = token("STRING");
    public static final IElementType LINE_COMMENT = token("LINE_COMMENT");
    public static final IElementType BLOCK_COMMENT = token("BLOCK_COMMENT");
    public static final IElementType OPERATOR = token("OPERATOR");

    // Keyword groups (Phase 2.1)
    public static final IElementType KEYWORD_CONTROL = token("KEYWORD_CONTROL"); // if, case
    public static final IElementType KEYWORD_LOGICAL = token("KEYWORD_LOGICAL"); // and, or, not
    public static final IElementType KEYWORD_TYPE = token("KEYWORD_TYPE"); // boolean, int, etc
    public static final IElementType KEYWORD_FUNCTION = token("KEYWORD_FUNCTION"); // functions like Abs, Date, Get(), etc

    @NotNull
    private static IElementType token(@NonNls @NotNull String debugName) {
        return new IElementType(debugName, FileMakerCalculationLanguage.INSTANCE);
    }
}
