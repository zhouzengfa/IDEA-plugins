package com.gameale.massive.tools.cursor;

import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Brings an already-running Cursor window to the foreground on Windows.
 * Avoids waiting for a new Cursor.exe / Electron process to steal focus.
 *
 * @author zhouzengfa
 * @date 2026/09/20
 */
final class WindowsCursorSwitcher {

    private static final Logger LOG = Logger.getInstance(WindowsCursorSwitcher.class);
    private static final int SW_RESTORE = 9;
    private static final int ASFW_ANY = -1;
    private static final int WM_NULL = 0;
    private static final int SMTO_ABORTIFHUNG = 0x0002;
    private static final int RESPONSIVE_TIMEOUT_MS = 300;
    private static final long SHUTDOWN_POLL_MS = 150;
    private static final long IPC_SETTLE_MS = 200;

    private WindowsCursorSwitcher() {
    }

    /**
     * Focuses a healthy Cursor window. Returns false if none exists or it is closing.
     */
    static boolean activateRunningWindow() {
        try {
            Pointer hwnd = findVisibleCursorWindow();
            if (hwnd == null || !isResponsive(hwnd)) {
                return false;
            }
            focusWindow(hwnd);
            return true;
        } catch (Throwable t) {
            LOG.warn("Failed to activate existing Cursor window", t);
            return false;
        }
    }

    /**
     * Waits until leftover Cursor.exe processes exit so CLI does not talk to a dying instance.
     * No-op if Cursor is not running. Must run off the EDT.
     */
    static void waitUntilCursorProcessesExit(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        boolean sawProcess = false;
        while (System.currentTimeMillis() < deadline) {
            if (!isCursorProcessRunning()) {
                if (sawProcess) {
                    sleepQuietly(IPC_SETTLE_MS);
                }
                return;
            }
            sawProcess = true;
            sleepQuietly(SHUTDOWN_POLL_MS);
        }
        LOG.warn("Timed out waiting for Cursor.exe to exit");
    }

    private static @Nullable Pointer findVisibleCursorWindow() {
        Pointer[] found = new Pointer[1];
        User32Lib.WndEnumProc callback = (hWnd, lParam) -> {
            if (!User32Lib.INSTANCE.IsWindowVisible(hWnd)) {
                return true;
            }
            char[] title = new char[512];
            int length = User32Lib.INSTANCE.GetWindowTextW(hWnd, title, title.length);
            if (length <= 0) {
                return true;
            }
            String text = new String(title, 0, length);
            if (text.contains(" - Cursor") || text.endsWith("Cursor")) {
                found[0] = hWnd;
                return false;
            }
            return true;
        };
        User32Lib.INSTANCE.EnumWindows(callback, null);
        return found[0];
    }

    private static boolean isResponsive(@NotNull Pointer hwnd) {
        int[] result = new int[1];
        int sent = User32Lib.INSTANCE.SendMessageTimeoutW(
                hwnd, WM_NULL, 0, 0, SMTO_ABORTIFHUNG, RESPONSIVE_TIMEOUT_MS, result);
        return sent != 0;
    }

    private static boolean isCursorProcessRunning() {
        for (ProcessInfo info : OSProcessUtil.getProcessList()) {
            String name = info.getExecutableName();
            if (name != null && name.equalsIgnoreCase("Cursor.exe")) {
                return true;
            }
        }
        return false;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void focusWindow(@NotNull Pointer hwnd) {
        if (User32Lib.INSTANCE.IsIconic(hwnd)) {
            User32Lib.INSTANCE.ShowWindow(hwnd, SW_RESTORE);
        }
        User32Lib.INSTANCE.AllowSetForegroundWindow(ASFW_ANY);
        Pointer foreground = User32Lib.INSTANCE.GetForegroundWindow();
        int[] foregroundPid = new int[1];
        int[] targetPid = new int[1];
        int foregroundTid = foreground == null ? 0 : User32Lib.INSTANCE.GetWindowThreadProcessId(foreground, foregroundPid);
        int targetTid = User32Lib.INSTANCE.GetWindowThreadProcessId(hwnd, targetPid);
        if (foregroundTid != 0 && targetTid != 0 && foregroundTid != targetTid) {
            User32Lib.INSTANCE.AttachThreadInput(foregroundTid, targetTid, true);
        }
        User32Lib.INSTANCE.BringWindowToTop(hwnd);
        User32Lib.INSTANCE.SetForegroundWindow(hwnd);
        if (foregroundTid != 0 && targetTid != 0 && foregroundTid != targetTid) {
            User32Lib.INSTANCE.AttachThreadInput(foregroundTid, targetTid, false);
        }
    }

    private interface User32Lib extends StdCallLibrary {
        User32Lib INSTANCE = Native.load("user32", User32Lib.class, W32APIOptions.DEFAULT_OPTIONS);

        interface WndEnumProc extends StdCallCallback {
            boolean callback(Pointer hWnd, Pointer lParam);
        }

        boolean EnumWindows(WndEnumProc proc, Pointer lParam);

        boolean IsWindowVisible(Pointer hWnd);

        boolean IsIconic(Pointer hWnd);

        boolean ShowWindow(Pointer hWnd, int nCmdShow);

        boolean SetForegroundWindow(Pointer hWnd);

        boolean BringWindowToTop(Pointer hWnd);

        boolean AllowSetForegroundWindow(int dwProcessId);

        boolean AttachThreadInput(int idAttach, int idAttachTo, boolean fAttach);

        Pointer GetForegroundWindow();

        int GetWindowThreadProcessId(Pointer hWnd, int[] lpdwProcessId);

        int GetWindowTextW(Pointer hWnd, char[] lpString, int nMaxCount);

        int SendMessageTimeoutW(
                Pointer hWnd,
                int msg,
                int wParam,
                int lParam,
                int fuFlags,
                int uTimeout,
                int[] lpdwResult
        );
    }
}
