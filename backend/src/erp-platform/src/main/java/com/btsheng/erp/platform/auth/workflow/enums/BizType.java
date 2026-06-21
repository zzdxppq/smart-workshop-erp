package com.btsheng.erp.platform.auth.workflow.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 业务类型（V1.3.7 · Story 1.2 · sys_approval_record.biz_type）
 *
 * <p>与下游业务模块的工单类型对齐（QUOTE 销售 / ORDER 销售 / PURCHASE 采购 / PAYMENT 财务）。
 * {@code OTHER} 作为兜底类型，便于业务侧扩展。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "业务类型（决定走哪套工作流模板）")
public enum BizType {

    QUOTE("QUOTE", "报价单", "QUOTE_FLOW"),
    ORDER("ORDER", "订单", "ORDER_FLOW"),
    PURCHASE("PURCHASE", "采购单", "PURCHASE_FLOW"),
    PAYMENT("PAYMENT", "付款单", "PAYMENT_FLOW"),
    OTHER("OTHER", "其他", "CUSTOM_FLOW");

    @JsonValue
    private final String code;
    private final String description;
    private final String defaultWorkflowCode;

    BizType(String code, String description, String defaultWorkflowCode) {
        this.code = code;
        this.description = description;
        this.defaultWorkflowCode = defaultWorkflowCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultWorkflowCode() {
        return defaultWorkflowCode;
    }

    public static BizType fromCode(String code) {
        if (code == null) return null;
        for (BizType t : values()) {
            if (t.code.equalsIgnoreCase(code)) {
                return t;
            }
        }
        return null;
    }
}
