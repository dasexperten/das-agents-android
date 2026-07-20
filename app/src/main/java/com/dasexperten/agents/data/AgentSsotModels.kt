package com.dasexperten.agents.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AgentSsotPack(
    val ok: Boolean = false,
    val slug: String = "",
    val name: String? = null,
    val role: String? = null,
    @SerialName("synced_at") val syncedAt: String? = null,
    @SerialName("pack_text") val packText: String? = null,
    @SerialName("open_actions") val openActions: List<SsotAction> = emptyList(),
    @SerialName("memory_excerpt") val memoryExcerpt: String? = null,
    @SerialName("foundation_excerpt") val foundationExcerpt: String? = null,
    @SerialName("learning_excerpt") val learningExcerpt: String? = null,
    val knowledge: JsonElement? = null,
    val sources: SsotSources? = null,
)

@Serializable
data class SsotAction(
    @SerialName("task_id") val taskId: String? = null,
    val status: String? = null,
    val title: String? = null,
    val note: String? = null,
)

@Serializable
data class SsotSources(
    val charter: Boolean = false,
    val memory: Boolean = false,
    val learning: Boolean = false,
    val dig: Boolean = false,
    @SerialName("open_actions") val openActions: Int = 0,
    @SerialName("recent_chat") val recentChat: Int = 0,
)

@Serializable
data class SsotIndexResponse(
    val ok: Boolean = false,
    val packs: List<SsotIndexRow> = emptyList(),
    @SerialName("interval_hours") val intervalHours: Int = 3,
)

@Serializable
data class SsotIndexRow(
    val slug: String,
    @SerialName("synced_at") val syncedAt: String? = null,
    @SerialName("pack_len") val packLen: Int = 0,
)
