package com.dasexperten.agents.data

import android.content.Context
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.model.AgentInitials
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CachedAgent(
    val slug: String,
    val name: String,
    val initials: String,
    val photoUrl: String,
    val fullPhotoUrl: String,
)

@Serializable
private data class RosterCacheFile(
    val syncedAtMs: Long,
    val agents: List<CachedAgent>,
)

/**
 * Local snapshot of last successful organizacia SSOT roster pull.
 */
object RosterCache {
    private const val PREFS = "organizacia_ssot"
    private const val KEY = "roster_json"
    private val json = Json { ignoreUnknownKeys = true }

    fun save(context: Context, agents: List<Agent>) {
        val payload = RosterCacheFile(
            syncedAtMs = System.currentTimeMillis(),
            agents = agents.map {
                CachedAgent(it.slug, it.name, it.initials, it.photoUrl, it.fullPhotoUrl)
            },
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json.encodeToString(payload))
            .apply()
    }

    fun load(context: Context): Pair<List<Agent>, Long>? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return null
        return runCatching {
            val file = json.decodeFromString<RosterCacheFile>(raw)
            val agents = file.agents.map {
                Agent(
                    slug = it.slug,
                    name = it.name,
                    initials = it.initials.ifBlank { AgentInitials.of(it.slug, it.name) },
                    photoUrl = it.photoUrl,
                    fullPhotoUrl = it.fullPhotoUrl,
                )
            }
            agents to file.syncedAtMs
        }.getOrNull()
    }
}
