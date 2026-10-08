package dev.fmcuttingboard;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import dev.fmcuttingboard.language.folding.FileMakerCalculationFoldingBuilder;

import java.util.ArrayList;
import java.util.List;

/** shared/fixtures/golden/cases.json "folding" through the real folding builder (VS Code runs the same cases). */
public class SharedFoldingGoldenTest extends BasePlatformTestCase {

    public void testSharedFoldingCases() throws Exception {
        JsonObject cases = SharedFormattingGoldenTest.cases();
        List<String> failures = new ArrayList<>();
        for (JsonElement e : cases.getAsJsonArray("folding")) {
            JsonObject c = e.getAsJsonObject();
            List<String> expected = new ArrayList<>();
            for (JsonElement r : c.getAsJsonArray("expect")) {
                JsonObject o = r.getAsJsonObject();
                expected.add(o.get("start").getAsInt() + "-" + o.get("end").getAsInt() + " " + o.get("placeholder").getAsString());
            }
            myFixture.configureByText("calc.fmcalc", c.get("calc").getAsString());
            List<String> actual = new ArrayList<>();
            for (FoldingDescriptor d : new FileMakerCalculationFoldingBuilder()
                    .buildFoldRegions(myFixture.getFile(), myFixture.getEditor().getDocument(), false)) {
                actual.add(d.getRange().getStartOffset() + "-" + d.getRange().getEndOffset() + " " + d.getPlaceholderText());
            }
            expected.sort(null);
            actual.sort(null);
            if (!expected.equals(actual)) failures.add(c.get("id").getAsString() + ": expected " + expected + " actual " + actual);
        }
        assertTrue(String.join("\n", failures), failures.isEmpty());
    }
}
