package dev.vibecode.mobileagent.agent

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 通过 Termux 官方 [RUN_COMMAND](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent) 在 Termux 用户空间执行 OpenClaude 的 **非交互** 调用（`-p` / `--print`）。
 *
 * 需要：已安装 Termux（`com.termux`）、Termux ≥ 0.109（以便通过 `PendingIntent` 取回 stdout）、用户在系统设置中授予本应用
 * `com.termux.permission.RUN_COMMAND`，且在 Termux 内 `~/.termux/termux.properties` 设置 `allow-external-apps=true`。
 *
 * OpenClaude 本体为 Node CLI，需在 Termux 中按 `third_party/openclaude/ANDROID_INSTALL.md` 或上游 README 完成构建与 `dist/cli.mjs`。
 */
class TermuxOpenClaudeBridge(
    private val appContext: Context,
    private val config: TermuxOpenClaudeConfig = TermuxOpenClaudeConfig(),
) : AgentBridge {

    override fun streamAssistantResponse(userMessage: String): Flow<String> = callbackFlow {
        val pm = appContext.packageManager
        val termuxInstalled =
            try {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= 33) {
                    pm.getPackageInfo(TERMUX_PACKAGE, PackageManager.PackageInfoFlags.of(0))
                } else {
                    pm.getPackageInfo(TERMUX_PACKAGE, 0)
                }
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }

        if (!termuxInstalled) {
            trySend(
                "未安装 Termux（包名 $TERMUX_PACKAGE）。请从 F-Droid 安装官方 Termux，再按 README 配置 OpenClaude 与 RUN_COMMAND 权限。\n",
            )
            close()
            return@callbackFlow
        }

        val safeMessage = truncateForIntent(userMessage)
        val executionId = nextExecutionId.incrementAndGet()
        val resultAction = "${appContext.packageName}.TERMUX_OPENCLAUDE_RESULT_$executionId"

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var timeoutJob: Job? = null

        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    timeoutJob?.cancel()
                    val bundle = intent.getBundleExtra(EXTRA_PLUGIN_RESULT_BUNDLE)
                    if (bundle == null) {
                        trySend("Termux 返回结果缺少数据包（extra `$EXTRA_PLUGIN_RESULT_BUNDLE`）。\n")
                    } else {
                        deliverBundle(bundle)
                    }
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {
                    }
                    close()
                }

                private fun deliverBundle(bundle: Bundle) {
                    val err = bundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_ERR, 0)
                    val errmsg = bundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG).orEmpty()
                    if (err != android.app.Activity.RESULT_OK && errmsg.isNotBlank()) {
                        trySend("Termux 内部错误 ($err): $errmsg\n")
                    }
                    val stdout = bundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT).orEmpty()
                    val stderr = bundle.getString(EXTRA_PLUGIN_RESULT_BUNDLE_STDERR).orEmpty()
                    val exit = bundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE, -1)
                    val origOut = bundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH, stdout.length)
                    val origErr = bundle.getInt(EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH, stderr.length)
                    if (stdout.isNotEmpty()) {
                        chunkedEmit(stdout)
                    }
                    if (stderr.isNotEmpty()) {
                        trySend("\n--- stderr ---\n")
                        chunkedEmit(stderr)
                    }
                    if (origOut > stdout.length || origErr > stderr.length) {
                        trySend(
                            "\n（提示：stdout/stderr 可能被 Termux 截断至约 100KB，原始长度 out=$origOut err=$origErr）\n",
                        )
                    }
                    trySend("\n[exit code: $exit]\n")
                }

                private fun chunkedEmit(text: String) {
                    text.chunked(256).forEach { chunk -> trySend(chunk) }
                }
            }

        val filter = IntentFilter(resultAction)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }

        val pendingFlags =
            PendingIntent.FLAG_ONE_SHOT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_MUTABLE
                } else {
                    0
                }
        val pendingIntent =
            PendingIntent.getBroadcast(
                appContext,
                executionId,
                Intent(resultAction).setPackage(appContext.packageName),
                pendingFlags,
            )

        timeoutJob =
            scope.launch {
                delay(RESULT_TIMEOUT_MS)
                if (!isActive) return@launch
                trySend(
                    "\n[超时 ${RESULT_TIMEOUT_MS / 1000}s：未收到 Termux 回调。请确认 Termux ≥0.109、已授予「在 Termux 环境中运行命令」、allow-external-apps=true，且 node/cli 路径正确。]\n",
                )
                try {
                    appContext.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
                close()
            }

        val args =
            arrayOf(
                config.cliRelativePath,
                "-p",
                safeMessage,
            )

        val runIntent =
            Intent().apply {
                setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE_CLASS)
                action = ACTION_RUN_COMMAND
                putExtra(EXTRA_COMMAND_PATH, config.nodeBinaryPath)
                putExtra(EXTRA_ARGUMENTS, args)
                putExtra(EXTRA_WORKDIR, config.workDir)
                putExtra(EXTRA_BACKGROUND, true)
                putExtra(EXTRA_PENDING_INTENT, pendingIntent)
                putExtra(EXTRA_COMMAND_LABEL, "openclaude -p")
                putExtra(
                    EXTRA_COMMAND_DESCRIPTION,
                    "由 Mobile AI Shell 通过 RUN_COMMAND 调用 OpenClaude `--print` 模式。",
                )
            }

        try {
            appContext.startService(runIntent)
        } catch (e: Exception) {
            timeoutJob?.cancel()
            trySend("无法向 Termux 发送 RUN_COMMAND：${e.message ?: e.javaClass.simpleName}\n")
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
            close()
        }

        awaitClose {
            timeoutJob?.cancel()
            try {
                appContext.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    private fun truncateForIntent(message: String): String {
        if (message.length <= MAX_PROMPT_CHARS) return message
        return message.take(MAX_PROMPT_CHARS) +
            "\n…（消息过长已截断至 ${MAX_PROMPT_CHARS} 字符以适配 Intent 大小限制）"
    }

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val RUN_COMMAND_SERVICE_CLASS = "com.termux.app.RunCommandService"
        private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

        private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
        private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
        private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"

        private const val EXTRA_PLUGIN_RESULT_BUNDLE = "result"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"
        private const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"

        private const val MAX_PROMPT_CHARS = 80_000
        private const val RESULT_TIMEOUT_MS = 120_000L

        private val nextExecutionId = AtomicInteger((System.currentTimeMillis() and 0x7FFF_FFFF).toInt())
    }
}

data class TermuxOpenClaudeConfig(
    /** Termux 内 `node` 可执行文件绝对路径。 */
    val nodeBinaryPath: String = "/data/data/com.termux/files/usr/bin/node",
    /** OpenClaude 仓库根目录（含 `dist/cli.mjs`）。默认与 ANDROID_INSTALL.md 一致。 */
    val workDir: String = "/data/data/com.termux/files/home/openclaude",
    /** 相对于 [workDir] 的 CLI 路径。 */
    val cliRelativePath: String = "dist/cli.mjs",
)
