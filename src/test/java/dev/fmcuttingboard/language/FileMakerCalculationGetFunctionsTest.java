package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that multiple Get() variants are recognized as function keywords by the lexer.
 * We intentionally do not try to highlight the argument names (e.g., AccountName) as keywords;
 * only the function identifier "Get" should be KEYWORD_FUNCTION.
 */
public class FileMakerCalculationGetFunctionsTest {

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
    public void multiple_get_calls_are_marked_as_function_keywords() {
        String sample = String.join("; ", List.of(
                "Get(AccountName)",
                "Get(HostName)",
                "Get(LastError)",
                "Get(SystemPlatform)",
                "Get(WindowWidth)",
                "Get(UserName)",
                "Get(CurrentDate)",
                "Get(CurrentTimestamp)",
                "Get(RecordNumber)",
                "Get(FoundCount)"
        ));

        List<IElementType> tokens = tokenize(sample);

        long functionCount = tokens.stream()
                .filter(t -> t == FileMakerCalculationTokenType.KEYWORD_FUNCTION)
                .count();

        // We expect exactly 10 occurrences of the Get function
        assertEquals(10, functionCount, "Each Get(...) call should produce a KEYWORD_FUNCTION token for 'Get'");
    }
}
