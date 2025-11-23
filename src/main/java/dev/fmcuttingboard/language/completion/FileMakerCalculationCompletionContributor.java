package dev.fmcuttingboard.language.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

/**
 * Advanced IDE Feature: Code completion for FileMaker functions (Post-MVP).
 * Provides a lightweight completion list of common FileMaker functions, including Get().
 */
public class FileMakerCalculationCompletionContributor extends CompletionContributor {

    private static final List<String> FUNCTIONS = Arrays.asList(
            // Math
            "Abs", "Acos", "Asin", "Atan", "Ceiling", "Cos", "Degrees", "Div", "Exp", "Floor", "Int", "Lg", "Ln", "Log", "Max", "Min", "Mod", "Pi", "Radians", "Round", "Sign", "Sin", "Sqrt", "Tan", "Truncate",
            // Statistics
            "Average", "Count", "StDev", "StDevP", "Sum", "Variance", "VarianceP",
            // Text
            "Char", "Code", "Exact", "Filter", "FilterValues", "Left", "LeftValues", "LeftWords", "Length", "Lower", "Middle", "MiddleValues", "MiddleWords", "Position", "Proper", "Quote", "Replace", "Right", "RightValues", "RightWords", "Substitute", "TextColor", "TextColorRemove", "TextFont", "TextFontRemove", "TextFormatRemove", "TextSize", "TextSizeRemove", "TextStyleAdd", "TextStyleRemove", "Trim", "TrimAll", "Upper", "WordCount",
            // Date/Time
            "Date", "Day", "DayName", "DayNameJ", "DayOfWeek", "DayOfYear", "Hour", "Minute", "Month", "MonthName", "MonthNameJ", "Seconds", "Time", "Timestamp", "WeekOfYear", "WeekOfYearFiscal", "Year", "YearName",
            // Conversion
            "GetAsBoolean", "GetAsCSS", "GetAsDate", "GetAsNumber", "GetAsSVG", "GetAsText", "GetAsTime", "GetAsTimestamp", "GetAsURLEncoded",
            // Generic Get family
            "Get",
            // Lists / Layout / etc (representative subset)
            "List", "ValueCount", "ValueListItems", "ValueListNames", "GetLayoutObjectAttribute",
            // Logical / special
            "Case", "Choose", "Evaluate", "EvaluationError", "If", "IsEmpty", "IsValid", "IsValidExpression", "Let", "PatternCount", "Random", "RGB", "Self", "SerialIncrement", "SetPrecision"
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
                        // Basic filter: avoid offering in comments/strings if possible
                        String elementText = position.getText();
                        if (elementText != null && (elementText.startsWith("//") || elementText.startsWith("/*") || elementText.startsWith("\"") || elementText.startsWith("'"))) {
                            return;
                        }
                        for (String fn : FUNCTIONS) {
                            result.addElement(LookupElementBuilder.create(fn + "(")
                                    .withPresentableText(fn)
                                    .withTypeText("FileMaker function", true));
                        }
                    }
                });
    }
}
