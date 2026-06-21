/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type OutsourceStateRollbackRequest = {
    /**
     * 委外单主键 ID
     */
    outsourceId: number;
    /**
     * 回退原因（必填）
     */
    reason: string;
    operatorRole?: string;
};

