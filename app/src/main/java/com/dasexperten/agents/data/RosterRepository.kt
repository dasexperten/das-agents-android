package com.dasexperten.agents.data

import android.content.Context
import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.model.AgentInitials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RosterRepository(
    private val api: OrgApi = OrgApi(),
    private val appContext: Context? = null,
) {
    fun appContextOrNull(): Context? = appContext
    /**
     * Live SSOT from organizacia Worker (`GET /api/agents`).
     * On success, writes local cache when [appContext] is set.
     */
    suspend fun loadAgents(): List<Agent> = withContext(Dispatchers.IO) {
        val list = api.listAgents()
            .map { dto ->
                Agent(
                    slug = dto.slug,
                    name = dto.name,
                    initials = AgentInitials.of(dto.slug, dto.name),
                    photoUrl = api.photoUrl(dto.slug, full = false),
                    fullPhotoUrl = api.photoUrl(dto.slug, full = true),
                )
            }
            .sortedBy { it.name.lowercase() }
        appContext?.let { RosterCache.save(it, list) }
        list
    }

    fun loadCached(): Pair<List<Agent>, Long>? =
        appContext?.let { RosterCache.load(it) }
}
