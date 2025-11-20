package dev.fmcuttingboard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanityTest {

    @Test
    void additionWorks() {
        int a = 2;
        int b = 2;
        assertEquals(4, a + b, "Basic arithmetic should work in test environment");
    }
}
