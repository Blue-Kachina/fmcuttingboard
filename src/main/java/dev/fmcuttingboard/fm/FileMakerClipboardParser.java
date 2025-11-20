package dev.fmcuttingboard.fm;

import java.util.Optional;

/**
 * Phase 2.2 — FileMaker Clipboard Content Detection
 *
 * Defines a minimal protocol for detecting and extracting FileMaker-related
 * clipboard content. This focuses on text payloads that either already are
 * fmxmlsnippet XML or can be normalized to such text later.
 */
public interface FileMakerClipboardParser {
    /**
     * Heuristically determines if the provided text is likely sourced from
     * FileMaker's clipboard formats that we care about.
     *
     * For the initial pass we treat content containing an fmxmlsnippet root
     * element (case-insensitive, allowing optional whitespace) as FileMaker-
     * related. This is intentionally conservative and will evolve.
     *
     * @param clipboardText raw text from the clipboard (may be null)
     * @return true when the content appears to be FileMaker-related
     */
    boolean isLikelyFileMakerContent(String clipboardText);

    /**
     * Extracts or normalizes the text representation that will become XML.
     * If the clipboard contains an fmxmlsnippet, this should return exactly
     * that snippet (trimmed). Otherwise, return empty.
     *
     * @param clipboardText raw text from the clipboard (may be null)
     * @return Optional of normalized XML text if present
     */
    Optional<String> normalizeToXmlText(String clipboardText);
}
