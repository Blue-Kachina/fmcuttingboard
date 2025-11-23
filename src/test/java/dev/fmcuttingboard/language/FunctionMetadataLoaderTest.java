package dev.fmcuttingboard.language;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 – 1.1 Consolidate Function Information
 *
 * Verifies we can extract a comprehensive set of function names from the bundled
 * VSCode snippets JSON without adding a JSON dependency.
 */
public class FunctionMetadataLoaderTest {

    @Test
    public void extracts_many_function_names_from_vscode_snippets() throws IOException {
        // Load our committed copy of the VSCode snippets (renamed and stored at repo root resources)
        // See resources/filemaker_functions.json
        String resourcePath = "/filemaker_functions.json";
        String text;
        var in = FunctionMetadataLoaderTest.class.getResourceAsStream(resourcePath);
        if (in != null) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } else {
            // Fallback to repo-relative path for local runs
            Path json = Path.of("resources", "filemaker_functions.json");
            text = Files.readString(json, StandardCharsets.UTF_8);
        }

        Set<String> names = FunctionMetadataLoader.extractSnippetFunctionNames(text);

        System.out.println("[DEBUG_LOG] Extracted VSCode snippet function names count: " + names.size());
        // Ensure we collected a broad set
        assertTrue(names.size() >= 120, "Expected to extract at least 120 function names, got " + names.size());
        // Sanity checks for some well-known functions
        assertTrue(names.contains("If"), "Should include 'If'");
        assertTrue(names.contains("Case"), "Should include 'Case'");
        assertTrue(names.contains("Let"), "Should include 'Let'");
        assertTrue(names.contains("Substitute"), "Should include 'Substitute'");
        // Some Get() family examples
        assertTrue(names.contains("GetLayoutObjectAttribute"), "Should include 'GetLayoutObjectAttribute'");
        assertTrue(names.contains("Get(WindowWidth)"), "Should include 'Get(WindowWidth)'");
    }
}
