package dev.fmcuttingboard.language;

import com.intellij.lang.Commenter;
import org.jetbrains.annotations.Nullable;

/**
 * Enables Toggle Line Comment and Block Comment actions for .fmcalc files.
 *
 * FileMaker calculations commonly use // for line comments and C-style block comments
 * in many editors. Our lexer already recognizes both, so wiring a Commenter gives
 * first-class IDE commenting support.
 */
public class FileMakerCalculationCommenter implements Commenter {

    @Override
    public @Nullable String getLineCommentPrefix() {
        return "//";
    }

    @Override
    public @Nullable String getBlockCommentPrefix() {
        return "/*";
    }

    @Override
    public @Nullable String getBlockCommentSuffix() {
        return "*/";
    }

    @Override
    public @Nullable String getCommentedBlockCommentPrefix() {
        // Returning null keeps default behavior (no nested block comment markers).
        return null;
    }

    @Override
    public @Nullable String getCommentedBlockCommentSuffix() {
        return null;
    }
}
