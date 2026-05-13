package dev.vibecode.mobileagent.agent

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over a coding agent / LLM backend.
 *
 * TODO: Add implementations that shell out to Termux, embed a local runtime, or call HTTP APIs.
 */
interface AgentBridge {
    /**
     * Streams assistant text chunks for the given user message (UTF-8).
     */
    fun streamAssistantResponse(userMessage: String): Flow<String>
}
