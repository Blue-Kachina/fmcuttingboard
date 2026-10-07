package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Syntax highlighter for the FileMaker Calculation language. Token classification (functions, Get constants,
 * named constants) comes from FileMakerCalculationLexerAdapter.
 */
public class FileMakerCalculationSyntaxHighlighter extends SyntaxHighlighterBase {

    // TextAttributesKey constants, falling back to the active color scheme's defaults
    public static final TextAttributesKey KEYWORD_LOGICAL = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_LOGICAL", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey CONSTANT = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);

    public static final TextAttributesKey GET_CONSTANT = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_GET_CONSTANT", DefaultLanguageHighlighterColors.CONSTANT);

    public static final TextAttributesKey FIELD = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD);

    public static final TextAttributesKey PARAGRAPH_MARK = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_PARAGRAPH_MARK", DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE);

    public static final TextAttributesKey FUNCTION = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_FUNCTION", DefaultLanguageHighlighterColors.FUNCTION_CALL);

    public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);

    public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_STRING", DefaultLanguageHighlighterColors.STRING);

    public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_NUMBER", DefaultLanguageHighlighterColors.NUMBER);

    public static final TextAttributesKey OPERATOR = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN);

    public static final TextAttributesKey BAD_CHAR = HighlighterColors.BAD_CHARACTER;

    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new FileMakerCalculationLexerAdapter();
    }

    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (tokenType == FileMakerCalculationTokenType.KEYWORD_LOGICAL) {
            return pack(KEYWORD_LOGICAL);
        }
        if (tokenType == FileMakerCalculationTokenType.CONSTANT) {
            return pack(CONSTANT);
        }
        if (tokenType == FileMakerCalculationTokenType.GET_CONSTANT) {
            return pack(GET_CONSTANT);
        }
        if (tokenType == FileMakerCalculationTokenType.FIELD_REFERENCE || tokenType == FileMakerCalculationTokenType.QUOTED_NAME) {
            return pack(FIELD);
        }
        if (tokenType == FileMakerCalculationTokenType.PARAGRAPH_MARK) {
            return pack(PARAGRAPH_MARK);
        }
        if (tokenType == FileMakerCalculationTokenType.KEYWORD_FUNCTION) {
            return pack(FUNCTION);
        }
        if (tokenType == FileMakerCalculationTokenType.LINE_COMMENT || tokenType == FileMakerCalculationTokenType.BLOCK_COMMENT) {
            return pack(COMMENT);
        }
        if (tokenType == FileMakerCalculationTokenType.STRING) {
            return pack(STRING);
        }
        if (tokenType == FileMakerCalculationTokenType.NUMBER) {
            return pack(NUMBER);
        }
        if (tokenType == FileMakerCalculationTokenType.OPERATOR) {
            return pack(OPERATOR);
        }
        if (tokenType == FileMakerCalculationTokenType.BAD_CHARACTER) {
            return pack(BAD_CHAR);
        }
        return TextAttributesKey.EMPTY_ARRAY;
    }
}
