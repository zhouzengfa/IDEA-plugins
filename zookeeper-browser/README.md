# ZooKeeper Browser

只读 IntelliJ 插件：配置 ZooKeeper 连接串，左侧浏览节点树，右侧展开显示 JSON。

Plugin id：`com.zookeeper.browser`

## Introduction

- A lightweight, read-only ZooKeeper browser for IntelliJ IDEA.
- Configure a connect string, browse the node tree, and view node data as pretty-printed JSON.
- If you find a bug or have an idea, please open an issue.

## Features

- Configure the connection under **Settings | Tools | ZooKeeper Browser** (connect string, root path, session timeout).
- Use **Test** on the settings page to verify the connection.
- Open the tool window from the toolbar, **Tools | ZooKeeper Browser**, or **View | Tool Windows | ZooKeeper Browser**.
- Click **Connect** / refresh to load the ZooKeeper node tree.
- Expand tree nodes to browse children; click a node to show foldable JSON on the right.
- This version is read-only: no authentication UI, and no create / delete / ACL edits.

## 设置

**Settings → Tools → ZooKeeper Browser**

- **Connect string**：`127.0.0.1:2181` 或 `host1:2181,host2:2181,host3:2181`
- **Path**：例如 `/zhouzengfa`，只看该节点及其子节点；留空则从 `/` 看全部
- **Session timeout (ms)**：默认 `10000`
- 连接串右侧 **Test**：弹出对话框。成功显示 Successfully connected!；失败显示原因。

按工程保存。

## 使用

顶部工具栏或 **Tools → ZooKeeper Browser** 打开工具窗口；也可 **View → Tool Windows → ZooKeeper Browser**。

1. Connect
2. 左栏展开节点看 children
3. 单击节点，右栏显示带颜色的缩进 JSON；可折叠节点前方有折叠标记，点一下收起，再点展开

第一版只读，无认证、不改数据。

## 构建

```powershell
cd E:\plan\IDEA-plugins\zookeeper-browser
.\gradlew.bat buildPlugin
```

产物：`build\distributions\zookeeper-browser-1.0.0.zip`

IDEA：**Settings → Plugins → ⚙ → Install Plugin from Disk…**
