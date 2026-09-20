package com.gameale.massive.tools.cursor;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Opens a path or file:line:column in Cursor through the official CLI.
 * On Windows, focuses a healthy existing window; if Cursor is closing, waits
 * for that process to exit and opens the project folder so workspace config loads.
 *
 * @author zhouzengfa
 * @date 2026/09/20
 */
public final class CursorCliLauncher {

    private static final Logger LOG = Logger.getInstance(CursorCliLauncher.class);

    private CursorCliLauncher() {
    }

    public static void openPath(@NotNull String absolutePath, @Nullable String workDirectory) throws IOException {
        switchToCursor(absolutePath, null, null, workDirectory);
    }

    public static void openPathInBackground(
            @NotNull Project project,
            @NotNull String absolutePath,
            @Nullable String workDirectory
    ) {
        runInBackground(project, () -> openPath(absolutePath, workDirectory));
    }

    public static void gotoLocation(
            @NotNull String absoluteFilePath,
            int line1Based,
            int column1Based,
            @Nullable String workDirectory
    ) throws IOException {
        switchToCursor(absoluteFilePath, line1Based, column1Based, workDirectory);
    }

    public static void gotoLocationInBackground(
            @NotNull Project project,
            @NotNull String absoluteFilePath,
            int line1Based,
            int column1Based,
            @Nullable String workDirectory
    ) {
        runInBackground(project, () -> gotoLocation(absoluteFilePath, line1Based, column1Based, workDirectory));
    }

    private static void runInBackground(@NotNull Project project, @NotNull SwitchWork work) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                work.run();
            } catch (IOException ex) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog(project, ex.getMessage(), "Switch IDEA to Cursor"));
            }
        });
    }

    @FunctionalInterface
    private interface SwitchWork {
        void run() throws IOException;
    }

    private static void switchToCursor(
            @NotNull String absolutePath,
            @Nullable Integer line1Based,
            @Nullable Integer column1Based,
            @Nullable String workDirectory
    ) throws IOException {
        boolean reuseWindow = OpenInCursorSettings.getInstance().isReuseWindow();
        if (SystemInfo.isWindows) {
            boolean healthyWindow = WindowsCursorSwitcher.activateRunningWindow();
            if (!healthyWindow) {
                reuseWindow = false;
                WindowsCursorSwitcher.waitUntilCursorProcessesExit(8_000);
            }
        }
        launchCli(absolutePath, line1Based, column1Based, workDirectory, reuseWindow);
    }

    public static @NotNull String resolveExecutable() throws IOException {
        return resolveInstall().cliOrExe().toAbsolutePath().toString();
    }

    private static void launchCli(
            @NotNull String absolutePath,
            @Nullable Integer line1Based,
            @Nullable Integer column1Based,
            @Nullable String workDirectory,
            boolean reuseWindow
    ) throws IOException {
        CursorInstall install = resolveInstall();
        GeneralCommandLine command = new GeneralCommandLine();
        if (install.guiExe() != null && install.cliJs() != null && Files.isRegularFile(install.cliJs())) {
            command.setExePath(install.guiExe().toAbsolutePath().toString());
            command.withEnvironment("ELECTRON_RUN_AS_NODE", "1");
            command.withEnvironment("VSCODE_DEV", "");
            command.addParameter(install.cliJs().toAbsolutePath().toString());
        } else {
            command.setExePath(install.cliOrExe().toAbsolutePath().toString());
        }
        if (workDirectory != null && !workDirectory.isBlank()) {
            command.setWorkDirectory(workDirectory);
        }
        if (reuseWindow) {
            command.addParameter("--reuse-window");
        }
        if (line1Based != null) {
            addWorkspaceFolder(command, absolutePath, workDirectory);
            String target = absolutePath + ":" + line1Based + ":" + (column1Based == null ? 1 : column1Based);
            command.addParameters("-g", target);
        } else {
            command.addParameter(absolutePath);
        }
        LOG.info("Launching Cursor CLI: " + command.getCommandLineString());
        try {
            command.createProcess();
        } catch (ExecutionException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static void addWorkspaceFolder(
            @NotNull GeneralCommandLine command,
            @NotNull String absolutePath,
            @Nullable String workDirectory
    ) {
        if (workDirectory == null || workDirectory.isBlank()) {
            return;
        }
        Path folder = Path.of(workDirectory).toAbsolutePath().normalize();
        Path file = Path.of(absolutePath).toAbsolutePath().normalize();
        if (!folder.equals(file)) {
            command.addParameter(folder.toString());
        }
    }

    private static @NotNull CursorInstall resolveInstall() throws IOException {
        OpenInCursorSettings settings = OpenInCursorSettings.getInstance();
        String configured = settings.getCursorExecutable().trim();
        if (!configured.isEmpty()) {
            Path path = Path.of(configured);
            if (!Files.isRegularFile(path)) {
                throw new IOException("Cursor executable not found: " + configured);
            }
            return inspect(path);
        }
        for (String candidate : defaultCandidates()) {
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                return inspect(path);
            }
        }
        if (SystemInfo.isWindows) {
            return new CursorInstall(null, null, Path.of("cursor.cmd"));
        }
        return new CursorInstall(null, null, Path.of("cursor"));
    }

    private static @NotNull CursorInstall inspect(@NotNull Path path) {
        Path absolute = path.toAbsolutePath();
        String name = absolute.getFileName().toString().toLowerCase(Locale.ROOT);
        if ("cursor.exe".equals(name)) {
            Path cliJs = absolute.getParent() == null
                    ? null
                    : absolute.getParent().resolve("resources").resolve("app").resolve("out").resolve("cli.js");
            return new CursorInstall(absolute, cliJs, absolute);
        }
        if ("cursor.cmd".equals(name) || "cursor".equals(name) || "code.cmd".equals(name)) {
            Path binDir = absolute.getParent();
            Path appDir = binDir == null ? null : binDir.getParent();
            Path resourcesDir = appDir == null ? null : appDir.getParent();
            Path installDir = resourcesDir == null ? null : resourcesDir.getParent();
            Path guiExe = installDir == null ? null : installDir.resolve(SystemInfo.isWindows ? "Cursor.exe" : "Cursor");
            if (guiExe != null && !Files.isRegularFile(guiExe) && installDir != null) {
                Path macExe = installDir.resolve("MacOS").resolve("Cursor");
                if (Files.isRegularFile(macExe)) {
                    guiExe = macExe;
                }
            }
            Path cliJs = appDir == null ? null : appDir.resolve("out").resolve("cli.js");
            Path fallback = guiExe != null && Files.isRegularFile(guiExe) ? guiExe : absolute;
            return new CursorInstall(
                    guiExe != null && Files.isRegularFile(guiExe) ? guiExe : null,
                    cliJs,
                    fallback
            );
        }
        return new CursorInstall(null, null, absolute);
    }

    private static @NotNull List<String> defaultCandidates() {
        List<String> candidates = new ArrayList<>();
        if (SystemInfo.isWindows) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                candidates.add(programFiles + "\\cursor\\cursor\\resources\\app\\bin\\cursor.cmd");
                candidates.add(programFiles + "\\cursor\\cursor\\Cursor.exe");
                candidates.add(programFiles + "\\cursor\\resources\\app\\bin\\cursor.cmd");
                candidates.add(programFiles + "\\cursor\\Cursor.exe");
                candidates.add(programFiles + "\\Cursor\\resources\\app\\bin\\cursor.cmd");
                candidates.add(programFiles + "\\Cursor\\Cursor.exe");
            }
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                candidates.add(localAppData + "\\Programs\\cursor\\resources\\app\\bin\\cursor.cmd");
                candidates.add(localAppData + "\\Programs\\cursor\\Cursor.exe");
                candidates.add(localAppData + "\\Programs\\Cursor\\resources\\app\\bin\\cursor.cmd");
                candidates.add(localAppData + "\\Programs\\Cursor\\Cursor.exe");
            }
        } else if (SystemInfo.isMac) {
            candidates.add("/Applications/Cursor.app/Contents/Resources/app/bin/cursor");
            candidates.add("/Applications/Cursor.app/Contents/MacOS/Cursor");
            candidates.add("/usr/local/bin/cursor");
        } else {
            candidates.add("/usr/local/bin/cursor");
            candidates.add("/usr/bin/cursor");
        }
        return candidates;
    }

    private record CursorInstall(@Nullable Path guiExe, @Nullable Path cliJs, @NotNull Path cliOrExe) {
    }
}
