package dev.fmcuttingboard.language;

import com.intellij.lexer.FlexAdapter;

/**
 * Lexer adapter that wraps the generated JFlex lexer for the FileMaker Calculation language.
 * This is used by IntelliJ Platform APIs (e.g., syntax highlighter) to obtain tokens.
 */
public class FileMakerCalculationLexerAdapter extends FlexAdapter {
    public FileMakerCalculationLexerAdapter() {
        // The JFlex-generated lexer uses the standard idea-flex.skeleton which expects a Reader in the ctor
        super(new _FileMakerCalculationLexer((java.io.Reader) null));
    }
}
