package com.btsheng.erp.util

/**
 * V1.3.9 Sprint 14 Story 13.6 · 7 角色 + FINANCE 测试账号矩阵
 *
 * <p>对应 Story 13.6 §4.1 测试账号矩阵表。
 * <p>8 测试账号：ENGINEER(1001) / PROD_PLANNER(1002) / SALES(1003) / PURCHASER(1004) /
 * WAREHOUSE(1005) / QC(1006) / OPERATOR(1007) / FINANCE(1008)
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
object TestAccount {

    data class Account(
        val role: String,
        val loginUserId: String,
        val password: String,
        val displayName: String,
        val expectedScope: String
    )

    val ENGINEER = Account(
        role = "ENGINEER",
        loginUserId = "engineer_1001",
        password = "Test@123",
        displayName = "张工程师",
        expectedScope = "GLOBAL"   // 5 全 · 图纸预览/打印/下载/上传/删除
    )

    val PROD_PLANNER = Account(
        role = "PROD_PLANNER",
        loginUserId = "prod_planner_1002",
        password = "Test@123",
        displayName = "李计划员",
        expectedScope = "WORKORDER"   // 2 全 · 图纸预览/打印
    )

    val SALES = Account(
        role = "SALES",
        loginUserId = "sales_1003",
        password = "Test@123",
        displayName = "王销售",
        expectedScope = "ORDER"   // 2 关联 · 订单图纸
    )

    val PURCHASER = Account(
        role = "PURCHASER",
        loginUserId = "purchaser_1004",
        password = "Test@123",
        displayName = "赵采购",
        expectedScope = "PO"   // 2 关联 · PO 图纸
    )

    val WAREHOUSE = Account(
        role = "WAREHOUSE",
        loginUserId = "warehouse_1005",
        password = "Test@123",
        displayName = "陈仓管",
        expectedScope = "INCOMING"   // 2 关联 · 入库单图纸
    )

    val QC = Account(
        role = "QC",
        loginUserId = "qc_1006",
        password = "Test@123",
        displayName = "周质检",
        expectedScope = "INSPECTION"   // 2 关联 · 质检单图纸
    )

    val OPERATOR = Account(
        role = "OPERATOR",
        loginUserId = "operator_1007",
        password = "Test@123",
        displayName = "吴操作工",
        expectedScope = "WORKORDER_PROCESS"   // 1 关联 · 当前工序图纸
    )

    val FINANCE = Account(
        role = "FINANCE",
        loginUserId = "finance_1008",
        password = "Test@123",
        displayName = "郑财务",
        expectedScope = "NONE"   // 0 · 无图纸权限
    )

    /** 8 角色全量（顺序：业务 7 角色 + 验证 FINANCE） */
    val ALL_ACCOUNTS: List<Account> = listOf(
        ENGINEER, PROD_PLANNER, SALES, PURCHASER, WAREHOUSE, QC, OPERATOR, FINANCE
    )
}
