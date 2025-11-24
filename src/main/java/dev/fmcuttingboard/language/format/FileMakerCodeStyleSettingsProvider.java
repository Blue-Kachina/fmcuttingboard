package dev.fmcuttingboard.language.format;

import com.intellij.application.options.CodeStyleAbstractConfigurable;
import com.intellij.application.options.CodeStyleAbstractPanel;
import com.intellij.application.options.IndentOptionsEditor;
import com.intellij.lang.Language;
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.CodeStyleConfigurable;
import dev.fmcuttingboard.language.FileMakerCalculationLanguage;
import dev.fmcuttingboard.language.FileMakerCalculationFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterClient;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Minimal Code Style settings provider for FileMaker calculations.
 * Currently provides only indent options in a simple, non-tabbed UI.
 *
 * FUTURE ENHANCEMENT: To add tabbed interface with preview panel:
 * 1. Replace SimpleIndentOptionsPanel with TabbedLanguageCodeStylePanel
 * 2. Override getFileType() to return FileMakerCalculationFileType.INSTANCE
 * 3. Ensure FormattingModelBuilder is fully implemented for proper preview rendering
 * 4. Add additional tabs (Spaces, Wrapping and Braces, Blank Lines, etc.) as needed
 */
public class FileMakerCodeStyleSettingsProvider extends LanguageCodeStyleSettingsProvider {

    @Override
    public @NotNull Language getLanguage() {
        return FileMakerCalculationLanguage.INSTANCE;
    }

    @Override
    public void customizeSettings(@NotNull CodeStyleSettingsCustomizable consumer, @NotNull SettingsType settingsType) {
        // Use default common settings; future work can expose custom options (Phase 5.3 details)
    }

    /**
     * Creates a simple configurable with only indent options.
     * This avoids the complexity of TabbedLanguageCodeStylePanel which requires
     * a fully functional preview editor (which was causing AssertionError at runtime).
     */
    @Override
    public @NotNull CodeStyleConfigurable createConfigurable(@NotNull CodeStyleSettings settings,
                                                             @NotNull CodeStyleSettings originalSettings) {
        return new CodeStyleAbstractConfigurable(settings, originalSettings, "FileMaker Calculation") {
            @Override
            protected @NotNull CodeStyleAbstractPanel createPanel(@NotNull CodeStyleSettings settings) {
                return new SimpleIndentOptionsPanel(settings);
            }
        };
    }

    /**
     * Simple panel that displays only indent options without tabs or preview.
     * This is sufficient for basic indent configuration and avoids UI complexity.
     */
    private static class SimpleIndentOptionsPanel extends CodeStyleAbstractPanel {
        private IndentOptionsEditor myIndentOptionsEditor;

        public SimpleIndentOptionsPanel(CodeStyleSettings settings) {
            super(FileMakerCalculationLanguage.INSTANCE, null, settings);
        }

        @Override
        protected int getRightMargin() {
            return 0; // No right margin indicator needed for indent-only panel
        }

        @Override
        @NotNull
        protected FileType getFileType() {
            return FileMakerCalculationFileType.INSTANCE;
        }

        @Override
        protected void resetImpl(CodeStyleSettings settings) {
            CommonCodeStyleSettings commonSettings = settings.getCommonSettings(FileMakerCalculationLanguage.INSTANCE);
            if (myIndentOptionsEditor != null) {
                myIndentOptionsEditor.setEnabled(true);
                myIndentOptionsEditor.reset(settings, commonSettings.getIndentOptions());
            }
        }

        @Override
        public void apply(CodeStyleSettings settings) {
            CommonCodeStyleSettings commonSettings = settings.getCommonSettings(FileMakerCalculationLanguage.INSTANCE);
            if (myIndentOptionsEditor != null) {
                myIndentOptionsEditor.apply(settings, commonSettings.getIndentOptions());
            }
        }

        @Override
        public boolean isModified(CodeStyleSettings settings) {
            CommonCodeStyleSettings commonSettings = settings.getCommonSettings(FileMakerCalculationLanguage.INSTANCE);
            return myIndentOptionsEditor != null && myIndentOptionsEditor.isModified(settings, commonSettings.getIndentOptions());
        }

        @Override
        @Nullable
        public JComponent getPanel() {
            if (myIndentOptionsEditor == null) {
                myIndentOptionsEditor = new IndentOptionsEditor();
            }
            return myIndentOptionsEditor.createPanel();
        }

        @Override
        @Nullable
        protected String getPreviewText() {
            // No preview for this simple panel
            return null;
        }

        @Override
        @NotNull
        protected EditorHighlighter createHighlighter(@NotNull EditorColorsScheme scheme) {
            // Return a simple highlighter since we don't have a preview panel
            return new EditorHighlighter() {
                @Override
                public void setText(@NotNull CharSequence text) {}

                @Override
                public void setEditor(@NotNull HighlighterClient editor) {}

                @Override
                @NotNull
                public HighlighterIterator createIterator(int startOffset) {
                    return new HighlighterIterator() {
                        @Override
                        public void advance() {}

                        @Override
                        public void retreat() {}

                        @Override
                        public boolean atEnd() {
                            return true;
                        }

                        @Override
                        public int getStart() {
                            return 0;
                        }

                        @Override
                        public int getEnd() {
                            return 0;
                        }

                        @Override
                        @NotNull
                        public TextAttributes getTextAttributes() {
                            return new TextAttributes();
                        }

                        @Override
                        @NotNull
                        public IElementType getTokenType() {
                            return TokenType.WHITE_SPACE;
                        }

                        @Override
                        @NotNull
                        public Document getDocument() {
                            // Return a minimal document stub
                            return new com.intellij.openapi.editor.impl.DocumentImpl("");
                        }
                    };
                }
            };
        }
    }

    @Override
    public String getCodeSample(@NotNull SettingsType settingsType) {
        return "Let(\n" +
               "  [ a = Abs(-3.14159); b = Round(Sin(1.2345); 3) ];\n" +
               "  If( a > b; \"x\"; \"y\" )\n" +
               ")";
    }

    @Override
    public @NotNull CommonCodeStyleSettings getDefaultCommonSettings() {
        // Provide safe defaults so the preview panel can render without relying on IDE defaults.
        CommonCodeStyleSettings settings = new CommonCodeStyleSettings(getLanguage());
        CommonCodeStyleSettings.IndentOptions indent = settings.initIndentOptions();
        // Conservative 2-space indentation, no tabs by default
        indent.INDENT_SIZE = 2;
        indent.CONTINUATION_INDENT_SIZE = 2;
        indent.TAB_SIZE = 2;
        settings.SPACE_AROUND_ASSIGNMENT_OPERATORS = true;
        settings.SPACE_AROUND_LOGICAL_OPERATORS = true;
        settings.SPACE_AROUND_EQUALITY_OPERATORS = true;
        settings.SPACE_WITHIN_PARENTHESES = false;
        return settings;
    }
}
