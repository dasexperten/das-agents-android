package com.dasexperten.agents.update

data class UpdateUiState(
    val checking: Boolean = false,
    val available: Boolean = false,
    val downloading: Boolean = false,
    val versionName: String = "",
    val versionCode: Int = 0,
    val notes: String = "",
    val error: String? = null,
    val readyPath: String? = null,
)
