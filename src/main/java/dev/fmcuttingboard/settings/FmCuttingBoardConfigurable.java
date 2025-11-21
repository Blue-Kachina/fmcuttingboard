package dev.fmcuttingboard.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Phase 7.1 — Plugin Settings UI (Project-level)
 */
public class FmCuttingBoardConfigurable implements Configurable {
    private final Project project;

    private JPanel mainPanel;
    private JTextField baseDirField;
    private JTextField filePatternField;
    private JLabel helpLabel;
    private JCheckBox previewBeforeClipboardWriteCheckbox;

    public FmCuttingBoardConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "FMCuttingBoard";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (mainPanel == null) {
            mainPanel = new JPanel(new BorderLayout(8, 8));

            JPanel fields = new JPanel();
            fields.setLayout(new GridBagLayout());
            GridBagConstraints gc = new GridBagConstraints();
            gc.insets = new Insets(4, 4, 4, 4);
            gc.gridx = 0; gc.gridy = 0; gc.anchor = GridBagConstraints.LINE_END;
            fields.add(new JLabel("Base directory for saved XML:"), gc);
            gc.gridx = 1; gc.gridy = 0; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.LINE_START;
            baseDirField = new JTextField();
            fields.add(baseDirField, gc);

            gc.gridx = 0; gc.gridy = 1; gc.weightx = 0; gc.fill = GridBagConstraints.NONE; gc.anchor = GridBagConstraints.LINE_END;
            fields.add(new JLabel("Filename pattern:"), gc);
            gc.gridx = 1; gc.gridy = 1; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.LINE_START;
            filePatternField = new JTextField();
            fields.add(filePatternField, gc);

            gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 2; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.LINE_START;
            previewBeforeClipboardWriteCheckbox = new JCheckBox("Show preview confirmation before writing to clipboard");
            fields.add(previewBeforeClipboardWriteCheckbox, gc);

            mainPanel.add(fields, BorderLayout.NORTH);

            helpLabel = new JLabel("Use {timestamp} for epoch millis. Defaults: .fmCuttingBoard and fmclip-{timestamp}.xml");
            helpLabel.setForeground(new Color(90, 90, 90));
            mainPanel.add(helpLabel, BorderLayout.SOUTH);
        }

        reset();
        return mainPanel;
    }

    @Override
    public boolean isModified() {
        FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
        String bd = baseDirField.getText().trim();
        String pat = filePatternField.getText().trim();
        boolean preview = previewBeforeClipboardWriteCheckbox.isSelected();
        return !bd.equals(st.getBaseDirName()) || !pat.equals(st.getFileNamePattern()) || preview != st.isPreviewBeforeClipboardWrite();
    }

    @Override
    public void apply() {
        FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
        st.setBaseDirName(baseDirField.getText().trim());
        st.setFileNamePattern(filePatternField.getText().trim());
        st.setPreviewBeforeClipboardWrite(previewBeforeClipboardWriteCheckbox.isSelected());
    }

    @Override
    public void reset() {
        FmCuttingBoardSettingsState st = FmCuttingBoardSettingsState.getInstance(project);
        baseDirField.setText(st.getBaseDirName());
        filePatternField.setText(st.getFileNamePattern());
        previewBeforeClipboardWriteCheckbox.setSelected(st.isPreviewBeforeClipboardWrite());
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
        baseDirField = null;
        filePatternField = null;
        helpLabel = null;
        previewBeforeClipboardWriteCheckbox = null;
    }
}
