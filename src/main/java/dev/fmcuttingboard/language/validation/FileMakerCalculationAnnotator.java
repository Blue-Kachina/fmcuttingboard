package dev.fmcuttingboard.language.validation;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Advanced IDE Feature: Basic error detection for invalid syntax (Post-MVP).
 *
 * This annotator performs a lightweight scan to detect obvious issues without a full parser:
 * - Reports the first unmatched closing parenthesis/bracket/brace.
 * - Flags any control characters (excluding whitespace) as invalid.
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
    }

    private static void annotateUnmatched(AnnotationHolder holder, int offset, String brace) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing " + brace)
                .range(new TextRange(offset, offset + 1))
                .create();
    }
}
