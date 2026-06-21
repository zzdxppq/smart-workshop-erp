package com.btsheng.erp.core.web;

/**
 * 授权异常（V1.3.7）· HTTP 403
 *
 * <p>触发场景：无权限 / 数据范围越界 / 金额超限 / 工序分配越权（V1.3.7）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class PermException extends BizException {

    public PermException(int code, String message) {
        super(code, message);
    }
}
