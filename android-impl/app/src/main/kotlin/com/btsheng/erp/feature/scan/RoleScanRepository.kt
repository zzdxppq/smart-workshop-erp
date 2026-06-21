package com.btsheng.erp.feature.scan

import com.btsheng.erp.feature.qc.QcInspectionRepository
import javax.inject.Inject
import javax.inject.Singleton

data class QcScanResult(
    val inspectionId: Long? = null,
    val inspectionNo: String? = null,
    val message: String? = null,
)

data class WorkorderProgressInfo(
    val workorderNo: String,
    val productName: String? = null,
    val status: String? = null,
    val qtyPlanned: Int? = null,
    val qtyCompleted: Int? = null,
    val progressPercent: Int? = null,
    val alertMessage: String? = null,
    val found: Boolean = true,
)

@Singleton
class RoleScanRepository @Inject constructor(
    private val qcRepository: QcInspectionRepository,
    private val dashboardApi: com.btsheng.erp.core.network.ProductionDashboardApi,
) {

    suspend fun resolveQcScan(rawCode: String): QcScanResult {
        return qcRepository.resolveOrCreateScan(rawCode).fold(
            onSuccess = { QcScanResult(inspectionId = it) },
            onFailure = { QcScanResult(message = it.message ?: "未找到或无法创建检验单") },
        )
    }

    suspend fun lookupWorkorderProgress(rawCode: String): WorkorderProgressInfo {
        val parsed = com.btsheng.erp.core.scan.QrCodeParser.parse(rawCode.trim())
        val woHint = when (parsed.type) {
            com.btsheng.erp.core.scan.QrCodeParser.TYPE_WORK_ORDER -> parsed.code ?: rawCode.trim()
            com.btsheng.erp.core.scan.QrCodeParser.TYPE_FLOW -> rawCode.trim()
            else -> rawCode.trim()
        }
        val resp = dashboardApi.workorders(limit = 100)
        if (!resp.ok || resp.data.isNullOrEmpty()) {
            return WorkorderProgressInfo(workorderNo = woHint, found = false, alertMessage = resp.message ?: "未查到工单")
        }
        val row = resp.data!!.firstOrNull { matchesWorkorder(it, woHint) }
            ?: return WorkorderProgressInfo(workorderNo = woHint, found = false, alertMessage = "看板中未找到该工单")
        val pct = row.progress?.toInt() ?: row.qtyPlanned?.let { p ->
            if (p > 0) ((row.qtyCompleted ?: 0) * 100 / p) else 0
        }
        return WorkorderProgressInfo(
            workorderNo = row.workorderNo ?: woHint,
            productName = row.productName,
            status = row.workorderStatus,
            qtyPlanned = row.qtyPlanned,
            qtyCompleted = row.qtyCompleted,
            progressPercent = pct,
            alertMessage = row.alertMessage ?: row.alertType,
            found = true,
        )
    }

    private fun matchesWorkorder(row: com.btsheng.erp.core.network.ProductionDashboardRow, hint: String): Boolean {
        val a = row.workorderNo?.trim().orEmpty()
        val b = hint.trim()
        if (a.isEmpty() || b.isEmpty()) return false
        return a.equals(b, ignoreCase = true) ||
            a.contains(b, ignoreCase = true) ||
            b.contains(a, ignoreCase = true)
    }
}
