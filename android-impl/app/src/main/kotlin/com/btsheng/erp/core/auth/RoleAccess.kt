package com.btsheng.erp.core.auth

/**
 * APP Tab / 功能 RBAC（与 web-impl roleAccess · PRD V1.3.5+ 对齐）
 *
 * 仓管：仓储 · 待办 · 消息 · 我的（无三码 Tab）
 * 操作工 / 品检 / 生管：扫码 · 待办 · 消息 · 我的
 * 采购 / 采购主管：待办 · 消息 · 我的（PC 为主 · APP 只看审批与通知）
 */
object RoleAccess {

    private val ADMIN = setOf("SYS_ADMIN", "ADMIN")

    fun normalize(roles: List<String>): Set<String> {
        val out = linkedSetOf<String>()
        for (r in roles) {
            if (r.isBlank()) continue
            out.add(r)
            when (r) {
                "SYS_ADMIN", "ADMIN" -> out.addAll(
                    listOf("WAREHOUSE", "BUYER", "PURCHASER", "PROCUREMENT_MANAGER", "QC", "OPERATOR", "PROD_MGR", "GM", "SALES"),
                )
                "BUYER" -> out.add("PURCHASER")
                "PROD_MGR" -> out.add("PRODUCTION_MANAGER")
                "SALES_MGR" -> out.add("SALES_MANAGER")
            }
        }
        return out
    }

    fun hasAny(userRoles: List<String>, required: Collection<String>): Boolean {
        val n = normalize(userRoles)
        if (n.any { it in ADMIN }) return true
        return required.any { it in n }
    }

    enum class AppTab { SCAN, WAREHOUSE, TODO, MESSAGE, PROFILE }

    /** 三码开工/报工/过站（GD/LZ/SB）— 不含仓管 */
    private fun canUseProductionScanTab(roles: List<String>) =
        hasAny(roles, listOf("OPERATOR", "PROD_MGR", "PRODUCTION_MANAGER", "QC"))

    /** 仓储扫码（WL- 出入库 · WW- 到货 · PO 分批）— 仅仓管 */
    fun canUseWarehouseTab(roles: List<String>) =
        hasAny(roles, listOf("WAREHOUSE", "WAREHOUSE_LEAD"))

    /** 采购类 APP：无扫码/仓储 · 仅待办+消息（PRD · 采购 PC 为主） */
    private fun canUseProcurementAppTabs(roles: List<String>) =
        hasAny(roles, listOf("BUYER", "PURCHASER", "PROCUREMENT_MANAGER")) &&
            !canUseWarehouseTab(roles) &&
            !canUseProductionScanTab(roles)

    fun visibleTabs(roles: List<String>): List<AppTab> {
        if (roles.isEmpty()) {
            return listOf(AppTab.SCAN, AppTab.TODO, AppTab.MESSAGE, AppTab.PROFILE)
        }
        val tabs = mutableListOf<AppTab>()
        when {
            canUseProcurementAppTabs(roles) -> { /* 仅待办+消息 */ }
            canUseWarehouseTab(roles) && !canUseProductionScanTab(roles) ->
                tabs.add(AppTab.WAREHOUSE)
            else -> {
                if (canUseProductionScanTab(roles)) tabs.add(AppTab.SCAN)
                if (canUseWarehouseTab(roles)) tabs.add(AppTab.WAREHOUSE)
            }
        }
        tabs.add(AppTab.TODO)
        tabs.add(AppTab.MESSAGE)
        tabs.add(AppTab.PROFILE)
        return tabs.distinct()
    }

    fun defaultTab(roles: List<String>): AppTab = when {
        canUseWarehouseTab(roles) && !canUseProductionScanTab(roles) -> AppTab.WAREHOUSE
        canUseProductionScanTab(roles) -> AppTab.SCAN
        else -> AppTab.TODO
    }

    fun canUseWarehouseTools(roles: List<String>) = canUseWarehouseTab(roles)

    fun canUseScanThreeCode(roles: List<String>) =
        hasAny(roles, listOf("OPERATOR", "PROD_MGR", "PRODUCTION_MANAGER"))

    fun isPcOnlyRole(roles: List<String>) =
        hasAny(roles, listOf("FINANCE", "SALES", "SALES_MGR", "ENGINEER", "HR", "GM")) &&
            !canUseWarehouseTab(roles) &&
            !canUseProductionScanTab(roles) &&
            !canUseProcurementAppTabs(roles)

    fun messageEmptyHint(roles: List<String>): String = when (AppRoleSpec.primaryRole(roles)) {
        AppRoleSpec.PrimaryRole.OPERATOR ->
            "开工提醒、报工回执、扫码记录将显示在这里"
        AppRoleSpec.PrimaryRole.WAREHOUSE ->
            "到货通知、入库回执、库存预警将显示在这里"
        AppRoleSpec.PrimaryRole.QC ->
            "待检任务、检验结果、异常上报将显示在这里"
        AppRoleSpec.PrimaryRole.PROD_MGR ->
            "工单预警、逾期提醒、异常上报将显示在这里"
        AppRoleSpec.PrimaryRole.BUYER ->
            "采购审批、到货催办、逾期提醒将显示在这里"
        AppRoleSpec.PrimaryRole.PROCUREMENT_MANAGER ->
            "审批通知、采购预警、返修预警将显示在这里"
        AppRoleSpec.PrimaryRole.OTHER ->
            "审批通知、逾期提醒、异常上报将显示在这里"
    }
}
