package dev.fmcuttingboard.language;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 5.3 – Integration Testing
 */
public class Phase5IntegrationTest {

    @Test
    public void file_type_basic_recognition() {
        FileType ft = FileMakerCalculationFileType.INSTANCE;
        assertEquals("fmcalc", ft.getDefaultExtension());
        assertEquals("FileMaker Calculation", ft.getName());
        assertNotNull(ft.getIcon(), "Icon should be present");
    }

    @Test
    public void syntax_highlighter_instantiates() {
        var highlighter = new FileMakerCalculationSyntaxHighlighter();
        assertNotNull(highlighter.getHighlightingLexer(), "Highlighting lexer should be available");
    }

    @Test
    public void large_input_tokenization_completes_quickly() {
        // Build a large calculation by repetition
        String unit = "Let([a=Abs(-3.14159);b=Round(Sin(1.2345);3)]; If(a>b and not IsEmpty(Get(AccountName)); \"x\"; \"y\"))";
        StringBuilder sb = new StringBuilder(unit.length() * 2000);
        for (int i = 0; i < 2000; i++) sb.append(unit).append("\n");
        String large = sb.toString();

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            var lexer = new FileMakerCalculationLexerAdapter();
            lexer.start(large);
            int count = 0;
            while (lexer.getTokenType() != null) { lexer.advance(); count++; }
            assertTrue(count > 0, "Should produce tokens for large input");
        }, "Tokenization should finish within timeout for large inputs");
    }
}
