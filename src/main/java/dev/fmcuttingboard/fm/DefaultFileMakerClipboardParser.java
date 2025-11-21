package dev.fmcuttingboard.fm;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-pass heuristic parser for FileMaker clipboard text payloads.
 *
 * Performance & robustness notes (Phase 8.2):
 * - Avoid catastrophic regex on very large inputs by using a fast index-based
 *   case-insensitive search for <fmxmlsnippet> ... </fmxmlsnippet>.
 * - Apply a simple time budget and size guard during normalization to prevent
 *   UI freezes if invoked on the EDT.
 */
public class DefaultFileMakerClipboardParser implements FileMakerClipboardParser {

    // Kept as a fallback for moderate-size inputs only.
    private static final Pattern FMXMLSNIPPET_PATTERN =
            Pattern.compile("(?is)<fmxmlsnippet\\b[\\s\\S]*?</fmxmlsnippet>");

    // Guards for performance; large clipboard payloads are not uncommon.
    static final int MAX_SCAN_CHARS = 2_000_000; // ~2 MB of characters
    static final long TIME_BUDGET_MS = 200;      // fail fast after 200ms

    @Override
    public boolean isLikelyFileMakerContent(String clipboardText) {
        if (clipboardText == null || clipboardText.isBlank()) return false;

        // Very fast path: look for opening tag only (case-insensitive).
        if (indexOfIgnoreCase(clipboardText, "<fmxmlsnippet") >= 0) return true;

        // Basic rejection heuristics for common non-FileMaker content.
        String trimmed = clipboardText.trim();
        // If it looks like XML but not fmxmlsnippet, reject.
        if ((trimmed.startsWith("<?xml") || trimmed.startsWith("<")) && !containsFmxmlsnippetTag(trimmed)) {
            return false;
        }

        // If it's very short or obviously plain text without angle brackets, reject.
        if (trimmed.length() < 20 && !trimmed.contains("<") && !trimmed.contains(">")) {
            return false;
        }

        // Otherwise, first pass returns false (we're conservative).
        return false;
    }

    @Override
    public Optional<String> normalizeToXmlText(String clipboardText) {
        if (clipboardText == null) return Optional.empty();

        final long start = System.nanoTime();

        // 1) Fast, allocation-light index search for open/close tags (case-insensitive)
        int openIdx = indexOfIgnoreCase(clipboardText, "<fmxmlsnippet");
        if (openIdx >= 0) {
            // Find the end of the opening tag '>' starting from openIdx
            int openEnd = clipboardText.indexOf('>', openIdx);
            if (openEnd > openIdx) {
                int closeIdx = indexOfIgnoreCase(clipboardText, "</fmxmlsnippet>", openEnd + 1);
                if (closeIdx > openEnd) {
                    String snippet = clipboardText.substring(openIdx, closeIdx + "</fmxmlsnippet>".length()).trim();
                    return Optional.of(snippet);
                }
            }
        }

        // 2) If content is too large, avoid regex to protect performance.
        if (clipboardText.length() > MAX_SCAN_CHARS) {
            return Optional.empty();
        }

        // 3) Fallback to regex for moderate inputs, but respect time budget.
        Matcher m = FMXMLSNIPPET_PATTERN.matcher(clipboardText);
        boolean found = false;
        String snippet = null;
        while (!found && m.find()) {
            snippet = m.group();
            found = true;
            long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
            if (elapsedMs > TIME_BUDGET_MS) {
                // Time budget exceeded; abort to avoid UI stalls.
                return Optional.empty();
            }
        }
        if (found && snippet != null) return Optional.of(snippet.trim());
        return Optional.empty();
    }

    private static boolean containsFmxmlsnippetTag(String s) {
        return indexOfIgnoreCase(s, "<fmxmlsnippet") >= 0;
    }

    /**
     * Case-insensitive indexOf without allocating lowercase copies.
     */
    static int indexOfIgnoreCase(String text, String needle) {
        final int n = text.length();
        final int m = needle.length();
        if (m == 0) return 0;
        char c0 = Character.toLowerCase(needle.charAt(0));
        for (int i = 0; i <= n - m; i++) {
            if (Character.toLowerCase(text.charAt(i)) != c0) continue;
            int j = 1;
            while (j < m && Character.toLowerCase(text.charAt(i + j)) == Character.toLowerCase(needle.charAt(j))) {
                j++;
            }
            if (j == m) return i;
        }
        return -1;
    }

    static int indexOfIgnoreCase(String text, String needle, int fromIndex) {
        if (fromIndex < 0) fromIndex = 0;
        final int n = text.length();
        final int m = needle.length();
        if (m == 0) return Math.min(fromIndex, n);
        char c0 = Character.toLowerCase(needle.charAt(0));
        for (int i = fromIndex; i <= n - m; i++) {
            if (Character.toLowerCase(text.charAt(i)) != c0) continue;
            int j = 1;
            while (j < m && Character.toLowerCase(text.charAt(i + j)) == Character.toLowerCase(needle.charAt(j))) {
                j++;
            }
            if (j == m) return i;
        }
        return -1;
    }
}
