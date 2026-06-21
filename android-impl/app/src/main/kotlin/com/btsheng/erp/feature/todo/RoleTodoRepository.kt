package com.btsheng.erp.feature.todo

import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.network.ApprovalApi
import com.btsheng.erp.core.network.E5ScanApi
import com.btsheng.erp.core.network.ProductionDashboardApi
import com.btsheng.erp.feature.qc.QcInspectionRepository
import com.btsheng.erp.core.security.SessionStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoleTodoRepository @Inject constructor(
    private val scanApi: E5ScanApi,
    private val dashboardApi: ProductionDashboardApi,
    private val qcInspectionRepository: com.btsheng.erp.feature.qc.QcInspectionRepository,
    private val approvalApi: ApprovalApi,
) {

    suspend fun load(roles: List<String>): List<TodoItem> {
        val primary = AppRoleSpec.primaryRole(roles)
        val userId = SessionStore.session?.userId ?: 1L
        return try {
            when (primary) {
                AppRoleSpec.PrimaryRole.OPERATOR -> loadOperator(userId)
                AppRoleSpec.PrimaryRole.WAREHOUSE -> loadWarehouse()
                AppRoleSpec.PrimaryRole.QC -> loadQc()
                AppRoleSpec.PrimaryRole.PROD_MGR -> loadProdMgr()
                AppRoleSpec.PrimaryRole.BUYER -> loadBuyer(userId)
                AppRoleSpec.PrimaryRole.PROCUREMENT_MANAGER -> loadProcMgr(userId)
                AppRoleSpec.PrimaryRole.OTHER -> emptyList()
            }
        } catch (_: Exception) {
            fallbackSeed(primary)
        }
    }

    private suspend fun loadOperator(userId: Long): List<TodoItem> {
        val resp = scanApi.listPending(userId)
        if (!resp.ok || resp.data == null) return fallbackSeed(AppRoleSpec.PrimaryRole.OPERATOR)
        val d = resp.data!!
        val items = mutableListOf<TodoItem>()
        d.pendingStart.orEmpty().forEach { p ->
            items += TodoItem(
                category = "开工",
                title = p.workorderNo ?: "待开工工单",
                subtitle = listOfNotNull(p.productName, p.stepName).joinToString(" · ").ifBlank { "扫码 Tab · 扫 GD- 开工" },
                action = TodoAction.SCAN_WORKORDER,
                payload = p.workorderNo,
            )
        }
        d.pendingReport.orEmpty().forEach { p ->
            items += TodoItem(
                category = "报工",
                title = p.workorderNo ?: "待报工",
                subtitle = listOfNotNull(p.stepName, p.status).joinToString(" · ").ifBlank { "扫 GD- 报工" },
                action = TodoAction.SCAN_WORKORDER,
                payload = p.workorderNo,
            )
        }
        d.pendingStation.orEmpty().forEach { p ->
            items += TodoItem(
                category = "过站",
                title = p.workorderNo ?: "待过站",
                subtitle = p.stepName ?: "扫 LZ- 过站",
                action = TodoAction.SCAN_WORKORDER,
                payload = p.workorderNo,
            )
        }
        return items.ifEmpty { fallbackSeed(AppRoleSpec.PrimaryRole.OPERATOR) }
    }

    private fun loadWarehouse(): List<TodoItem> = listOf(
        TodoItem("入库", "待入库物料", "仓储 Tab · 扫 WL- 入库", TodoAction.OPEN_WAREHOUSE),
        TodoItem("到货", "委外/采购待到货", "仓储 Tab · 扫 WW- / PO 分批", TodoAction.OPEN_WAREHOUSE),
    )

    private suspend fun loadQc(): List<TodoItem> {
        val pending = qcInspectionRepository.listPending()
        val concession = qcInspectionRepository.listConcessionPending()
        val inspectItems = pending.map { q ->
            TodoItem(
                category = when (q.type) {
                    "IQC" -> "来料检"
                    "IPQC" -> "过程检"
                    "OQC" -> "成品检"
                    else -> "检验"
                },
                title = q.inspectionNo ?: "待检单",
                subtitle = listOfNotNull(q.type, q.materialCode, q.workOrderNo).joinToString(" · "),
                action = TodoAction.INSPECTION_DETAIL,
                actionId = q.id,
            )
        }
        val approvalItems = concession.map { q ->
            TodoItem(
                category = "让步审批",
                title = q.inspectionNo ?: "待审批",
                subtitle = listOfNotNull(q.materialCode, q.type).joinToString(" · "),
                action = TodoAction.CONCESSION_APPROVAL,
                actionId = q.id,
            )
        }
        return (approvalItems + inspectItems).ifEmpty { fallbackSeed(AppRoleSpec.PrimaryRole.QC) }
    }

    private suspend fun loadProdMgr(): List<TodoItem> {
        val concession = qcInspectionRepository.listConcessionPending()
        val approvalTodos = concession.map { q ->
            TodoItem(
                category = "让步审批",
                title = q.inspectionNo ?: "待审批",
                subtitle = "生管双签 · ${q.materialCode ?: "-"}",
                action = TodoAction.CONCESSION_APPROVAL,
                actionId = q.id,
            )
        }
        val resp = dashboardApi.alerts(limit = 20)
        val alerts = if (!resp.ok || resp.data.isNullOrEmpty()) emptyList() else resp.data!!.map { a ->
            TodoItem(
                category = "预警",
                title = a.workorderNo ?: "工单预警",
                subtitle = a.alertMessage ?: a.alertType ?: a.productName.orEmpty(),
                action = TodoAction.WORKORDER_PROGRESS,
                payload = a.workorderNo,
            )
        }
        return (approvalTodos + alerts).ifEmpty { fallbackSeed(AppRoleSpec.PrimaryRole.PROD_MGR) }
    }

    private suspend fun loadBuyer(userId: Long): List<TodoItem> {
        val resp = approvalApi.myPending(applicantUserId = userId)
        if (!resp.ok || resp.data?.records.isNullOrEmpty()) return fallbackSeed(AppRoleSpec.PrimaryRole.BUYER)
        return resp.data!!.records!!.map { a ->
            TodoItem(
                category = "采购",
                title = "${a.bizType ?: "单据"} ${a.bizId ?: ""}".trim(),
                subtitle = listOfNotNull(a.status, a.reason).joinToString(" · ").ifBlank { "我发起的审批" },
                action = TodoAction.APPROVAL_DETAIL,
                actionId = a.id,
                payload = a.bizType,
            )
        }
    }

    private suspend fun loadProcMgr(userId: Long): List<TodoItem> {
        val resp = approvalApi.pending(approverUserId = userId)
        if (!resp.ok || resp.data?.records.isNullOrEmpty()) return fallbackSeed(AppRoleSpec.PrimaryRole.PROCUREMENT_MANAGER)
        return resp.data!!.records!!.map { a ->
            val overdue = if (a.isOverdue == true) " · 已逾期" else ""
            TodoItem(
                category = "审批",
                title = "${a.bizType ?: "PO/PR"} ${a.bizId ?: ""}".trim(),
                subtitle = "${a.status ?: "待审批"}$overdue",
                action = TodoAction.APPROVAL_DETAIL,
                actionId = a.id,
                payload = a.bizType,
            )
        }
    }

    private fun fallbackSeed(primary: AppRoleSpec.PrimaryRole): List<TodoItem> = when (primary) {
        AppRoleSpec.PrimaryRole.OPERATOR -> listOf(
            TodoItem("开工", "待开工工单", "扫码 Tab · 扫 GD- 开工", TodoAction.SCAN_WORKORDER),
        )
        AppRoleSpec.PrimaryRole.WAREHOUSE -> loadWarehouse()
        AppRoleSpec.PrimaryRole.QC -> listOf(
            TodoItem("检验", "待检任务", "扫码 GD-/WL- 开始录入 · 手机端提交", TodoAction.NONE),
        )
        AppRoleSpec.PrimaryRole.PROD_MGR -> listOf(
            TodoItem("预警", "工单逾期 / 机台负荷", "扫 GD- 查进度 · PC 生产工作台", TodoAction.NONE),
        )
        AppRoleSpec.PrimaryRole.BUYER -> listOf(
            TodoItem("采购", "待转单 PR / 待确认到货", "消息中心 · PC 采购模块", TodoAction.NONE),
        )
        AppRoleSpec.PrimaryRole.PROCUREMENT_MANAGER -> listOf(
            TodoItem("审批", "待审批 PO / PR", "消息中心 · PC 端审批", TodoAction.APPROVAL_DETAIL),
        )
        AppRoleSpec.PrimaryRole.OTHER -> emptyList()
    }
}

enum class TodoAction {
    NONE,
    SCAN_WORKORDER,
    INSPECTION_DETAIL,
    WORKORDER_PROGRESS,
    OPEN_WAREHOUSE,
    CONCESSION_APPROVAL,
    APPROVAL_DETAIL,
}
