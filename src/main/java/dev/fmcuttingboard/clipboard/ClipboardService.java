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

    // writeText will be added in a subsequent task as per roadmap 2.1
}
