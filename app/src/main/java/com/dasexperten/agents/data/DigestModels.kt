package com.dasexperten.agents.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DigestAsksResponse(
    val asks: List<DigestSlide> = emptyList(),
    val roles: Map<String, String> = emptyMap(),
    val rule: String? = null,
    val count: Int = 0,
    val locale: String? = null,
)

/**
 * One Daily Digest slide — **Отчёт** (brief) + **Взгляд** (opinion).
 * Options/choices are ignored on Android (Owner: no selection here).
 */
@Serializable
data class DigestSlide(
    val slug: String,
    val role: String? = null,
    val name: String? = null,
    val question: String? = null,
    /** Report paragraphs (board: Report). */
    val brief: List<String> = emptyList(),
    /** My view (board: opinion). */
    val opinion: String? = null,
    val status: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    val locale: String? = null,
    val correspondence: String? = null,
) {
    fun reportText(): String =
        brief.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")

    fun viewText(): String = opinion?.trim().orEmpty()
}
