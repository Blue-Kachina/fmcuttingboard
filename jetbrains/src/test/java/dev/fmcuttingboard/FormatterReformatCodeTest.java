package dev.fmcuttingboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.fmcuttingboard.language.FileMakerCalculationFileType;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.format.FileMakerCustomCodeStyleSettings;
import dev.fmcuttingboard.language.format.FmCalcFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * The golden "formatting" cases through the IDE's real Reformat Code (code style settings → formatting model →
 * document), so what users get matches FmCalcFormatter and the VS Code extension exactly.
 */
public class FormatterReformatCodeTest extends BasePlatformTestCase {

    public void testReformatCodeMatchesGoldenCases() throws Exception {
        JsonObject cases = SharedFormattingGoldenTest.cases();
        JsonObject defaults = cases.getAsJsonObject("formattingDefaults");
        List<String> failures = new ArrayList<>();
        for (JsonElement e : cases.getAsJsonArray("formatting")) {
            JsonObject c = e.getAsJsonObject();
            String input = c.get("input").getAsString().replace("\r\n", "\n"); // documents always use \n
            String expected = c.get("expect").isJsonNull() ? input : c.get("expect").getAsString();
            String actual = reformat(input, SharedFormattingGoldenTest.options(defaults, c));
            if (!expected.equals(actual)) {
                failures.add(c.get("id").getAsString() + "\n--- expected\n" + expected + "\n--- actual\n" + actual);
            }
        }
        assertTrue(String.join("\n\n", failures), failures.isEmpty());
    }

    private String reformat(String text, FmCalcFormatter.Options options) {
        CodeStyleSettings settings = CodeStyle.getSettings(getProject());
        CommonCodeStyleSettings.IndentOptions indent = settings.getIndentOptions(FileMakerCalculationFileType.INSTANCE);
        boolean tabs = options.indentUnit().equals("\t");
        indent.USE_TAB_CHARACTER = tabs;
        indent.TAB_SIZE = options.indentWidth();
        indent.INDENT_SIZE = options.indentWidth();
        settings.getCommonSettings(FileMakerCalculationLanguage.INSTANCE).RIGHT_MARGIN = options.maxWidth();
        settings.getCustomSettings(FileMakerCustomCodeStyleSettings.class).DO_NOT_INDENT_TOP_LET_VARIABLES =
                options.doNotIndentTopLetVariables();

        myFixture.configureByText("calc.fmcalc", text);
        WriteCommandAction.runWriteCommandAction(getProject(), (Runnable) () ->
                CodeStyleManager.getInstance(getProject()).reformat(myFixture.getFile()));
        return myFixture.getEditor().getDocument().getText();
    }
}
