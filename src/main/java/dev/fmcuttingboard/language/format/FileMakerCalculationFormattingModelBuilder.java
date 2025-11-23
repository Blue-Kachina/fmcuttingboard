package dev.fmcuttingboard.language.format;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

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
        SpacingBuilder spacingBuilder = createSpacingBuilder(settings);
        Block rootBlock = new SimpleBlock(file.getNode(), null, Indent.getNoneIndent(), null, spacingBuilder);
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

    private static SpacingBuilder createSpacingBuilder(CodeStyleSettings settings) {
        CommonCodeStyleSettings common = settings.getCommonSettings(FileMakerCalculationLanguage.INSTANCE);
        // We keep rules conservative due to flat PSI; this primarily ensures spaces after commas
        // and optional spaces around binary operators if user enabled them.
        return new SpacingBuilder(settings, FileMakerCalculationLanguage.INSTANCE)
                // Space after comma
                .after(commaOrSemicolon()).spaces(1)
                // No space before comma/semicolon
                .before(commaOrSemicolon()).spaces(0)
                // Basic: parentheses tight spacing
                .after(FileMakerCalculationTokenType.LPAREN).spaces(0)
                .before(FileMakerCalculationTokenType.RPAREN).spaces(0)
                // Allow IDE setting to control spaces around operators generically
                .around(FileMakerCalculationTokenType.OPERATOR)
                .spaceIf(common.SPACE_AROUND_ASSIGNMENT_OPERATORS || common.SPACE_AROUND_LOGICAL_OPERATORS || common.SPACE_AROUND_BITWISE_OPERATORS || common.SPACE_AROUND_EQUALITY_OPERATORS || common.SPACE_AROUND_RELATIONAL_OPERATORS || common.SPACE_AROUND_MULTIPLICATIVE_OPERATORS || common.SPACE_AROUND_ADDITIVE_OPERATORS, true);
    }

    private static TokenSet commaOrSemicolon() {
        // Comma and semicolon are produced as OPERATOR tokens by the lexer; we can only target
        // them specifically when they are represented as concrete element types. Since we don't
        // distinguish, keep returning OPERATOR for both cases to at least format separators.
        return TokenSet.create(FileMakerCalculationTokenType.OPERATOR);
    }

    private static class SimpleBlock implements Block {
        private final ASTNode node;
        private final Alignment alignment;
        private final Indent indent;
        private final Wrap wrap;
        private final SpacingBuilder spacingBuilder;
        private List<Block> subBlocks;

        SimpleBlock(ASTNode node, @Nullable Alignment alignment, @Nullable Indent indent, @Nullable Wrap wrap, SpacingBuilder spacingBuilder) {
            this.node = node;
            this.alignment = alignment;
            this.indent = indent;
            this.wrap = wrap;
            this.spacingBuilder = spacingBuilder;
        }

        @Override
        public @NotNull TextRange getTextRange() {
            return node != null ? node.getTextRange() : TextRange.EMPTY_RANGE;
        }

        @Override
        public @NotNull List<Block> getSubBlocks() {
            if (subBlocks == null) {
                subBlocks = buildSubBlocks();
            }
            return subBlocks;
        }

        private List<Block> buildSubBlocks() {
            if (node == null) return Collections.emptyList();
            List<Block> result = new ArrayList<>();
            for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
                IElementType type = child.getElementType();
                if (type == FileMakerCalculationTokenType.WHITE_SPACE) continue; // whitespace handled by formatter
                result.add(new SimpleBlock(child, null, calcIndent(child), null, spacingBuilder));
            }
            return result;
        }

        private Indent calcIndent(ASTNode child) {
            // With a flat PSI we cannot infer structural blocks; keep no indent for now.
            return Indent.getNoneIndent();
        }

        @Override
        public @Nullable Wrap getWrap() { return wrap; }

        @Override
        public @Nullable Indent getIndent() { return indent; }

        @Override
        public @Nullable Alignment getAlignment() { return alignment; }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            return this.spacingBuilder.getSpacing(this, child1, child2);
        }

        @Override
        public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            return new ChildAttributes(Indent.getNoneIndent(), null);
        }

        @Override
        public boolean isIncomplete() { return false; }

        @Override
        public boolean isLeaf() { return node == null || node.getFirstChildNode() == null; }
    }
}
