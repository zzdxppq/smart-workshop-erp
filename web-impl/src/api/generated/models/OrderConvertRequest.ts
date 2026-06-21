/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 1.5→1.6 转订单请求（继承 1.5 quantityAdjustment 字段）
 */
export type OrderConvertRequest = {
    /**
     * V1.3.7 P2 修补 3：报价 100 → 订单 80 数量调整
     */
    quantityAdjustment?: number;
    comment?: string;
};

