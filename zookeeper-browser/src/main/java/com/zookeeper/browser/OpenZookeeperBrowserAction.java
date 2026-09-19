package com.zookeeper.browser;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

/**
 * Toolbar entrance that opens the ZooKeeper Browser tool window.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class OpenZookeeperBrowserAction extends AnAction implements DumbAware {

    public OpenZookeeperBrowserAction() {
        super("ZooKeeper Browser", "Open ZooKeeper Browser", AllIcons.Nodes.DataSchema);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("ZooKeeper Browser");
        if (toolWindow != null) {
            toolWindow.activate(null);
        }
    }
}
