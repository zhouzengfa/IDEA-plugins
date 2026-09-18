package com.gameale.massive.tools.cursor;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Persists Cursor CLI path and launch options.
 *
 * @author zhouzengfa
 * @date 2026/03/20
 */
@State(name = "OpenInCursorSettings", storages = @Storage("openInCursor.xml"))
public final class OpenInCursorSettings implements PersistentStateComponent<OpenInCursorSettings.State> {

    public static final class State {
        public String cursorExecutable = "";
        public boolean reuseWindow = true;
    }

    private State state = new State();

    public static OpenInCursorSettings getInstance() {
        return ApplicationManager.getApplication().getService(OpenInCursorSettings.class);
    }

    public @NotNull String getCursorExecutable() {
        return state.cursorExecutable;
    }

    public void setCursorExecutable(@NotNull String cursorExecutable) {
        state.cursorExecutable = cursorExecutable;
    }

    public boolean isReuseWindow() {
        return state.reuseWindow;
    }

    public void setReuseWindow(boolean reuseWindow) {
        state.reuseWindow = reuseWindow;
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
