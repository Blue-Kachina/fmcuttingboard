package dev.fmcuttingboard.language.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UnterminatedStringTest {

    private static boolean terminated(String s) {
        return FileMakerCalculationAnnotator.isTerminatedString(s, 0, s.length());
    }

    @Test
    void detectsTheClosingQuote() {
        assertTrue(terminated("\"abc\""));
        assertTrue(terminated("\"\""));
        assertTrue(terminated("\"ends with escaped backslash \\\\\""));
        assertFalse(terminated("\"abc"));
        assertFalse(terminated("\""));
        assertFalse(terminated("\"ends with escaped quote \\\""));
    }
}
