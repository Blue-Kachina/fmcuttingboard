package dev.fmcuttingboard.language.validation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.util.PsiTreeUtil;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FunctionMetadata;
import dev.fmcuttingboard.language.FunctionParameter;
import dev.fmcuttingboard.language.FileMakerFunctionRegistry;
import dev.fmcuttingboard.language.psi.FileMakerPsiElements;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;

/**
 * Phase 6.1 Enhanced Error Detection
 *
 * Adds lightweight PSI-aware validations on top of basic lexical checks:
 * - Existing: unmatched closing delimiters and invalid control characters
 * - New: function existence and parameter count validation using FunctionRegistry
 */
public class FileMakerCalculationAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // Only operate once at file root to avoid repeated scans
        if (element.getParent() != null) return;

        CharSequence text = element.getContainingFile().getViewProvider().getContents();

        // Detect unmatched closing delimiters using a simple stack counter
        int round = 0, square = 0, curly = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '(': round++; break;
                case ')': round--; if (round < 0) { annotateUnmatched(holder, i, ")"); return; } break;
                case '[': square++; break;
                case ']': square--; if (square < 0) { annotateUnmatched(holder, i, "]"); return; } break;
                case '{': curly++; break;
                case '}': curly--; if (curly < 0) { annotateUnmatched(holder, i, "}"); return; } break;
                default:
                    // Flag control chars except common whitespace (\t,\r,\n)
                    if (c < 32 && c != '\t' && c != '\r' && c != '\n') {
                        holder.newAnnotation(HighlightSeverity.ERROR, "Invalid control character")
                                .range(new TextRange(i, i + 1))
                                .create();
                        return;
                    }
            }
        }
        // Do not flag unmatched opening here to reduce noise; IDE brace matcher highlights it already.

        // PSI-based validations (function calls)
        validateFunctions(element, holder);
    }

    private static void annotateUnmatched(AnnotationHolder holder, int offset, String brace) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing " + brace)
                .range(new TextRange(offset, offset + 1))
                .create();
    }

    private static void validateFunctions(@NotNull PsiElement root, @NotNull AnnotationHolder holder) {
        // Traverse all function call PSI nodes
        for (FileMakerPsiElements.FileMakerFunctionCallImpl call : PsiTreeUtil.findChildrenOfType(root, FileMakerPsiElements.FileMakerFunctionCallImpl.class)) {
            String fnName = extractFunctionName(call);
            if (fnName == null || fnName.isEmpty()) continue;

            FunctionMetadata meta = FileMakerFunctionRegistry.findByName(fnName);
            if (meta == null) {
                // Unknown function – weak warning
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unknown function '" + fnName + "'")
                        .range(call.getTextRange())
                        .create();
                continue;
            }

            int argCount = countArguments(call);
            // Compute min/max based on metadata
            int min = 0;
            int max = 0;
            boolean hasRepeating = false;
            for (FunctionParameter p : meta.getParameters()) {
                if (!p.isOptional() && !p.isRepeating()) min++;
                if (p.isRepeating()) {
                    hasRepeating = true;
                } else {
                    max++;
                }
            }
            if (hasRepeating) {
                max = Integer.MAX_VALUE;
            }

            if (argCount < min) {
                String msg = String.format("Too few arguments for %s: expected at least %d, got %d", meta.getName(), min, argCount);
                holder.newAnnotation(HighlightSeverity.ERROR, msg)
                        .range(call.getTextRange())
                        .create();
            } else if (argCount > max) {
                String expected = hasRepeating ? (min + "+") : String.valueOf(max);
                String msg = String.format("Too many arguments for %s: expected %s, got %d", meta.getName(), expected, argCount);
                holder.newAnnotation(HighlightSeverity.ERROR, msg)
                        .range(call.getTextRange())
                        .create();
            }
        }
    }

    private static String extractFunctionName(FileMakerPsiElements.FileMakerFunctionCallImpl call) {
        // FUNCTION_CALL node layout: NAME TOKEN, '(', ARG_LIST?, ')'
        PsiElement first = call.getFirstChild();
        if (first == null) return null;
        String text = first.getText();
        return text != null ? text : null;
    }

    private static int countArguments(FileMakerPsiElements.FileMakerFunctionCallImpl call) {
        // Find ARG_LIST child and count ARGUMENT children
        for (PsiElement child : call.getChildren()) {
            ASTNode node = child.getNode();
            if (node == null) continue;
            IElementType type = node.getElementType();
            if (type == FileMakerCalculationElementType.ARG_LIST) {
                int count = 0;
                for (PsiElement argChild : child.getChildren()) {
                    ASTNode n = argChild.getNode();
                    if (n != null && n.getElementType() == FileMakerCalculationElementType.ARGUMENT) count++;
                }
                return count;
            }
        }
        return 0; // no args
    }
}
