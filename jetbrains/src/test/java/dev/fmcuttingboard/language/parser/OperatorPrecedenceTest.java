package dev.fmcuttingboard.language.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Precedence comes from shared/data/calc-language.json (Claris "Using operators in formulas"). */
public class OperatorPrecedenceTest {

    @Test
    void followsClarisOrder() {
        int pow = FileMakerCalculationPsiParser.binaryPrecedence("^");
        int mul = FileMakerCalculationPsiParser.binaryPrecedence("*");
        int add = FileMakerCalculationPsiParser.binaryPrecedence("+");
        int concat = FileMakerCalculationPsiParser.binaryPrecedence("&");
        int eq = FileMakerCalculationPsiParser.binaryPrecedence("=");
        int and = FileMakerCalculationPsiParser.binaryPrecedence("and");
        int or = FileMakerCalculationPsiParser.binaryPrecedence("or");
        assertTrue(pow > mul && mul > add && add > concat && concat > eq && eq > and && and > or);
        assertEquals(or, FileMakerCalculationPsiParser.binaryPrecedence("xor"));
        assertEquals(mul, FileMakerCalculationPsiParser.binaryPrecedence("/"));
    }

    @Test
    void operatorsTheOldTableMissed() {
        // Before the shared data, &, ^, xor and the ASCII spellings stopped the parser
        for (String op : new String[] {"&", "^", "xor", "XOR", "<>", "<=", ">=", "≠", "≤", "≥"}) {
            assertTrue(FileMakerCalculationPsiParser.binaryPrecedence(op) > 0, op);
        }
        assertEquals(FileMakerCalculationPsiParser.binaryPrecedence("≠"), FileMakerCalculationPsiParser.binaryPrecedence("<>"));
    }

    @Test
    void separatorsAndUnaryNotAreNotBinary() {
        assertEquals(-1, FileMakerCalculationPsiParser.binaryPrecedence(";"));
        assertEquals(-1, FileMakerCalculationPsiParser.binaryPrecedence("not"));
        assertEquals(-1, FileMakerCalculationPsiParser.binaryPrecedence("::"));
    }
}
