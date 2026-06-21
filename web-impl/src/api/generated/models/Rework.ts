/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 委外返修单（crm_rework）· 4 状态机：DRAFT/IN_PROGRESS/COMPLETED/CANCELLED
 */
export type Rework = {
    id?: number;
    /**
     * RW{yyyyMMdd}{seq:4}
     */
    reworkNo?: string;
    outsourceId?: number;
    outsourceNo?: string;
    /**
     * 返修原因（必填 · P1 修补 2）
     */
    reason?: string;
    /**
     * 返修成本（≥ 0 · 计入月度对账 · P1 修补 3）
     */
    cost?: number;
    /**
     * 本次返修次序
     */
    reworkCount?: number;
    status?: 'DRAFT' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
    expectedFinishDate?: string;
    finishedAt?: string;
    createdBy?: number;
    createdAt?: string;
    updatedAt?: string;
};

