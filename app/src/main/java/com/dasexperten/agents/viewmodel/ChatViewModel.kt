package com.dasexperten.agents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dasexperten.agents.data.ChatRepository
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.model.ChatMessage
import com.dasexperten.agents.model.ChatRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val agents: List<Agent> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val loadingHistory: Boolean = true,
    val sending: Boolean = false,
    val pendingSlugs: Set<String> = emptySet(),
    /** Focus for initials strip (null = show all). Double-tap style: filter mode via [filterMode]. */
    val focusSlug: String? = null,
    val filterMode: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val agents: List<Agent>,
    private val repo: ChatRepository = ChatRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatUiState(
            agents = agents,
            focusSlug = agents.firstOrNull()?.slug,
        )
    )
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    init {
        reloadHistory()
    }

    fun reloadHistory() {
        viewModelScope.launch {
            _state.update { it.copy(loadingHistory = true, error = null) }
            runCatching { repo.loadMergedHistory(agents) }
                .onSuccess { msgs ->
                    _state.update { it.copy(loadingHistory = false, messages = msgs, error = null) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loadingHistory = false,
                            error = e.message ?: "История не загрузилась",
                        )
                    }
                }
        }
    }

    fun onDraftChange(text: String) {
        _state.update { it.copy(draft = text) }
    }

    fun focusAgent(slug: String) {
        _state.update { s ->
            if (s.focusSlug == slug) {
                // second tap toggles filter for that agent
                s.copy(filterMode = !s.filterMode, focusSlug = slug)
            } else {
                s.copy(focusSlug = slug, filterMode = false)
            }
        }
    }

    fun clearFilter() {
        _state.update { it.copy(filterMode = false) }
    }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.sending || agents.isEmpty()) return

        val userMsg = ChatMessage(
            id = "u-${UUID.randomUUID()}",
            role = ChatRole.User,
            content = text,
        )
        val typing = agents.map { a ->
            ChatMessage(
                id = "t-${a.slug}",
                role = ChatRole.Typing,
                content = "…",
                agentSlug = a.slug,
                agentName = a.name,
                initials = a.initials,
            )
        }

        _state.update { s ->
            s.copy(
                draft = "",
                sending = true,
                pendingSlugs = agents.map { it.slug }.toSet(),
                messages = s.messages + userMsg + typing,
                error = null,
            )
        }

        viewModelScope.launch {
            val prior = _state.value.messages.filter {
                it.role == ChatRole.User || it.role == ChatRole.Assistant
            }
            // prior already includes the new user message
            val replies = repo.sendToAll(agents, text, prior)
            _state.update { s ->
                val withoutTyping = s.messages.filter { it.role != ChatRole.Typing }
                s.copy(
                    sending = false,
                    pendingSlugs = emptySet(),
                    messages = withoutTyping + replies,
                )
            }
        }
    }

    fun visibleMessages(): List<ChatMessage> {
        val s = _state.value
        if (!s.filterMode || s.focusSlug == null) return s.messages
        val focus = s.focusSlug
        return s.messages.filter { m ->
            m.role == ChatRole.User || m.agentSlug == focus
        }
    }

    companion object {
        fun factory(agents: List<Agent>): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ChatViewModel(agents) as T
                }
            }
    }
}
