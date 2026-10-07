package dev.fmcuttingboard.language.validation;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;

import java.util.List;

/**
 * Let/While variable scoping, on real PSI (parser + annotator). Rules from the Claris help for Let and While; see
 * FunctionVariableScopes.
 */
public class FunctionVariableScopesTest extends BasePlatformTestCase {

    private List<String> outOfScope(String calc) {
        PsiFile file = myFixture.configureByText("calc.fmcalc", calc);
        return FunctionVariableScopes.findOutOfScopeUses(file).stream()
                .map(u -> u.name() + "@" + u.use().getTextRange().getStartOffset())
                .toList();
    }

    private PsiFile parse(String calc) {
        return myFixture.configureByText("calc.fmcalc", calc);
    }

    private long countOf(PsiFile file, Object type) {
        return PsiTreeUtil.collectElements(file, e -> e.getNode() != null && e.getNode().getElementType() == type).length;
    }

    // ----- parser -----

    public void testLetBindingsParseIntoAListOfDefinitions() {
        PsiFile file = parse("Let ( [ a = 1 ; b = a + 1 ] ; a + b )");
        assertEquals(1, countOf(file, FileMakerCalculationElementType.BRACKET_LIST));
        PsiElement list = PsiTreeUtil.collectElements(file,
                e -> e.getNode() != null && e.getNode().getElementType() == FileMakerCalculationElementType.BRACKET_LIST)[0];
        assertEquals(2, list.getChildren().length);
        assertEquals("a = 1", list.getChildren()[0].getText());
        assertEquals("b = a + 1", list.getChildren()[1].getText());
    }

    public void testBinaryExpressionsSpanBothOperands() {
        PsiFile file = parse("a + b * c");
        PsiElement top = file.getFirstChild();
        assertEquals(FileMakerCalculationElementType.BINARY_EXPRESSION, top.getNode().getElementType());
        assertEquals("a + b * c", top.getText());
    }

    public void testRepetitions() {
        assertEquals(2, countOf(parse("Table::Field[2] + $v[i]"), FileMakerCalculationElementType.REPETITION_EXPRESSION));
    }

    public void testLetAfterBindingsStillParsesTheCalculation() {
        // Before bracket lists were parsed, everything after "[" fell out of the Let call
        PsiFile file = parse("Let ( [ a = 1 ] ; a & \"x\" )");
        assertEquals(1, countOf(file, FileMakerCalculationElementType.FUNCTION_CALL));
        assertEquals(2, countOf(file, FileMakerCalculationElementType.ARGUMENT));
    }

    // ----- scopes -----

    public void testVariablesInScopeAreFine() {
        assertEquals(List.of(), outOfScope("Let ( [ a = 1 ; b = a + 1 ] ; a + b )"));
    }

    public void testUnknownNamesAreFieldsAndNeverReported() {
        assertEquals(List.of(), outOfScope("Let ( [ a = 1 ] ; a + Amount ) & Amount"));
    }

    public void testUseAfterTheLetIsReported() {
        assertEquals(List.of("total@32"), outOfScope("Let ( [ total = 1 ] ; total ) + total"));
    }

    public void testUseBeforeItsDefinitionIsReported() {
        // Let sets variables left to right
        assertEquals(List.of("b@12"), outOfScope("Let ( [ a = b ; b = 1 ] ; a )"));
    }

    public void testSingleDefinitionWithoutBrackets() {
        assertEquals(List.of("x@20"), outOfScope("Let ( x = 1 ; x ) & x"));
    }

    public void testNestedLetsSeeOuterVariables() {
        assertEquals(List.of(), outOfScope("Let ( [ x = 1 ] ; Let ( [ y = x ] ; x + y ) )"));
    }

    public void testNamesAreCaseInsensitive() {
        assertEquals(List.of(), outOfScope("Let ( [ Total = 1 ] ; total )"));
    }

    public void testScriptVariablesAreIgnored() {
        // $ / $$ variables are script/file-scoped and outlive the Let
        assertEquals(List.of(), outOfScope("Let ( [ $x = 1 ; $$y = 2 ] ; $x ) & $x & $$y"));
    }

    public void testWhileVariables() {
        assertEquals(List.of(), outOfScope(
                "While ( [ i = 0 ; total = 0 ] ; i < 3 ; [ i = i + 1 ; total = total + i ] ; total )"));
    }

    public void testWhileLogicVariablesAreVisibleInTheCondition() {
        // logic runs every iteration, before the condition is evaluated again
        assertEquals(List.of(), outOfScope("While ( [ i = 0 ] ; step < 3 ; [ step = i ; i = i + 1 ] ; i )"));
    }

    public void testWhileVariablesAfterTheWhileAreReported() {
        assertEquals(List.of("i@50"), outOfScope("While ( [ i = 0 ] ; i < 3 ; [ i = i + 1 ] ; i ) + i"));
    }

    // ----- annotator end to end -----

    public void testAnnotatorReportsOutOfScopeUseAsWeakWarning() {
        myFixture.configureByText("calc.fmcalc",
                "Let ( [ total = 1 ] ; total ) + <weak_warning descr=\"'total' is used outside the Let() that defines it (here it refers to a field)\">total</weak_warning>");
        myFixture.checkHighlighting(false, false, true);
    }

    public void testNoFalseWarningsForFieldsGetConstantsOrStrings() {
        myFixture.configureByText("calc.fmcalc",
                "Let ( [ a = Get ( AccountName ) ; b = \"x)\" ] ; a & b & Contacts::Name & Amount & $script )");
        myFixture.checkHighlighting(true, false, true);
    }
}
