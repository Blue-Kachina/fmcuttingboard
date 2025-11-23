package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5.1 – Manual Testing via automated proxies.
 *
 * Loads sample .fmcalc files from test resources and verifies that the lexer
 * recognizes core token groups: functions, operators, strings, comments, numbers, identifiers.
 */
public class Phase5ManualSamplesTest {

    private static String readResource(String path) {
        InputStream in = Phase5ManualSamplesTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) throw new IllegalStateException("Missing test resource: " + path);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

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
    public void sample_basic_highlights_core_tokens() {
        String text = readResource("test-snippets/samples/basic.fmcalc");
        List<IElementType> tokens = tokenize(text);

        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.KEYWORD_FUNCTION), "Should contain function tokens");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.OPERATOR), "Should contain operators");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.IDENTIFIER), "Should contain identifiers");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.NUMBER), "Should contain numbers");
    }

    @Test
    public void sample_comments_and_strings_are_highlighted() {
        String text = readResource("test-snippets/samples/comments_strings.fmcalc");
        List<IElementType> tokens = tokenize(text);

        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.LINE_COMMENT), "Should contain line comment");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.BLOCK_COMMENT), "Should contain block comment");
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.STRING), "Should contain strings");
    }

    @Test
    public void sample_nested_expressions_and_functions() {
        String text = readResource("test-snippets/samples/nested.fmcalc");
        List<IElementType> tokens = tokenize(text);

        // Expect both control/logical keywords and functions
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.KEYWORD_LOGICAL
                || t == FileMakerCalculationTokenType.KEYWORD_CONTROL), "Should contain control/logical keywords");
        // Ensure parentheses and semicolons recognized as operators
        assertTrue(tokens.stream().anyMatch(t -> t == FileMakerCalculationTokenType.OPERATOR), "Should contain punctuation operators");
    }

    @Test
    public void sample_empty_file_yields_no_tokens() {
        String text = readResource("test-snippets/samples/empty.fmcalc");
        List<IElementType> tokens = tokenize(text);
        // Only whitespace at most
        assertFalse(tokens.stream().anyMatch(t -> t != FileMakerCalculationTokenType.WHITE_SPACE), "Empty file should produce no significant tokens");
    }
}
