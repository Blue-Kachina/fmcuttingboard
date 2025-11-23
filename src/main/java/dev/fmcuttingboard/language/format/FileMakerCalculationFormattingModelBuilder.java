package dev.fmcuttingboard.language.format;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Minimal formatter to let the IDE run Reformat Code for .fmcalc files.
 * This provides a trivial formatting model that does not alter whitespace yet,
 * but unlocks the action and lays groundwork for future rules.
 */
public class FileMakerCalculationFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        PsiFile file = element.getContainingFile();
        Block rootBlock = new SimpleRootBlock(file.getNode());
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

    // Backwards compatibility with older IDEs (may be deprecated in newer SDKs)
    @Override
    public @NotNull FormattingModel createModel(@NotNull PsiElement element, @NotNull CodeStyleSettings settings) {
        PsiFile file = element.getContainingFile();
        Block rootBlock = new SimpleRootBlock(file.getNode());
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

    private static class SimpleRootBlock implements Block {
        private final ASTNode node;

        SimpleRootBlock(ASTNode node) {
            this.node = node;
        }

        @Override
        public @NotNull TextRange getTextRange() {
            return node != null ? node.getTextRange() : TextRange.EMPTY_RANGE;
        }

        @Override
        public @NotNull List<Block> getSubBlocks() {
            // No PSI parser yet: keep it flat
            return Collections.emptyList();
        }

        @Override
        public @Nullable Wrap getWrap() { return null; }

        @Override
        public @Nullable Indent getIndent() { return Indent.getNoneIndent(); }

        @Override
        public @Nullable Alignment getAlignment() { return null; }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) { return null; }

        @Override
        public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            return new ChildAttributes(Indent.getNoneIndent(), null);
        }

        @Override
        public boolean isIncomplete() { return false; }

        @Override
        public boolean isLeaf() { return true; }
    }
}
