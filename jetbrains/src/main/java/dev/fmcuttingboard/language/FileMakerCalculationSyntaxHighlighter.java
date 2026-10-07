package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Syntax highlighter for the FileMaker Calculation language (Phase 3.1).
 */
public class FileMakerCalculationSyntaxHighlighter extends SyntaxHighlighterBase {

    // TextAttributesKey constants, falling back to the active color scheme's defaults
    public static final TextAttributesKey KEYWORD_CONTROL_FLOW = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_CONTROL_FLOW", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey KEYWORD_LOGICAL = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_LOGICAL", DefaultLanguageHighlighterColors.KEYWORD);

    public static final TextAttributesKey KEYWORD_TYPE = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_TYPE", DefaultLanguageHighlighterColors.KEYWORD);

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
        if (tokenType == FileMakerCalculationTokenType.KEYWORD_CONTROL) {
            return pack(KEYWORD_CONTROL_FLOW);
        }
        if (tokenType == FileMakerCalculationTokenType.KEYWORD_LOGICAL) {
            return pack(KEYWORD_LOGICAL);
        }
        if (tokenType == FileMakerCalculationTokenType.KEYWORD_TYPE) {
            return pack(KEYWORD_TYPE);
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
