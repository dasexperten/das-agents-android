package com.dasexperten.agents.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
    val index: Int = 0,
    val error: String? = null,
)

/**
 * One Digest slide — fields prepared like org.dasexperten.com renderDigestSlide().
 */
data class DigestSlideUi(
    val slug: String,
    val name: String,
    val role: String,
    val initials: String,
    val photoUrl: String,
    val fullPhotoUrl: String,
    val question: String,
    /** Отчёт — one flowing paragraph */
    val report: String,
    /** Мой взгляд — one paragraph */
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
                    val pack = api.digestAsks(locale = "ru")
                    val roster = runCatching { api.listAgents() }.getOrDefault(emptyList())
                    val names = roster.associate { it.slug to it.name }
                    pack.asks.map { slide ->
                        val name = names[slide.slug]
                            ?: slide.name
                            ?: displayNameFromSlug(slide.slug)
                        val role = (slide.role ?: pack.roles[slide.slug].orEmpty())
                            .split(Regex("[—–-]"))
                            .firstOrNull()
                            ?.trim()
                            ?.take(48)
                            .orEmpty()
                            .ifBlank { pack.roles[slide.slug].orEmpty() }

                        val firstName = name.split(Regex("\\s+")).firstOrNull().orEmpty()
                        val briefParts = slide.brief
                            .map { cleanPara(it, firstName) }
                            .filter { it.isNotBlank() }

                        var reportPara = if (briefParts.isNotEmpty()) {
                            briefParts.joinToString(" ").replace(Regex("\\s+"), " ").trim()
                        } else {
                            "Краткий отчёт по этой работе ещё не подгрузился."
                        }

                        var viewPara = slide.opinion
                            ?.let { stripViewLead(it) }
                            ?.let { cleanPara(it, firstName) }
                            .orEmpty()

                        if (viewPara.isBlank() && briefParts.size > 1) {
                            viewPara = briefParts.last()
                        }
                        if (viewPara.isBlank()) {
                            viewPara = "Пока без отдельной рекомендации."
                        }
                        // If report absorbed same last line as view without opinion, drop last from report
                        if (briefParts.size > 1 && viewPara == briefParts.last() &&
                            slide.opinion.isNullOrBlank()
                        ) {
                            reportPara = briefParts.dropLast(1)
                                .joinToString(" ")
                                .replace(Regex("\\s+"), " ")
                                .trim()
                        }

                        val question = cleanPara(slide.question.orEmpty(), firstName)

                        DigestSlideUi(
                            slug = slide.slug,
                            name = name,
                            role = role,
                            initials = AgentInitials.of(slide.slug, name),
                            photoUrl = api.photoUrl(slide.slug, full = false),
                            fullPhotoUrl = api.photoUrl(slide.slug, full = true),
                            question = question,
                            report = reportPara,
                            view = viewPara,
                        )
                    }.filter {
                        it.report.isNotBlank() || it.view.isNotBlank() || it.question.isNotBlank()
                    }
                }
            }.onSuccess { slides ->
                _state.update {
                    it.copy(loading = false, slides = slides, index = 0, error = null)
                }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.message ?: "Не удалось загрузить дайджест",
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
        _state.update { s -> s.copy(index = (s.index - 1).coerceAtLeast(0)) }
    }

    private fun displayNameFromSlug(slug: String): String =
        slug.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    /** Board: strip leading agent first name, then sentence-case. */
    private fun cleanPara(text: String, firstName: String): String {
        var x = text.trim()
        if (firstName.isNotBlank() && x.isNotBlank()) {
            val f = Regex.escape(firstName)
            x = x.replace(
                Regex(
                    "^$f(?:\\s+here)?(?:\\s+[\\p{L}'-]+)?\\s*[.。,:—–\\-]?\\s*",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            ).trim()
        }
        if (x.isEmpty()) return text.trim()
        // sentence case first letter
        val chars = x.toCharArray()
        for (i in chars.indices) {
            if (chars[i].isLetter()) {
                chars[i] = chars[i].uppercaseChar()
                break
            }
        }
        return String(chars)
    }

    private fun stripViewLead(text: String): String =
        text.trim()
            .replace(Regex("^(my\\s+view|мой\\s+взгляд)\\s*[:—–\\-]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()

    companion object {
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DigestViewModel() as T
            }
        }
    }
}
