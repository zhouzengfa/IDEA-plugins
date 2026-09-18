package com.gameale.massive.tools.cursor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Opens the IntelliJ project root directory in Cursor.
 *
 * @author zhouzengfa
 * @date 2026/03/20
 */
public final class OpenProjectInCursorAction extends AnAction {

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        event.getPresentation().setEnabledAndVisible(project != null && project.getBasePath() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            Messages.showErrorDialog(project, "Project has no base directory.", "Switch IDEA to Cursor");
            return;
        }
        Path root = Path.of(basePath).toAbsolutePath().normalize();
        try {
            CursorCliLauncher.openPath(root.toString(), root.toString());
        } catch (IOException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "Switch IDEA to Cursor");
        }
    }
}
