/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 图纸 ACL 错误响应（统一 40304）
 */
export type DrawingAclErrorResponse = {
    code?: number;
    /**
     * 角色定制消息：
     * - FINANCE → "FINANCE 角色无图纸权限"
     * - SALES 不关联订单 → "该图纸未关联您的订单"
     * - PURCHASER/WAREHOUSE/QC 类似
     * - OPERATOR 工序不关联 → "当前工序未关联该图纸"
     *
     */
    message?: string;
};

