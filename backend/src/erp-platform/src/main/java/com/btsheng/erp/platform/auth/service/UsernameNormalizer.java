package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 用户名归一化（V1.3.7 · BR-1）
 *
 * <p>规则：{@code ^[A-Za-z0-9_]{3,20}$}；统一小写比较。应用层不区分大小写，DB 仍存原值。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class UsernameNormalizer {

    public static final int MIN_LENGTH = 3;
    public static final int MAX_LENGTH = 20;

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    private UsernameNormalizer() {
    }

    /**
     * 校验 + 归一化为小写。
     */
    public static String normalize(String username) {
        if (username == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "用户名仅支持 3-20 位字母数字下划线");
        }
        if (!PATTERN.matcher(username).matches()) {
            throw new BizException(Result.CODE_PARAM_MISSING, "用户名仅支持 3-20 位字母数字下划线");
        }
        return username.toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String username) {
        return username != null && PATTERN.matcher(username).matches();
    }
}
