package com.btsheng.erp.core.auth

/**
 * 登录角色解析（演示账号 username → role_code 对齐 · 云端 DB 错位兜底）
 *
 * sys_role 存 role_code（如 OPERATOR），不是 username（operator）。
 */
object RoleResolver {

    /** 演示/联调账号：username → role_code */
    private val DEMO_USERNAME_ROLES = mapOf(
        "operator" to "OPERATOR",
        "warehouse" to "WAREHOUSE",
        "qc" to "QC",
        "prod_mgr" to "PROD_MGR",
        "buyer" to "BUYER",
        "procurement_manager" to "PROCUREMENT_MANAGER",
        "engineer" to "ENGINEER",
        "sales" to "SALES",
        "sales_mgr" to "SALES_MGR",
        "gm" to "GM",
        "finance" to "FINANCE",
        "hr" to "HR",
        "admin" to "SYS_ADMIN",
    )

    fun resolve(apiRoles: List<String>?, username: String?): List<String> {
        val user = username?.trim()?.lowercase().orEmpty()
        DEMO_USERNAME_ROLES[user]?.let { expected ->
            val fromApi = apiRoles.orEmpty().filter { it.isNotBlank() }
            if (fromApi.isEmpty() || !fromApi.contains(expected)) {
                return listOf(expected)
            }
        }
        val fromApi = apiRoles.orEmpty().filter { it.isNotBlank() }
        if (fromApi.isNotEmpty()) return fromApi
        return listOf("OPERATOR")
    }
}
