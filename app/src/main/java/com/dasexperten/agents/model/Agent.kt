package com.dasexperten.agents.model

data class Agent(
    val slug: String,
    val name: String,
    val initials: String,
    val photoUrl: String,
    val fullPhotoUrl: String,
)

/** SSOT initials from organizacia docs/AGENT_AVATARS.md (+ live roster fallbacks). */
object AgentInitials {
    private val map = mapOf(
        "viktor-palich" to "VP",
        "lena-sergeeva" to "LS",
        "lauda-briana" to "LB",
        "alexandra-obnorskaya" to "SO",
        "roberta-di-maria" to "RB",
        "julian-farah" to "JF",
        "marika-nowicka" to "ML",
        "valentina-korolyeva" to "VK",
        "justina-timber" to "JT",
        "mina-rutunya" to "MN",
        "mina" to "MN",
        "zina-pevtsova" to "ZP",
        "maya-krasochkina" to "MK",
        "tamara-haar" to "TH",
        "andrea" to "AN",
        "taras-ryzhiy" to "TR",
        "arina-volkova" to "AV",
        "dasha-kozlovskaya" to "DK",
        "jurgen-witt" to "JW",
    )

    fun of(slug: String, name: String): String {
        map[slug]?.let { return it }
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
            parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts.size == 1 -> parts[0].take(1).uppercase()
            else -> slug.take(2).uppercase()
        }
    }
}
