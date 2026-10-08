package dev.fmcuttingboard.language.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Folding for .fmcalc, shared with the VS Code extension (vscode/src/core/CalcFolding.ts) and pinned by
 * shared/fixtures/golden/cases.json "folding":
 * <ul>
 *   <li>the inside of multi-line Let / Case / If / While calls (any case, any spacing): "…" between the parentheses;</li>
 *   <li>the inside of multi-line [ … ] lists (e.g. Let variables);</li>
 *   <li>multi-line block comments: "/*…*&#47;".</li>
 * </ul>
 * Uses the PSI, so parentheses inside strings and comments are ignored and nested calls fold too.
 */
public class FileMakerCalculationFoldingBuilder extends FoldingBuilderEx {

    private static final Set<String> FOLDING_CALLS = Set.of("let", "case", "if", "while");

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        List<FoldingDescriptor> out = new ArrayList<>();
        CharSequence text = document.getCharsSequence();
        visit(root.getNode(), text, out);
        return out.toArray(FoldingDescriptor[]::new);
    }

    private static void visit(ASTNode node, CharSequence text, List<FoldingDescriptor> out) {
        IElementType type = node.getElementType();
        if (type == FileMakerCalculationElementType.FUNCTION_CALL) {
            ASTNode name = node.getFirstChildNode();
            ASTNode open = node.findChildByType(FileMakerCalculationTokenType.LPAREN);
            ASTNode close = lastChildOfType(node, FileMakerCalculationTokenType.RPAREN);
            if (name != null && open != null && close != null
                    && FOLDING_CALLS.contains(name.getText().toLowerCase(Locale.ROOT))) {
                addIfMultiLine(node, open.getTextRange().getEndOffset(), close.getStartOffset(), "…", text, out);
            }
        } else if (type == FileMakerCalculationElementType.BRACKET_LIST) {
            ASTNode open = node.getFirstChildNode();
            ASTNode close = node.getLastChildNode();
            if (open != null && close != null && open != close
                    && open.getElementType() == FileMakerCalculationTokenType.LBRACKET
                    && close.getElementType() == FileMakerCalculationTokenType.RBRACKET) {
                addIfMultiLine(node, open.getTextRange().getEndOffset(), close.getStartOffset(), "…", text, out);
            }
        } else if (type == FileMakerCalculationTokenType.BLOCK_COMMENT) {
            addIfMultiLine(node, node.getStartOffset(), node.getTextRange().getEndOffset(), "/*…*/", text, out);
        }
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) visit(child, text, out);
    }

    private static @Nullable ASTNode lastChildOfType(ASTNode node, IElementType type) {
        for (ASTNode c = node.getLastChildNode(); c != null; c = c.getTreePrev()) if (c.getElementType() == type) return c;
        return null;
    }

    private static void addIfMultiLine(ASTNode node, int start, int end, String placeholder, CharSequence text,
                                       List<FoldingDescriptor> out) {
        if (end > start && text.subSequence(start, end).toString().contains("\n")) {
            out.add(new FoldingDescriptor(node, new TextRange(start, end), null, placeholder));
        }
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode node) {
        return "…";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }
}
