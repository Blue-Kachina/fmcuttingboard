package dev.fmcuttingboard.language;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards the move of function data from hardcoded Java into shared/data/filemaker-functions.json.
 *
 * shared/fixtures/golden/function-signatures.txt was generated from the hardcoded registry (plugin 1.0.6)
 * before the move; the VS Code extension checks the same file. If you deliberately change
 * shared/data/filemaker-functions.json, update that file to match.
 */
public class FunctionRegistryBaselineTest {

    @Test
    void registryLoadedFromSharedJsonMatchesBaseline() throws Exception {
        List<String> expected;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("golden/function-signatures.txt")) {
            assertNotNull(in, "shared/fixtures/golden/function-signatures.txt missing from test classpath");
            expected = Arrays.asList(new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n"));
        }

        List<String> actual = new ArrayList<>();
        for (FunctionMetadata m : FileMakerFunctionRegistry.getAll()) {
            actual.add(m.getName() + " | " + m.getCategory() + " | " + m.getReturnType() + " | " + m.getSignature()
                    + " | " + m.getSimpleSignature() + " | " + m.getDescription());
        }

        assertEquals(expected, actual);
    }

    @Test
    void lookupIsCaseInsensitive() {
        assertNotNull(FileMakerFunctionRegistry.findByName("substitute"));
        assertNotNull(FileMakerFunctionRegistry.findByName("GETVALUE"));
        assertNull(FileMakerFunctionRegistry.findByName("NoSuchFunction"));
    }
}
