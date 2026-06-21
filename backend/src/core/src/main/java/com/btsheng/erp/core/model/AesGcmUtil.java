package com.btsheng.erp.core.model;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * AES-256-GCM 加解密（V1.3.6/V1.3.7 字段级加密核心）
 *
 * <p><b>安全红线</b>：DEK 必须 32 字节，由甲方 IT 独立保管（{@code /etc/erp/dek.key}）。
 * IV 12 字节每次随机，输出格式：{@code [12B IV][密文+16B GCM Tag]}。
 * 密文篡改、密钥错误均抛 {@link AEADBadTagException}（AEAD 完整性保证）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public final class AesGcmUtil {

    /** AES-256 块大小 = 32 字节 */
    public static final int DEK_LENGTH = 32;

    /** GCM 推荐 IV 长度（V1.3.6 标准） */
    public static final int GCM_IV_LENGTH = 12;

    /** GCM Tag 长度（bit） */
    public static final int GCM_TAG_BITS = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesGcmUtil() {
    }

    /**
     * 加密。
     *
     * @param dek   32 字节数据加密密钥
     * @param plain 明文 UTF-8 字节
     * @return [12B IV][密文+16B Tag] 二进制
     */
    public static byte[] encrypt(byte[] dek, byte[] plain) {
        validateDek(dek);
        if (plain == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(dek, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plain);
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return buffer.array();
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encrypt failed", e);
        }
    }

    public static byte[] encrypt(byte[] dek, String plain) {
        if (plain == null) {
            return null;
        }
        return encrypt(dek, plain.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密。
     *
     * @param dek   32 字节数据加密密钥
     * @param blob  [12B IV][密文+16B Tag]
     * @return 明文 UTF-8 字节
     */
    public static byte[] decrypt(byte[] dek, byte[] blob) {
        validateDek(dek);
        if (blob == null) {
            return null;
        }
        if (blob.length < GCM_IV_LENGTH + 16) {
            throw new IllegalArgumentException("AES-GCM blob too short: " + blob.length);
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(blob, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipher = new byte[blob.length - GCM_IV_LENGTH];
            System.arraycopy(blob, GCM_IV_LENGTH, cipher, 0, cipher.length);
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(dek, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(cipher);
        } catch (AEADBadTagException e) {
            throw new IllegalStateException("AES-GCM AEAD tag mismatch (tampered or wrong key)", e);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decrypt failed", e);
        }
    }

    public static String decryptToString(byte[] dek, byte[] blob) {
        byte[] bytes = decrypt(dek, blob);
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 生成 32 字节随机 DEK（仅供 dev 兜底 / 单元测试）。
     */
    public static byte[] generateDek() {
        byte[] dek = new byte[DEK_LENGTH];
        SECURE_RANDOM.nextBytes(dek);
        return dek;
    }

    private static void validateDek(byte[] dek) {
        if (dek == null) {
            throw new IllegalStateException("DEK 缺失：请在 /etc/erp/dek.key 部署 32 字节数据加密密钥 (V1.3.6 数据安全红线)");
        }
        if (dek.length != DEK_LENGTH) {
            throw new IllegalStateException("DEK 长度必须为 32 字节（实际 " + dek.length + "），请重新生成或恢复备份");
        }
    }
}
