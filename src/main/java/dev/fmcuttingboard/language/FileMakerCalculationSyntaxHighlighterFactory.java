package dev.fmcuttingboard.language;

import com.intellij.openapi.fileTypes.SingleLazyInstanceSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for FileMaker Calculation syntax highlighter (Phase 3.2).
 */
public class FileMakerCalculationSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {

    @Override
    protected @NotNull SyntaxHighlighter createHighlighter() {
        return new FileMakerCalculationSyntaxHighlighter();
    }
}
