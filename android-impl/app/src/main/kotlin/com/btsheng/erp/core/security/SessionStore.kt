package com.btsheng.erp.core.security

/**
 * 登录会话（角色 RBAC · 与 Web roleAccess 对齐）
 */
object SessionStore {

    data class UserSession(
        val userId: Long,
        val username: String,
        val realName: String?,
        val roles: List<String>,
        val accessToken: String,
    )

    @Volatile
    var session: UserSession? = null

    fun roles(): List<String> = session?.roles.orEmpty()

    fun displayName(): String =
        session?.realName?.takeIf { it.isNotBlank() } ?: session?.username.orEmpty()

    fun clear() {
        session = null
        TokenStore.clear()
    }

    fun saveFromLogin(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        username: String,
        realName: String?,
        roles: List<String>,
    ) {
        SecureSessionManager.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            username = username,
            realName = realName,
            roles = roles,
        )
    }

    fun logout() {
        SecureSessionManager.clearSession()
    }
}
