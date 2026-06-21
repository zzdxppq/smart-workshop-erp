package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/** V1.3.7 Story 1.7 · AC-3.1.4 · AES-256-GCM 加密单元测例 (4 测例 · V1.3.6 红线) */
class DrawingEncryptionTest {

    private final DrawingEncryptionService svc = new DrawingEncryptionService();

    @Test void encrypt_then_decrypt_roundtrip() {
        String plaintext = "签字扫描件内容 - 高精度连接器外壳 v1 - 张三 2026-06-12";
        String ciphertext = svc.encryptString(plaintext);
        assertNotNull(ciphertext);
        assertNotEquals(plaintext, ciphertext);
        String decrypted = svc.decryptString(ciphertext);
        assertEquals(plaintext, decrypted);
    }

    @Test void iv_is_unique_per_encryption() {
        String plaintext = "测试 IV 唯一性";
        String c1 = svc.encryptString(plaintext);
        String c2 = svc.encryptString(plaintext);
        // 相同明文两次加密应产生不同密文（IV 唯一）
            assertNotEquals(c1, c2, "AES-GCM IV 必须唯一");
    }

    @Test void wrong_key_decrypt_fails() {
        // 模拟篡改密文（GCM tag 校验）
            String plaintext = "原始签字扫描件";
        String ciphertext = svc.encryptString(plaintext);
        byte[] data = Base64.getDecoder().decode(ciphertext);
        // 翻转最后一个字节（破坏 GCM tag）
            data[data.length - 1] ^= 0x01;
        String tampered = Base64.getEncoder().encodeToString(data);
        assertThrows(RuntimeException.class, () -> svc.decryptString(tampered));
    }

    @Test void gcm_tag_length_128_bits() {
        String plaintext = "测试 GCM tag 128 位";
        String ciphertext = svc.encryptString(plaintext);
        // GCM 密文长度 = iv(12) + plaintext + tag(16) = plaintext_len + 28
            byte[] data = Base64.getDecoder().decode(ciphertext);
        int expected = plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 12 + 16;
        assertEquals(expected, data.length, "GCM 密文长度必须包含 12 字节 IV + 16 字节 tag");
    }
}
