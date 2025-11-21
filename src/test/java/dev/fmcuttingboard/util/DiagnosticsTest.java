package dev.fmcuttingboard.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticsTest {

    private static final String FLAG = "fmcuttingboard.verbose";

    @AfterEach
    void tearDown() {
        System.clearProperty(FLAG);
    }

    @Test
    void isVerbose_falseByDefault() {
        System.clearProperty(FLAG);
        assertFalse(Diagnostics.isVerbose());
    }

    @Test
    void isVerbose_trueWhenSystemPropertyIsTrue() {
        System.setProperty(FLAG, "true");
        assertTrue(Diagnostics.isVerbose());
    }

    @Test
    void isVerbose_caseInsensitive() {
        System.setProperty(FLAG, "TrUe");
        assertTrue(Diagnostics.isVerbose());
    }
}
