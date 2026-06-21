package com.btsheng.erp.platform.auth.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 工作流类型（V1.3.7 · Story 1.2 · 4 套内置模板）
 *
 * <p>对应 {@code sys_workflow.workflow_code} 取值集合。与 init.sql:1080-1085 种子数据保持一致。
 *
 * <p>本枚举使用 {@code String} 持久化（VARCHAR(50)）而非 MySQL ENUM，
 * 遵循 architect P2 反馈 ①："workflow_type ENUM→VARCHAR + 字段兼容性更好"。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "工作流类型（4 套内置模板）")
public enum WorkflowType {

    /** 报价审批（业务员 → 部门经理 → 总经理） */
    QUOTE_FLOW("QUOTE_FLOW", "报价审批"),

    /** 订单审批（同 QUOTE_FLOW + 信用额度检查） */
    ORDER_FLOW("ORDER_FLOW", "订单审批"),

    /** 采购审批（采购员 → 销售经理 → 总经理） */
    PURCHASE_FLOW("PURCHASE_FLOW", "采购审批"),

    /** 付款审批（财务 → 总经理，双签 >10万） */
    PAYMENT_FLOW("PAYMENT_FLOW", "付款审批"),

    /** 自定义工作流（管理员通过 POST /workflows 创建） */
    CUSTOM_FLOW("CUSTOM_FLOW", "自定义工作流");

    @JsonValue
    private final String code;
    private final String description;

    WorkflowType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /** 根据 code 解析（不区分大小写） */
    public static WorkflowType fromCode(String code) {
        if (code == null) return null;
        for (WorkflowType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }

    /** 4 套内置模板（不可物理删除） */
    public static boolean isBuiltin(WorkflowType type) {
        return type == QUOTE_FLOW || type == ORDER_FLOW
                || type == PURCHASE_FLOW || type == PAYMENT_FLOW;
    }

    public boolean isBuiltin() {
        return isBuiltin(this);
    }
}
