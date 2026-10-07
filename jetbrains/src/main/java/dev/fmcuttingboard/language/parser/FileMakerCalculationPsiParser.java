package dev.fmcuttingboard.language.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import dev.fmcuttingboard.shared.SharedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight recursive-descent parser creating a minimal PSI structure for FileMaker calculations.
 * Handles function calls, parenthesized expressions, identifiers and literals.
 *
 * Phase 4.3: Replaced bootstrap flat parser with this structured parser.
 * Phase 4.3 (refinement): Add basic unary/binary expression parsing with simple precedence.
 */
public class FileMakerCalculationPsiParser implements PsiParser {

    /**
     * Binary operator precedence (higher binds tighter) from shared/data/calc-language.json, which follows Claris's
     * "Using operators in formulas" order. Keys are lowercase symbols, including alternates such as {@code <>}.
     */
    private static final class Precedence {
        static final Map<String, Integer> BINARY = load();

        private static Map<String, Integer> load() {
            Map<String, Integer> map = new HashMap<>();
            for (JsonElement e : SharedData.readJson(SharedData.CALC_LANGUAGE).getAsJsonArray("operators")) {
                JsonObject op = e.getAsJsonObject();
                if (op.get("arity").getAsInt() != 2) continue;
                int prec = op.get("precedence").getAsInt();
                map.put(op.get("symbol").getAsString().toLowerCase(Locale.ROOT), prec);
                if (op.has("alternates")) {
                    for (JsonElement alt : op.getAsJsonArray("alternates")) map.put(alt.getAsString().toLowerCase(Locale.ROOT), prec);
                }
            }
            return map;
        }
    }

    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();
        // Parse a single top-level expression (FileMaker calcs are typically single expressions)
        parseExpression(builder);
        // Consume trailing tokens to avoid parser hanging on unexpected input
        while (!builder.eof()) builder.advanceLexer();
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    private void parseExpression(PsiBuilder builder) {
        if (builder.eof()) return;
        parseBinary(builder, 0);
    }

    private void parseArgumentList(PsiBuilder builder) {
        PsiBuilder.Marker listMarker = builder.mark();
        // Empty argument list
        if (builder.getTokenType() == FileMakerCalculationTokenType.RPAREN) {
            listMarker.done(FileMakerCalculationElementType.ARG_LIST);
            return;
        }

        // One or more arguments separated by semicolons
        parseArgument(builder);
        while (isSemicolon(builder)) {
            builder.advanceLexer(); // consume ';'
            parseArgument(builder);
        }
        listMarker.done(FileMakerCalculationElementType.ARG_LIST);
    }

    private void parseArgument(PsiBuilder builder) {
        PsiBuilder.Marker argMarker = builder.mark();
        parseExpression(builder);
        argMarker.done(FileMakerCalculationElementType.ARGUMENT);
    }

    // Pratt/precedence-climbing parser for binary expressions.
    private void parseBinary(PsiBuilder builder, int minPrec) {
        // Each BINARY_EXPRESSION spans left operand, operator and right operand (precede() re-wraps for chains,
        // so "a + b + c" nests as ((a + b) + c)); all binary operators are left-associative
        PsiBuilder.Marker left = builder.mark();
        parseUnary(builder);

        while (true) {
            int prec = currentOperatorPrecedence(builder);
            if (prec < minPrec) break;
            builder.advanceLexer(); // consume operator
            parseBinary(builder, prec + 1);
            left.done(FileMakerCalculationElementType.BINARY_EXPRESSION);
            left = left.precede();
        }
        left.drop();
    }

    private void parseUnary(PsiBuilder builder) {
        // Unary NOT, minus and plus
        boolean isNot = builder.getTokenType() == FileMakerCalculationTokenType.KEYWORD_LOGICAL && tokenTextIs(builder, "not");
        boolean isSign = builder.getTokenType() == FileMakerCalculationTokenType.OPERATOR
                && (tokenTextIs(builder, "-") || tokenTextIs(builder, "+"));
        if (isNot || isSign) {
            PsiBuilder.Marker m = builder.mark();
            builder.advanceLexer();
            parseUnary(builder);
            m.done(FileMakerCalculationElementType.UNARY_EXPRESSION);
            return;
        }
        parsePostfix(builder);
    }

    /** A primary expression followed by repetition indexes, e.g. {@code Table::Field[2]} or {@code $var[i]}. */
    private void parsePostfix(PsiBuilder builder) {
        boolean isBracketList = builder.getTokenType() == FileMakerCalculationTokenType.LBRACKET;
        PsiBuilder.Marker marker = builder.mark();
        parsePrimary(builder);
        if (!isBracketList && builder.getTokenType() == FileMakerCalculationTokenType.LBRACKET) {
            while (builder.getTokenType() == FileMakerCalculationTokenType.LBRACKET) parseBracketList(builder);
            marker.done(FileMakerCalculationElementType.REPETITION_EXPRESSION);
        } else {
            marker.drop();
        }
    }

    /**
     * {@code [ expr ; expr ; … ]}: the variable definitions of Let ( [ … ] ; … ) and While ( [ … ] ; … ; [ … ] ; … ),
     * or a repetition index. Each definition ({@code name = expression}) parses as a BINARY_EXPRESSION.
     */
    private void parseBracketList(PsiBuilder builder) {
        PsiBuilder.Marker marker = builder.mark();
        builder.advanceLexer(); // '['
        if (builder.getTokenType() != FileMakerCalculationTokenType.RBRACKET) {
            parseExpression(builder);
            while (isSemicolon(builder)) {
                builder.advanceLexer(); // ';'
                if (builder.getTokenType() == FileMakerCalculationTokenType.RBRACKET) break; // tolerate a trailing ';'
                parseExpression(builder);
            }
        }
        if (builder.getTokenType() == FileMakerCalculationTokenType.RBRACKET) {
            builder.advanceLexer();
        }
        marker.done(FileMakerCalculationElementType.BRACKET_LIST);
    }

    private void parsePrimary(PsiBuilder builder) {
        if (builder.eof()) return;

        IElementType token = builder.getTokenType();
        if (isNameToken(token)) {
            // Lookahead to see if it's a function call: name LPAREN
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer(); // consume name
            if (builder.getTokenType() == FileMakerCalculationTokenType.LPAREN) {
                // function call
                builder.advanceLexer(); // consume '('
                parseArgumentList(builder);
                if (builder.getTokenType() == FileMakerCalculationTokenType.RPAREN) {
                    builder.advanceLexer(); // consume ')'
                }
                marker.done(FileMakerCalculationElementType.FUNCTION_CALL);
            } else {
                // standalone identifier expression
                marker.done(FileMakerCalculationElementType.IDENTIFIER_EXPRESSION);
            }
            return;
        }

        if (token == FileMakerCalculationTokenType.LBRACKET) {
            parseBracketList(builder);
            return;
        }

        if (token == FileMakerCalculationTokenType.LPAREN) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer(); // '('
            parseExpression(builder);
            if (builder.getTokenType() == FileMakerCalculationTokenType.RPAREN) {
                builder.advanceLexer();
            }
            marker.done(FileMakerCalculationElementType.PAREN_EXPRESSION);
            return;
        }

        if (isLiteral(token)) {
            PsiBuilder.Marker marker = builder.mark();
            builder.advanceLexer();
            marker.done(FileMakerCalculationElementType.LITERAL);
            return;
        }

        // Fallback: consume one token to prevent infinite loop
        builder.advanceLexer();
    }

    private boolean isNameToken(IElementType type) {
        return type == FileMakerCalculationTokenType.IDENTIFIER
                || type == FileMakerCalculationTokenType.KEYWORD_FUNCTION;
    }

    private boolean isLiteral(IElementType type) {
        // Constants, Get ( X ) arguments, field references and ¶ are values, not variables, so they must not become
        // IDENTIFIER_EXPRESSIONs (the annotator checks those for undefined Let variables)
        return type == FileMakerCalculationTokenType.NUMBER
                || type == FileMakerCalculationTokenType.STRING
                || type == FileMakerCalculationTokenType.CONSTANT
                || type == FileMakerCalculationTokenType.GET_CONSTANT
                || type == FileMakerCalculationTokenType.FIELD_REFERENCE
                || type == FileMakerCalculationTokenType.QUOTED_NAME
                || type == FileMakerCalculationTokenType.PARAGRAPH_MARK;
    }

    private boolean isSemicolon(PsiBuilder builder) {
        IElementType t = builder.getTokenType();
        if (t != FileMakerCalculationTokenType.OPERATOR) return false;
        String text = builder.getTokenText();
        return ";".equals(text);
    }

    /** Binary precedence of an operator symbol or word (case-insensitive), or -1 if it is not a binary operator. */
    static int binaryPrecedence(String symbol) {
        Integer prec = Precedence.BINARY.get(symbol.toLowerCase(Locale.ROOT));
        return prec == null ? -1 : prec;
    }

    private int currentOperatorPrecedence(PsiBuilder builder) {
        IElementType t = builder.getTokenType();
        if (t != FileMakerCalculationTokenType.OPERATOR && t != FileMakerCalculationTokenType.KEYWORD_LOGICAL) return -1;
        String s = builder.getTokenText();
        if (s == null) return -1;
        // ";" separates arguments and "not" is unary, so neither is a binary operator here
        return binaryPrecedence(s);
    }

    private boolean tokenTextIs(PsiBuilder builder, String expectedLowercase) {
        String txt = builder.getTokenText();
        return txt != null && txt.equalsIgnoreCase(expectedLowercase);
    }
}
