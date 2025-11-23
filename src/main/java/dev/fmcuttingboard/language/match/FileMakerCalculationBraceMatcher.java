package dev.fmcuttingboard.language.match;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FileMakerCalculationBraceMatcher implements PairedBraceMatcher {

    private static final BracePair[] PAIRS = new BracePair[]{
            new BracePair(FileMakerCalculationTokenType.LPAREN, FileMakerCalculationTokenType.RPAREN, false),
            new BracePair(FileMakerCalculationTokenType.LBRACKET, FileMakerCalculationTokenType.RBRACKET, false),
            new BracePair(FileMakerCalculationTokenType.LBRACE, FileMakerCalculationTokenType.RBRACE, false)
    };

    @Override
    public BracePair @NotNull [] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
        return true;
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
