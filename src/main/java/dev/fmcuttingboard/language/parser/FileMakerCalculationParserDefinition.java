package dev.fmcuttingboard.language.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationLexerAdapter;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import dev.fmcuttingboard.language.psi.*;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal ParserDefinition wiring lexer and a flat parser to enable PSI/AST features.
 */
public class FileMakerCalculationParserDefinition implements ParserDefinition {

    public static final IFileElementType FILE = new IFileElementType(FileMakerCalculationLanguage.INSTANCE);

    private static final TokenSet WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE);
    private static final TokenSet COMMENTS = TokenSet.create(
            FileMakerCalculationTokenType.LINE_COMMENT,
            FileMakerCalculationTokenType.BLOCK_COMMENT
    );
    private static final TokenSet STRINGS = TokenSet.create(FileMakerCalculationTokenType.STRING);

    @Override
    public @NotNull Lexer createLexer(Project project) {
        return new FileMakerCalculationLexerAdapter();
    }

    @Override
    public @NotNull PsiParser createParser(Project project) {
        return new FileMakerCalculationPsiParser();
    }

    @Override
    public @NotNull IFileElementType getFileNodeType() {
        return FILE;
    }

    @Override
    public @NotNull TokenSet getWhitespaceTokens() {
        return WHITE_SPACES;
    }

    @Override
    public @NotNull TokenSet getCommentTokens() {
        return COMMENTS;
    }

    @Override
    public @NotNull TokenSet getStringLiteralElements() {
        return STRINGS;
    }

    @Override
    public @NotNull PsiElement createElement(@NotNull ASTNode node) {
        IElementType type = node.getElementType();
        if (type == FileMakerCalculationElementType.FUNCTION_CALL) return new FileMakerPsiElements.FileMakerFunctionCallImpl(node);
        if (type == FileMakerCalculationElementType.ARG_LIST) return new FileMakerPsiElements.FileMakerArgListImpl(node);
        if (type == FileMakerCalculationElementType.ARGUMENT) return new FileMakerPsiElements.FileMakerArgumentImpl(node);
        if (type == FileMakerCalculationElementType.PAREN_EXPRESSION) return new FileMakerPsiElements.FileMakerParenExpressionImpl(node);
        if (type == FileMakerCalculationElementType.IDENTIFIER_EXPRESSION) return new FileMakerPsiElements.FileMakerIdentifierExpressionImpl(node);
        if (type == FileMakerCalculationElementType.LITERAL) return new FileMakerPsiElements.FileMakerLiteralImpl(node);
        return new ASTWrapperPsiElement(node);
    }

    @Override
    public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
        return new FileMakerCalculationFile(viewProvider);
    }
}
