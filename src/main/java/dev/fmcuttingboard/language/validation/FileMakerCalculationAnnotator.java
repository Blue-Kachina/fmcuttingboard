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

        // Undefined variable checks (Let() local variables and $/$$ script variables)
        validateUndefinedVariables(element, holder);
    }

    private static void annotateUnmatched(AnnotationHolder holder, int offset, String brace) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing " + brace)
                .range(new TextRange(offset, offset + 1))
                .create();
    }

    // === Phase 6.1: Undefined variable warnings (best-effort) ===
    private static void validateUndefinedVariables(@NotNull PsiElement root, @NotNull AnnotationHolder holder) {
        // Build simple Let() scopes: for each Let(bindings; result) collect variable names from bindings
        java.util.List<LetScope> scopes = new java.util.ArrayList<>();

        for (FileMakerPsiElements.FileMakerFunctionCallImpl call : PsiTreeUtil.findChildrenOfType(root, FileMakerPsiElements.FileMakerFunctionCallImpl.class)) {
            String fn = extractFunctionName(call);
            if (fn == null || !fn.equalsIgnoreCase("Let")) continue;

            PsiElement bindingsArg = getArgumentAt(call, 0);
            PsiElement resultArg = getArgumentAt(call, 1);
            if (bindingsArg == null || resultArg == null) continue;

            java.util.Set<String> names = extractLetBindingNames(bindingsArg);
            if (!names.isEmpty()) {
                scopes.add(new LetScope(resultArg.getTextRange(), names));
            }
        }

        if (scopes.isEmpty()) return;

        // For each identifier expression, if inside any scope but not defined in a containing scope chain, warn
        for (FileMakerPsiElements.FileMakerIdentifierExpressionImpl id : PsiTreeUtil.findChildrenOfType(root, FileMakerPsiElements.FileMakerIdentifierExpressionImpl.class)) {
            String name = id.getText();
            if (name == null || name.isEmpty()) continue;

            // Function names are not IDENTIFIER_EXPRESSIONs (they appear in FUNCTION_CALL), so safe.
            // Skip literals disguised as identifiers (unlikely) or True/False which lexer marks differently anyway.

            // Determine if this identifier is within any Let() result scope
            int offset = id.getTextRange().getStartOffset();
            LetScope innermost = findInnermostScope(scopes, offset);
            if (innermost == null) {
                // Outside Let result; only warn for $/$$ variables as a weak warning that they may be undefined
                if (isScriptVariable(name)) {
                    holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Script variable may be undefined here: " + name)
                            .range(id.getTextRange())
                            .create();
                }
                continue;
            }

            // Inside a Let() result. Check if defined in any containing scope.
            if (!isDefinedInAnyContainingScope(scopes, offset, name)) {
                holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Undefined variable '" + name + "' (not bound in any Let())")
                        .range(id.getTextRange())
                        .create();
            }
        }
    }

    private static boolean isDefinedInAnyContainingScope(java.util.List<LetScope> scopes, int offset, String name) {
        // Consider all scopes that contain the offset; if any defines the name, return true.
        for (LetScope s : scopes) {
            if (s.range.containsOffset(offset) && s.definedNames.contains(name)) return true;
        }
        return false;
    }

    private static LetScope findInnermostScope(java.util.List<LetScope> scopes, int offset) {
        LetScope best = null;
        for (LetScope s : scopes) {
            if (s.range.containsOffset(offset)) {
                if (best == null || (s.range.getLength() < best.range.getLength())) {
                    best = s;
                }
            }
        }
        return best;
    }

    private static boolean isScriptVariable(String name) {
        return name.startsWith("$"); // $ or $$
    }

    private static PsiElement getArgumentAt(FileMakerPsiElements.FileMakerFunctionCallImpl call, int index) {
        // Find ARG_LIST child and the Nth ARGUMENT
        for (PsiElement child : call.getChildren()) {
            ASTNode node = child.getNode();
            if (node == null) continue;
            if (node.getElementType() == FileMakerCalculationElementType.ARG_LIST) {
                int i = 0;
                for (PsiElement argChild : child.getChildren()) {
                    ASTNode n = argChild.getNode();
                    if (n != null && n.getElementType() == FileMakerCalculationElementType.ARGUMENT) {
                        if (i == index) return argChild;
                        i++;
                    }
                }
            }
        }
        return null;
    }

    private static java.util.Set<String> extractLetBindingNames(PsiElement bindingsArg) {
        String text = bindingsArg.getText();
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        if (text == null || text.isEmpty()) return names;
        // Heuristic: find tokens that look like identifiers or $/$$ vars immediately followed by '='
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?m)(?:^|\\[|;|\\s)\\s*([\u0024]{0,2}[A-Za-z_][A-Za-z0-9_]*)\\s*=");
        java.util.regex.Matcher m = p.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (name != null && !name.isEmpty()) names.add(name);
        }
        return names;
    }

    private static class LetScope {
        final TextRange range;
        final java.util.Set<String> definedNames;
        LetScope(TextRange range, java.util.Set<String> names) {
            this.range = range;
            this.definedNames = names;
        }
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
