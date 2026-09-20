package com.zookeeper.browser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Settings page for the ZooKeeper connect string.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class ZookeeperConfigurable implements Configurable {

    private final Project project;
    private JBTextField connectStringField;
    private JBTextField pathField;
    private JBTextField sessionTimeoutField;
    private JButton testButton;

    public ZookeeperConfigurable(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "ZooKeeper Browser";
    }

    @Override
    public @Nullable JComponent createComponent() {
        connectStringField = new JBTextField();
        pathField = new JBTextField();
        sessionTimeoutField = new JBTextField();
        testButton = new JButton("Test");
        testButton.addActionListener(e -> testConnection());
        connectStringField.addActionListener(e -> testConnection());
        sessionTimeoutField.addActionListener(e -> testConnection());

        JPanel connectRow = new JPanel(new BorderLayout(8, 0));
        connectRow.add(connectStringField, BorderLayout.CENTER);
        connectRow.add(testButton, BorderLayout.EAST);

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Connect string:", connectRow, 1, false)
                .addLabeledComponent("Path (empty = all):", pathField, 1, false)
                .addLabeledComponent("Session timeout (ms):", sessionTimeoutField, 1, false)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(8, 0));
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        ZookeeperSettings settings = ZookeeperSettings.getInstance(project);
        return !connectStringField.getText().trim().equals(settings.getConnectString())
                || !pathField.getText().trim().equals(settings.getPath())
                || parseTimeout(sessionTimeoutField.getText()) != settings.getSessionTimeoutMs();
    }

    @Override
    public void apply() throws ConfigurationException {
        int timeout = parseTimeout(sessionTimeoutField.getText());
        if (timeout <= 0) {
            throw new ConfigurationException("Session timeout must be a positive number.");
        }
        ZookeeperSettings settings = ZookeeperSettings.getInstance(project);
        settings.setConnectString(connectStringField.getText().trim());
        settings.setPath(pathField.getText().trim());
        settings.setSessionTimeoutMs(timeout);
    }

    @Override
    public void reset() {
        ZookeeperSettings settings = ZookeeperSettings.getInstance(project);
        connectStringField.setText(settings.getConnectString());
        pathField.setText(settings.getPath());
        sessionTimeoutField.setText(String.valueOf(settings.getSessionTimeoutMs()));
    }

    private void testConnection() {
        String connectString = connectStringField.getText().trim();
        int timeout = parseTimeout(sessionTimeoutField.getText());
        String title = "Connection to " + hostLabel(connectString);
        testButton.setEnabled(false);
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Testing ZooKeeper connection", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ZookeeperClientService.testConnection(connectString, timeout, pathField.getText());
                    ApplicationManager.getApplication().invokeLater(() -> {
                        testButton.setEnabled(true);
                        Messages.showMessageDialog(
                                connectStringField,
                                "Successfully connected!",
                                title,
                                Messages.getInformationIcon()
                        );
                    });
                } catch (Exception ex) {
                    String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    ApplicationManager.getApplication().invokeLater(() -> {
                        testButton.setEnabled(true);
                        Messages.showMessageDialog(
                                connectStringField,
                                reason,
                                title,
                                Messages.getErrorIcon()
                        );
                    });
                }
            }
        });
    }

    private static @NotNull String hostLabel(@NotNull String connectString) {
        if (connectString.isEmpty()) {
            return "ZooKeeper";
        }
        int comma = connectString.indexOf(',');
        String first = comma < 0 ? connectString : connectString.substring(0, comma);
        int colon = first.lastIndexOf(':');
        return colon < 0 ? first : first.substring(0, colon);
    }

    private static int parseTimeout(@NotNull String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
