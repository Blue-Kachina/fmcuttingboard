package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Basic smoke tests for the FileMaker Calculation lexer (Phase 2.2 – Test lexer with sample code).
 *
 * These tests intentionally avoid asserting exact offsets and instead verify that key
 * tokens are recognized from a representative sample.
 */
public class FileMakerCalculationLexerTest {

    private static List<IElementType> tokenize(String text) {
        Lexer lexer = new FileMakerCalculationLexerAdapter();
        lexer.start(text);
        List<IElementType> tokens = new ArrayList<>();
        for (IElementType t = lexer.getTokenType(); t != null; lexer.advance(), t = lexer.getTokenType()) {
            tokens.add(t);
        }
        return tokens;
    }

    @Test
    public void smoke_sampleCalculation_tokensRecognized() {
        String sample = "Let( [ a = Abs(-3.2); b = 5 ], If(a > b and not IsEmpty(Get(AccountName)); \"hi\"; 'lo') ) // end";

        List<IElementType> tokens = tokenize(sample);

        // Filter out whitespace and comments for simple contains checks
        List<String> names = tokens.stream()
                .filter(t -> t != FileMakerCalculationTokenType.WHITE_SPACE)
                .filter(t -> t != FileMakerCalculationTokenType.LINE_COMMENT)
                .map(Object::toString)
                .toList();

        // Expect function keywords
        assertTrue(names.contains("KEYWORD_FUNCTION"), "Should recognize function keywords like Let/Abs/If/Get/IsEmpty");

        // Expect logical keywords (and/not)
        assertTrue(names.contains("KEYWORD_LOGICAL"), "Should recognize logical keywords 'and'/'not'");

        // Expect numbers and strings
        assertTrue(names.contains("NUMBER"), "Should recognize numeric literals");
        assertTrue(names.contains("STRING"), "Should recognize string literals for both \"...\" and '...'");

        // Expect operators and identifiers
        assertTrue(names.contains("OPERATOR"), "Should recognize operators like ( ) ; = >");
        assertTrue(names.contains("IDENTIFIER"), "Should recognize identifiers such as variable names and field names");
    }
}
