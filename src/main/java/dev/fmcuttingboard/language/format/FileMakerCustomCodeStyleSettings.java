package dev.fmcuttingboard.language.format;

import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;

/**
 * Custom code style settings for FileMaker Calculation language.
 */
public class FileMakerCustomCodeStyleSettings extends CustomCodeStyleSettings {

    // Tabs and Indents - additional behavior
    public boolean DO_NOT_INDENT_TOP_LET_VARIABLES = true;

    public FileMakerCustomCodeStyleSettings(CodeStyleSettings container) {
        super("FileMakerCustomCodeStyleSettings", container);
    }
}
