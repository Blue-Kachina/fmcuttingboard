package dev.fmcuttingboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Runs shared/fixtures/golden/cases.json "diagnostics" through the real annotator on real PSI. The VS Code extension
 * runs the same cases against its diagnostics, so both IDEs report exactly the same problems.
 */
public class SharedDiagnosticsGoldenTest extends BasePlatformTestCase {

    private static final Map<HighlightSeverity, String> SEVERITIES = Map.of(
            HighlightSeverity.ERROR, "error",
            HighlightSeverity.WARNING, "warning",
            HighlightSeverity.WEAK_WARNING, "weak_warning");

    public void testSharedDiagnosticsCases() throws Exception {
        JsonObject cases;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("golden/cases.json")) {
            assertNotNull("shared/fixtures is not on the test classpath", in);
            cases = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
        List<String> failures = new ArrayList<>();
        for (JsonElement e : cases.getAsJsonArray("diagnostics")) {
            JsonObject c = e.getAsJsonObject();
            List<String> expected = new ArrayList<>();
            for (JsonElement x : c.getAsJsonArray("expect")) {
                JsonObject d = x.getAsJsonObject();
                expected.add(d.get("severity").getAsString() + " " + d.get("start").getAsInt() + "-" + d.get("end").getAsInt()
                        + " " + d.get("message").getAsString());
            }
            List<String> actual = diagnostics(c.get("calc").getAsString());
            expected.sort(null);
            actual.sort(null);
            if (!expected.equals(actual)) {
                failures.add(c.get("id").getAsString() + "\n  expected " + expected + "\n  actual   " + actual);
            }
        }
        assertTrue(String.join("\n", failures), failures.isEmpty());
    }

    private List<String> diagnostics(String calc) {
        myFixture.configureByText("calc.fmcalc", calc);
        List<String> out = new ArrayList<>();
        for (HighlightInfo info : myFixture.doHighlighting()) {
            String severity = SEVERITIES.get(info.getSeverity());
            if (severity == null || info.getDescription() == null) continue;
            out.add(severity + " " + info.getStartOffset() + "-" + info.getEndOffset() + " " + info.getDescription());
        }
        return out;
    }
}
