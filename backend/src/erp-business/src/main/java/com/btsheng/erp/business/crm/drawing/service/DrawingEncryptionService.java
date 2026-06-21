package com.btsheng.erp.business.crm.drawing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * V1.3.7 · Story 1.7 · AC-3.1.4 · 图纸签字扫描件加密服务
 *
 * <p><b>红线（V1.3.6 继承）</b>：所有签字扫描件必须 AES-256-GCM 加密存储
 * <br>IV 唯一：每次加密随机生成 12 字节 IV
 * <br>密钥来源：Nacos `app.crypto.master-key`（简化：从系统属性或默认 key 派生）
 * <br>认证：128-bit GCM 标签
 */
@Slf4j
@Service
public class DrawingEncryptionService {

    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;        // bits
            private static final int IV_LENGTH = 12;              // bytes
    private static final int AES_KEY_LENGTH = 256;        // bits

    /**
     * 默认主密钥（生产应从 Nacos app.crypto.master-key 拉）
     */
    private static final String DEFAULT_MASTER_KEY = "smart-workshop-erp-v137-drawing-encryption-master-key";

    /**
     * 加密字节数组（签字扫描件）
     *
     * @param plaintext 明文
     * @return [iv(12 bytes) | ciphertext(16 bytes tag) | encrypted]
     */
    public String encrypt(byte[] plaintext) {
        try {
            SecretKey key = deriveKey(DEFAULT_MASTER_KEY);
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM 加密失败：" + e.getMessage(), e);
        }
    }

    /**
     * 解密 Base64 字符串
     */
    public byte[] decrypt(String base64Ciphertext) {
        try {
            byte[] data = Base64.getDecoder().decode(base64Ciphertext);
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);
            byte[] ciphertext = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, ciphertext, 0, ciphertext.length);

            SecretKey key = deriveKey(DEFAULT_MASTER_KEY);
            Cipher cipher = Cipher.getInstance(ALGO);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES-256-GCM 解密失败：" + e.getMessage(), e);
        }
    }

    /**
     * 派生 256 位 AES 密钥（SHA-256 散列主密钥）
     */
    private SecretKey deriveKey(String masterKey) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(masterKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(hash, 0, AES_KEY_LENGTH / 8, "AES");
    }

    /**
     * 生成新的 256 位 AES 密钥（用于签字扫描件独立密钥）
     */
    public SecretKey generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(AES_KEY_LENGTH);
            return kg.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("生成 AES 密钥失败：" + e.getMessage(), e);
        }
    }

    /**
     * 加密字符串（便利方法）
     */
    public String encryptString(String plaintext) {
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密字符串（便利方法）
     */
    public String decryptString(String base64Ciphertext) {
        return new String(decrypt(base64Ciphertext), StandardCharsets.UTF_8);
    }
}
