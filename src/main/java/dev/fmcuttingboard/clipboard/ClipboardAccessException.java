package dev.fmcuttingboard.clipboard;

/**
 * Exception representing a failure to access the system clipboard.
 */
public class ClipboardAccessException extends Exception {
    public ClipboardAccessException(String message) {
        super(message);
    }

    public ClipboardAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
