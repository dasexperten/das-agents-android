package com.dasexperten.agents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dasexperten.agents.data.DigestSlide
import com.dasexperten.agents.data.OrgApi
import com.dasexperten.agents.model.AgentInitials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DigestUiState(
    val loading: Boolean = true,
    val slides: List<DigestSlideUi> = emptyList(),
    /** 0-based index of current card in the deck. */
    val index: Int = 0,
    val error: String? = null,
    val locale: String = "ru",
)

data class DigestSlideUi(
    val slug: String,
    val name: String,
    val role: String,
    val initials: String,
    val photoUrl: String,
    val fullPhotoUrl: String,
    /** Headline / open work line (not a choice). */
    val question: String,
    /** Отчёт */
    val report: String,
    /** Взгляд */
    val view: String,
)

class DigestViewModel(
    private val api: OrgApi = OrgApi(),
) : ViewModel() {

    private val _state = MutableStateFlow(DigestUiState())
    val state: StateFlow<DigestUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                withContext(Dispatchers.IO) {
                    api.digestAsks(locale = "ru")
                }
            }.onSuccess { pack ->
                val roles = pack.roles
                val slides = pack.asks.map { slide ->
                    val name = slide.name
                        ?: roles[slide.slug]?.let { /* role only */ null }
                        ?: displayNameFromSlug(slide.slug)
                    val role = slide.role ?: roles[slide.slug].orEmpty()
                    DigestSlideUi(
                        slug = slide.slug,
                        name = name.ifBlank { displayNameFromSlug(slide.slug) },
                        role = role,
                        initials = AgentInitials.of(slide.slug, name.ifBlank { slide.slug }),
                        photoUrl = api.photoUrl(slide.slug, full = false),
                        fullPhotoUrl = api.photoUrl(slide.slug, full = true),
                        question = slide.question.orEmpty().trim(),
                        report = slide.reportText(),
                        view = slide.viewText(),
                    )
                }.filter { it.report.isNotBlank() || it.view.isNotBlank() || it.question.isNotBlank() }

                // Enrich names from roster when possible
                val rosterNames = runCatching {
                    withContext(Dispatchers.IO) { api.listAgents() }
                }.getOrDefault(emptyList()).associate { it.slug to it.name }

                val named = slides.map { s ->
                    val n = rosterNames[s.slug] ?: s.name
                    s.copy(
                        name = n,
                        initials = AgentInitials.of(s.slug, n),
                    )
                }

                _state.update {
                    it.copy(
                        loading = false,
                        slides = named,
                        index = 0,
                        error = null,
                        locale = pack.locale ?: "ru",
                    )
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Дайджест не загрузился",
                    )
                }
            }
        }
    }

    fun next() {
        _state.update { s ->
            if (s.slides.isEmpty()) s
            else s.copy(index = (s.index + 1).coerceAtMost(s.slides.lastIndex))
        }
    }

    fun prev() {
        _state.update { s ->
            s.copy(index = (s.index - 1).coerceAtLeast(0))
        }
    }

    fun goTo(i: Int) {
        _state.update { s ->
            if (s.slides.isEmpty()) s
            else s.copy(index = i.coerceIn(0, s.slides.lastIndex))
        }
    }

    private fun displayNameFromSlug(slug: String): String =
        slug.split("-").joinToString(" ") { part ->
            part.replaceFirstChar { c -> c.uppercaseChar() }
        }

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DigestViewModel() as T
            }
        }
    }
}
