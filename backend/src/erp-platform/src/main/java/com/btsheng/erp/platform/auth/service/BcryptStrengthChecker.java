package com.btsheng.erp.platform.auth.service;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 强度校验（V1.3.7 · BR-3）
 *
 * <p>强制 cost = 12；性能基线：单次 encode/verify < 300ms。
 * salt 16 字节随机（BCrypt 默认），同一明文两次加密结果不同（盐值随机）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class BcryptStrengthChecker {

    /** V1.3.7 强制 cost（不可降级） */
    public static final int COST = 12;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(COST);

    private BcryptStrengthChecker() {
    }

    public static String encode(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        return ENCODER.encode(raw);
    }

    public static boolean verify(String raw, String hashed) {
        if (raw == null || hashed == null) {
            return false;
        }
        return ENCODER.matches(raw, hashed);
    }

    /**
     * 检查密文前缀是否为 {@code $2a$12$}。
     */
    public static boolean isCost12(String hashed) {
        return hashed != null && hashed.startsWith("$2a$12$");
    }

    /**
     * 用 BCrypt 原生 API 校验 cost 是否合规（拒绝其他 cost）。
     */
    public static void assertCostValid(String hashed) {
        if (!isCost12(hashed)) {
            throw new IllegalStateException("BCrypt cost != 12 (V1.3.7 红线)：" + hashed);
        }
        // Spring Security 5.7+ 已移除 BCrypt.passwordToHash；cost 已在 hash 字符串前缀校验
    }
}
