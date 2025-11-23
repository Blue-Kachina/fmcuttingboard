package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5.2 – Edge Cases
 */
public class Phase5EdgeCasesTest {

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
    public void mixed_case_keywords_should_not_match_lowercase_keywords() {
        List<IElementType> tokens = tokenize("AnD OR Not and or not");
        // lower case should be recognized as logical; mixed case should be identifiers
        assertTrue(tokens.contains(FileMakerCalculationTokenType.KEYWORD_LOGICAL), "lowercase logical keywords should be recognized");
        assertTrue(tokens.contains(FileMakerCalculationTokenType.IDENTIFIER), "mixed-case variants should be identifiers");
    }

    @Test
    public void multiline_calculation_tokenizes_across_newlines() {
        String sample = "Let(\n  [ a = 1; b = 2 ],\n  If(\n    a ≤ b;\n    \"ok\";\n    \"ng\"\n  )\n)";
        List<IElementType> tokens = tokenize(sample);
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.KEYWORD_FUNCTION), "Should have functions Let/If");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.OPERATOR), "Should have operators/parentheses/semicolon");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.STRING), "Should have strings");
    }

    @Test
    public void special_operator_symbols_are_recognized() {
        List<IElementType> tokens = tokenize("1 ≥ 0; 2 ≤ 3; 4 ≠ 5");
        long opCount = tokens.stream().filter(t -> t == FileMakerCalculationTokenType.OPERATOR).count();
        assertTrue(opCount >= 3, "Should recognize ≥ ≤ ≠ as operators");
    }

    @Test
    public void unicode_in_strings_supported() {
        List<IElementType> tokens = tokenize("\"カタカナ ひらがな 漢字\"; Hiragana('あいう')");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.STRING), "Unicode strings should be tokenized as STRING");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.KEYWORD_FUNCTION), "Hiragana should be recognized as function");
    }

    @Test
    public void unterminated_string_results_in_bad_character() {
        List<IElementType> tokens = tokenize("\"unterminated");
        // Expect at least one bad character token due to missing closing quote
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.BAD_CHARACTER), "Unterminated string should emit BAD_CHARACTER");
    }

    @Test
    public void unterminated_block_comment_stays_in_comment_state() {
        List<IElementType> tokens = tokenize("/* comment without end\nline two");
        assertFalse(tokens.isEmpty(), "Should produce tokens");
        // All produced tokens should be BLOCK_COMMENT after the opening token
        assertTrue(tokens.stream().allMatch(t -> t == FileMakerCalculationTokenType.BLOCK_COMMENT), "Unterminated comment should remain BLOCK_COMMENT tokens");
    }
}
