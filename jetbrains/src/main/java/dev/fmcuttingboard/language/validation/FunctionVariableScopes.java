package dev.fmcuttingboard.language.validation;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import dev.fmcuttingboard.language.FileMakerCalculationElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import dev.fmcuttingboard.language.psi.FileMakerPsiElements.FileMakerFunctionCallImpl;
import dev.fmcuttingboard.language.psi.FileMakerPsiElements.FileMakerIdentifierExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scopes of function variables defined by {@code Let ( [ … ] ; calculation )} and
 * {@code While ( [ initialVariable ] ; condition ; [ logic ] ; result )}, following the Claris help:
 * <ul>
 *   <li>Let sets variables left to right: each one is visible to later definitions and to the calculation.</li>
 *   <li>While's initialVariable definitions are visible to the following parameters; logic definitions are
 *       re-evaluated each iteration, so they are also visible to the condition.</li>
 *   <li>{@code $} and {@code $$} variables are script/file-scoped, not function variables, so they are ignored.</li>
 * </ul>
 * A plain name in a calculation is usually a field, so an unknown name is never reported. The only confident finding
 * is a name that IS defined by a Let/While in this calculation but used where none of its definitions is visible:
 * there it silently means a field of the same name, which is almost always a mistake.
 */
final class FunctionVariableScopes {

    /** A use of {@code name} outside every {@code function} (Let/While) that defines it. */
    record OutOfScopeUse(@NotNull PsiElement use, @NotNull String name, @NotNull String function) {}

    private record Definition(String name, String function, TextRange visibleIn) {}

    private FunctionVariableScopes() {}

    static @NotNull List<OutOfScopeUse> findOutOfScopeUses(@NotNull PsiElement root) {
        List<Definition> definitions = new ArrayList<>();
        Set<PsiElement> declarations = new HashSet<>();

        for (FileMakerFunctionCallImpl call : PsiTreeUtil.findChildrenOfType(root, FileMakerFunctionCallImpl.class)) {
            String fn = functionName(call);
            List<PsiElement> args = arguments(call);
            int callEnd = call.getTextRange().getEndOffset();
            if ("let".equals(fn) && !args.isEmpty()) {
                for (Binding b : bindings(args.get(0))) {
                    definitions.add(new Definition(b.name, "Let", new TextRange(b.end, callEnd)));
                    declarations.add(b.declaration);
                }
            } else if ("while".equals(fn) && !args.isEmpty()) {
                for (Binding b : bindings(args.get(0))) {
                    definitions.add(new Definition(b.name, "While", new TextRange(b.end, callEnd)));
                    declarations.add(b.declaration);
                }
                if (args.size() > 2) {
                    int conditionStart = args.get(1).getTextRange().getStartOffset();
                    for (Binding b : bindings(args.get(2))) {
                        definitions.add(new Definition(b.name, "While", new TextRange(conditionStart, callEnd)));
                        declarations.add(b.declaration);
                    }
                }
            }
        }
        if (definitions.isEmpty()) return List.of();

        List<OutOfScopeUse> out = new ArrayList<>();
        for (FileMakerIdentifierExpressionImpl id : PsiTreeUtil.findChildrenOfType(root, FileMakerIdentifierExpressionImpl.class)) {
            if (declarations.contains(id)) continue;
            String name = id.getText();
            if (name == null || name.isEmpty() || name.startsWith("$")) continue;
            String key = name.toLowerCase(Locale.ROOT);
            int offset = id.getTextRange().getStartOffset();

            Definition anyDefinition = null;
            boolean visible = false;
            for (Definition d : definitions) {
                if (!d.name.equals(key)) continue;
                if (anyDefinition == null) anyDefinition = d;
                if (d.visibleIn.containsOffset(offset)) { visible = true; break; }
            }
            if (anyDefinition != null && !visible) out.add(new OutOfScopeUse(id, name, anyDefinition.function));
        }
        return out;
    }

    private record Binding(String name, PsiElement declaration, int end) {}

    /** Definitions in a Let/While variables argument: {@code [ a = 1 ; b = a ]} or a single {@code a = 1}. */
    private static List<Binding> bindings(@NotNull PsiElement argument) {
        List<Binding> out = new ArrayList<>();
        PsiElement expr = firstChild(argument);
        if (expr == null) return out;
        List<PsiElement> items = isType(expr, FileMakerCalculationElementType.BRACKET_LIST)
                ? List.of(expr.getChildren())
                : List.of(expr);
        for (PsiElement item : items) {
            if (!isType(item, FileMakerCalculationElementType.BINARY_EXPRESSION)) continue;
            PsiElement left = firstChild(item);
            ASTNode op = item.getNode().findChildByType(FileMakerCalculationTokenType.OPERATOR);
            if (left instanceof FileMakerIdentifierExpressionImpl && op != null && "=".equals(op.getText())) {
                out.add(new Binding(left.getText().toLowerCase(Locale.ROOT), left, item.getTextRange().getEndOffset()));
            }
        }
        return out;
    }

    private static @Nullable String functionName(@NotNull FileMakerFunctionCallImpl call) {
        PsiElement first = call.getFirstChild();
        return first == null ? null : first.getText().toLowerCase(Locale.ROOT);
    }

    private static List<PsiElement> arguments(@NotNull FileMakerFunctionCallImpl call) {
        List<PsiElement> out = new ArrayList<>();
        for (PsiElement child : call.getChildren()) {
            if (!isType(child, FileMakerCalculationElementType.ARG_LIST)) continue;
            for (PsiElement arg : child.getChildren()) {
                if (isType(arg, FileMakerCalculationElementType.ARGUMENT)) out.add(arg);
            }
        }
        return out;
    }

    private static @Nullable PsiElement firstChild(@NotNull PsiElement element) {
        PsiElement[] children = element.getChildren();
        return children.length == 0 ? null : children[0];
    }

    private static boolean isType(@NotNull PsiElement element, @NotNull Object type) {
        return element.getNode() != null && element.getNode().getElementType() == type;
    }
}
