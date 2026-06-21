package com.btsheng.erp.business.crm.noorderpurchase.enums;

/**
 * V1.3.8 · Story 4.1 · 采购理由枚举（仅 NO_ORDER 模式必填）
 *
 * <p>对应 sys_dict PURCHASE_REASON 4 项：
 * <ul>
 *   <li>URGENT_REPLENISH 紧急补料（生产中途物料损坏）</li>
 *   <li>CUSTOMER_ADD     客户加单（口头/微信下单）</li>
 *   <li>STOCK_SWAP       库存置换（同行互换）</li>
 *   <li>OTHER            其他</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
public enum PurchaseReason {
    URGENT_REPLENISH("URGENT_REPLENISH", "紧急补料", "red"),
    CUSTOMER_ADD("CUSTOMER_ADD", "客户加单", "orange"),
    STOCK_SWAP("STOCK_SWAP", "库存置换", "blue"),
    OTHER("OTHER", "其他", "gray");

    private final String code;
    private final String name;
    private final String color;

    PurchaseReason(String code, String name, String color) {
        this.code = code;
        this.name = name;
        this.color = color;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getColor() { return color; }

    public static PurchaseReason fromCode(String code) {
        for (PurchaseReason r : values()) {
            if (r.code.equals(code)) return r;
        }
        return null;
    }
}