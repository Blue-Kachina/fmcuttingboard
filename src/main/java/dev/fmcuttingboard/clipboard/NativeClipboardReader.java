package dev.fmcuttingboard.clipboard;

import java.util.Optional;

/**
 * Abstraction for a last-resort native clipboard read. Platform-specific implementations
 * may use JNA or other mechanisms to access OS clipboard formats directly (e.g.,
 * on Windows: CF_UNICODETEXT).
 */
public interface NativeClipboardReader {
    /**
     * Attempts to read textual clipboard content using native OS APIs.
     * Implementations should perform normalization (e.g., strip embedded NULs) and
     * return non-blank results only.
     *
     * @return Optional non-blank text if available; otherwise empty.
     */
    Optional<String> read();
}
