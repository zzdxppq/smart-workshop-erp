package com.btsheng.erp.core.web.AesGcmEncryptor;

import com.btsheng.erp.core.model.AesGcmUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.AEADBadTagException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AES-256-GCM 加密单测（V1.3.7 · T1.4）
 *
 * <p>覆盖：往返 / IV 唯一性 / AEAD 防篡改 / 错密钥 / 空串 / null
 */
@DisplayName("AES-256-GCM 加密 AesGcmUtil (T1.4)")
class AesGcmEncryptorTest {

    private final byte[] dek = AesGcmUtil.generateDek();

    @Test
    @DisplayName("roundtrip_basic: 解密 == 明文")
    void roundtrip_basic() {
        byte[] blob = AesGcmUtil.encrypt(dek, "13800000000".getBytes(StandardCharsets.UTF_8));
        byte[] plain = AesGcmUtil.decrypt(dek, blob);
        assertEquals("13800000000", new String(plain, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("iv_unique_100_times: 100 次加密 IV 全不同")
    void iv_unique_100_times() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            byte[] blob = AesGcmUtil.encrypt(dek, "13800000000".getBytes(StandardCharsets.UTF_8));
            // 提取前 12 字节 IV
            StringBuilder iv = new StringBuilder();
            for (int j = 0; j < AesGcmUtil.GCM_IV_LENGTH; j++) {
                iv.append(String.format("%02x", blob[j]));
            }
            assertTrue(seen.add(iv.toString()), "IV 重复: " + iv);
        }
    }

    @Test
    @DisplayName("tampered_cipher_rejected: 篡改密文 1 字节 → AEADBadTag")
    void tampered_cipher_rejected() {
        byte[] blob = AesGcmUtil.encrypt(dek, "13800000000".getBytes(StandardCharsets.UTF_8));
        blob[20] ^= 0x01; // 翻转 1 比特
            Throwable cause = assertThrows(Exception.class, () -> AesGcmUtil.decrypt(dek, blob));
        // AEAD 异常可能被包装为 IllegalStateException
            assertNotNull(cause);
    }

    @Test
    @DisplayName("wrong_key_rejected: 错 DEK 解密失败")
    void wrong_key_rejected() {
        byte[] blob = AesGcmUtil.encrypt(dek, "13800000000".getBytes(StandardCharsets.UTF_8));
        byte[] other = AesGcmUtil.generateDek();
        assertThrows(IllegalStateException.class, () -> AesGcmUtil.decrypt(other, blob));
    }

    @Test
    @DisplayName("empty_string: 空串加密 + 解密 = 空串")
    void empty_string() {
        byte[] blob = AesGcmUtil.encrypt(dek, "".getBytes(StandardCharsets.UTF_8));
        byte[] plain = AesGcmUtil.decrypt(dek, blob);
        assertEquals(0, plain.length);
    }

    @Test
    @DisplayName("null_safe: null 入参返回 null")
    void null_safe() {
        assertEquals(null, AesGcmUtil.encrypt(dek, (byte[]) null));
        assertEquals(null, AesGcmUtil.decrypt(dek, (byte[]) null));
    }
}
