/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DrawingPermissionBits } from './DrawingPermissionBits';
/**
 * 图纸权限位 DTO（permission 端点返回）
 */
export type DrawingPermissionDTO = {
    drawingId?: number;
    /**
     * 用户主角色
     */
    role?: 'ENGINEER' | 'PROD_PLANNER' | 'SALES' | 'PURCHASER' | 'WAREHOUSE' | 'QC' | 'OPERATOR' | 'FINANCE';
    /**
     * 权限 scope（与金额 ACL 独立命名空间）
     */
    scope?: 'ALL' | 'ORDER' | 'PO' | 'INCOMING' | 'INSPECTION' | 'WORKORDER_PROCESS' | 'NONE';
    permissions?: DrawingPermissionBits;
    /**
     * 关联业务单据 ID 列表（按 biz_type 分桶）
     */
    linkedBizIds?: {
        ORDER?: Array<number>;
        PO?: Array<number>;
        INCOMING?: Array<number>;
        INSPECTION?: Array<number>;
        WORKORDER_PROCESS?: Array<number>;
    };
    /**
     * 权限查询结果缓存过期时间（5 分钟）
     */
    expiresAt?: string;
};

