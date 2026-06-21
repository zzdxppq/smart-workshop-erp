package com.btsheng.erp.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** WorkManager 调度（V1.3.7 · Story 1.4 · 周期 5min） */
object SyncScheduler {
    private const val WORK_NAME = "pending_sync_worker"
    fun schedule(context: Context) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = PeriodicWorkRequestBuilder<PendingSyncWorker>(5, TimeUnit.MINUTES)
            .setConstraints(constraints).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
