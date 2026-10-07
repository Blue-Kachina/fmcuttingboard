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

    // Braces and punctuation (for brace matching/folding)
    public static final IElementType LPAREN = token("LPAREN");
    public static final IElementType RPAREN = token("RPAREN");
    public static final IElementType LBRACKET = token("LBRACKET");
    public static final IElementType RBRACKET = token("RBRACKET");
    public static final IElementType LBRACE = token("LBRACE");
    public static final IElementType RBRACE = token("RBRACE");

    // Syntax-level tokens from the lexer
    public static final IElementType KEYWORD_LOGICAL = token("KEYWORD_LOGICAL"); // and, or, xor, not (any case)
    public static final IElementType FIELD_REFERENCE = token("FIELD_REFERENCE"); // Table::Field
    public static final IElementType QUOTED_NAME = token("QUOTED_NAME"); // ${ reserved name }
    public static final IElementType PARAGRAPH_MARK = token("PARAGRAPH_MARK"); // ¶ outside strings

    // Identifier classifications made by FileMakerCalculationLexerAdapter (from shared data)
    public static final IElementType KEYWORD_FUNCTION = token("KEYWORD_FUNCTION"); // any name followed by "("
    public static final IElementType GET_CONSTANT = token("GET_CONSTANT"); // X in Get ( X )
    public static final IElementType CONSTANT = token("CONSTANT"); // True, JSONString, Bold, ...

    @NotNull
    private static IElementType token(@NonNls @NotNull String debugName) {
        return new IElementType(debugName, FileMakerCalculationLanguage.INSTANCE);
    }
}
