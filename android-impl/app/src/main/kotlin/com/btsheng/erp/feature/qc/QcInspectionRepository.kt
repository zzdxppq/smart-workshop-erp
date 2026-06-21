package com.btsheng.erp.feature.qc

import com.btsheng.erp.core.network.*
import com.btsheng.erp.core.scan.QrCodeParser
import com.btsheng.erp.core.security.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

enum class SubmitOverallResult { PASS, FAIL, CONDITIONAL }

data class InspectionSubmitParams(
    val overallResult: SubmitOverallResult,
    val disposition: String? = null,
    val defectQty: Int? = null,
    val conditionalReason: String? = null,
    val remark: String? = null,
    val rejectReason: String? = null,
)

@Singleton
class QcInspectionRepository @Inject constructor(
    private val api: QualityInspectionApi,
) {

    private fun userId(): Long = SessionStore.session?.userId ?: 1L

    suspend fun listPending(): List<InspectionListItem> {
        val resp = api.list(status = "PENDING", pageSize = 50)
        if (!resp.ok || resp.data == null) return emptyList()
        return (resp.data!!.items ?: resp.data!!.records).orEmpty()
            .filter { isPending(it.result) }
    }

    suspend fun listConcessionPending(): List<InspectionListItem> {
        val resp = api.list(status = "PENDING_APPROVAL", pageSize = 50)
        if (!resp.ok || resp.data == null) return emptyList()
        return (resp.data!!.items ?: resp.data!!.records).orEmpty()
    }

    suspend fun getDetail(id: Long): Result<InspectionDetailDto> {
        val resp = api.detail(id)
        return if (resp.ok && resp.data != null) Result.success(resp.data!!)
        else Result.failure(IllegalStateException(resp.message ?: "加载失败"))
    }

    suspend fun getConcessionApprovals(id: Long): Result<List<ConcessionApprovalDto>> {
        val resp = api.concessionApprovals(id)
        return if (resp.ok && resp.data != null) Result.success(resp.data!!)
        else Result.failure(IllegalStateException(resp.message ?: "加载审批任务失败"))
    }

    suspend fun approveConcession(
        inspectionId: Long,
        approverRole: String,
        action: String,
        comment: String?,
    ): Result<String> {
        val resp = api.approveConcession(
            inspectionId,
            ConcessionApproveRequestDto(approverRole, action, comment),
            userId(),
        )
        return if (resp.ok) {
            val label = resp.data?.get("statusLabel")?.toString()
                ?: resp.data?.get("status")?.toString()
                ?: "已更新"
            Result.success("审批完成 · $label")
        } else {
            Result.failure(IllegalStateException(resp.message ?: "审批失败"))
        }
    }

    suspend fun resolveOrCreateScan(rawCode: String): Result<Long> {
        val parsed = QrCodeParser.parse(rawCode.trim())
        val keyword = when (parsed.type) {
            QrCodeParser.TYPE_WORK_ORDER, QrCodeParser.TYPE_MATERIAL -> parsed.code ?: rawCode.trim()
            else -> rawCode.trim()
        }
        val pending = api.list(keyword = keyword, status = "PENDING", pageSize = 10)
        if (pending.ok && pending.data != null) {
            val hit = (pending.data!!.items ?: pending.data!!.records).orEmpty().firstOrNull()
            if (hit?.id != null) return Result.success(hit.id!!)
        }
        return createFromScan(parsed, keyword)
    }

    private suspend fun createFromScan(parsed: QrCodeParser.ParseResult, keyword: String): Result<Long> {
        val (type, materialCode, workOrderNo) = when (parsed.type) {
            QrCodeParser.TYPE_WORK_ORDER -> Triple("IN_PROCESS", keyword, keyword)
            QrCodeParser.TYPE_MATERIAL -> Triple("INCOMING", keyword, null)
            else -> Triple("INCOMING", keyword, null)
        }
        val items = if (type == "IN_PROCESS") QcTemplates.ipqcItems() else QcTemplates.iqcItems()
        val body = InspectionCreateRequestDto(
            materialCode = materialCode,
            inspectionType = type,
            workOrderNo = workOrderNo,
            processName = if (type == "IN_PROCESS") "现场工序" else null,
            remark = "APP 扫码创建",
            inspectItems = items.map {
                InspectionCreateItemDto(it.itemName, it.standard, "", null)
            },
        )
        return try {
            val created = api.create(body, userId())
            val id = created.inspectionId
            if (id != null) Result.success(id)
            else Result.failure(IllegalStateException("创建检验单失败"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submit(
        inspectionId: Long,
        items: List<EditableInspectionItem>,
        params: InspectionSubmitParams,
    ): Result<String> {
        val overall = params.overallResult.name
        val conclusion = when (params.overallResult) {
            SubmitOverallResult.PASS -> "PASS"
            SubmitOverallResult.FAIL -> "FAIL"
            SubmitOverallResult.CONDITIONAL -> "PASS"
        }
        val body = InspectionSubmitRequestDto(
            conclusion = conclusion,
            items = items.map {
                InspectionSubmitItemDto(
                    id = it.id,
                    itemName = it.itemName,
                    measuredValue = it.measuredValue,
                    result = if (it.passed) "PASS" else "FAIL",
                    severity = it.severity,
                    defectDesc = it.defectDesc.takeIf { d -> d.isNotBlank() },
                )
            },
            remark = params.remark,
            rejectReason = params.rejectReason,
            overallResult = overall,
            disposition = params.disposition,
            defectQty = params.defectQty,
            conditionalReason = params.conditionalReason,
        )
        val resp = api.submit(inspectionId, body, userId())
        return if (resp.ok) {
            val label = resp.data?.get("statusLabel")?.toString()
                ?: when (params.overallResult) {
                    SubmitOverallResult.PASS -> "已合格"
                    SubmitOverallResult.FAIL -> when (params.disposition) {
                        "RETURN" -> "已退货"
                        "REWORK" -> "待返工"
                        "SCRAP" -> "已报废"
                        else -> "不合格"
                    }
                    SubmitOverallResult.CONDITIONAL -> "待审批"
                }
            Result.success("检验报告已提交！状态：$label")
        } else {
            Result.failure(IllegalStateException(resp.message ?: "提交失败"))
        }
    }

    private fun isPending(result: String?): Boolean =
        result.isNullOrBlank() ||
            result.equals("DRAFT", ignoreCase = true) ||
            result.equals("PENDING", ignoreCase = true)
}

object QcTemplates {
    data class TemplateItem(val itemName: String, val standard: String)

    fun iqcItems() = listOf(
        TemplateItem("外观", "无锈蚀/无划伤"),
        TemplateItem("尺寸", "按图纸公差"),
        TemplateItem("材质", "合格证/材质书核对"),
    )

    fun ipqcItems() = listOf(
        TemplateItem("尺寸", "按图纸公差"),
        TemplateItem("粗糙度", "Ra ≤ 1.6"),
        TemplateItem("外观", "无毛刺/无划伤"),
    )
}

data class EditableInspectionItem(
    val id: Long?,
    val itemName: String,
    val standard: String,
    val measuredValue: String,
    val passed: Boolean,
    val severity: String,
    val defectDesc: String,
)

fun InspectionDetailDto.toEditableItems(): List<EditableInspectionItem> =
    items.orEmpty().map { row ->
        EditableInspectionItem(
            id = row.id,
            itemName = row.itemName.orEmpty(),
            standard = row.standard.orEmpty(),
            measuredValue = row.actual.orEmpty(),
            passed = row.result.equals("PASS", ignoreCase = true),
            severity = row.severity ?: "INFO",
            defectDesc = row.defectDesc.orEmpty(),
        )
    }

fun InspectionDetailDto.isEditable(): Boolean =
    result.isNullOrBlank() ||
        result.equals("DRAFT", ignoreCase = true) ||
        result.equals("PENDING", ignoreCase = true)
