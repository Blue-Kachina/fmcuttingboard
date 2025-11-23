package dev.fmcuttingboard.language.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal PSI element implementations for Phase 4 structured parsing.
 * These are thin wrappers over AST nodes to enable visitors and type checks.
 *
 * Phase 4.2: Generated (hand-written stand-in) PSI classes.
 */
public final class FileMakerPsiElements {
    private FileMakerPsiElements() {}

    public static class FileMakerFunctionCallImpl extends ASTWrapperPsiElement {
        public FileMakerFunctionCallImpl(@NotNull ASTNode node) { super(node); }
    }

    public static class FileMakerArgListImpl extends ASTWrapperPsiElement {
        public FileMakerArgListImpl(@NotNull ASTNode node) { super(node); }
    }

    public static class FileMakerArgumentImpl extends ASTWrapperPsiElement {
        public FileMakerArgumentImpl(@NotNull ASTNode node) { super(node); }
    }

    public static class FileMakerParenExpressionImpl extends ASTWrapperPsiElement {
        public FileMakerParenExpressionImpl(@NotNull ASTNode node) { super(node); }
    }

    public static class FileMakerIdentifierExpressionImpl extends ASTWrapperPsiElement {
        public FileMakerIdentifierExpressionImpl(@NotNull ASTNode node) { super(node); }
    }

    public static class FileMakerLiteralImpl extends ASTWrapperPsiElement {
        public FileMakerLiteralImpl(@NotNull ASTNode node) { super(node); }
    }
}
