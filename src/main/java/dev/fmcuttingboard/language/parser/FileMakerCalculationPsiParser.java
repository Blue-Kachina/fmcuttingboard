package dev.fmcuttingboard.language.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;

/**
 * Lightweight recursive-descent parser creating a minimal PSI structure for FileMaker calculations.
 * Handles function calls, parenthesized expressions, identifiers and literals.
 *
 * Phase 4.3: Replaced bootstrap flat parser with this structured parser.
 * Phase 4.3 (refinement): Add basic unary/binary expression parsing with simple precedence.
 */
public class FileMakerCalculationPsiParser implements PsiParser {
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
        PsiBuilder.Marker leftMarker = builder.mark();
        parseUnary(builder);
        leftMarker.drop(); // we'll wrap as we see operators

        while (true) {
            int prec = currentOperatorPrecedence(builder);
            if (prec < minPrec) break;
            String opText = builder.getTokenText();
            PsiBuilder.Marker exprMarker = builder.mark();
            builder.advanceLexer(); // consume operator
            // Right-assoc not needed here; all supported operators left-assoc
            parseBinary(builder, prec + 1);
            exprMarker.done(FileMakerCalculationElementType.BINARY_EXPRESSION);
        }
    }

    private void parseUnary(PsiBuilder builder) {
        // Unary NOT
        if (builder.getTokenType() == FileMakerCalculationTokenType.KEYWORD_LOGICAL
                && tokenTextIs(builder, "not")) {
            PsiBuilder.Marker m = builder.mark();
            builder.advanceLexer();
            parseUnary(builder);
            m.done(FileMakerCalculationElementType.UNARY_EXPRESSION);
            return;
        }
        parsePrimary(builder);
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
        return type == FileMakerCalculationTokenType.NUMBER
                || type == FileMakerCalculationTokenType.STRING;
    }

    private boolean isSemicolon(PsiBuilder builder) {
        IElementType t = builder.getTokenType();
        if (t != FileMakerCalculationTokenType.OPERATOR) return false;
        String text = builder.getTokenText();
        return ";".equals(text);
    }

    private int currentOperatorPrecedence(PsiBuilder builder) {
        IElementType t = builder.getTokenType();
        if (t == null) return -1;
        // Do not treat semicolon as operator here; it's an argument separator
        if (t == FileMakerCalculationTokenType.OPERATOR) {
            String s = builder.getTokenText();
            if (s == null) return -1;
            if (";".equals(s)) return -1;
            // Arithmetic
            if ("*".equals(s) || "/".equals(s)) return 40;
            if ("+".equals(s) || "-".equals(s)) return 30;
            // Comparison
            if ("=".equals(s) || "≠".equals(s) || ">".equals(s) || "<".equals(s)
                    || "≥".equals(s) || "≤".equals(s)) return 20;
        }
        if (t == FileMakerCalculationTokenType.KEYWORD_LOGICAL) {
            // logical and/or
            if (tokenTextIs(builder, "and")) return 10;
            if (tokenTextIs(builder, "or")) return 5;
        }
        return -1;
    }

    private boolean tokenTextIs(PsiBuilder builder, String expectedLowercase) {
        String txt = builder.getTokenText();
        return txt != null && txt.equalsIgnoreCase(expectedLowercase);
    }
}
