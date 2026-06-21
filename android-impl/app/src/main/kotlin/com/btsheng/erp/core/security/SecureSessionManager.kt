package com.btsheng.erp.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 登录会话与凭据加密持久化（AC-1.4.1 · EncryptedSharedPreferences AES-256-GCM）
 */
object SecureSessionManager {

    private const val PREFS = "erp_secure_session"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_REAL_NAME = "real_name"
    private const val KEY_ROLES = "roles"
    private const val KEY_TOKEN_CREATED_AT = "token_created_at"
    private const val KEY_REMEMBER_PASSWORD = "remember_password"
    private const val KEY_SAVED_PASSWORD = "saved_password"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    private const val KEY_PUSH_ENABLED = "push_enabled"

    /** access_token 有效期 2h（与 backend Story 1.1 对齐） */
    private const val ACCESS_TOKEN_TTL_MS = 2L * 60 * 60 * 1000

    @Volatile
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (_: Exception) {
            TokenStore.keystoreAvailable = false
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
        restoreSession()
    }

    private fun p(): SharedPreferences = prefs ?: error("SecureSessionManager not initialized")

    fun restoreSession(): Boolean {
        val token = p().getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return false
        if (!isTokenValid()) return false
        val userId = p().getLong(KEY_USER_ID, 0L)
        if (userId <= 0L) return false
        val username = p().getString(KEY_USERNAME, "") ?: ""
        val realName = p().getString(KEY_REAL_NAME, null)
        val roles = p().getString(KEY_ROLES, "")?.split(',')?.filter { it.isNotBlank() }.orEmpty()
        val refreshToken = p().getString(KEY_REFRESH_TOKEN, "") ?: ""
        val createdAt = p().getLong(KEY_TOKEN_CREATED_AT, System.currentTimeMillis())
        SessionStore.session = SessionStore.UserSession(userId, username, realName, roles, token)
        TokenStore.save(
            TokenStore.Token(
                accessToken = token,
                refreshToken = refreshToken,
                jti = "",
                userId = userId,
                createdAt = createdAt,
            ),
        )
        return true
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: Long,
        username: String,
        realName: String?,
        roles: List<String>,
    ) {
        val createdAt = System.currentTimeMillis()
        p().edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .putString(KEY_REAL_NAME, realName)
            .putString(KEY_ROLES, roles.joinToString(","))
            .putLong(KEY_TOKEN_CREATED_AT, createdAt)
            .apply()
        SessionStore.session = SessionStore.UserSession(userId, username, realName, roles, accessToken)
        TokenStore.save(
            TokenStore.Token(
                accessToken = accessToken,
                refreshToken = refreshToken,
                jti = "",
                userId = userId,
                createdAt = createdAt,
            ),
        )
    }

    fun clearSession() {
        p().edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_REAL_NAME)
            .remove(KEY_ROLES)
            .remove(KEY_TOKEN_CREATED_AT)
            .apply()
        SessionStore.session = null
        TokenStore.clear()
    }

    fun clearAll() {
        val remember = isRememberPassword()
        val username = savedUsername()
        val password = savedPassword()
        val biometric = isBiometricEnabled()
        p().edit().clear().apply()
        if (remember) {
            setRememberPassword(true, username, password)
            setBiometricEnabled(biometric)
        }
        SessionStore.clear()
    }

    fun isTokenValid(): Boolean {
        val token = p().getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() } ?: return false
        val createdAt = p().getLong(KEY_TOKEN_CREATED_AT, 0L)
        return token.isNotBlank() && System.currentTimeMillis() - createdAt < ACCESS_TOKEN_TTL_MS
    }

    fun setRememberPassword(enabled: Boolean, username: String = "", password: String = "") {
        p().edit()
            .putBoolean(KEY_REMEMBER_PASSWORD, enabled)
            .putString(KEY_USERNAME, if (enabled) username else p().getString(KEY_USERNAME, ""))
            .putString(KEY_SAVED_PASSWORD, if (enabled) password else null)
            .apply()
    }

    fun isRememberPassword(): Boolean = p().getBoolean(KEY_REMEMBER_PASSWORD, false)

    fun savedUsername(): String = p().getString(KEY_USERNAME, "") ?: ""

    fun savedPassword(): String = p().getString(KEY_SAVED_PASSWORD, "") ?: ""

    fun setBiometricEnabled(enabled: Boolean) {
        p().edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = p().getBoolean(KEY_BIOMETRIC_ENABLED, false)

    fun setPushEnabled(enabled: Boolean) {
        p().edit().putBoolean(KEY_PUSH_ENABLED, enabled).apply()
    }

    fun isPushEnabled(): Boolean = p().getBoolean(KEY_PUSH_ENABLED, true)

    fun updateRoles(roles: List<String>) {
        p().edit().putString(KEY_ROLES, roles.joinToString(",")).apply()
    }
}
