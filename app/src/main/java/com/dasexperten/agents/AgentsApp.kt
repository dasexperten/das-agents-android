package com.dasexperten.agents

import android.app.Application
import com.dasexperten.agents.data.AgentSsotSyncWorker
import com.dasexperten.agents.data.RosterSyncWorker
import com.dasexperten.agents.update.AppUpdateWorker

class AgentsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Roster list: every hour
        RosterSyncWorker.schedule(this)
        // Full SSOT per agent (MEMORY + knowledge + actions + foundation): every 3 hours
        AgentSsotSyncWorker.schedule(this)
        // APK auto-update check: on start + every 6 hours
        AppUpdateWorker.schedule(this)
    }
}
