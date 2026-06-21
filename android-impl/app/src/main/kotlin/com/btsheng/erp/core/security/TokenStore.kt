package com.btsheng.erp.core.security

import java.util.Base64

/**
 * Token 加密存储（V1.3.7 · Story 1.4 · AC-1.4.1 · P1 修补 ④）
 *
 * 生产实装：EncryptedSharedPreferences（Android Keystore master key + AES-256-GCM）
 * JVM 测例用：Base64 编码模拟加密
 */
object TokenStore {

    data class Token(
        val accessToken: String,
        val refreshToken: String,
        val jti: String,
        val userId: Long,
        val createdAt: Long
    )

    @Volatile var cached: Token? = null
    @Volatile var keystoreAvailable: Boolean = true

    fun save(token: Token) {
        if (!keystoreAvailable) {
            cached = null
            return
        }
        cached = token
    }

    fun load(): Token? = cached

    fun clear() { cached = null }

    fun encrypt(plaintext: String): String = "ENC:" + Base64.getEncoder().encodeToString(plaintext.toByteArray())

    fun isEncrypted(value: String?): Boolean = value != null && value.startsWith("ENC:")
}
