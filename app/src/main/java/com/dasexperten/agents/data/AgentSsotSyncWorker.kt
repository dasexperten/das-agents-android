package com.dasexperten.agents.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Every **3 hours**: full SSOT pull for **each** agent from organizacia —
 * CHARTER (foundation) + MEMORY + LEARNING + open actions + knowledge.
 *
 * Server also runs the same interval (CF cron). This keeps the phone cache warm
 * for offline paint and faster chat context.
 */
class AgentSsotSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val api = OrgApi()
            val roster = api.listAgents()
            RosterCache.save(
                applicationContext,
                roster.map {
                    com.dasexperten.agents.model.Agent(
                        slug = it.slug,
                        name = it.name,
                        initials = com.dasexperten.agents.model.AgentInitials.of(it.slug, it.name),
                        photoUrl = api.photoUrl(it.slug, full = false),
                        fullPhotoUrl = api.photoUrl(it.slug, full = true),
                    )
                },
            )
            var ok = 0
            for (a in roster) {
                runCatching {
                    val pack = api.agentSsot(a.slug)
                    AgentSsotCache.save(applicationContext, pack)
                    ok += 1
                }
            }
            AgentSsotCache.saveAllMeta(applicationContext, System.currentTimeMillis(), ok)
            if (ok == 0) Result.retry() else Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "organizacia_agent_ssot_3h"
        const val INTERVAL_HOURS = 3L

        fun schedule(context: Context) {
            val req = PeriodicWorkRequestBuilder<AgentSsotSyncWorker>(
                INTERVAL_HOURS,
                TimeUnit.HOURS,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
            // Kick once soon so first install does not wait 3h
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<AgentSsotSyncWorker>().build()
            )
        }
    }
}
