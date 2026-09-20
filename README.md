# IDEA-plugins

个人 IntelliJ IDEA 插件集合。每个子目录是独立 Gradle 工程，可单独构建、从磁盘安装。

| 插件 | 目录 | Plugin id | 作用 |
|------|------|-----------|------|
| [ZooKeeper Browser](#zookeeper-browser) | `zookeeper-browser/` | `com.zookeeper.browser` | 只读浏览 ZooKeeper 节点与 JSON |
| [Switch IDEA to Cursor](#switch-idea-to-cursor) | `switch-idea-to-cursor/` | `com.gameale.massive.switch-idea-to-cursor` | 从 IDEA 跳到 Cursor 并定位文件行 |

安装通用步骤：`.\gradlew.bat buildPlugin` → **Settings → Plugins → ⚙ → Install Plugin from Disk…** → 选 zip → 重启 IDEA。`since-build` 为 `233`（2023.3+），**不限制**更高 IDEA 版本。

---

## ZooKeeper Browser

只读 ZooKeeper 浏览器：配置连接，左栏节点树，右栏带颜色、可折叠的 JSON。

当前打包版本：`1.0.11`  
产物：`zookeeper-browser/build/distributions/zookeeper-browser-1.0.11.zip`

### 入口

- 顶部工具栏 **ZooKeeper Browser**
- **Tools → ZooKeeper Browser**
- **View → Tool Windows → ZooKeeper Browser**

### 配置（Settings → Tools → ZooKeeper Browser）

按 **工程** 保存。

| 项 | 说明 |
|----|------|
| **Connect string** | 单机或多机：`10.30.122.224:2181` 或 `host1:2181,host2:2181` |
| **Path** | 例如 `/zhouzengfa`：左栏只显示该节点及其 children。留空或 `/`：从根看全部节点 |
| **Session timeout (ms)** | 默认 `10000` |
| **Test**（连接串右侧） | 真正去连 ZooKeeper（并校验 Path 是否存在）。成功弹窗：标题 `Connection to <host>`，内容 `Successfully connected!`（信息图标）。失败同样弹窗，显示原因 |

Path 只在配置页填写，工具窗口不再放 Path 输入框。Connect 使用配置里保存的 Path。

### 工具窗口

| 区域 | 功能 |
|------|------|
| 顶部 | **Connect** / **Disconnect**，状态栏显示连接串和当前 Path |
| **左栏** | 节点树。根节点为配置的 Path（或 `/`）。展开时懒加载 children |
| **右栏** | 选中节点的 data：缩进 pretty-print JSON，语法着色（key / 字符串 / 数字 / true·false·null） |
| 折叠 | 可折叠对象、数组前方有折叠标记。点一下收起（`{...}` / `[...]`），再点展开。无额外 Expand/Fold 按钮 |
| 非 JSON | 按 UTF-8 原文显示；空数据 `(empty)`；二进制提示字节数 |

### 范围（第一版）

- 只读：不创建、不修改、不删除节点
- 无 Digest / SASL 认证
- 无 chroot 以外的多连接配置（每个 IDEA 工程一套设置）

### 构建

```powershell
cd E:\plan\IDEA-plugins\zookeeper-browser
.\gradlew.bat buildPlugin
```

---

## Switch IDEA to Cursor

从 IDEA 打开 Cursor，并跳到当前文件的光标行/列，或打开工程根目录。

产物：`switch-idea-to-cursor/build/distributions/switch-idea-to-cursor-1.0.1.zip`（以本地 `build.gradle.kts` 的 `version` 为准）

### 入口

| 入口 | 行为 |
|------|------|
| 编辑器右键 **Switch to Cursor** | 激活已有 Cursor 窗口，并 `cursor -g 路径:行:列` |
| **Tools → Switch to Cursor** | 同上 |
| **Alt+Shift+O** | 同上 |
| **Tools → Switch Project to Cursor** | 打开工程根目录 |

设置：**Settings → Tools → Switch IDEA to Cursor**（`Cursor.exe` / `cursor.cmd` 路径、是否 `--reuse-window`）。

### 说明

- 需本机安装 Cursor；Windows 上优先复用已有窗口，避免再起一个实例
- 行/列为 1-based；仅支持本地文件

### 构建

```powershell
cd E:\plan\IDEA-plugins\switch-idea-to-cursor
.\gradlew.bat buildPlugin
```
