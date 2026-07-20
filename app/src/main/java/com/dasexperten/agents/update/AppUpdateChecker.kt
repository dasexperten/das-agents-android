package com.dasexperten.agents.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dasexperten.agents.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Sideload auto-update: reads manifest, downloads APK, opens installer.
 * Android still requires one user tap to confirm install (Play policy / security).
 */
class AppUpdateChecker(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    data class CheckResult(
        val available: Boolean,
        val manifest: AppUpdateManifest? = null,
        val localCode: Int = BuildConfig.VERSION_CODE,
        val error: String? = null,
    )

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val body = httpGet(BuildConfig.UPDATE_MANIFEST_URL)
            val manifest = json.decodeFromString<AppUpdateManifest>(body)
            val available = manifest.versionCode > BuildConfig.VERSION_CODE &&
                manifest.apkUrl.isNotBlank()
            CheckResult(available = available, manifest = manifest)
        }.getOrElse {
            CheckResult(available = false, error = it.message)
        }
    }

    /**
     * Download APK to cache and return content Uri via FileProvider.
     */
    suspend fun downloadApk(manifest: AppUpdateManifest): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "agents-v${manifest.versionCode}.apk")
        if (out.exists()) out.delete()

        val req = Request.Builder()
            .url(manifest.apkUrl)
            .header("Accept", "application/vnd.android.package-archive,application/octet-stream,*/*")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("download HTTP ${resp.code}")
            }
            val bytes = resp.body?.bytes()
                ?: throw IllegalStateException("empty apk body")
            if (bytes.size < 100_000) {
                throw IllegalStateException("apk too small (${bytes.size})")
            }
            // APK is a zip: PK header
            if (bytes[0] != 'P'.code.toByte() || bytes[1] != 'K'.code.toByte()) {
                throw IllegalStateException("not an apk/zip")
            }
            out.writeBytes(bytes)
        }
        out
    }

    fun contentUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun installPermissionSettingsIntent(): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /**
     * Open system package installer for the downloaded APK.
     */
    fun startInstall(file: File) {
        val uri = contentUri(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Download + install in one step (still needs user confirm on installer screen).
     */
    suspend fun downloadAndInstall(manifest: AppUpdateManifest): File {
        val file = downloadApk(manifest)
        withContext(Dispatchers.Main) {
            if (!canRequestPackageInstalls() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startActivity(installPermissionSettingsIntent())
            } else {
                startInstall(file)
            }
        }
        return file
    }

    private fun httpGet(url: String): String {
        val req = Request.Builder().url(url).get().build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("manifest HTTP ${resp.code}")
            }
            return body
        }
    }

    companion object {
        const val PREFS = "app_update"
        const val KEY_SKIPPED_CODE = "skipped_version_code"
        const val KEY_LAST_CHECK = "last_check_ms"
    }
}
