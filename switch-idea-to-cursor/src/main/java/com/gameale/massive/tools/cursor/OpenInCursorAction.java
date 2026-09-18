package com.gameale.massive.tools.cursor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Opens the active editor file at the caret position in Cursor.
 *
 * @author zhouzengfa
 * @date 2026/03/20
 */
public final class OpenInCursorAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null && editor != null) {
            file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        }
        boolean enabled = project != null && editor != null && file != null && file.isInLocalFileSystem();
        event.getPresentation().setEnabledAndVisible(enabled);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || editor == null) {
            return;
        }
        if (file == null) {
            file = FileDocumentManager.getInstance().getFile(editor.getDocument());
        }
        if (file == null || !file.isInLocalFileSystem()) {
            Messages.showErrorDialog(project, "Only local files can be opened in Cursor.", "Switch IDEA to Cursor");
            return;
        }

        Path absolutePath = Path.of(file.getPath()).toAbsolutePath().normalize();
        int line = editor.getCaretModel().getLogicalPosition().line + 1;
        int column = editor.getCaretModel().getLogicalPosition().column + 1;
        String workDirectory = project.getBasePath();

        try {
            CursorCliLauncher.gotoLocation(absolutePath.toString(), line, column, workDirectory);
        } catch (IOException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Switch IDEA to Cursor");
        }
    }
}
