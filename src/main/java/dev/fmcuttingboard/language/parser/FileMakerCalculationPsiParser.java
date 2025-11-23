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
}
