package com.gameale.massive.tools.cursor;

import com.intellij.execution.process.OSProcessUtil;
import com.intellij.execution.process.ProcessInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Brings an already-running Cursor window to the foreground on Windows.
 * Avoids waiting for a new Cursor.exe / Electron process to steal focus.
 *
 * @author zhouzengfa
 * @date 2026/03/20
 */
final class WindowsCursorSwitcher {

    private static final Logger LOG = Logger.getInstance(WindowsCursorSwitcher.class);
    private static final int SW_RESTORE = 9;
    private static final int ASFW_ANY = -1;

    private WindowsCursorSwitcher() {
    }

    static boolean activateRunningWindow() {
        try {
            Set<Integer> cursorPids = findCursorPids();
            if (cursorPids.isEmpty()) {
                return false;
            }
            Pointer[] found = new Pointer[1];
            User32Lib.WndEnumProc callback = (hWnd, lParam) -> {
                if (!User32Lib.INSTANCE.IsWindowVisible(hWnd)) {
                    return true;
                }
                int[] pid = new int[1];
                User32Lib.INSTANCE.GetWindowThreadProcessId(hWnd, pid);
                if (!cursorPids.contains(pid[0])) {
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
                if (found[0] == null) {
                    found[0] = hWnd;
                }
                return true;
            };
            User32Lib.INSTANCE.EnumWindows(callback, null);
            if (found[0] == null) {
                return false;
            }
            focusWindow(found[0]);
            return true;
        } catch (Throwable t) {
            LOG.warn("Failed to activate existing Cursor window", t);
            return false;
        }
    }

    private static @NotNull Set<Integer> findCursorPids() {
        Set<Integer> pids = new HashSet<>();
        for (ProcessInfo info : OSProcessUtil.getProcessList()) {
            String name = info.getExecutableName();
            if (name != null && name.equalsIgnoreCase("Cursor.exe")) {
                pids.add(info.getPid());
            }
        }
        return pids;
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
    }
}
