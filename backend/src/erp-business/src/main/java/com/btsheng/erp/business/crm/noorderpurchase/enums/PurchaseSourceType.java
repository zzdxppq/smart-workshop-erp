package com.btsheng.erp.business.crm.noorderpurchase.enums;

/**
 * V1.3.8 · Story 4.1 · 采购来源类型枚举
 *
 * <p>对应 sys_dict PURCHASE_REASON 的 dict_type + 与 crm_purchase_order.source_type 枚举一致。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
public enum PurchaseSourceType {
    FROM_ORDER("FROM_ORDER", "订单转 PO"),
    FROM_MRP("FROM_MRP", "MRP 触发"),
    NO_ORDER("NO_ORDER", "无订单采购");

    private final String code;
    private final String name;

    PurchaseSourceType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() { return code; }
    public String getName() { return name; }

    public static PurchaseSourceType fromCode(String code) {
        for (PurchaseSourceType t : values()) {
            if (t.code.equals(code)) return t;
        }
        throw new IllegalArgumentException("Unknown PurchaseSourceType: " + code);
    }
}