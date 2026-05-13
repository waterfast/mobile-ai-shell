package dev.vibecode.mobileagent.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.vibecode.mobileagent.agent.AgentBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val agentBridge: AgentBridge,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun onInputChange(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun onSend() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isStreaming) return

        val userMessage = ChatMessage(role = ChatRole.User, content = text)
        val assistantShell = ChatMessage(role = ChatRole.Assistant, content = "")

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + assistantShell,
                input = "",
                isStreaming = true,
                error = null,
            )
        }

        viewModelScope.launch {
            try {
                agentBridge.streamAssistantResponse(text).collect { chunk ->
                    _uiState.update { state ->
                        val msgs = state.messages.toMutableList()
                        if (msgs.isEmpty()) return@update state
                        val last = msgs.last()
                        if (last.role != ChatRole.Assistant) return@update state
                        msgs[msgs.lastIndex] = last.copy(content = last.content + chunk)
                        state.copy(messages = msgs)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "未知错误") }
            } finally {
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    fun onNewSession() {
        if (_uiState.value.isStreaming) return
        _uiState.value = ChatUiState()
    }

    fun onErrorConsumed() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        fun factory(agentBridge: AgentBridge): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                        return ChatViewModel(agentBridge) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
    }
}
