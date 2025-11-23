package dev.fmcuttingboard.language.hints;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Advanced IDE Feature: Parameter hints for functions (Post-MVP).
 *
 * Lightweight implementation that parses the document text around the caret to detect a function call
 * and shows a simple signature with the current argument highlighted. Works without a full PSI grammar.
 */
public class FileMakerCalculationParameterInfoHandler implements ParameterInfoHandler<PsiElement, String> {

    @Override
    public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        PsiFile file = context.getFile();
        int offset = context.getOffset();
        CharSequence text = file.getViewProvider().getContents();

        CallInfo call = findEnclosingCall(text, offset);
        if (call == null) return null;

        String signature = buildSignature(call.functionName);
        if (signature == null) return null;

        context.setItemsToShow(new Object[]{signature});
        return file; // use file as anchor element
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextOffset(), this);
    }

    @Override
    public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        return context.getFile();
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement element, @NotNull UpdateParameterInfoContext context) {
        Editor editor = context.getEditor();
        CharSequence text = context.getFile().getViewProvider().getContents();
        CallInfo call = findEnclosingCall(text, editor.getCaretModel().getOffset());
        int paramIndex = call != null ? call.paramIndex : 0;
        context.setCurrentParameter(paramIndex);
    }

    @Override
    public void updateUI(String signature, @NotNull ParameterInfoUIContext uiContext) {
        if (signature == null) {
            uiContext.setUIComponentEnabled(false);
            return;
        }
        int index = uiContext.getCurrentParameterIndex();
        // Parameters are separated by ';' or ',' in FileMaker
        int[] segments = parameterSegments(signature);
        int start = 0, end = 0;
        if (index >= 0 && index < segments.length / 2) {
            start = segments[index * 2];
            end = segments[index * 2 + 1];
        }
        uiContext.setupUIComponentPresentation(
                signature,
                start,
                end,
                false,
                false,
                false,
                uiContext.getDefaultParameterColor()
        );
    }

    @Override
    public @Nullable Object[] getParametersForLookup(@NotNull LookupElement item, @NotNull ParameterInfoContext context) {
        return null;
    }

    @Override
    public @Nullable Object[] getParametersForDocumentation(@NotNull String p, @NotNull ParameterInfoContext context) {
        return null;
    }

    @Override
    public boolean couldShowInLookup() {
        return true;
    }

    @Override
    public @NotNull String getParameterCloseChars() {
        return ",);";
    }

    @Override
    public @Nullable String getHelpId(@NotNull PsiElement element) {
        return "editing.parameter.info";
    }

    // Helpers
    private static class CallInfo {
        final String functionName;
        final int paramIndex;
        CallInfo(String fn, int idx) { this.functionName = fn; this.paramIndex = idx; }
    }

    private static @Nullable CallInfo findEnclosingCall(CharSequence text, int offset) {
        int i = Math.min(offset, text.length());
        // Find preceding '('
        int open = -1;
        for (int p = i - 1; p >= 0; p--) {
            char c = text.charAt(p);
            if (c == '(') { open = p; break; }
            if (c == ';' || c == '\n') break; // bail out unlikely
        }
        if (open < 1) return null;
        // Find function name before '('
        int end = open;
        int start = end - 1;
        while (start >= 0) {
            char c = text.charAt(start);
            if (Character.isLetterOrDigit(c) || c == '_') start--; else break;
        }
        start++;
        if (start >= end) return null;
        String fn = text.subSequence(start, end).toString();
        // Compute parameter index inside parentheses
        int idx = 0; int depth = 0;
        for (int p = open + 1; p < i; p++) {
            char c = text.charAt(p);
            if (c == '(') depth++;
            else if (c == ')') { if (depth == 0) break; depth--; }
            else if ((c == ';' || c == ',') && depth == 0) idx++;
        }
        return new CallInfo(fn, idx);
    }

    private static @Nullable String buildSignature(String functionNameRaw) {
        String fn = functionNameRaw.trim();
        String fnLower = fn.toLowerCase(Locale.ROOT);
        switch (fnLower) {
            case "get":
                return "Get(name)";
            case "if":
                return "If(test; resultTrue; resultFalse)";
            case "case":
                return "Case(test1; result1; …; default)";
            case "let":
                return "Let([name = expr; …]; result)";
            default:
                // Generic fallback: function(arg1; arg2; …)
                return fn + "(arg1; arg2; …)";
        }
    }

    private static int[] parameterSegments(String signature) {
        // Return start/end offsets for each parameter inside parentheses [] or () after function name
        int l = signature.indexOf('(');
        int r = signature.lastIndexOf(')');
        if (l < 0 || r <= l) return new int[0];
        int depth = 0; int start = l + 1;
        java.util.List<Integer> segs = new java.util.ArrayList<>();
        for (int i = l + 1; i < r; i++) {
            char c = signature.charAt(i);
            if (c == '(' || c == '[') depth++;
            else if (c == ')' || c == ']') depth--;
            else if ((c == ';' || c == ',') && depth == 0) {
                segs.add(start);
                segs.add(i);
                start = i + 1;
            }
        }
        if (start < r) { segs.add(start); segs.add(r); }
        int[] arr = new int[segs.size()];
        for (int i = 0; i < segs.size(); i++) arr[i] = segs.get(i);
        return arr;
    }
}
