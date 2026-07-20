package com.dasexperten.agents.data

import com.dasexperten.agents.model.Agent
import com.dasexperten.agents.model.AgentInitials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RosterRepository(
    private val api: OrgApi = OrgApi(),
) {
    suspend fun loadAgents(): List<Agent> = withContext(Dispatchers.IO) {
        api.listAgents()
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
    }
}
