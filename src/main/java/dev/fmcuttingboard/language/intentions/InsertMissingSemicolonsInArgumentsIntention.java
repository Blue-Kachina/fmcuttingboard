package dev.fmcuttingboard.language.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 6.2 Quick Fix: Insert missing semicolons between arguments for stacked function calls.
 *
 * Typical case:
 * If (
 *   test
 *   resultTrue
 *   resultFalse
 * )
 * This intention inserts semicolons before line breaks inside the parentheses when they are missing.
 */
public class InsertMissingSemicolonsInArgumentsIntention implements IntentionAction {

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getText() {
        return "Insert missing semicolons between function arguments";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "FileMaker Quick Fixes";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file.getLanguage() instanceof FileMakerCalculationLanguage)) return false;
        // Heuristic: if caret is inside parentheses region and there exists a newline not preceded by ';'
        int offset = editor.getCaretModel().getOffset();
        CharSequence text = editor.getDocument().getCharsSequence();
        int lparen = findMatchingLeftParen(text, offset);
        int rparen = findRightParen(text, offset);
        if (lparen < 0 || rparen < 0 || rparen <= lparen) return false;
        return hasLineBreakMissingSemicolon(text, lparen + 1, rparen);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        Document document = editor.getDocument();
        CharSequence text = document.getCharsSequence();
        int offset = editor.getCaretModel().getOffset();
        int lparen = findMatchingLeftParen(text, offset);
        int rparen = findRightParen(text, offset);
        if (lparen < 0 || rparen < 0 || rparen <= lparen) return;

        // Insert semicolons before line breaks that separate arguments
        StringBuilder sb = new StringBuilder();
        int start = lparen + 1;
        int end = rparen;
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                // handle CRLF as a unit
                int prev = previousNonSpace(text, i - 1, start);
                if (prev >= start) {
                    char p = text.charAt(prev);
                    if (p != ';' && p != '(') {
                        sb.append(';');
                    }
                }
                sb.append(c);
                // copy following \n if present
                if (i + 1 < end && text.charAt(i + 1) == '\n') {
                    sb.append('\n');
                    i++; // skip LF too
                }
                continue;
            } else if (c == '\n') {
                int prev = previousNonSpace(text, i - 1, start);
                if (prev >= start) {
                    char p = text.charAt(prev);
                    if (p != ';' && p != '(') {
                        sb.append(';');
                    }
                }
                sb.append('\n');
                continue;
            }
            sb.append(c);
        }

        document.replaceString(start, end, sb.toString());
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }

    private static boolean hasLineBreakMissingSemicolon(CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                int prev = previousNonSpace(text, i - 1, start);
                if (prev >= start) {
                    char p = text.charAt(prev);
                    if (p != ';' && p != '(') return true;
                }
            }
        }
        return false;
    }

    private static int previousNonSpace(CharSequence text, int idx, int lowerBound) {
        for (int i = idx; i >= lowerBound; i--) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c)) return i;
        }
        return -1;
    }

    private static int findRightParen(CharSequence text, int from) {
        int n = text.length();
        for (int i = from; i < n; i++) {
            char c = text.charAt(i);
            if (c == ')') return i;
        }
        return -1;
    }

    private static int findMatchingLeftParen(CharSequence text, int from) {
        for (int i = from; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '(') return i;
        }
        return -1;
    }
}
