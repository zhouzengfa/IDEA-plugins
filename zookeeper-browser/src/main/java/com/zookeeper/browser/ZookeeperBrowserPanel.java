package com.zookeeper.browser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Left node tree and right pretty-printed content.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class ZookeeperBrowserPanel implements Disposable {

    private final Project project;
    private final JPanel root = new JPanel(new BorderLayout());
    private final JBLabel statusLabel = new JBLabel("Disconnected");
    private final JButton connectButton = new JButton("Connect");
    private final JButton disconnectButton = new JButton("Disconnect");
    private final DefaultTreeModel treeModel = new DefaultTreeModel(new ZkNode("/", "/"));
    private final Tree tree = new Tree(treeModel);
    private final JsonContentPane contentPane;

    public ZookeeperBrowserPanel(@NotNull Project project) {
        this.project = project;
        this.contentPane = new JsonContentPane(project);
        buildUi();
        setConnected(false);
    }

    public @NotNull JComponent getComponent() {
        return root;
    }

    private void buildUi() {
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
        toolbar.setBorder(JBUI.Borders.empty(4));
        toolbar.add(connectButton);
        toolbar.add(disconnectButton);
        toolbar.add(statusLabel);

        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> disconnect());

        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                Object last = event.getPath().getLastPathComponent();
                if (last instanceof ZkNode node) {
                    loadChildren(node);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) {
            }
        });
        tree.addTreeSelectionListener(e -> {
            Object last = tree.getLastSelectedPathComponent();
            if (last instanceof ZkNode node) {
                loadContent(node);
            }
        });

        JBSplitter splitter = new JBSplitter(false, 0.32f);
        splitter.setFirstComponent(new JBScrollPane(tree));
        splitter.setSecondComponent(contentPane.getComponent());

        root.add(toolbar, BorderLayout.NORTH);
        root.add(splitter, BorderLayout.CENTER);
    }

    private void connect() {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Connecting to ZooKeeper", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    ZookeeperClientService.getInstance(project).connect();
                    SwingUtilities.invokeLater(() -> {
                        setConnected(true);
                        resetTree();
                        statusLabel.setText("Connected: " + ZookeeperSettings.getInstance(project).getConnectString()
                                + "  " + ZookeeperSettings.getInstance(project).normalizedPath());
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        setConnected(false);
                        statusLabel.setText("Connect failed: " + ex.getMessage());
                        contentPane.showText("", ex.getMessage());
                    });
                }
            }
        });
    }

    private void disconnect() {
        ZookeeperClientService.getInstance(project).disconnect();
        setConnected(false);
        resetTree();
        contentPane.showText("", "Disconnected.");
        statusLabel.setText("Disconnected");
    }

    private void resetTree() {
        String path = ZookeeperSettings.getInstance(project).normalizedPath();
        ZkNode rootNode = new ZkNode(ZookeeperSettings.displayName(path), path);
        if (ZookeeperClientService.getInstance(project).isConnected()) {
            rootNode.add(new PlaceholderNode());
        }
        treeModel.setRoot(rootNode);
        tree.collapsePath(new TreePath(rootNode.getPath()));
    }

    private void loadChildren(@NotNull ZkNode node) {
        if (node.loaded) {
            return;
        }
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                List<String> names = ZookeeperClientService.getInstance(project).listChildren(node.path);
                SwingUtilities.invokeLater(() -> applyChildren(node, names, null));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> applyChildren(node, List.of(), ex.getMessage()));
            }
        });
    }

    private void applyChildren(@NotNull ZkNode node, @NotNull List<String> names, @Nullable String error) {
        node.removeAllChildren();
        node.loaded = true;
        if (error != null) {
            node.add(new DefaultMutableTreeNode("(" + error + ")"));
        } else {
            for (String name : names) {
                String childPath = "/".equals(node.path) ? "/" + name : node.path + "/" + name;
                ZkNode child = new ZkNode(name, childPath);
                child.add(new PlaceholderNode());
                node.add(child);
            }
        }
        treeModel.reload(node);
    }

    private void loadContent(@NotNull ZkNode node) {
        if (!ZookeeperClientService.getInstance(project).isConnected()) {
            contentPane.showText(node.path, "Not connected.");
            return;
        }
        contentPane.showText(node.path, "Loading ...");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                byte[] data = ZookeeperClientService.getInstance(project).getData(node.path);
                SwingUtilities.invokeLater(() -> contentPane.showData(node.path, data));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> contentPane.showText(node.path, ex.getMessage()));
            }
        });
    }

    private void setConnected(boolean connected) {
        connectButton.setEnabled(!connected);
        disconnectButton.setEnabled(connected);
        tree.setEnabled(connected);
    }

    @Override
    public void dispose() {
        contentPane.dispose();
        ZookeeperClientService.getInstance(project).disconnect();
    }

    private static final class ZkNode extends DefaultMutableTreeNode {
        private final String path;
        private boolean loaded;

        private ZkNode(@NotNull String name, @NotNull String path) {
            super(name);
            this.path = path;
        }
    }

    private static final class PlaceholderNode extends DefaultMutableTreeNode {
        private PlaceholderNode() {
            super("...");
        }
    }
}
