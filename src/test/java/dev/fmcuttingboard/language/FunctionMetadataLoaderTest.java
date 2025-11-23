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
        Path json = Path.of("resources", "filemaker-vscode-bundle-master", "snippets", "filemaker.json");
        String text = Files.readString(json, StandardCharsets.UTF_8);

        Set<String> names = FunctionMetadataLoader.extractSnippetFunctionNames(text);

        System.out.println("[DEBUG_LOG] Extracted function names count: " + names.size());
        // The VSCode bundle contains 280+ function snippets; be lenient but ensure we cross 200.
        assertTrue(names.size() >= 200, "Expected to extract at least 200 function names, got " + names.size());
        // Sanity checks for some well-known functions
        assertTrue(names.contains("If"), "Should include 'If'");
        assertTrue(names.contains("Case"), "Should include 'Case'");
        assertTrue(names.contains("Let"), "Should include 'Let'");
        // VSCode snippets represent Get variants via GetLayoutObjectAttribute etc., not a raw standalone 'Get'.
        assertTrue(names.contains("Substitute"), "Should include 'Substitute'");
    }
}
