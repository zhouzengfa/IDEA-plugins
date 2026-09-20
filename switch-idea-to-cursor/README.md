# switch-idea-to-cursor

独立的 IntelliJ IDEA 插件工程：从 IDEA 一键切到 Cursor，并打开当前文件（含光标行/列）或工程根目录。

本仓库与 `pixel_server` 无关，可单独 clone、构建、安装。

## Introduction

- Jump from IntelliJ IDEA to Cursor in one action.
- Opens the current file at the same caret line and column, or opens the project root.
- Uses the official Cursor CLI and reuses an existing Cursor window on Windows.

## Features

- Editor right-click **Switch to Cursor** (also on the gutter popup).
- **Tools | Switch to Cursor**, or shortcut **Alt+Shift+O**.
- **Tools | Switch Project to Cursor** opens the project root in Cursor.
- Configure the `Cursor.exe` path and reuse-window behavior under **Settings | Tools | Switch IDEA to Cursor**.
- Only local filesystem files are supported (not files inside jars or remote VFS).

## 前置条件

1. 本机已安装 [Cursor](https://cursor.com/)。
2. Windows 上插件会先把已打开的 Cursor 窗口拉到前台，再用官方 CLI（`ELECTRON_RUN_AS_NODE` + `cli.js`）通知已有进程打开文件。不要用 `cursor://`，那个会再拉起一个实例并弹出确认框。

验证 CLI（PowerShell）：

```powershell
$env:ELECTRON_RUN_AS_NODE=1
& "C:\Program Files\cursor\cursor\Cursor.exe" "C:\Program Files\cursor\cursor\resources\app\out\cli.js" --reuse-window -g "D:\work\switch-idea-to-cursor\README.md:1:1"
```

## 功能

| 入口 | 行为 |
|------|------|
| 编辑器右键 **Switch to Cursor** | 激活已有窗口 + CLI `-g 路径:行:列` |
| **Tools → Switch to Cursor** | 同上 |
| 快捷键 **Alt+Shift+O** | 同上 |
| **Tools → Switch Project to Cursor** | 激活已有窗口 + CLI 打开工程根目录 |

设置：**Settings → Tools → Switch IDEA to Cursor**（可配置 `Cursor.exe` 路径、是否 `--reuse-window`）。

## 构建与安装

```powershell
cd D:\work\switch-idea-to-cursor
.\gradlew.bat buildPlugin
```

产物：`build\distributions\switch-idea-to-cursor-1.0.6.zip`

在 IDEA：**Settings → Plugins → ⚙ → Install Plugin from Disk…** 选择上述 zip，重启 IDE。

## 本地调试

```powershell
.\gradlew.bat runIde
```

会启动带本插件的沙箱 IDEA。

## 说明

- 行/列与 Cursor CLI 一致，为 **1-based**（与 IDEA caret 逻辑位置对齐）。
- 仅支持本地文件系统上的文件（非 jar 内 / 远程虚拟文件）。
- Windows 路径含括号等特殊字符时，若启动失败，可在设置中指定稳定的 `Cursor.exe` 路径。
