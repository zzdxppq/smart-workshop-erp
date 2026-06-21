package com.btsheng.erp.feature.scan

import com.btsheng.erp.core.data.local.PendingScanDao
import com.btsheng.erp.core.data.local.PendingScanEntity
import com.btsheng.erp.core.network.E5ScanApi
import com.btsheng.erp.core.network.ScanReportBody
import com.btsheng.erp.core.network.ScanStartBody
import com.btsheng.erp.core.network.ScanTransferBody
import com.btsheng.erp.core.scan.QrCodeParser
import com.btsheng.erp.core.security.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val scanApi: E5ScanApi,
    private val pendingScanDao: PendingScanDao,
) {
    suspend fun pendingCount(): Int = pendingScanDao.countPending()

    suspend fun enqueueOffline(code: String, deviceId: String = "android") {
        pendingScanDao.insert(
            PendingScanEntity(code = code, localTs = System.currentTimeMillis(), deviceId = deviceId),
        )
    }

    private suspend fun <T> callOrQueue(code: String, block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: Exception) {
        enqueueOffline(code)
        Result.failure(e)
    }

    private fun userId(): Long = SessionStore.session?.userId ?: 1L

    suspend fun startWorkorder(barcode: String): Result<Unit> = callOrQueue(barcode) {
        val resp = scanApi.startWorkorder(barcode, ScanStartBody(), userId())
        check(resp.ok) { resp.message ?: "开工失败" }
    }

    suspend fun reportWorkorder(
        workorderBarcode: String,
        qtyDone: Int,
        qtyOk: Int,
        qtyScrap: Int,
    ): Result<Unit> = callOrQueue(workorderBarcode) {
        val resp = scanApi.reportWorkorder(
            workorderBarcode,
            ScanReportBody(qtyDone = qtyDone, qtyOk = qtyOk, qtyScrap = qtyScrap),
            userId(),
        )
        check(resp.ok) { resp.message ?: "报工失败" }
    }

    suspend fun transferNext(lzBarcode: String, workorderNo: String): Result<Unit> = callOrQueue(lzBarcode) {
        val resp = scanApi.transferNext(
            lzBarcode,
            ScanTransferBody(workorderNo = workorderNo),
            userId(),
        )
        check(resp.ok) { resp.message ?: "过站失败" }
    }

    suspend fun submitThreeCodeStep(
        step: Int,
        code: String,
        workorderCode: String,
        qtyDone: Int,
        qtyOk: Int,
        qtyScrap: Int,
    ): Result<Unit> {
        val parsed = QrCodeParser.parse(code)
        return when (step) {
            0 -> startWorkorder(code)
            1 -> {
                if (parsed.type != QrCodeParser.TYPE_FLOW) {
                    return Result.failure(IllegalArgumentException("请扫描 LZ- 流转码"))
                }
                transferNext(code, workorderCode)
            }
            else -> reportWorkorder(workorderCode, qtyDone, qtyOk, qtyScrap)
        }
    }
}
