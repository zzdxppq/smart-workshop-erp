/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 委外状态机迁移历史 · 100% 留痕
 */
export type OutsourceStateHistory = {
    id?: number;
    outsourceId?: number;
    outsourceNo?: string;
    fromState?: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';
    toState?: 'DRAFT' | 'SUBMITTED' | 'ACCEPTED' | 'IN_PROGRESS' | 'COMPLETED' | 'REWORK' | 'CLOSED';
    transitionType?: 'ADVANCE' | 'ROLLBACK' | 'REWORK';
    operatorUserId?: number;
    /**
     * 生管/采购/品检/财务/admin（V1.3.7 AD-1）
     */
    operatorRole?: string;
    reason?: string;
    occurredAt?: string;
};

