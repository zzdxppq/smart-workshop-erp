package com.btsheng.erp.core.web;

/**
 * 认证异常（V1.3.7）· HTTP 401
 *
 * <p>触发场景：未登录 / Token 过期 / 黑名单 / 账户锁定。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class AuthException extends BizException {

    public AuthException(int code, String message) {
        super(code, message);
    }
}
