package dev.vibecode.mobileagent.ui.chat

enum class ChatRole {
    User,
    Assistant,
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
)
