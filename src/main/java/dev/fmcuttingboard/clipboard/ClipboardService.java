package dev.fmcuttingboard.clipboard;

import java.util.Optional;

/**
 * Abstraction for interacting with the system clipboard.
 * Phase 2.1 — Clipboard Access Abstraction
 */
public interface ClipboardService {
    /**
     * Attempts to read plain text from the system clipboard.
     *
     * @return Optional containing text if available, otherwise empty
     * @throws ClipboardAccessException when clipboard cannot be accessed
     */
    Optional<String> readText() throws ClipboardAccessException;

    /**
     * Writes the provided text into the system clipboard.
     *
     * @param text text to write (null treated as empty string)
     * @throws ClipboardAccessException when clipboard cannot be written
     */
    void writeText(String text) throws ClipboardAccessException;
}
