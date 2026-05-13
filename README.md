# Mobile AI Shell

面向学生的「聊天优先」移动端 AI 编程伴侣壳应用（Kotlin + Jetpack Compose + Material 3）。本仓库集成了上游 **[OpenClaude](https://github.com/Gitlawb/openclaude)** 源码树（见 `third_party/openclaude/`），并通过 **Termux 官方 `RUN_COMMAND` Intent** 在设备上诚实地调用其 CLI（`-p` / `--print` 非交互模式）。Stock Android 本身无法直接运行 Node/OpenClaude，因此 **必须** 依赖 Termux（或同类用户态）提供运行时。

## OpenClaude 与 Termux 集成说明

- **源码位置**：`third_party/openclaude/`（当前为 **vendor 快照**，非 git 子模块；原因与如何改为子模块见 `third_party/OPENCLAUDE_SOURCE.md`）。
- **许可证**：OpenClaude 使用 MIT，见 `third_party/openclaude/LICENSE`。
- **Android 上安装 OpenClaude**：请优先阅读上游 **`third_party/openclaude/ANDROID_INSTALL.md`**（Termux + proot Ubuntu + Bun/Node 构建 `dist/cli.mjs`）。本应用默认在 Termux 中执行：
  - 工作目录：`/data/data/com.termux/files/home/openclaude`
  - 命令：`/data/data/com.termux/files/usr/bin/node dist/cli.mjs -p "<你的聊天内容>"`
  - 可在 `TermuxOpenClaudeBridge.kt` 的 `TermuxOpenClaudeConfig` 中调整路径（若你把仓库 clone 到其他目录）。
- **Termux RUN_COMMAND 前置条件**（摘自 [Termux Wiki](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)）：
  1. 安装官方 Termux（包名 `com.termux`，建议 F-Droid 构建）。
  2. 系统 **应用信息 → 权限 → 其他权限**：授予本应用 **「在 Termux 环境中运行命令」**（`com.termux.permission.RUN_COMMAND`）。
  3. Termux 内 `~/.termux/termux.properties` 设置 **`allow-external-apps=true`**。
  4. 建议 Termux **≥ 0.109**，以便通过 `PendingIntent` 将 stdout/stderr 回传给本应用。
  5. Android 10+ 上若前台会话被系统拦截，可能需为 Termux 开启「在其他应用上层显示」等权限（见 Wiki）。

应用内 **设置 → Agent 后端** 可选择：

| 选项 | 行为 |
|------|------|
| **Stub（本地模拟流）** | 不调用 Termux，用于 UI 与管道验证。 |
| **OpenClaude（Termux RUN_COMMAND）** | 通过 `TermuxOpenClaudeBridge` 向 Termux 发送 `RUN_COMMAND`，在 Termux 中执行 `node dist/cli.mjs -p …` 并流式展示返回文本（受 Termux 单次回调约 100KB 截断限制）。 |

## 架构概览

- **UI**：Jetpack Compose（Material 3），`MainActivity` 启动后进入 `MobileNavHost`（`chat` / `settings`）。
- **状态**：`ChatViewModel`、`SettingsViewModel` 持有 `UiState`（单向数据流）；Compose 侧 `collectAsState()`。
- **Agent 抽象**：`AgentBridge` 将用户消息映射为 `Flow<String>` 分块流。
  - `StubStreamingAgentBridge`：本地协程模拟流式输出。
  - `TermuxOpenClaudeBridge`：Termux `RUN_COMMAND` + `PendingIntent` 取回结果。
  - `NoOpAgentBridge`：空流（占位 / 测试）。

## 在 Android Studio 中打开

1. 安装 [Android Studio](https://developer.android.com/studio)（建议最新稳定版）与 **JDK 17**。
2. **File → Open**，选择本目录。
3. 首次同步 Gradle；若命令行构建，请在项目根创建 `local.properties`（Android Studio 通常会自动生成）：

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

4. 运行 `app` 配置到模拟器或真机（minSdk 26，targetSdk 34）。

命令行调试构建（需已配置 SDK 与网络拉依赖）：

```bash
./gradlew :app:assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`。

**环境要求**：需安装 **JDK 17**（Gradle / AGP 8.7 需要）；若 `java -version` 报错，请在 macOS 上安装 Temurin 17 或 Android Studio 自带的 JBR，并确保 `JAVA_HOME` 指向该 JDK。另需 Android SDK：将 `sdk.dir=...` 写入 `local.properties`。

## GitHub Actions

- 向 `main` 或 `master` 分支 **push**，或针对这两个分支发起 **pull_request** 时，会运行工作流 [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)：在 Ubuntu 上使用 Temurin 17、安装 Android SDK（与 `compileSdk` 34 对齐），执行 `./gradlew :app:assembleDebug --stacktrace`。
- 构建成功后，在 GitHub 仓库页打开 **Actions** → 选择对应工作流运行 → **Artifacts** 中下载 **`debug-apk`**（内含 debug APK），保留 **14 天**。
- **Debug 构建不需要** 在仓库中配置 Secrets（不涉及发布签名或密钥上传）。

若 CI 报 Gradle Wrapper 相关错误，可在本地执行 `./gradlew --version` 校验；确保 `gradle/wrapper/gradle-wrapper.jar` 为完整二进制且未被错误地以文本方式提交或过滤。

## Git 子模块（可选）

若你本地 Git 环境正常，可删除 vendor 目录后改用语义更清晰的子模块（需自行处理网络与权限）：

```bash
git submodule add https://github.com/Gitlawb/openclaude.git third_party/openclaude
git submodule update --init --recursive
```

初始化克隆：

```bash
git submodule update --init --recursive
```

## API Key 与安全声明

- API Key 仅写入本机 **加密 SharedPreferences**，不会在本 MVP 中自动上传到任何服务器。
- Termux 集成会在用户显式选择后端且授予权限后，由 **Termux** 在其隔离环境中执行命令；请勿在不可信目录下使用 `-p` 自动执行不受信任内容。

## Roadmap

| 阶段 | 内容 |
|------|------|
| **Phase 1（当前）** | 聊天 UI、Stub 流、`TermuxOpenClaudeBridge`、`AgentBridge` 扩展点、加密设置页、导航骨架。 |
| **Phase 2** | 可配置 node/cli/workdir UI、结果文件落盘（`EXTRA_RESULT_DIRECTORY`）、错误遥测与重试。 |
| **Phase 3** | 真实 HTTP Agent：OpenAI 兼容与多提供商、流式解析、错误重试。 |
| **Phase 4** | Git 向导：分支、提交、diff 预览（只读为主）、与聊天上下文的轻量联动。 |

## 许可与第三方

- OpenClaude：`third_party/openclaude/LICENSE`（MIT）。
- 其他依赖见各 Maven 坐标与上游许可证；应用图标当前使用系统占位 drawable，发布前请替换为自有品牌资源。
