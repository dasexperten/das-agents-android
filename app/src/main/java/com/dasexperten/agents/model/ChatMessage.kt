package com.dasexperten.agents.model

enum class ChatRole {
    User,
    Assistant,
    System,
    Error,
    Typing,
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val agentSlug: String? = null,
    val agentName: String? = null,
    val initials: String? = null,
    /** Server or local epoch millis for merge/order. */
    val createdAtMs: Long = System.currentTimeMillis(),
)
