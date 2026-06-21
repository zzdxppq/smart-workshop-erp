/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ReworkCreateRequest = {
    /**
     * 委外单主键 ID
     */
    outsourceId: number;
    /**
     * 返修原因（必填）
     */
    reason: string;
    /**
     * 返修成本（≥ 0）
     */
    cost?: number;
};

