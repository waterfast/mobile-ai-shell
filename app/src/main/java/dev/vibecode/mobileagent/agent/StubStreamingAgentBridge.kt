package dev.vibecode.mobileagent.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Deterministic fake streaming to validate the chat pipeline without network or subprocesses.
 *
 * TODO: Replace with [TermuxAgentBridge] or similar once a controlled shell/runtime is wired.
 */
class StubStreamingAgentBridge : AgentBridge {
    override fun streamAssistantResponse(userMessage: String): Flow<String> = flow {
        val body =
            buildString {
                appendLine("（Stub）已收到你的消息：")
                appendLine(userMessage)
                appendLine()
                appendLine("这是本地模拟的分块流式输出，用于验证 UI。")
                appendLine("后续可在此接入：HTTP 兼容 API、Termux 子进程、或嵌入式运行时。")
            }
        body.chunked(10).forEach { chunk ->
            emit(chunk)
            delay(45L)
        }
    }
}
