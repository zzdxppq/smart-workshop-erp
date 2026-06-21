package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.Result;

/**
 * 业务异常（V1.3.7）
 *
 * <p>对应错误码 4xxxx 业务错误（参数/状态机/冲突等）。HTTP 200，由
 * {@link GlobalExceptionHandler} 统一包装为 {@code Result.fail(code, message)}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(Result<?> result) {
        super(result.getMessage());
        this.code = result.getCode();
    }

    public int getCode() {
        return code;
    }
}
