package dev.fmcuttingboard.fm;

/**
 * Exception thrown when conversion from clipboard content to XML fails
 * due to unexpected or unrecognized structures.
 */
public class ConversionException extends Exception {
    public ConversionException(String message) {
        super(message);
    }

    public ConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
