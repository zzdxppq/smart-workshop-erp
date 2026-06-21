/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type CostAggregate = {
    materialCode?: string;
    period?: string;
    /**
     * 材料成本
     */
    materialCost?: number;
    /**
     * 工时成本
     */
    laborCost?: number;
    /**
     * 表处成本
     */
    surfaceCost?: number;
    /**
     * 外协成本
     */
    outsourceCost?: number;
    /**
     * 管理费分摊
     */
    mfgCost?: number;
    /**
     * 总成本
     */
    totalCost?: number;
    version?: number;
    computedAt?: string;
};

