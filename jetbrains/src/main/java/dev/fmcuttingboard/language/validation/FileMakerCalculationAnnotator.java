package dev.fmcuttingboard.language.validation;

import com.intellij.lexer.Lexer;
import com.intellij.psi.TokenType;
import dev.fmcuttingboard.language.FileMakerCalculationLexerAdapter;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
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
        // Run once per file. (A PsiFile's parent is its directory, so the old "getParent() != null" guard meant this
        // annotator never ran in a real project.)
        if (!(element instanceof PsiFile)) return;

        CharSequence text = element.getContainingFile().getViewProvider().getContents();

        // Token-based checks, so brackets and control characters inside strings and comments are ignored
        Lexer lexer = new FileMakerCalculationLexerAdapter();
        lexer.start(text);
        int round = 0, square = 0, curly = 0;
        for (IElementType t = lexer.getTokenType(); t != null; lexer.advance(), t = lexer.getTokenType()) {
            int start = lexer.getTokenStart();
            if (t == FileMakerCalculationTokenType.LPAREN) round++;
            else if (t == FileMakerCalculationTokenType.LBRACKET) square++;
            else if (t == FileMakerCalculationTokenType.LBRACE) curly++;
            else if (t == FileMakerCalculationTokenType.RPAREN && --round < 0) { annotateUnmatched(holder, start, ")"); return; }
            else if (t == FileMakerCalculationTokenType.RBRACKET && --square < 0) { annotateUnmatched(holder, start, "]"); return; }
            else if (t == FileMakerCalculationTokenType.RBRACE && --curly < 0) { annotateUnmatched(holder, start, "}"); return; }
            else if (t == FileMakerCalculationTokenType.STRING && !isTerminatedString(text, start, lexer.getTokenEnd())) {
                holder.newAnnotation(HighlightSeverity.ERROR, "Unterminated text constant (missing closing quotation mark)")
                        .range(new TextRange(start, start + 1))
                        .create();
                return;
            } else if (t == TokenType.BAD_CHARACTER && text.charAt(start) < 32) {
                // Tab, CR and LF are whitespace tokens, so any control character here is invalid
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid control character")
                        .range(new TextRange(start, start + 1))
                        .create();
                return;
            }
        }
        // Do not flag unmatched opening here to reduce noise; IDE brace matcher highlights it already.

        // PSI-based validations (function calls)
        validateFunctions(element, holder);

        // Let/While variables used outside their scope
        validateFunctionVariableScopes(element, holder);
    }

    /** A string token ends with a closing quote that is not escaped by an odd run of backslashes. */
    static boolean isTerminatedString(CharSequence text, int start, int end) {
        if (end - start < 2 || text.charAt(end - 1) != '"') return false;
        int backslashes = 0;
        for (int i = end - 2; i > start && text.charAt(i) == '\\'; i--) backslashes++;
        return backslashes % 2 == 0;
    }

    private static void annotateUnmatched(AnnotationHolder holder, int offset, String brace) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Unmatched closing " + brace)
                .range(new TextRange(offset, offset + 1))
                .create();
    }

    /**
     * Let/While variables used where their definitions are not visible (they silently mean a field there). Unknown
     * names are never reported, since a plain name in a calculation is usually a field. See FunctionVariableScopes.
     */
    private static void validateFunctionVariableScopes(@NotNull PsiElement root, @NotNull AnnotationHolder holder) {
        for (FunctionVariableScopes.OutOfScopeUse use : FunctionVariableScopes.findOutOfScopeUses(root)) {
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING,
                            "'" + use.name() + "' is used outside the " + use.function() + "() that defines it (here it refers to a field)")
                    .range(use.use().getTextRange())
                    .create();
        }
    }

    private static void validateFunctions(@NotNull PsiElement root, @NotNull AnnotationHolder holder) {
        // Traverse all function call PSI nodes
        for (FileMakerPsiElements.FileMakerFunctionCallImpl call : PsiTreeUtil.findChildrenOfType(root, FileMakerPsiElements.FileMakerFunctionCallImpl.class)) {
            String fnName = extractFunctionName(call);
            if (fnName == null || fnName.isEmpty()) continue;

            FunctionMetadata meta = FileMakerFunctionRegistry.findByName(fnName);
            if (meta == null) {
                // Only claim "unknown" when the function list is complete (otherwise most real functions, and every
                // custom function, would be flagged); shared/data/filemaker-functions.json "complete"
                if (FileMakerFunctionRegistry.isComplete()) {
                    holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Unknown function '" + fnName + "'")
                            .range(call.getTextRange())
                            .create();
                }
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
