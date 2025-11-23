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
     * Best-effort parser for VSCode snippet JSON (resources/filemaker-vscode-bundle-master/snippets/filemaker.json).
     *
     * This implementation purposefully avoids introducing a JSON library dependency. It uses regex to
     * extract entries of the form:
     *   "If": { "prefix": "If", "body": "If ( ${1:test} ; ${2:resultTrue}${3: ; ${4:resultFalse}} )" }
     * or with body as an array of strings. It normalizes names like "Case [inline]" → "Case".
     * Parameters are derived from placeholders: ${index:name}. Optional/variadic parameters are
     * heuristically detected when they appear inside optional fragments like "${3: ; ${4:resultFalse}}".
     */
    public static @NotNull List<FunctionMetadata> parseVsCodeSnippets(@NotNull String jsonText) {
        List<FunctionMetadata> list = new ArrayList<>();

        // Extract top-level entries: "Name": { ... }
        java.util.regex.Pattern entryPattern = java.util.regex.Pattern.compile(
                "\\\"([^\\\"]+)\\\"\\s*:\\s*\\{(.*?)\\}\",??",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher entryMatcher = entryPattern.matcher(jsonText);
        while (entryMatcher.find()) {
            String rawName = entryMatcher.group(1);
            String obj = entryMatcher.group(2);

            // Normalize name by trimming variant suffix after first space, e.g., "Case [inline]" → "Case"
            String name = rawName.contains(" ") ? rawName.substring(0, rawName.indexOf(' ')) : rawName;

            // Extract body which can be a string or array
            String body = extractJsonBody(obj);
            if (body == null) continue;

            // Derive parameter segment inside first parentheses
            int lp = body.indexOf('(');
            int rp = body.lastIndexOf(')');
            if (lp < 0 || rp < lp) {
                // Not a function shape (some snippets are boilerplate text), skip
                continue;
            }
            String inside = body.substring(lp + 1, rp);

            // Split on semicolons as FileMaker parameter delimiter
            List<FunctionParameter> params = new ArrayList<>();
            for (String rawPart : splitSemicolonTopLevel(inside)) {
                String part = rawPart.trim();
                if (part.isEmpty()) continue;
                // Try to extract placeholder name ${n:name}
                java.util.regex.Matcher pm = java.util.regex.Pattern
                        .compile("\\$\\{\\d+:([^}]+)}`?", java.util.regex.Pattern.DOTALL)
                        .matcher(part);
                String pname = null;
                if (pm.find()) {
                    pname = pm.group(1).trim();
                }
                if (pname == null || pname.isEmpty()) {
                    // Fallback: use the literal text without placeholder markup
                    pname = part.replaceAll("\\$\\{\\d+:?", "").replace("}", "").trim();
                    if (pname.isEmpty()) pname = "param" + (params.size() + 1);
                }
                // Heuristic: if the segment contains "+" or ellipsis, mark variadic
                boolean variadic = part.contains("...");
                // Heuristic: consider optional if the segment contains nested optional expansion "${n: ; ${m:...}}"
                boolean optional = part.contains("${") && part.contains("}") && part.contains(";");
                params.add(new FunctionParameter(pname, "Any", optional, variadic));
            }

            // Avoid duplicates caused by multiple variants of the same name
            boolean exists = list.stream().anyMatch(f -> f.getName().equals(name));
            if (!exists) {
                list.add(new FunctionMetadata(name, params, "Unknown", "Any", "Imported from VSCode snippets"));
            }
        }
        return list;
    }

    private static String extractJsonBody(String obj) {
        // body can be: "body": "..." or "body": ["...", "..."]
        java.util.regex.Matcher mString = java.util.regex.Pattern.compile(
                "\\\"body\\\"\\s*:\\s*\\\"(.*?)\\\"",
                java.util.regex.Pattern.DOTALL).matcher(obj);
        if (mString.find()) {
            return unescapeJson(mString.group(1));
        }
        java.util.regex.Matcher mArray = java.util.regex.Pattern.compile(
                "\\\"body\\\"\\s*:\\s*\\[(.*?)\\]",
                java.util.regex.Pattern.DOTALL).matcher(obj);
        if (mArray.find()) {
            String arr = mArray.group(1);
            java.util.regex.Matcher item = java.util.regex.Pattern.compile("\\\"(.*?)\\\"",
                    java.util.regex.Pattern.DOTALL).matcher(arr);
            StringBuilder sb = new StringBuilder();
            while (item.find()) {
                sb.append(unescapeJson(item.group(1)));
                sb.append('\n');
            }
            return sb.toString();
        }
        return null;
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static List<String> splitSemicolonTopLevel(String inside) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0; // parentheses depth
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            if (c == ';' && depth == 0) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) parts.add(cur.toString());
        return parts;
    }

    /**
     * Extract top-level snippet keys (function names) from VSCode snippets JSON using a simple pattern.
     * This is a best-effort approach to support Phase 1 consolidation without adding a JSON dependency.
     */
    public static @NotNull java.util.Set<String> extractSnippetFunctionNames(@NotNull String jsonText) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        // Matches entries like: "Abs": { "prefix": "Abs", ... }
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\\"([^\\\"]+)\\\"\\s*:\\s*\\{[^}]*?\\\"prefix\\\"\\s*:\\s*\\\".*?\\\"",
                java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(jsonText);
        while (m.find()) {
            String key = m.group(1);
            // Filter out templated variants like "Case [inline]" → normalize to base name before first space
            String normalized = key.contains(" ") ? key.substring(0, key.indexOf(' ')) : key;
            names.add(normalized);
        }
        return names;
    }

    /**
     * Extract the Notepad++ Words4 list (Get() constants) and other keyword lists as plain tokens.
     * The file is a small XML; a regex approach keeps dependencies minimal.
     */
    public static @NotNull java.util.Set<String> extractNotepadPlusPlusWords(@NotNull String xmlText, @NotNull String listName) {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        String start = "<Keywords name=\"" + listName + "\">";
        int s = xmlText.indexOf(start);
        if (s >= 0) {
            int e = xmlText.indexOf("</Keywords>", s);
            if (e > s) {
                String content = xmlText.substring(s + start.length(), e).trim();
                // Replace HTML entities
                content = content.replace("&quot;", "\"").replace("&apos;", "'");
                for (String token : content.split("\\s+")) {
                    if (!token.isEmpty()) names.add(token);
                }
            }
        }
        return names;
    }
}
