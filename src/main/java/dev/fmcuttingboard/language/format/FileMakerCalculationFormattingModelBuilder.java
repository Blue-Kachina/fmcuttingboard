package dev.fmcuttingboard.language.format;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
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
 * Phase 5 – Initial intelligent formatting for FileMaker calculations.
 *
 * Scope (minimal, safe defaults):
 * - Keep one space around operators when appropriate
 * - No space just inside parentheses
 * - Ensure a space after semicolons when used as parameter separators
 * - Indent nested argument lists (e.g., Let/If/Case bodies)
 *
 * Note: This is the initial implementation for Phase 5.2 and is intentionally
 * conservative to avoid surprising rewrites. Future phases may introduce
 * configurable code style options and more granular rules.
 */
public class FileMakerCalculationFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiElement element = formattingContext.getPsiElement();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        PsiFile file = element.getContainingFile();
        SpacingBuilder spacing = createSpacingBuilder(FileMakerCalculationLanguage.INSTANCE, settings);
        Block rootBlock = new FmBlock(file.getNode(), null, null, spacing, Indent.getNoneIndent());
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

    // Backwards compatibility with older IDEs (may be deprecated in newer SDKs)
    @Override
    public @NotNull FormattingModel createModel(@NotNull PsiElement element, @NotNull CodeStyleSettings settings) {
        PsiFile file = element.getContainingFile();
        SpacingBuilder spacing = createSpacingBuilder(FileMakerCalculationLanguage.INSTANCE, settings);
        Block rootBlock = new FmBlock(file.getNode(), null, null, spacing, Indent.getNoneIndent());
        return FormattingModelProvider.createFormattingModelForPsiFile(file, rootBlock, settings);
    }

    private static SpacingBuilder createSpacingBuilder(Language language, CodeStyleSettings settings) {
        // Basic defaults without exposing settings yet. These can be wired to CodeStyle later (Phase 5.3).
        return new SpacingBuilder(settings, language)
                // No space just inside parentheses: (expr) → "(" no space, and before ")" no space
                .after(FileMakerCalculationTokenType.LPAREN).spaces(0)
                .before(FileMakerCalculationTokenType.RPAREN).spaces(0)

                // Space around generic operators (includes punctuation; keep conservative):
                .around(FileMakerCalculationTokenType.OPERATOR).spaces(1)

                // After semicolons (treated as OPERATOR by lexer) ensure a single space
                // SpacingBuilder cannot distinguish semicolon vs other operators here, but
                // the around(OPERATOR) rule already ensures spaces around common separators.
                ;
    }

    private static final TokenSet WHITES = TokenSet.create(FileMakerCalculationTokenType.WHITE_SPACE);

    /**
     * Simple AST-based block that builds children for all non-whitespace leaf nodes
     * and applies a SpacingBuilder for spacing decisions. Indentation is increased
     * one level inside parentheses to improve readability of multi-line argument lists.
     */
    private static class FmBlock extends AbstractBlock {
        private final SpacingBuilder spacingBuilder;
        private final Indent myIndent;

        protected FmBlock(@NotNull ASTNode node,
                          @Nullable Wrap wrap,
                          @Nullable Alignment alignment,
                          @NotNull SpacingBuilder spacingBuilder,
                          @NotNull Indent indent) {
            super(node, wrap, alignment);
            this.spacingBuilder = spacingBuilder;
            this.myIndent = indent;
        }

        @Override
        protected List<Block> buildChildren() {
            ASTNode child = myNode.getFirstChildNode();
            if (child == null) return Collections.emptyList();
            List<Block> result = new ArrayList<>();
            while (child != null) {
                IElementType type = child.getElementType();
                if (!WHITES.contains(type) && child.getTextRange().getLength() > 0) {
                    // Indent one level when inside parentheses content
                    Indent indent = computeChildIndent(type);
                    result.add(new FmBlock(child, null, null, spacingBuilder, indent));
                }
                child = child.getTreeNext();
            }
            return result;
        }

        private Indent computeChildIndent(IElementType type) {
            // If this block is the content between parentheses, increase indent.
            ASTNode parent = myNode.getTreeParent();
            if (parent != null) {
                IElementType pType = parent.getElementType();
                // Heuristic: if parent is an ARG_LIST or we are a sibling between LPAREN/RPAREN, indent
                if ("ARG_LIST".equals(pType.toString())) return Indent.getNormalIndent();
            }
            // Also indent for top-level children inside a parenthesized expression
            if (myNode.getElementType() == FileMakerCalculationTokenType.LPAREN) {
                return Indent.getNormalIndent();
            }
            return myIndent;
        }

        @Override
        public Indent getIndent() {
            return myIndent;
        }

        @Override
        public boolean isLeaf() {
            return myNode.getFirstChildNode() == null;
        }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            return this.spacingBuilder.getSpacing(this, child1, child2);
        }

        @Override
        public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            // New children within this block are indented normally if we are inside parentheses or argument lists
            return new ChildAttributes(Indent.getNormalIndent(), null);
        }
    }
}
