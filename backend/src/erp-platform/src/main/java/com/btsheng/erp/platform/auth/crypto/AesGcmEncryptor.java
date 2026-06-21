package com.btsheng.erp.platform.auth.crypto;

import com.btsheng.erp.core.web.DekLoader;
import com.btsheng.erp.core.model.AesGcmUtil;

import java.nio.charset.StandardCharsets;

/**
 * AES-256-GCM 加密器（V1.3.6/V1.3.7 字段级加密门面）
 *
 * <p>门面模式：业务代码调 {@link #encrypt(String)} 即可，DEK 由 {@link DekLoader} 启动加载。
 * 输出格式：{@code [12B IV][密文+16B Tag]}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class AesGcmEncryptor {

    private AesGcmEncryptor() {
    }

    public static byte[] encrypt(String plain) {
        if (plain == null) return null;
        return AesGcmUtil.encrypt(DekLoader.requireDek(), plain.getBytes(StandardCharsets.UTF_8));
    }

    public static String decryptToString(byte[] blob) {
        if (blob == null) return null;
        return AesGcmUtil.decryptToString(DekLoader.requireDek(), blob);
    }

    public static byte[] decrypt(byte[] blob) {
        if (blob == null) return null;
        return AesGcmUtil.decrypt(DekLoader.requireDek(), blob);
    }
}
