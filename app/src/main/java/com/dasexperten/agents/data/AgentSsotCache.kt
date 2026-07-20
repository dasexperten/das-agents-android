package com.dasexperten.agents.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local cache of full per-agent SSOT packs (MEMORY + foundation + actions + knowledge).
 * Refreshed every 3 hours from organizacia.
 */
object AgentSsotCache {
    private const val PREFS = "organizacia_agent_ssot"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun save(context: Context, pack: AgentSsotPack) {
        if (pack.slug.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(pack.slug, json.encodeToString(pack))
            .putLong("${pack.slug}__at", System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context, slug: String): AgentSsotPack? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(slug, null) ?: return null
        return runCatching { json.decodeFromString<AgentSsotPack>(raw) }.getOrNull()
    }

    fun lastSyncMs(context: Context, slug: String): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("${slug}__at", 0L)

    fun saveAllMeta(context: Context, atMs: Long, count: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong("full_sync_at", atMs)
            .putInt("full_sync_count", count)
            .apply()
    }

    fun fullSyncAt(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("full_sync_at", 0L)
}
