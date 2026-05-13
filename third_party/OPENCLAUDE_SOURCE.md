# OpenClaude 第三方源码（Vendor）

本目录下的 `openclaude/` 来自上游仓库 **https://github.com/Gitlawb/openclaude**（`main` 分支源码归档）。

由于当前环境无法可靠初始化 `git submodule`（`.git` 钩子/隐藏目录权限限制），采用 **固定快照 vendor** 方式纳入，而非子模块。

- **许可证**：见 `openclaude/LICENSE`（MIT）。
- **更新方式**：重新下载 `https://github.com/Gitlawb/openclaude/archive/refs/heads/main.tar.gz` 并解压覆盖 `openclaude/`（若 `tar` 因 `.vscode` 等隐藏路径失败，可使用 `--exclude='*/.vscode'`），然后在本文档记录日期与可选的 `git rev-parse` 提交哈希。

若你本地可正常使用 Git，仍建议改为官方子模块：

```bash
git submodule add https://github.com/Gitlawb/openclaude.git third_party/openclaude
git submodule update --init --recursive
```
