package dev.fmcuttingboard.language;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins how every function in shared/data/fm-calc-catalogue.json is displayed.
 *
 * shared/fixtures/golden/function-signatures.txt is written by shared/tools/generate-function-signatures.mjs, an
 * implementation independent of both plugins; the VS Code extension checks the same file. After vendoring a new
 * catalogue, regenerate it and review the diff.
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
