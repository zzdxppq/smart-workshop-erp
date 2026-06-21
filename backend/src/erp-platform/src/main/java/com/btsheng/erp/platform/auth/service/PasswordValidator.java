package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;

import java.util.regex.Pattern;

/**
 * 密码强度校验（V1.3.7 · BR-2）
 *
 * <p>规则：长度 ≥ 8 位；同时包含大小写字母 + 数字。
 * 正则：{@code ^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$}
 *
 * <p>严禁用 {@code null} 入参静默通过。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class PasswordValidator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;

    private static final Pattern PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{" + MIN_LENGTH + ",}$");

    private PasswordValidator() {
    }

    /**
     * 校验密码，失败抛 {@link BizException(40002)}。
     */
    public static void validate(String password) {
        if (password == null) {
            throw new BizException(Result.CODE_PARAM_FORMAT, "密码必须 ≥ 8 位含大小写字母+数字");
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            throw new BizException(Result.CODE_PARAM_FORMAT, "密码必须 ≥ 8 位含大小写字母+数字");
        }
        if (!PATTERN.matcher(password).matches()) {
            throw new BizException(Result.CODE_PARAM_FORMAT, "密码必须 ≥ 8 位含大小写字母+数字");
        }
    }

    /**
     * 校验密码（布尔版本，供 service 层条件分支使用）。
     */
    public static boolean isValid(String password) {
        if (password == null) return false;
        return password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH
                && PATTERN.matcher(password).matches();
    }
}
