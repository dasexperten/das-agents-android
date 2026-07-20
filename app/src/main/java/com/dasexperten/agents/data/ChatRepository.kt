package com.dasexperten.agents.data

import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.model.ChatMessage
import com.dasexperten.agents.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

class ChatRepository(
    private val api: OrgApi = OrgApi(),
) {
    private val fanOutLimit = Semaphore(4)

    suspend fun loadMergedHistory(agents: List<Agent>, limitPerAgent: Int = 60): List<ChatMessage> =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val byAgent = agents.map { agent ->
                    async {
                        runCatching { api.chatHistory(agent.slug, limitPerAgent) }
                            .getOrNull()
                            ?.messages
                            .orEmpty()
                            .mapNotNull { row -> row.toChatMessage(agent) }
                    }
                }.awaitAll().flatten()

                mergeTimeline(byAgent)
            }
        }

    /**
     * Fan-out one Boss line to every selected agent. Returns assistant (or error) messages.
     */
    suspend fun sendToAll(
        agents: List<Agent>,
        userText: String,
        priorUi: List<ChatMessage>,
    ): List<ChatMessage> = coroutineScope {
        val outbound = buildOutboundMessages(priorUi, userText)
        agents.map { agent ->
            async {
                fanOutLimit.withPermit {
                    withContext(Dispatchers.IO) {
                        runCatching {
                            val res = api.chat(agent.slug, outbound)
                            ChatMessage(
                                id = "a-${agent.slug}-${UUID.randomUUID()}",
                                role = ChatRole.Assistant,
                                content = res.reply.orEmpty().trim(),
                                agentSlug = agent.slug,
                                agentName = res.agent?.name ?: agent.name,
                                initials = agent.initials,
                            )
                        }.getOrElse { e ->
                            ChatMessage(
                                id = "e-${agent.slug}-${UUID.randomUUID()}",
                                role = ChatRole.Error,
                                content = "${agent.initials}: ${e.message ?: "ошибка"}",
                                agentSlug = agent.slug,
                                agentName = agent.name,
                                initials = agent.initials,
                            )
                        }
                    }
                }
            }
        }.awaitAll()
    }

    private fun buildOutboundMessages(
        priorUi: List<ChatMessage>,
        userText: String,
    ): List<Pair<String, String>> {
        // Thin client: last turns + new user. Server merges board history via use_history.
        val recent = priorUi
            .filter { it.role == ChatRole.User || it.role == ChatRole.Assistant }
            .takeLast(10)
            .map {
                val role = if (it.role == ChatRole.User) "user" else "assistant"
                role to it.content
            }
        // priorUi may already include the just-typed Boss line — do not double-append.
        val last = recent.lastOrNull()
        if (last != null && last.first == "user" && last.second == userText) {
            return recent
        }
        return recent + ("user" to userText)
    }

    private fun mergeTimeline(raw: List<ChatMessage>): List<ChatMessage> {
        val sorted = raw.sortedWith(compareBy({ it.createdAtMs }, { it.id }))
        val out = ArrayList<ChatMessage>(sorted.size)
        var lastUserKey: String? = null
        for (m in sorted) {
            if (m.role == ChatRole.User) {
                val key = "${m.content.trim()}|${m.createdAtMs / 15_000}"
                if (key == lastUserKey) continue
                lastUserKey = key
                out += m.copy(agentSlug = null, agentName = null, initials = null)
            } else {
                out += m
            }
        }
        return out
    }

    private fun HistoryMessageDto.toChatMessage(agent: Agent): ChatMessage? {
        val role = when (role.lowercase()) {
            "user" -> ChatRole.User
            "assistant" -> ChatRole.Assistant
            "system" -> ChatRole.System
            else -> return null
        }
        val content = content.trim()
        if (content.isEmpty()) return null
        return ChatMessage(
            id = "h-${agent.slug}-${id ?: createdAt ?: content.hashCode()}",
            role = role,
            content = content,
            agentSlug = if (role == ChatRole.Assistant) agent.slug else null,
            agentName = if (role == ChatRole.Assistant) agent.name else null,
            initials = if (role == ChatRole.Assistant) agent.initials else null,
            createdAtMs = parseSqliteTime(createdAt),
        )
    }

    private fun parseSqliteTime(raw: String?): Long {
        if (raw.isNullOrBlank()) return System.currentTimeMillis()
        // D1 default: "YYYY-MM-DD HH:MM:SS" (UTC-ish)
        return runCatching {
            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime.parse(raw.trim().take(19), fmt).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.recoverCatching {
            Instant.parse(raw.trim()).toEpochMilli()
        }.getOrDefault(System.currentTimeMillis())
    }
}
