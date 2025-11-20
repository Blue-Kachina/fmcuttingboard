package dev.fmcuttingboard.fm;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-pass heuristic parser for FileMaker clipboard text payloads.
 */
public class DefaultFileMakerClipboardParser implements FileMakerClipboardParser {

    // Case-insensitive, DOTALL to match newlines. Captures the fmxmlsnippet block.
    private static final Pattern FMXMLSNIPPET_PATTERN =
            Pattern.compile("(?is)<fmxmlsnippet\\b[\\s\\S]*?</fmxmlsnippet>");

    @Override
    public boolean isLikelyFileMakerContent(String clipboardText) {
        if (clipboardText == null || clipboardText.isBlank()) return false;

        // Fast path: does it contain an fmxmlsnippet block?
        Matcher m = FMXMLSNIPPET_PATTERN.matcher(clipboardText);
        if (m.find()) return true;

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
        Matcher m = FMXMLSNIPPET_PATTERN.matcher(clipboardText);
        if (m.find()) {
            String snippet = m.group().trim();
            return Optional.of(snippet);
        }
        return Optional.empty();
    }

    private static boolean containsFmxmlsnippetTag(String s) {
        return s.toLowerCase().contains("<fmxmlsnippet");
    }
}
