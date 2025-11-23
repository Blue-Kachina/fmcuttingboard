package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

/**
 * Syntax highlighter for the FileMaker Calculation language (Phase 3.1).
 */
public class FileMakerCalculationSyntaxHighlighter extends SyntaxHighlighterBase {

    // TextAttributesKey constants matching Notepad++ colors
    public static final TextAttributesKey KEYWORD_CONTROL_FLOW = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_CONTROL_FLOW",
            new TextAttributes(new Color(0x00, 0x00, 0xFF), null, null, null, Font.BOLD)); // #0000FF bold

    public static final TextAttributesKey KEYWORD_LOGICAL = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_LOGICAL",
            new TextAttributes(new Color(0x00, 0x66, 0x99), null, null, null, Font.BOLD)); // #006699 bold

    public static final TextAttributesKey KEYWORD_TYPE = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_KEYWORD_TYPE",
            new TextAttributes(new Color(0xFF, 0x80, 0x00), null, null, null, Font.BOLD)); // #FF8000 bold

    public static final TextAttributesKey FUNCTION = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_FUNCTION",
            new TextAttributes(new Color(0x80, 0x00, 0xFF), null, null, null, Font.BOLD)); // #8000FF bold

    public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_COMMENT",
            new TextAttributes(new Color(0x00, 0x80, 0x00), null, null, null, Font.PLAIN)); // #008000

    public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_STRING",
            new TextAttributes(new Color(0xDB, 0x59, 0x9D), null, null, null, Font.PLAIN)); // #DB599D

    public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_NUMBER",
            new TextAttributes(new Color(0xFF, 0x00, 0x00), null, null, null, Font.PLAIN)); // #FF0000

    public static final TextAttributesKey OPERATOR = TextAttributesKey.createTextAttributesKey(
            "FM_CALC_OPERATOR",
            new TextAttributes(new Color(0x80, 0x40, 0x00), null, null, null, Font.BOLD)); // #804000 bold

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
        return EMPTY;
    }
}
