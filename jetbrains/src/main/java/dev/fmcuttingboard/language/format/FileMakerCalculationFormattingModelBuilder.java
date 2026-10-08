package dev.fmcuttingboard.language.format;

import com.intellij.formatting.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.common.AbstractBlock;
import dev.fmcuttingboard.language.FileMakerCalculationFileType;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Formats .fmcalc files with {@link FmCalcFormatter} (shared rules with the VS Code extension, pinned by the golden
 * "formatting" cases). The formatted text is expressed to the IDE as a flat list of token blocks with exact spacing,
 * line feeds and indentation between them, so Reformat Code, formatting a selection, and the indent settings work as
 * usual. A calculation that isn't well-formed is left untouched.
 */
public class FileMakerCalculationFormattingModelBuilder implements FormattingModelBuilder {

    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        PsiFile file = formattingContext.getContainingFile();
        CodeStyleSettings settings = formattingContext.getCodeStyleSettings();
        FmCalcFormatter.Options options = optionsFor(settings);
        String formatted = FmCalcFormatter.format(file, options);
        List<ASTNode> leaves = new ArrayList<>();
        collectLeaves(file.getNode(), leaves);
        Layout layout = formatted == null ? null : Layout.of(leaves, formatted, options);
        Block root = new RootBlock(file.getNode(), leaves, layout, options);
        return FormattingModelProvider.createFormattingModelForPsiFile(file, root, settings);
    }

    static FmCalcFormatter.Options optionsFor(CodeStyleSettings settings) {
        CommonCodeStyleSettings.IndentOptions indent = settings.getIndentOptions(FileMakerCalculationFileType.INSTANCE);
        FileMakerCustomCodeStyleSettings custom = settings.getCustomSettings(FileMakerCustomCodeStyleSettings.class);
        boolean tabs = indent.USE_TAB_CHARACTER;
        return new FmCalcFormatter.Options(
                tabs ? "\t" : " ".repeat(indent.INDENT_SIZE),
                tabs ? indent.TAB_SIZE : indent.INDENT_SIZE,
                settings.getRightMargin(FileMakerCalculationLanguage.INSTANCE),
                custom.DO_NOT_INDENT_TOP_LET_VARIABLES);
    }

    /** Every non-whitespace leaf, in document order. */
    private static void collectLeaves(ASTNode node, List<ASTNode> out) {
        ASTNode child = node.getFirstChildNode();
        if (child == null) {
            if (node.getTextLength() > 0 && node.getElementType() != FileMakerCalculationTokenType.WHITE_SPACE) out.add(node);
            return;
        }
        for (; child != null; child = child.getTreeNext()) collectLeaves(child, out);
    }

    /** For each leaf, the whitespace the formatted text puts before it (only whitespace differs from the original). */
    private record Layout(List<String> gapsBefore, int indentWidth) {

        static @Nullable Layout of(List<ASTNode> leaves, String formatted, FmCalcFormatter.Options options) {
            List<String> gaps = new ArrayList<>(leaves.size());
            int pos = 0;
            for (ASTNode leaf : leaves) {
                int start = pos;
                while (pos < formatted.length() && Character.isWhitespace(formatted.charAt(pos))) pos++;
                String text = leaf.getText();
                if (!formatted.startsWith(text, pos)) return null; // should not happen: formatting only changes whitespace
                gaps.add(formatted.substring(start, pos));
                pos += text.length();
            }
            return new Layout(gaps, options.indentWidth());
        }

        Spacing spacingBefore(int index) {
            String gap = gapsBefore.get(index);
            int lineFeeds = (int) gap.chars().filter(c -> c == '\n').count();
            if (lineFeeds > 0) return Spacing.createSpacing(0, 0, lineFeeds, false, 0);
            return Spacing.createSpacing(gap.length(), gap.length(), 0, false, 0);
        }

        Indent indentOf(int index) {
            String gap = gapsBefore.get(index);
            int nl = gap.lastIndexOf('\n');
            if (index == 0 || nl < 0) return Indent.getNoneIndent();
            int columns = 0;
            for (char c : gap.substring(nl + 1).toCharArray()) columns += c == '\t' ? indentWidth : 1;
            return Indent.getSpaceIndent(columns);
        }
    }

    private static final class RootBlock extends AbstractBlock {
        private final List<ASTNode> leaves;
        private final @Nullable Layout layout;
        private final FmCalcFormatter.Options options;

        RootBlock(ASTNode node, List<ASTNode> leaves, @Nullable Layout layout, FmCalcFormatter.Options options) {
            super(node, null, null);
            this.leaves = leaves;
            this.layout = layout;
            this.options = options;
        }

        @Override
        protected List<Block> buildChildren() {
            if (layout == null) return Collections.emptyList();
            List<Block> blocks = new ArrayList<>(leaves.size());
            for (int i = 0; i < leaves.size(); i++) blocks.add(new LeafBlock(leaves.get(i), layout.indentOf(i), i));
            return blocks;
        }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            if (layout == null || !(child2 instanceof LeafBlock leaf)) return null;
            return leaf.index > 0 ? layout.spacingBefore(leaf.index) : null;
        }

        /** Enter: indent one level per bracket still open before the caret. */
        @Override
        public @NotNull ChildAttributes getChildAttributes(int newChildIndex) {
            int depth = 0;
            for (int i = 0; i < Math.min(newChildIndex, leaves.size()); i++) {
                var type = leaves.get(i).getElementType();
                if (type == FileMakerCalculationTokenType.LPAREN || type == FileMakerCalculationTokenType.LBRACKET) depth++;
                else if (type == FileMakerCalculationTokenType.RPAREN || type == FileMakerCalculationTokenType.RBRACKET) depth--;
            }
            return new ChildAttributes(Indent.getSpaceIndent(Math.max(0, depth) * options.indentWidth()), null);
        }

        @Override
        public boolean isLeaf() {
            return layout == null;
        }

        @Override
        public Indent getIndent() {
            return Indent.getNoneIndent();
        }
    }

    private static final class LeafBlock extends AbstractBlock {
        private final Indent indent;
        final int index;

        LeafBlock(ASTNode node, Indent indent, int index) {
            super(node, null, null);
            this.indent = indent;
            this.index = index;
        }

        @Override
        protected List<Block> buildChildren() {
            return Collections.emptyList();
        }

        @Override
        public @Nullable Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
            return null;
        }

        @Override
        public boolean isLeaf() {
            return true;
        }

        @Override
        public Indent getIndent() {
            return indent;
        }
    }
}
