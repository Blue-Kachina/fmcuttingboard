package dev.fmcuttingboard.language;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static dev.fmcuttingboard.language.FileMakerCalculationTokenType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The lexer gaps found while building the VS Code grammar (docs/parity.md, "JetBrains lexer follow-ups").
 * Expectations mirror vscode/test/grammar.test.ts so both IDEs tokenize FileMaker calculations the same way.
 */
public class LexerFollowUpsTest {

    /** Non-whitespace tokens as "TYPE:text". */
    private static List<String> tokens(String text) {
        Lexer lexer = new FileMakerCalculationLexerAdapter();
        lexer.start(text);
        List<String> out = new ArrayList<>();
        for (IElementType t = lexer.getTokenType(); t != null; lexer.advance(), t = lexer.getTokenType()) {
            if (t == WHITE_SPACE) continue;
            out.add(t + ":" + text.substring(lexer.getTokenStart(), lexer.getTokenEnd()));
        }
        return out;
    }

    @Test
    void fieldReferencesAreOneToken() {
        assertEquals(List.of("FIELD_REFERENCE:Contacts::FirstName", "OPERATOR:&", "FIELD_REFERENCE:Contacts :: Last_Name"),
                tokens("Contacts::FirstName & Contacts :: Last_Name"));
    }

    @Test
    void paragraphMarkOutsideStrings() {
        assertEquals(List.of("STRING:\"a\"", "OPERATOR:&", "PARAGRAPH_MARK:¶", "OPERATOR:&", "STRING:\"b\""),
                tokens("\"a\" & ¶ & \"b\""));
    }

    @Test
    void quotedReservedNames() {
        assertEquals(List.of("QUOTED_NAME:${Table::If}", "OPERATOR:+", "NUMBER:1"), tokens("${Table::If} + 1"));
    }

    @Test
    void noJavaKeywords() {
        assertEquals(List.of("IDENTIFIER:boolean", "OPERATOR:+", "IDENTIFIER:int", "OPERATOR:+", "IDENTIFIER:void"),
                tokens("boolean + int + void"));
    }

    @Test
    void onlyDoubleQuotedStrings() {
        List<String> t = tokens("'lo'");
        assertTrue(t.contains(TokenType.BAD_CHARACTER + ":'"), "a single quote is not a FileMaker string delimiter");
        assertEquals(List.of("STRING:\"multi\nline \\\"quoted\\\" \\¶\""), tokens("\"multi\nline \\\"quoted\\\" \\¶\""));
    }

    @Test
    void anyNameFollowedByParenIsAFunctionCallInAnyCase() {
        assertEquals(List.of("KEYWORD_FUNCTION:substitute", "LPAREN:(", "IDENTIFIER:x", "RPAREN:)"), tokens("substitute(x)"));
        assertEquals(List.of("KEYWORD_FUNCTION:MyCustomFunction", "LPAREN:(", "RPAREN:)"), tokens("MyCustomFunction ()"));
        assertEquals("KEYWORD_FUNCTION:JSONGetElementPath", tokens("JSONGetElementPath ( j ; k )").get(0));
    }

    @Test
    void getConstantsForAnyName() {
        assertEquals(List.of("KEYWORD_FUNCTION:Get", "LPAREN:(", "GET_CONSTANT:AccountName", "RPAREN:)"), tokens("Get ( AccountName )"));
        assertEquals("GET_CONSTANT:SomeFutureConstant", tokens("get(SomeFutureConstant)").get(2));
        assertEquals("IDENTIFIER:x", tokens("Left ( x ; 1 )").get(2), "only Get's argument is a Get constant");
    }

    @Test
    void namedConstantsFromSharedDataInAnyCase() {
        assertEquals(List.of("CONSTANT:True", "KEYWORD_LOGICAL:and", "CONSTANT:jsonstring", "KEYWORD_LOGICAL:or", "CONSTANT:Bold"),
                tokens("True and jsonstring or Bold"));
        assertEquals("IDENTIFIER:Trueness", tokens("Trueness").get(0));
    }

    @Test
    void variablesAndUnicodeNames() {
        assertEquals(List.of("IDENTIFIER:$local", "OPERATOR:&", "IDENTIFIER:$$global", "OPERATOR:&", "IDENTIFIER:$~x"),
                tokens("$local & $$global & $~x"));
        assertEquals(List.of("FIELD_REFERENCE:顧客::名前"), tokens("顧客::名前"));
    }

    @Test
    void wordsContainingKeywordsAreIdentifiers() {
        assertEquals(List.of("IDENTIFIER:android", "OPERATOR:+", "IDENTIFIER:notes"), tokens("android + notes"));
    }
}
