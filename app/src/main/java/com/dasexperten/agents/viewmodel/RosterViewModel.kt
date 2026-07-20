package com.dasexperten.agents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dasexperten.agents.data.RosterRepository
import com.dasexperten.agents.model.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RosterUiState(
    val loading: Boolean = true,
    val agents: List<Agent> = emptyList(),
    val selectedSlugs: Set<String> = emptySet(),
    val error: String? = null,
)

class RosterViewModel(
    private val repo: RosterRepository = RosterRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(RosterUiState())
    val state: StateFlow<RosterUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { repo.loadAgents() }
                .onSuccess { list ->
                    _state.update {
                        it.copy(
                            loading = false,
                            agents = list,
                            // keep selection that still exists
                            selectedSlugs = it.selectedSlugs.intersect(list.map { a -> a.slug }.toSet()),
                            error = null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: "Не удалось загрузить",
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
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RosterViewModel() as T
            }
        }
    }
}
