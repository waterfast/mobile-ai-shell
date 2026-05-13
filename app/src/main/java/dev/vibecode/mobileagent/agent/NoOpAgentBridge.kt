package dev.vibecode.mobileagent.agent

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Drops all traffic; useful for UI tests or when the agent is intentionally disabled. */
class NoOpAgentBridge : AgentBridge {
    override fun streamAssistantResponse(userMessage: String): Flow<String> = emptyFlow()
}
