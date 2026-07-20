package com.dasexperten.agents.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dasexperten.agents.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val checker = AppUpdateChecker(app)
    private val _state = MutableStateFlow(UpdateUiState())
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        val prefs = app.getSharedPreferences(AppUpdateChecker.PREFS, Context.MODE_PRIVATE)
        val readyCode = prefs.getInt("ready_version_code", 0)
        val path = prefs.getString("ready_apk_path", null)
        if (readyCode > BuildConfig.VERSION_CODE && path != null && File(path).exists()) {
            _state.update {
                it.copy(
                    available = true,
                    versionCode = readyCode,
                    versionName = prefs.getString("ready_version_name", "") ?: "",
                    notes = prefs.getString("ready_notes", "") ?: "",
                    readyPath = path,
                )
            }
        }
        checkNow()
    }

    fun checkNow() {
        viewModelScope.launch {
            _state.update { it.copy(checking = true, error = null) }
            val result = checker.check()
            if (!result.available || result.manifest == null) {
                _state.update {
                    it.copy(
                        checking = false,
                        available = it.readyPath != null && it.available,
                        error = result.error,
                    )
                }
                return@launch
            }
            val m = result.manifest
            _state.update {
                it.copy(
                    checking = false,
                    available = true,
                    versionCode = m.versionCode,
                    versionName = m.versionName,
                    notes = m.notes,
                )
            }
            autoDownload(m)
        }
    }

    private fun autoDownload(manifest: AppUpdateManifest) {
        viewModelScope.launch {
            val existing = _state.value.readyPath?.let { File(it) }
            if (existing != null && existing.exists() &&
                _state.value.versionCode == manifest.versionCode
            ) {
                if (checker.canRequestPackageInstalls()) {
                    // already downloaded — offer install, auto if force
                    if (manifest.force) installReady()
                }
                return@launch
            }
            _state.update { it.copy(downloading = true, error = null) }
            runCatching { checker.downloadApk(manifest) }
                .onSuccess { file ->
                    getApplication<Application>()
                        .getSharedPreferences(AppUpdateChecker.PREFS, Context.MODE_PRIVATE)
                        .edit()
                        .putInt("ready_version_code", manifest.versionCode)
                        .putString("ready_version_name", manifest.versionName)
                        .putString("ready_apk_path", file.absolutePath)
                        .putString("ready_notes", manifest.notes)
                        .apply()
                    _state.update {
                        it.copy(downloading = false, readyPath = file.absolutePath)
                    }
                    // Auto-open installer when installs-from-this-app already allowed
                    if (checker.canRequestPackageInstalls()) {
                        installReady()
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(downloading = false, error = e.message ?: "download failed")
                    }
                }
        }
    }

    fun installReady() {
        val path = _state.value.readyPath ?: return
        val file = File(path)
        if (!file.exists()) {
            _state.update { it.copy(error = "файл обновления не найден") }
            return
        }
        if (!checker.canRequestPackageInstalls()) {
            getApplication<Application>().startActivity(checker.installPermissionSettingsIntent())
            return
        }
        checker.startInstall(file)
    }

    fun dismiss() {
        val code = _state.value.versionCode
        getApplication<Application>()
            .getSharedPreferences(AppUpdateChecker.PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(AppUpdateChecker.KEY_SKIPPED_CODE, code)
            .apply()
        _state.update { it.copy(available = false) }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return UpdateViewModel(app) as T
                }
            }
    }
}
