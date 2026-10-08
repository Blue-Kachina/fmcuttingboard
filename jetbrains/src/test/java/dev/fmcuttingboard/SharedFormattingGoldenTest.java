package dev.fmcuttingboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.fmcuttingboard.language.format.FmCalcFormatter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs shared/fixtures/golden/cases.json "formatting" through FmCalcFormatter (on real PSI). The VS Code extension
 * runs the same cases (vscode/test/calcFormatter.test.ts). See also FormatterReformatCodeTest for the IDE path.
 */
public class SharedFormattingGoldenTest extends BasePlatformTestCase {

    static JsonObject cases() throws Exception {
        try (InputStream in = SharedFormattingGoldenTest.class.getClassLoader().getResourceAsStream("golden/cases.json")) {
            assertNotNull("shared/fixtures is not on the test classpath", in);
            return JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    static FmCalcFormatter.Options options(JsonObject defaults, JsonObject c) {
        JsonObject o = defaults.deepCopy();
        if (c.has("options")) for (var e : c.getAsJsonObject("options").entrySet()) o.add(e.getKey(), e.getValue());
        return new FmCalcFormatter.Options(o.get("indentUnit").getAsString(), o.get("indentWidth").getAsInt(),
                o.get("maxWidth").getAsInt(), o.get("doNotIndentTopLetVariables").getAsBoolean());
    }

    private String format(String text, FmCalcFormatter.Options options) {
        PsiFile file = myFixture.configureByText("calc.fmcalc", text);
        return FmCalcFormatter.format(file, options);
    }

    public void testSharedFormattingCases() throws Exception {
        JsonObject cases = cases();
        JsonObject defaults = cases.getAsJsonObject("formattingDefaults");
        List<String> failures = new ArrayList<>();
        for (JsonElement e : cases.getAsJsonArray("formatting")) {
            JsonObject c = e.getAsJsonObject();
            FmCalcFormatter.Options options = options(defaults, c);
            String expected = c.get("expect").isJsonNull() ? null : c.get("expect").getAsString();
            String actual = format(c.get("input").getAsString(), options);
            if (!Objects.equals(expected, actual)) {
                failures.add(c.get("id").getAsString() + "\n--- expected\n" + expected + "\n--- actual\n" + actual);
            } else if (expected != null && !expected.equals(format(expected, options))) {
                failures.add(c.get("id").getAsString() + ": not idempotent");
            }
        }
        assertTrue(String.join("\n\n", failures), failures.isEmpty());
    }
}
