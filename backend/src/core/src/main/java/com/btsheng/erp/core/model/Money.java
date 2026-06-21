package com.btsheng.erp.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额工具类（V1.3.7）
 *
 * <p><b>业务红线</b>：所有金额运算必须用 {@link Money}，禁止 double / float。
 * 统一 {@code scale = 2}，HALF_UP 四舍五入，比较一律用 {@code compareTo}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public final class Money {

    /** 默认金额精度（人民币分位） */
    public static final int DEFAULT_SCALE = 2;

    private Money() {
    }

    /**
     * 构造带默认精度的 BigDecimal（避免科学计数法）。
     */
    public static BigDecimal of(String amount) {
        if (amount == null || amount.isEmpty()) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE);
        }
        return new BigDecimal(amount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(0).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE);
        }
        return BigDecimal.valueOf(amount).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 安全加法（null 视为 0）。
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal x = a == null ? BigDecimal.ZERO : a;
        BigDecimal y = b == null ? BigDecimal.ZERO : b;
        return x.add(y).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 安全减法。
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal x = a == null ? BigDecimal.ZERO : a;
        BigDecimal y = b == null ? BigDecimal.ZERO : b;
        return x.subtract(y).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 安全乘法。
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE);
        }
        return a.multiply(b).setScale(DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 安全除法（被除数为 0 返回 0）。
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        if (a == null || b == null || b.signum() == 0) {
            return BigDecimal.ZERO.setScale(DEFAULT_SCALE);
        }
        return a.divide(b, DEFAULT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 大于（基于 compareTo，不依赖 equals 的 scale 差异）。
     */
    public static boolean gt(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) > 0;
    }

    public static boolean gte(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) >= 0;
    }

    public static boolean lt(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) < 0;
    }

    public static boolean lte(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) <= 0;
    }

    public static boolean eq(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return Objects.equals(a, b);
        }
        return a.compareTo(b) == 0;
    }

    /**
     * 判断是否为非负数（>= 0）。
     */
    public static boolean isNonNegative(BigDecimal a) {
        return a != null && a.signum() >= 0;
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.compareTo(b) >= 0 ? a : b;
    }
}
