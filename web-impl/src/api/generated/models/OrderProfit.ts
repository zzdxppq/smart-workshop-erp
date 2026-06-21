/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 订单利润分析（利润 = 订单金额 - 生产成本 - 委外成本 - 材料成本）
 */
export type OrderProfit = {
    orderId?: number;
    orderNo?: string;
    orderAmount?: number;
    productionCost?: number;
    outsourceCost?: number;
    materialCost?: number;
    totalCost?: number;
    profit?: number;
    profitRate?: string;
    /**
     * 利润 < 0 触发告警
     */
    isLoss?: boolean;
    alert?: string;
};

