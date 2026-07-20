package com.dasexperten.agents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dasexperten.agents.data.RosterRepository
import com.dasexperten.agents.model.Agent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RosterUiState(
    val loading: Boolean = true,
    val agents: List<Agent> = emptyList(),
    val selectedSlugs: Set<String> = emptySet(),
    val error: String? = null,
    /** Last successful SSOT pull (epoch ms). */
    val lastSyncMs: Long? = null,
    val lastSyncLabel: String? = null,
)

class RosterViewModel(
    private val repo: RosterRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RosterUiState())
    val state: StateFlow<RosterUiState> = _state.asStateFlow()

    init {
        // Instant paint from last hourly SSOT cache, then network.
        repo.loadCached()?.let { (cached, syncedAt) ->
            _state.update {
                it.copy(
                    loading = true,
                    agents = cached,
                    lastSyncMs = syncedAt,
                    lastSyncLabel = formatSync(syncedAt),
                )
            }
        }
        refresh(silent = _state.value.agents.isNotEmpty())
        // While process is alive: roster every hour; full SSOT packs every 3h.
        viewModelScope.launch {
            while (isActive) {
                delay(HOUR_MS)
                refresh(silent = true)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(THREE_HOURS_MS)
                refreshFullSsot()
            }
        }
    }

    /** Pull full SSOT pack for every agent (MEMORY + foundation + actions + knowledge). */
    fun refreshFullSsot() {
        viewModelScope.launch {
            val ctx = repo.appContextOrNull() ?: return@launch
            val api = com.dasexperten.agents.data.OrgApi()
            val list = _state.value.agents.ifEmpty {
                runCatching { repo.loadAgents() }.getOrDefault(emptyList())
            }
            var ok = 0
            for (a in list) {
                runCatching {
                    val pack = api.agentSsot(a.slug)
                    com.dasexperten.agents.data.AgentSsotCache.save(ctx, pack)
                    ok += 1
                }
            }
            if (ok > 0) {
                com.dasexperten.agents.data.AgentSsotCache.saveAllMeta(
                    ctx,
                    System.currentTimeMillis(),
                    ok,
                )
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        lastSyncMs = now,
                        lastSyncLabel = formatSync(now) + " · SSOT×$ok",
                    )
                }
            }
        }
    }

    /**
     * @param silent if true, keep current list visible (no full-screen spinner).
     */
    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _state.update { it.copy(loading = true, error = null) }
            }
            runCatching { repo.loadAgents() }
                .onSuccess { list ->
                    val now = System.currentTimeMillis()
                    _state.update {
                        it.copy(
                            loading = false,
                            agents = list,
                            selectedSlugs = it.selectedSlugs.intersect(list.map { a -> a.slug }.toSet()),
                            error = null,
                            lastSyncMs = now,
                            lastSyncLabel = formatSync(now),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = if (it.agents.isEmpty()) {
                                e.message ?: "Не удалось загрузить"
                            } else {
                                // keep list; surface soft error in label
                                it.error
                            },
                            lastSyncLabel = it.lastSyncLabel,
                        )
                    }
                }
        }
    }

    fun toggle(slug: String) {
        _state.update { s ->
            val next = s.selectedSlugs.toMutableSet()
            if (!next.add(slug)) next.remove(slug)
            s.copy(selectedSlugs = next)
        }
    }

    fun selectedAgents(): List<Agent> {
        val s = _state.value
        return s.agents.filter { it.slug in s.selectedSlugs }
    }

    companion object {
        const val HOUR_MS = 60L * 60L * 1000L
        const val THREE_HOURS_MS = 3L * 60L * 60L * 1000L

        private val timeFmt: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

        fun formatSync(ms: Long): String = timeFmt.format(Instant.ofEpochMilli(ms))

        fun factory(appContext: android.content.Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val app = appContext.applicationContext
                    return RosterViewModel(RosterRepository(appContext = app)) as T
                }
            }
    }
}
