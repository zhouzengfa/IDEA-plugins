package com.zookeeper.browser;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Project-level ZooKeeper connect string and session timeout.
 *
 * @author zhouzengfa
 * @date 2026/09/19
 */
@State(name = "ZookeeperBrowserSettings", storages = @Storage("zookeeperBrowser.xml"))
public final class ZookeeperSettings implements PersistentStateComponent<ZookeeperSettings.State> {

    public static final class State {
        public String connectString = "127.0.0.1:2181";
        public String path = "";
        public int sessionTimeoutMs = 10_000;
    }

    private final State state = new State();

    public static ZookeeperSettings getInstance(@NotNull Project project) {
        return project.getService(ZookeeperSettings.class);
    }

    public @NotNull String getConnectString() {
        return state.connectString;
    }

    public void setConnectString(@NotNull String connectString) {
        state.connectString = connectString;
    }

    public @NotNull String getPath() {
        return state.path == null ? "" : state.path;
    }

    public void setPath(@NotNull String path) {
        state.path = path;
    }

    /**
     * Empty path means all nodes from {@code /}.
     */
    public @NotNull String normalizedPath() {
        return normalizePath(getPath());
    }

    public static @NotNull String normalizePath(@NotNull String raw) {
        String path = raw.trim();
        if (path.isEmpty() || "/".equals(path)) {
            return "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public static @NotNull String displayName(@NotNull String path) {
        if ("/".equals(path)) {
            return "/";
        }
        int slash = path.lastIndexOf('/');
        return path.substring(slash + 1);
    }

    public int getSessionTimeoutMs() {
        return state.sessionTimeoutMs;
    }

    public void setSessionTimeoutMs(int sessionTimeoutMs) {
        state.sessionTimeoutMs = sessionTimeoutMs;
    }

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
}
