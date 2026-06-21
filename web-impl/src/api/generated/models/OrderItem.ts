/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 订单明细（crm_order_item，含 quantityAdjustment hook）
 */
export type OrderItem = {
    id?: number;
    orderId?: number;
    drawingNo?: string;
    material?: string;
    spec?: string;
    quantity?: number;
    unitPrice?: number;
    amount?: number;
    /**
     * V1.3.7 数量调整（来自 1.5 hook）
     */
    quantityAdjustment?: number;
    isFa?: boolean;
    isNew?: boolean;
    sort?: number;
    /**
     * 已生产数量
     */
    producedQty?: number;
    /**
     * 已发货数量（累计）
     */
    shippedQty?: number;
};

