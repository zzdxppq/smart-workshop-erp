package com.btsheng.erp.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.btsheng.erp.core.data.local.PendingScanDao
import com.btsheng.erp.core.network.E5ScanApi
import com.btsheng.erp.core.network.ScanStartBody
import com.btsheng.erp.core.scan.QrCodeParser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** WorkManager 离线扫码同步（Room → API） */
@HiltWorker
class PendingSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingScanDao: PendingScanDao,
    private val scanApi: E5ScanApi,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val pending = pendingScanDao.listPending()
        for (item in pending) {
            try {
                when (QrCodeParser.parse(item.code).type) {
                    QrCodeParser.TYPE_WORK_ORDER -> scanApi.startWorkorder(item.code, ScanStartBody())
                    else -> { /* 其他类型留待后续 */ }
                }
                pendingScanDao.markSynced(item.id)
            } catch (_: Exception) {
                return Result.retry()
            }
        }
        pendingScanDao.deleteSynced()
        return Result.success()
    }
}
