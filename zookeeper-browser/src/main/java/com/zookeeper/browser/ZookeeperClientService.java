package com.zookeeper.browser;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.apache.zookeeper.ZooKeeper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Read-only ZooKeeper session for the current project.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
public final class ZookeeperClientService implements Disposable {

    private final Project project;
    private volatile ZooKeeper zooKeeper;

    public ZookeeperClientService(@NotNull Project project) {
        this.project = project;
    }

    public static ZookeeperClientService getInstance(@NotNull Project project) {
        return project.getService(ZookeeperClientService.class);
    }

    public boolean isConnected() {
        ZooKeeper zk = zooKeeper;
        return zk != null && zk.getState().isAlive();
    }

    public synchronized void connect() throws IOException, InterruptedException {
        disconnect();
        ZookeeperSettings settings = ZookeeperSettings.getInstance(project);
        zooKeeper = openSession(settings.getConnectString(), settings.getSessionTimeoutMs());
    }

    /**
     * Tries the given connect string and closes the session. Does not change the browser session.
     */
    public static void testConnection(@NotNull String connectString, int sessionTimeoutMs)
            throws IOException, InterruptedException {
        ZooKeeper zk = openSession(connectString, sessionTimeoutMs);
        closeQuietly(zk);
    }

    private static @NotNull ZooKeeper openSession(@NotNull String connectString, int sessionTimeoutMs)
            throws IOException, InterruptedException {
        String trimmed = connectString.trim();
        if (trimmed.isEmpty()) {
            throw new IOException("Connect string is empty.");
        }
        if (sessionTimeoutMs <= 0) {
            throw new IOException("Session timeout must be a positive number.");
        }
        CountDownLatch connected = new CountDownLatch(1);
        String[] failReason = new String[1];
        ZooKeeper zk = new ZooKeeper(trimmed, sessionTimeoutMs, event -> {
            org.apache.zookeeper.Watcher.Event.KeeperState state = event.getState();
            if (state == org.apache.zookeeper.Watcher.Event.KeeperState.SyncConnected) {
                connected.countDown();
                return;
            }
            if (state == org.apache.zookeeper.Watcher.Event.KeeperState.AuthFailed) {
                failReason[0] = "Authentication failed.";
                connected.countDown();
                return;
            }
            if (state == org.apache.zookeeper.Watcher.Event.KeeperState.Closed
                    || state == org.apache.zookeeper.Watcher.Event.KeeperState.Expired) {
                failReason[0] = "Session " + state + ".";
                connected.countDown();
            }
        });
        boolean signaled = connected.await(sessionTimeoutMs, TimeUnit.MILLISECONDS);
        if (failReason[0] != null) {
            closeQuietly(zk);
            throw new IOException(failReason[0]);
        }
        if (!signaled || !zk.getState().isConnected()) {
            closeQuietly(zk);
            throw new IOException("Cannot connect to " + trimmed + ". Check host, port, and that ZooKeeper is running.");
        }
        return zk;
    }

    public synchronized void disconnect() {
        ZooKeeper zk = zooKeeper;
        zooKeeper = null;
        closeQuietly(zk);
    }

    public @NotNull List<String> listChildren(@NotNull String path) throws Exception {
        ZooKeeper zk = requireOpen();
        List<String> children = zk.getChildren(path, false);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        Collections.sort(children);
        return children;
    }

    public @Nullable byte[] getData(@NotNull String path) throws Exception {
        ZooKeeper zk = requireOpen();
        return zk.getData(path, false, null);
    }

    public static @NotNull String formatData(@Nullable byte[] data) {
        if (data == null || data.length == 0) {
            return "(empty)";
        }
        if (!looksLikeText(data)) {
            return "(binary data, " + data.length + " bytes)";
        }
        String text = new String(data, StandardCharsets.UTF_8);
        return JsonPretty.expand(text);
    }

    private @NotNull ZooKeeper requireOpen() throws IOException {
        ZooKeeper zk = zooKeeper;
        if (zk == null || !zk.getState().isAlive()) {
            throw new IOException("Not connected.");
        }
        return zk;
    }

    private static void closeQuietly(@Nullable ZooKeeper zk) {
        if (zk == null) {
            return;
        }
        try {
            zk.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean looksLikeText(@NotNull byte[] data) {
        int checked = Math.min(data.length, 512);
        int control = 0;
        for (int i = 0; i < checked; i++) {
            int b = data[i] & 0xFF;
            if (b == 0) {
                return false;
            }
            if (b < 0x09 || (b > 0x0D && b < 0x20)) {
                control++;
            }
        }
        return control * 10 < checked;
    }

    @Override
    public void dispose() {
        disconnect();
    }
}
