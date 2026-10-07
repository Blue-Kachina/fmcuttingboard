package dev.fmcuttingboard.language.validation;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * End-to-end annotator behavior on real PSI. Until the run-once guard was fixed, the annotator never ran in a real
 * project, so these pin down what users now see. Markup: {@code <error descr="…">} / {@code <weak_warning …>}.
 */
public class AnnotatorHighlightingTest extends BasePlatformTestCase {

    private void check(String calc) {
        myFixture.configureByText("calc.fmcalc", calc);
        myFixture.checkHighlighting(true, false, true);
    }

    public void testArgumentCounts() {
        check("<error descr=\"Too few arguments for Left: expected at least 2, got 1\">Left ( x )</error>");
        check("<error descr=\"Too many arguments for If: expected 3, got 4\">If ( a ; b ; c ; d )</error>");
        check("If ( a ; b ) & If ( a ; b ; c ) & Case ( a ; 1 ; b ; 2 ; 3 )");
    }

    public void testSubstituteAcceptsBracketedPairs() {
        // Claris: Substitute ( text ; [ search1 ; replace1 ] ; [ search2 ; replace2 ] ; … )
        check("Substitute ( t ; \"a\" ; \"A\" ) & Substitute ( t ; [ \"a\" ; \"A\" ] ) & Substitute ( t ; [ \"a\" ; \"A\" ] ; [ \"b\" ; \"B\" ] )");
    }

    public void testUnknownFunctionsAreNotReportedWhileTheListIsIncomplete() {
        check("PatternCount ( t ; \"a\" ) + MyCustomFunction ( 1 ) + JSONGetElement ( j ; \"k\" )");
    }

    public void testBracketsInsideStringsAndCommentsAreIgnored() {
        check("\"a)\" & \"]\" // )\n& /* ] */ 1");
        check("1 <error descr=\"Unmatched closing )\">)</error>");
    }

    public void testUnterminatedString() {
        check("Left ( <error descr=\"Unterminated text constant (missing closing quotation mark)\">\"</error>abc ; 1 )");
    }

    public void testSharedSampleCalculationsHaveNoFalseWarnings() throws IOException {
        for (String sample : new String[] {"basic", "comments_strings", "nested", "empty"}) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("fmcalc/" + sample + ".fmcalc")) {
                assertNotNull(sample, in);
                check(new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n"));
            }
        }
    }
}
