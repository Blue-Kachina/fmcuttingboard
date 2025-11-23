package dev.fmcuttingboard.language;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 1 – 1.1 Consolidate Function Information
 *
 * Loader utilities that will consolidate function definitions from bundled resources
 * (VSCode snippets, TextMate grammar, Notepad++ XML) into FunctionMetadata models.
 *
 * Note: For now this is a lightweight scaffold that can parse the VSCode snippets file
 * on a best-effort basis in later iterations. Keeping the API small allows us to evolve
 * the implementation without affecting callers.
 */
public final class FunctionMetadataLoader {

    private FunctionMetadataLoader() {}

    /**
     * Reads a text resource fully as String. Handy for ad-hoc parsing.
     */
    public static @NotNull String readAll(@NotNull InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    /**
     * Placeholder for future JSON-based extraction from VSCode snippets (resources/filemaker-vscode-bundle-master/snippets/filemaker.json).
     * Currently returns an empty list; to be implemented in subsequent Phase 1 iterations.
     */
    public static @NotNull List<FunctionMetadata> parseVsCodeSnippets(@NotNull String jsonText) {
        // TODO: Implement robust JSON parsing and convert snippet bodies like
        //  "If ( ${1:test} ; ${2:resultTrue}${3: ; ${4:resultFalse}} )" into typed parameters.
        return new ArrayList<>();
    }
}
