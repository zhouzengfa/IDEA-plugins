package com.gameale.massive.tools.cursor;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Settings UI for Cursor CLI path and launch behavior.
 *
 * @author zhouzengfa
 * @date 2026/03/20
 */
public final class OpenInCursorConfigurable implements Configurable {

    private TextFieldWithBrowseButton executableField;
    private JBCheckBox reuseWindowCheckBox;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Switch IDEA to Cursor";
    }

    @Override
    public @Nullable JComponent createComponent() {
        executableField = new TextFieldWithBrowseButton();
        executableField.addBrowseFolderListener(
                "Cursor executable",
                "Select Cursor.exe (preferred) or cursor.cmd",
                null,
                FileChooserDescriptorFactory.createSingleFileDescriptor()
        );
        reuseWindowCheckBox = new JBCheckBox("Reuse existing Cursor window (--reuse-window)", true);
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Cursor executable (optional):", executableField, 1, false)
                .addComponent(reuseWindowCheckBox)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        OpenInCursorSettings settings = OpenInCursorSettings.getInstance();
        return !executableField.getText().trim().equals(settings.getCursorExecutable())
                || reuseWindowCheckBox.isSelected() != settings.isReuseWindow();
    }

    @Override
    public void apply() {
        OpenInCursorSettings settings = OpenInCursorSettings.getInstance();
        settings.setCursorExecutable(executableField.getText().trim());
        settings.setReuseWindow(reuseWindowCheckBox.isSelected());
    }

    @Override
    public void reset() {
        OpenInCursorSettings settings = OpenInCursorSettings.getInstance();
        executableField.setText(settings.getCursorExecutable());
        reuseWindowCheckBox.setSelected(settings.isReuseWindow());
    }
}
