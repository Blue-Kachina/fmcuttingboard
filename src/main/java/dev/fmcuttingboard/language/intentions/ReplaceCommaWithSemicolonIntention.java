package dev.fmcuttingboard.language.intentions;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationTokenType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * Phase 6.2 Quick Fixes: Replace comma with semicolon in argument lists.
 * FileMaker uses ';' as the parameter separator. Users often paste examples with ','.
 */
public class ReplaceCommaWithSemicolonIntention implements IntentionAction {

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getText() {
        return "Replace comma with semicolon";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "FileMaker Quick Fixes";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file.getLanguage() instanceof FileMakerCalculationLanguage)) return false;
        PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
        if (at == null || at.getNode() == null) return false;
        if (at.getNode().getElementType() != FileMakerCalculationTokenType.OPERATOR) return false;
        return ",".equals(at.getText());
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
        if (at == null) return;
        // Simple replacement
        at.replace(createSemicolonElement(file, ";"));
    }

    private PsiElement createSemicolonElement(PsiFile file, String text) {
        // Create a tiny temporary file content with just a semicolon and grab its PSI element
        PsiFile temp = com.intellij.psi.PsiFileFactory.getInstance(file.getProject())
                .createFileFromText("_tmp.fmcalc", file.getLanguage(), text, false, false);
        return temp.getFirstChild();
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
