package dev.fmcuttingboard.language.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerFunctionRegistry;
import dev.fmcuttingboard.language.FunctionMetadata;
import dev.fmcuttingboard.language.FunctionParameter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Phase 2 – Enhanced Code Completion
 * 2.1 Context-Aware Completion using FileMakerFunctionRegistry
 * 2.2 Smart insertion with parameter placeholders via IntelliJ Live Templates (implemented)
 */
public class FileMakerCalculationCompletionContributor extends CompletionContributor {

    // Representative subset of Get() parameter constants (Phase 2.1 requirement)
    private static final List<String> GET_CONSTANTS = Arrays.asList(
            "AccountName", "ApplicationLanguage", "CurrentTimeUTCMilliseconds",
            "Device", "DocumentsPath", "FileName", "HostName", "LastError",
            "LayoutName", "ModelName", "ModifiedFields", "NetworkProtocol",
            "PersistentID", "PreferencesPath", "ScreenScale", "ScriptName",
            "SystemLanguage", "TotalRecordCount", "UserName", "WindowName"
    );

    public FileMakerCalculationCompletionContributor() {
        extend(CompletionType.BASIC,
                PlatformPatterns.psiElement().withLanguage(FileMakerCalculationLanguage.INSTANCE),
                new CompletionProvider<CompletionParameters>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                  @NotNull ProcessingContext context,
                                                  @NotNull CompletionResultSet result) {
                        PsiElement position = parameters.getPosition();
                        if (isInCommentOrString(position)) return;

                        // Offer function completions from the registry
                        for (FunctionMetadata meta : FileMakerFunctionRegistry.getAll()) {
                            result.addElement(
                                    LookupElementBuilder.create(meta.getName())
                                            .withPresentableText(meta.getName())
                                            .withTypeText(meta.getCategory() + (meta.getReturnType() != null ? " → " + meta.getReturnType() : ""), true)
                                            .withTailText("  " + meta.getSimpleSignature(), true)
                                            .withInsertHandler((context1, item) -> insertFunctionTemplate(context1, meta))
                            );
                        }

                        // Provide Get() constants when typing inside a Get( … ) call (simple heuristic)
                        if (looksLikeInsideGet(parameters)) {
                            for (String name : GET_CONSTANTS) {
                                result.addElement(
                                        LookupElementBuilder.create(name)
                                                .withTypeText("Get() constant", true)
                                );
                            }
                        }
                    }
                });
    }

    private static boolean isInCommentOrString(PsiElement position) {
        String text = position.getText();
        return text != null && (text.startsWith("//") || text.startsWith("/*") || text.startsWith("\"") || text.startsWith("'"));
    }

    // Heuristic detection: We are inside the parentheses of a Get( … ) call
    private static boolean looksLikeInsideGet(CompletionParameters parameters) {
        Document doc = parameters.getEditor().getDocument();
        int offset = parameters.getOffset();
        int start = Math.max(0, offset - 200); // look back a small window
        String before = doc.getText().substring(start, Math.max(start, offset));
        int getIdx = before.lastIndexOf("Get(");
        int closeIdx = before.lastIndexOf(")");
        return getIdx >= 0 && (closeIdx < 0 || closeIdx < getIdx);
    }

    // Insert a live template like: Name($param1$; $param2$)
    private static void insertFunctionTemplate(@NotNull InsertionContext context, @NotNull FunctionMetadata meta) {
        Project project = context.getProject();
        TemplateManager tm = TemplateManager.getInstance(project);

        Template template = tm.createTemplate("", "filemaker");
        template.setToReformat(false);

        // Build: Name( $p1$; $p2$ )
        template.addTextSegment(meta.getName());
        template.addTextSegment("(");
        List<FunctionParameter> params = meta.getParameters();
        for (int i = 0; i < params.size(); i++) {
            FunctionParameter p = params.get(i);
            if (i > 0) template.addTextSegment("; ");
            // use the raw parameter name as placeholder, without [] or ...
            String placeholder = p.getName().replace("[", "").replace("]", "").replace("...", "");
            template.addVariable(placeholder, new TextExpression(placeholder), new TextExpression(placeholder), true);
        }
        template.addTextSegment(")");

        // Replace current completion insertion (which inserted the name) with our full template
        int startOffset = context.getStartOffset();
        int tailOffset = context.getTailOffset();
        context.getDocument().deleteString(startOffset, tailOffset);
        context.getEditor().getCaretModel().moveToOffset(startOffset);
        tm.startTemplate(context.getEditor(), template);
    }
}
