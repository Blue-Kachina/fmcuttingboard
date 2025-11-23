package dev.fmcuttingboard.language.parser;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * Minimal PsiParser that builds a flat PSI tree for the entire file.
 * This is a bootstrap parser to unblock PSI-based features; we will
 * replace it with a Grammar-Kit generated parser in subsequent iterations.
 */
public class FileMakerCalculationPsiParser implements PsiParser {
    @Override
    public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
        PsiBuilder.Marker rootMarker = builder.mark();
        // Consume all tokens to create a simple, flat tree under the file root
        while (!builder.eof()) {
            builder.advanceLexer();
        }
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }
}
