/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type OutsourceStateAdvanceRequest = {
    /**
     * 委外单主键 ID
     */
    outsourceId: number;
    /**
     * 目标状态
     */
    targetState: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';
    /**
     * 操作员角色（V1.3.7 AD-1 生管/采购/品检/财务）
     */
    operatorRole?: string;
    /**
     * 推进原因
     */
    reason?: string;
};

