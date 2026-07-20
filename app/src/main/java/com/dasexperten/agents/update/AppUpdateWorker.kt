package com.dasexperten.agents.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background check for new APK every 6 hours + one-shot on app start.
 * Auto-downloads when a newer version is published.
 */
class AppUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val checker = AppUpdateChecker(applicationContext)
        val prefs = applicationContext.getSharedPreferences(
            AppUpdateChecker.PREFS,
            Context.MODE_PRIVATE,
        )
        prefs.edit()
            .putLong(AppUpdateChecker.KEY_LAST_CHECK, System.currentTimeMillis())
            .apply()

        val check = checker.check()
        if (!check.available || check.manifest == null) return Result.success()

        val skipped = prefs.getInt(AppUpdateChecker.KEY_SKIPPED_CODE, 0)
        if (check.manifest.versionCode == skipped && !check.manifest.force) {
            return Result.success()
        }

        return runCatching {
            val file = checker.downloadApk(check.manifest)
            prefs.edit()
                .putInt("ready_version_code", check.manifest.versionCode)
                .putString("ready_version_name", check.manifest.versionName)
                .putString("ready_apk_path", file.absolutePath)
                .putString("ready_notes", check.manifest.notes)
                .putString("ready_apk_url", check.manifest.apkUrl)
                .apply()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE_PERIODIC = "agents_app_update_6h"
        const val UNIQUE_ONCE = "agents_app_update_once"

        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<AppUpdateWorker>(6, TimeUnit.HOURS).build(),
            )
            wm.enqueueUniqueWork(
                UNIQUE_ONCE,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<AppUpdateWorker>().build(),
            )
        }
    }
}
