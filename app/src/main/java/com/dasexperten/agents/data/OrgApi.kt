package com.dasexperten.agents.data

import com.dasexperten.agents.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class AgentDto(
    val slug: String,
    val name: String,
    val role: String? = null,
    @SerialName("avatar_path") val avatarPath: String? = null,
)

@Serializable
data class ChatHistoryResponse(
    val ok: Boolean = false,
    val slug: String? = null,
    val messages: List<HistoryMessageDto> = emptyList(),
)

@Serializable
data class HistoryMessageDto(
    val id: Long? = null,
    val role: String,
    val content: String,
    val channel: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("agent_slug") val agentSlug: String? = null,
)

@Serializable
data class ChatReplyResponse(
    val ok: Boolean = false,
    val slug: String? = null,
    val reply: String? = null,
    val error: String? = null,
    val detail: String? = null,
    val agent: ChatAgentMeta? = null,
)

@Serializable
data class ChatAgentMeta(
    val name: String? = null,
    val role: String? = null,
)

class OrgApi(
    private val baseUrl: String = BuildConfig.ORG_BASE_URL.trimEnd('/'),
    private val apiKey: String = BuildConfig.ORG_API_KEY,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    fun photoUrl(slug: String, full: Boolean = false): String {
        val file = if (full) "$slug-full.png" else "$slug.png"
        return "$baseUrl/assets/agents/$file"
    }

    fun listAgents(): List<AgentDto> {
        val req = requestBuilder("/api/agents").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw OrgApiException("roster_failed", "Не удалось загрузить агентов (${resp.code})")
            }
            return json.decodeFromString(body)
        }
    }

    /**
     * Full SSOT pack for one agent (CHARTER + MEMORY + LEARNING + open actions + knowledge).
     * Server refreshes every 3h; may lazy-build if stale.
     */
    fun agentSsot(slug: String): AgentSsotPack {
        val req = requestBuilder("/api/agents/$slug/ssot").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw OrgApiException("ssot_failed", "SSOT $slug: ${resp.code}")
            }
            return json.decodeFromString(body)
        }
    }

    fun ssotIndex(): SsotIndexResponse {
        val req = requestBuilder("/api/ssot").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw OrgApiException("ssot_index_failed", "SSOT index: ${resp.code}")
            }
            return json.decodeFromString(body)
        }
    }

    fun chatHistory(slug: String, limit: Int = 80): ChatHistoryResponse {
        val req = requestBuilder("/api/agents/$slug/chat/history?limit=$limit").get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw OrgApiException("history_failed", "История $slug: ${resp.code}")
            }
            return json.decodeFromString(body)
        }
    }

    /**
     * Board-parity chat: channel=board, use_history=true.
     * Returns assistant reply text.
     */
    fun chat(
        slug: String,
        messages: List<Pair<String, String>>,
    ): ChatReplyResponse {
        val payload = buildJsonObject {
            put("channel", "board")
            put("use_history", true)
            put("source", "android")
            // Owner: human language + every term explained in parentheses (server enforces too).
            put("human_language", true)
            put(
                "messages",
                buildJsonArray {
                    messages.forEach { (role, content) ->
                        add(
                            buildJsonObject {
                                put("role", role)
                                put("content", content)
                            }
                        )
                    }
                }
            )
        }
        val body = payload.toString().toRequestBody(jsonMedia)
        val req = requestBuilder("/api/agents/$slug/chat")
            .post(body)
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            val parsed = runCatching { json.decodeFromString<ChatReplyResponse>(text) }
                .getOrElse {
                    throw OrgApiException("chat_parse", "Плохой ответ сервера (${resp.code})")
                }
            if (!resp.isSuccessful || parsed.ok != true || parsed.reply.isNullOrBlank()) {
                val detail = parsed.detail ?: parsed.error ?: "HTTP ${resp.code}"
                throw OrgApiException(parsed.error ?: "chat_failed", detail)
            }
            return parsed
        }
    }

    private fun requestBuilder(path: String): Request.Builder {
        val b = Request.Builder()
            .url("$baseUrl$path")
            .header("Accept", "application/json")
        if (apiKey.isNotBlank()) {
            b.header("Authorization", "Bearer $apiKey")
        }
        return b
    }
}

class OrgApiException(val code: String, message: String) : Exception(message)
