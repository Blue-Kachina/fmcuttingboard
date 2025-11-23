package dev.fmcuttingboard.language.hints;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.parameterInfo.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import dev.fmcuttingboard.language.FileMakerFunctionRegistry;
import dev.fmcuttingboard.language.FunctionMetadata;
import dev.fmcuttingboard.language.FunctionParameter;
import dev.fmcuttingboard.language.psi.FileMakerPsiUtil;

/**
 * Advanced IDE Feature: Parameter hints for functions (Post-MVP).
 *
 * Updated to use PSI-based traversal to detect enclosing function call and current argument index,
 * leveraging Phase 4 structured PSI.
 */
public class FileMakerCalculationParameterInfoHandler implements ParameterInfoHandler<PsiElement, String> {

    @Override
    public @Nullable PsiElement findElementForParameterInfo(@NotNull CreateParameterInfoContext context) {
        PsiFile file = context.getFile();
        int offset = context.getOffset();
        PsiElement at = file.findElementAt(Math.max(0, Math.min(offset, file.getTextLength() - 1)));
        if (at == null && offset > 0) at = file.findElementAt(offset - 1);
        FileMakerPsiUtil.FunctionCallInfo call = FileMakerPsiUtil.getEnclosingFunctionCall(at);
        if (call == null) return null;

        String signature = buildSignature(call.name);
        if (signature == null) return null;

        context.setItemsToShow(new Object[]{signature});
        return call.callElement; // use the function call PSI as anchor
    }

    @Override
    public void showParameterInfo(@NotNull PsiElement element, @NotNull CreateParameterInfoContext context) {
        context.showHint(element, element.getTextOffset(), this);
    }

    @Override
    public @Nullable PsiElement findElementForUpdatingParameterInfo(@NotNull UpdateParameterInfoContext context) {
        PsiFile file = context.getFile();
        int offset = context.getEditor().getCaretModel().getOffset();
        PsiElement at = file.findElementAt(Math.max(0, Math.min(offset, file.getTextLength() - 1)));
        if (at == null && offset > 0) at = file.findElementAt(offset - 1);
        FileMakerPsiUtil.FunctionCallInfo call = FileMakerPsiUtil.getEnclosingFunctionCall(at);
        return call != null ? call.callElement : null;
    }

    @Override
    public void updateParameterInfo(@NotNull PsiElement element, @NotNull UpdateParameterInfoContext context) {
        Editor editor = context.getEditor();
        PsiFile file = context.getFile();
        PsiElement at = file.findElementAt(Math.max(0, Math.min(editor.getCaretModel().getOffset(), file.getTextLength() - 1)));
        if (at == null && editor.getCaretModel().getOffset() > 0) at = file.findElementAt(editor.getCaretModel().getOffset() - 1);
        FileMakerPsiUtil.FunctionCallInfo call = FileMakerPsiUtil.getEnclosingFunctionCall(at);
        int paramIndex = (call != null && call.argIndex >= 0) ? call.argIndex : 0;
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

    // Note: Deprecated ParameterInfoHandler API methods (getParametersForLookup, getParametersForDocumentation,
    // couldShowInLookup, getHelpId) were intentionally omitted to align with modern IntelliJ Platform SDK.

    private static @Nullable String buildSignature(String functionNameRaw) {
        String fn = functionNameRaw.trim();
        // Prefer real metadata from the central registry (Phase 3.1)
        FunctionMetadata meta = FileMakerFunctionRegistry.findByName(fn);
        if (meta != null) {
            return buildTypedSignature(meta);
        }
        // Fallbacks for common built-ins (in case registry is incomplete)
        String fnLower = fn.toLowerCase(Locale.ROOT);
        switch (fnLower) {
            case "get":
                return "Get(name: Text)";
            case "if":
                return "If(test: Any; resultTrue: Any; [resultFalse: Any])";
            case "case":
                return "Case(test1: Any; result1: Any; [testN; resultN]...; [default: Any])";
            case "let":
                return "Let([name = expr; …]; result: Any)";
            default:
                // Generic fallback: function(arg1; arg2; …)
                return fn + "(arg1; arg2; …)";
        }
    }

    private static String buildTypedSignature(@NotNull FunctionMetadata meta) {
        StringBuilder sb = new StringBuilder();
        sb.append(meta.getName()).append("(");
        java.util.List<FunctionParameter> params = meta.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append("; ");
            FunctionParameter p = params.get(i);
            boolean opt = p.isOptional();
            if (opt) sb.append("[");
            // Render as name: Type and preserve variadic … marks
            sb.append(p.getName());
            if (p.isRepeating() && !p.getName().endsWith("...")) sb.append("...");
            if (p.getType() != null && !p.getType().isEmpty()) {
                sb.append(": ").append(p.getType());
            }
            if (opt) sb.append("]");
        }
        sb.append(")");
        return sb.toString();
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
