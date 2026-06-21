package com.btsheng.erp.core.auth

/**
 * APP 各登录角色 UI 边界（PRD FR-1-4 / ux-handoff §4.1 · §6.1 · §6.2）
 *
 * | 角色 | 主导航 | 扫码区 | 仓储区 | 待办 | 我的 |
 * | OPERATOR | 扫码·待办·消息·我的 | GD/LZ/SB 开工报工过站 | — | 待开工 | 扫码说明 |
 * | WAREHOUSE | 仓储·待办·消息·我的 | — | WL 入出库 · WW 到货 · PO 分批 | 待入库/到货 | 车间工具 |
 * | QC | 扫码·待办·消息·我的 | GD/WL 现场核对 | — | 待检任务 | 品检说明 |
 * | PROD_MGR | 扫码·待办·消息·我的 | GD/LZ 进度辅助 | — | 工单预警 | 生管说明 |
 * | BUYER | 待办·消息·我的 | — | — | 采购审批/到货催办 | 采购说明 |
 * | PROCUREMENT_MANAGER | 待办·消息·我的 | — | — | 审批待办 | 主管说明 |
 */
object AppRoleSpec {

    enum class PrimaryRole {
        OPERATOR,
        WAREHOUSE,
        QC,
        PROD_MGR,
        BUYER,
        PROCUREMENT_MANAGER,
        OTHER,
    }

    data class ProfileSection(
        val title: String,
        val items: List<ProfileToolItem>,
    )

    data class ProfileToolItem(
        val title: String,
        val subtitle: String,
        val kind: ProfileToolKind,
    )

    enum class ProfileToolKind {
        BATCH_INCOMING,
        OUTSOURCE_ARRIVAL,
        MATERIAL_BARCODE,
    }

    fun primaryRole(roles: List<String>): PrimaryRole {
        val n = RoleAccess.normalize(roles)
        return when {
            n.any { it in setOf("OPERATOR") } && !n.any { it in setOf("WAREHOUSE", "WAREHOUSE_LEAD") } ->
                PrimaryRole.OPERATOR
            n.any { it in setOf("WAREHOUSE", "WAREHOUSE_LEAD") } -> PrimaryRole.WAREHOUSE
            n.any { it in setOf("QC", "QUALITY") } -> PrimaryRole.QC
            n.any { it in setOf("PROD_MGR", "PRODUCTION_MANAGER") } -> PrimaryRole.PROD_MGR
            n.any { it in setOf("PROCUREMENT_MANAGER") } -> PrimaryRole.PROCUREMENT_MANAGER
            n.any { it in setOf("BUYER", "PURCHASER") } -> PrimaryRole.BUYER
            else -> PrimaryRole.OTHER
        }
    }

    fun roleDisplayName(roles: List<String>): String = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> "操作工"
        PrimaryRole.WAREHOUSE -> "仓管"
        PrimaryRole.QC -> "品检"
        PrimaryRole.PROD_MGR -> "生管"
        PrimaryRole.BUYER -> "采购"
        PrimaryRole.PROCUREMENT_MANAGER -> "采购主管"
        PrimaryRole.OTHER -> roles.firstOrNull()?.let(::codeToLabel) ?: "用户"
    }

    private fun codeToLabel(code: String) = when (code) {
        "OPERATOR" -> "操作工"
        "WAREHOUSE", "WAREHOUSE_LEAD" -> "仓管"
        "QC", "QUALITY" -> "品检"
        "PROD_MGR", "PRODUCTION_MANAGER" -> "生管"
        "BUYER", "PURCHASER" -> "采购"
        "PROCUREMENT_MANAGER" -> "采购主管"
        "ENGINEER" -> "工程师"
        "GM" -> "总经理"
        else -> code
    }

    fun scanTabTitle(roles: List<String>): String = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> "扫码开工"
        PrimaryRole.QC -> "现场核对"
        PrimaryRole.PROD_MGR -> "现场进度"
        else -> "扫码"
    }

    fun scanHints(roles: List<String>): List<String> = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> listOf(
            "扫 GD- 工单码 · 开工 / 报工",
            "扫 LZ- 流转码 · 过站",
            "扫 SB- 设备码 · 选机台",
        )
        PrimaryRole.QC -> listOf(
            "扫 GD- 工单 · 过程检 IPQC",
            "扫 WL- 物料码 · 来料检 IQC",
            "待检 / 录入 / 提交均在手机完成",
        )
        PrimaryRole.PROD_MGR -> listOf(
            "扫 GD- 工单 · 查进度",
            "扫 LZ- 流转码 · 跟踪过站",
            "逾期/负荷预警在「待办」",
        )
        else -> listOf("扫 GD- / LZ- / WL- / WW- 自动识别")
    }

    fun todoEmptyHint(roles: List<String>): String = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> "暂无待开工工单 · 扫 GD- 即可开工"
        PrimaryRole.WAREHOUSE -> "暂无待入库/到货 · 仓储 Tab 可扫码登记"
        PrimaryRole.QC -> "暂无待检任务 · 扫 WL-/GD- 可现场开单检验"
        PrimaryRole.PROD_MGR -> "暂无工单预警 · 生产看板请用 PC 端"
        PrimaryRole.BUYER -> "暂无采购待办 · 审批通知在消息中心"
        PrimaryRole.PROCUREMENT_MANAGER -> "暂无待审批单据 · 消息中心可查看"
        PrimaryRole.OTHER -> "暂无待办"
    }

    fun profileGuideTitle(roles: List<String>): String? = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> "扫码说明"
        PrimaryRole.WAREHOUSE -> null
        PrimaryRole.QC -> "品检说明"
        PrimaryRole.PROD_MGR -> "生管说明"
        PrimaryRole.BUYER -> "采购说明"
        PrimaryRole.PROCUREMENT_MANAGER -> "审批说明"
        PrimaryRole.OTHER -> null
    }

    fun profileGuideLines(roles: List<String>): List<String> = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> listOf(
            "O.1  扫 GD- 开工",
            "O.2  报工：录合格 / 报废数量",
            "O.3  扫 LZ- 过站 · 可选 SB- 机台",
        )
        PrimaryRole.QC -> listOf(
            "Q.1  待检任务在「待办」· 扫 GD-/WL- 快速开单",
            "Q.2  手机录入实测值 · 逐项合格/不合格",
            "Q.3  提交后自动触发入库/返修 · PC 可查阅报告",
        )
        PrimaryRole.PROD_MGR -> listOf(
            "P.1  待办查看逾期 / 负荷预警",
            "P.2  扫 GD- 查工单现场进度",
            "P.3  排产 / 工序分配请用 PC 生产模块",
        )
        PrimaryRole.BUYER -> listOf(
            "B.1  待办：待转单 PR / 待确认到货",
            "B.2  审批类通知在消息中心",
            "B.3  询比价 / 委外下单请用 PC 采购模块",
        )
        PrimaryRole.PROCUREMENT_MANAGER -> listOf(
            "M.1  待办：金额阈值内待审批 PO/PR",
            "M.2  消息中心：审批 / 逾期 / 返修预警",
            "M.3  汇总报表请用 PC 端",
        )
        else -> emptyList()
    }

    fun warehouseToolSections(roles: List<String>): ProfileSection? {
        if (!RoleAccess.canUseWarehouseTools(roles)) return null
        return ProfileSection(
            title = "车间工具",
            items = listOf(
                ProfileToolItem("PO 分批到货", "采购分批入库", ProfileToolKind.BATCH_INCOMING),
                ProfileToolItem("委外到货 WW-", "扫委外单码到货", ProfileToolKind.OUTSOURCE_ARRIVAL),
                ProfileToolItem("物料码扫码", "WL- 出入库", ProfileToolKind.MATERIAL_BARCODE),
            ),
        )
    }

    /** 离线 fallback 消息样例（按角色过滤类型） */
    fun sampleMessageTypes(roles: List<String>): Set<String> = when (primaryRole(roles)) {
        PrimaryRole.OPERATOR -> setOf("SCAN_RECEIPT", "OVERDUE_NOTIFY")
        PrimaryRole.WAREHOUSE -> setOf("SCAN_RECEIPT", "OVERDUE_NOTIFY", "EXCEPTION_REPORT")
        PrimaryRole.QC -> setOf("OVERDUE_NOTIFY", "EXCEPTION_REPORT", "SCAN_RECEIPT")
        PrimaryRole.PROD_MGR -> setOf("OVERDUE_NOTIFY", "EXCEPTION_REPORT", "SCAN_RECEIPT")
        PrimaryRole.BUYER, PrimaryRole.PROCUREMENT_MANAGER -> setOf("APPROVAL_NOTIFY", "OVERDUE_NOTIFY")
        PrimaryRole.OTHER -> setOf("APPROVAL_NOTIFY", "OVERDUE_NOTIFY", "SCAN_RECEIPT")
    }
}
