package dev.fmcuttingboard.language.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Advanced IDE Feature: Code folding for Let(), Case(), If() blocks (Post-MVP).
 *
 * This builder scans the file text for occurrences of Let( ... ), Case( ... ), If( ... )
 * and creates fold regions that collapse the content inside the outer parentheses.
 *
 * Note: This is a lightweight implementation that relies on balanced parentheses; it does not
 * require a full PSI parser and works purely on the document text.
 */
public class FileMakerCalculationFoldingBuilder extends FoldingBuilderEx {

    private static final String[] FOLDING_FUNCTIONS = new String[]{"Let(", "Case(", "If("};

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        CharSequence text = document.getCharsSequence();
        int length = text.length();

        for (String marker : FOLDING_FUNCTIONS) {
            int idx = 0;
            while (idx >= 0 && idx < length) {
                idx = indexOf(text, marker, idx);
                if (idx < 0) break;

                int openParen = idx + marker.length() - 1; // position of '('
                int closeParen = findMatchingParen(text, openParen + 1);
                if (closeParen > openParen + 1) {
                    // Create a fold inside the parentheses if there's content
                    int start = openParen + 1;
                    int end = closeParen;
                    if (end - start > 1) {
                        descriptors.add(new FoldingDescriptor(node, new TextRange(start, end)));
                    }
                    idx = closeParen + 1;
                } else {
                    idx = openParen + 1;
                }
            }
        }

        return descriptors.toArray(FoldingDescriptor[]::new);
    }

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull com.intellij.psi.PsiElement root,
                                                          @NotNull Document document,
                                                          boolean quick) {
        ASTNode node = root.getNode();
        if (node == null) return FoldingDescriptor.EMPTY;
        return buildFoldRegions(node, document);
    }

    @Nullable
    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        return "...";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode node) {
        return false;
    }

    private static int indexOf(CharSequence text, String needle, int from) {
        int max = text.length() - needle.length();
        outer:
        for (int i = Math.max(0, from); i <= max; i++) {
            for (int j = 0; j < needle.length(); j++) {
                if (text.charAt(i + j) != needle.charAt(j)) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int findMatchingParen(CharSequence text, int from) {
        int depth = 1;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1; // not found
    }
}
