package com.zookeeper.browser;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
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
    private JBTextField sessionTimeoutField;
    private JButton testButton;
    private JBLabel testResultLabel;

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
        sessionTimeoutField = new JBTextField();
        testButton = new JButton("Test");
        testResultLabel = new JBLabel("Click Test to verify the connect string.");
        testResultLabel.setForeground(JBColor.GRAY);
        testButton.addActionListener(e -> testConnection());
        connectStringField.addActionListener(e -> testConnection());
        sessionTimeoutField.addActionListener(e -> testConnection());

        JPanel connectRow = new JPanel(new BorderLayout(8, 0));
        connectRow.add(testButton, BorderLayout.WEST);
        connectRow.add(connectStringField, BorderLayout.CENTER);

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("Connect string:", connectRow, 1, false)
                .addLabeledComponent("Session timeout (ms):", sessionTimeoutField, 1, false)
                .addComponent(testResultLabel, 8)
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
        settings.setSessionTimeoutMs(timeout);
    }

    @Override
    public void reset() {
        ZookeeperSettings settings = ZookeeperSettings.getInstance(project);
        connectStringField.setText(settings.getConnectString());
        sessionTimeoutField.setText(String.valueOf(settings.getSessionTimeoutMs()));
    }

    private void testConnection() {
        String connectString = connectStringField.getText().trim();
        int timeout = parseTimeout(sessionTimeoutField.getText());
        testButton.setEnabled(false);
        testResultLabel.setForeground(JBColor.GRAY);
        testResultLabel.setText("Connecting...");
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Testing ZooKeeper connection", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ZookeeperClientService.testConnection(connectString, timeout);
                    ApplicationManager.getApplication().invokeLater(() -> showTestResult(true, "Connect string is correct. Connected to " + connectString));
                } catch (Exception ex) {
                    String reason = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                    ApplicationManager.getApplication().invokeLater(() -> showTestResult(false, reason));
                }
            }
        });
    }

    private void showTestResult(boolean ok, @NotNull String message) {
        testButton.setEnabled(true);
        testResultLabel.setForeground(ok ? new JBColor(0x2E7D32, 0x81C784) : JBColor.RED);
        testResultLabel.setText(ok ? message : "Cannot connect: " + message);
    }

    private static int parseTimeout(@NotNull String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
