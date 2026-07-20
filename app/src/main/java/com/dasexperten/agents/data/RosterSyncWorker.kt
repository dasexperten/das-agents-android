package com.dasexperten.agents.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Hourly pull of agent roster from organizacia SSOT
 * (`GET https://org.dasexperten.com/api/agents`).
 *
 * Caches last good JSON so cold start can show roster before network.
 */
class RosterSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val agents = RosterRepository().loadAgents()
            RosterCache.save(applicationContext, agents)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "organizacia_roster_hourly"

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<RosterSyncWorker>(1, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                req,
            )
        }
    }
}
