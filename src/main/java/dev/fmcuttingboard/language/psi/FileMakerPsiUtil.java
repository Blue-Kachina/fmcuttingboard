package dev.fmcuttingboard.language.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Read-only PSI helpers for navigating FileMaker Calculation PSI trees.
 */
public final class FileMakerPsiUtil {
    private FileMakerPsiUtil() {}

    public static class FunctionCallInfo {
        public final @NotNull String name;
        public final int argIndex; // -1 if not in args
        public final @NotNull PsiElement callElement;

        public FunctionCallInfo(@NotNull String name, int argIndex, @NotNull PsiElement callElement) {
            this.name = name;
            this.argIndex = argIndex;
            this.callElement = callElement;
        }
    }

    /**
     * Returns the enclosing function call info for a caret element, or null if not inside a call.
     */
    public static @Nullable FunctionCallInfo getEnclosingFunctionCall(@Nullable PsiElement element) {
        if (element == null) return null;
        // Find nearest ARG_LIST or FUNCTION_CALL
        PsiElement call = ascendTo(element, FileMakerCalculationElementType.FUNCTION_CALL);
        if (call == null) return null;

        String name = extractFunctionName(call);
        if (name == null || name.isEmpty()) return null;

        int argIndex = computeArgumentIndex(element, call);
        return new FunctionCallInfo(name, argIndex, call);
    }

    private static @Nullable PsiElement ascendTo(@NotNull PsiElement from, @NotNull Object targetType) {
        PsiElement e = from;
        while (e != null) {
            ASTNode node = e.getNode();
            if (node != null && node.getElementType() == targetType) return e;
            e = e.getParent();
        }
        return null;
    }

    private static int computeArgumentIndex(@NotNull PsiElement anchor, @NotNull PsiElement functionCall) {
        // Locate ARG_LIST under functionCall
        PsiElement argList = null;
        for (PsiElement child = functionCall.getFirstChild(); child != null; child = child.getNextSibling()) {
            ASTNode n = child.getNode();
            if (n != null && n.getElementType() == FileMakerCalculationElementType.ARG_LIST) {
                argList = child; break;
            }
        }
        if (argList == null) return -1;

        // If caret is before the arg_list range, return 0
        TextRange caretRange = anchor.getTextRange();
        if (caretRange == null) return -1;

        int index = 0;
        for (PsiElement child = argList.getFirstChild(); child != null; child = child.getNextSibling()) {
            ASTNode node = child.getNode();
            if (node == null) continue;
            if (node.getElementType() == FileMakerCalculationElementType.ARGUMENT) {
                TextRange r = child.getTextRange();
                if (r != null && r.contains(caretRange.getStartOffset())) {
                    return index;
                }
                index++;
            }
        }
        // If we are after the last argument but still in the call, return last index (append position)
        return Math.max(0, index - 1);
    }

    private static @Nullable String extractFunctionName(@NotNull PsiElement functionCall) {
        for (PsiElement child = functionCall.getFirstChild(); child != null; child = child.getNextSibling()) {
            ASTNode node = child.getNode();
            if (node == null) continue;
            if (node.getElementType() == FileMakerCalculationTokenType.IDENTIFIER ||
                node.getElementType() == FileMakerCalculationTokenType.KEYWORD_FUNCTION) {
                return child.getText();
            }
        }
        return null;
    }

    public static boolean isInStringOrComment(@Nullable PsiElement element) {
        if (element == null) return false;
        return isStringOrComment(element) || isStringOrComment(element.getParent());
    }

    private static boolean isStringOrComment(@Nullable PsiElement e) {
        if (e == null || e.getNode() == null) return false;
        var t = e.getNode().getElementType();
        return t == FileMakerCalculationTokenType.STRING
                || t == FileMakerCalculationTokenType.LINE_COMMENT
                || t == FileMakerCalculationTokenType.BLOCK_COMMENT;
    }
}
