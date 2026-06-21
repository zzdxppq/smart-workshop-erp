package com.btsheng.erp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.btsheng.erp.core.data.sync.SyncScheduler
import com.btsheng.erp.core.security.SecureSessionManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** V1.3.7 · android-impl Application（Hilt 入口 + WorkManager 调度） */
@HiltAndroidApp
class ErpApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        SecureSessionManager.init(this)
        SyncScheduler.schedule(this)
    }
}
