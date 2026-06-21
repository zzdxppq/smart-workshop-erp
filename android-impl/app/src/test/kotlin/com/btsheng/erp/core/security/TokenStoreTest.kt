package com.btsheng.erp.core.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TokenStoreTest {
    @Test fun `token encrypt marker`() {
        val enc = TokenStore.encrypt("hello")
        assertTrue(TokenStore.isEncrypted(enc))
        assertTrue(enc.startsWith("ENC:"))
    }
    @Test fun `keystore unavailable fallback`() {
        TokenStore.keystoreAvailable = false
        TokenStore.cached = null
        TokenStore.save(TokenStore.Token("a", "b", "c", 10086L, 1000L))
        assertNull(TokenStore.load())
        TokenStore.keystoreAvailable = true  // reset
    }
}
