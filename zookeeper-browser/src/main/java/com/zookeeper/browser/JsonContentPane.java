package com.zookeeper.browser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Right pane: pretty-printed JSON. Fold icons sit in front of foldable nodes.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class JsonContentPane implements Disposable {

    private final Project project;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JLabel pathLabel = new JLabel(" ");
    private final Document document;
    private final Editor editor;

    public JsonContentPane(@NotNull Project project) {
        this.project = project;
        document = EditorFactory.getInstance().createDocument("Select a node to view its data.");
        editor = EditorFactory.getInstance().createEditor(document, project, PlainTextFileType.INSTANCE, true);
        configureEditor();

        pathLabel.setBorder(JBUI.Borders.empty(4, 8));
        root.add(pathLabel, BorderLayout.NORTH);
        root.add(editor.getComponent(), BorderLayout.CENTER);
    }

    public @NotNull JComponent getComponent() {
        return root;
    }

    public void showText(@NotNull String path, @NotNull String text) {
        setDocument(path, text, false);
    }

    public void showData(@NotNull String path, @Nullable byte[] data) {
        setDocument(path, ZookeeperClientService.formatData(data), true);
    }

    private void setDocument(@NotNull String path, @NotNull String text, boolean foldJson) {
        pathLabel.setText(path.isEmpty() ? " " : path);
        WriteCommandAction.runWriteCommandAction(project, () -> document.setText(text.replace("\r\n", "\n")));
        if (foldJson) {
            JsonFold.apply(editor.getFoldingModel(), document.getText());
        } else {
            editor.getFoldingModel().runBatchFoldingOperation(() -> {
                for (var region : editor.getFoldingModel().getAllFoldRegions()) {
                    editor.getFoldingModel().removeFoldRegion(region);
                }
            });
        }
    }

    private void configureEditor() {
        EditorSettings settings = editor.getSettings();
        settings.setLineNumbersShown(true);
        settings.setFoldingOutlineShown(true);
        settings.setLineMarkerAreaShown(true);
        settings.setIndentGuidesShown(true);
        settings.setUseSoftWraps(false);
        settings.setAdditionalColumnsCount(0);
        if (editor instanceof EditorEx ex) {
            ex.setViewer(true);
        }
    }

    @Override
    public void dispose() {
        EditorFactory.getInstance().releaseEditor(editor);
    }
}
