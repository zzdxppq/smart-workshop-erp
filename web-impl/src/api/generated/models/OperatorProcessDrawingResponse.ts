/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ProcessDrawing } from './ProcessDrawing';
/**
 * OPERATOR 工序可访问图纸列表响应（13.3 端点 3 · Redis 5min 缓存）
 */
export type OperatorProcessDrawingResponse = {
    /**
     * 工序 ID
     */
    processId?: number;
    /**
     * 工序代号
     */
    processCode?: string;
    /**
     * 工序名称
     */
    processName?: string;
    /**
     * 工单 ID
     */
    workOrderId?: number;
    /**
     * 工单代号
     */
    workOrderCode?: string;
    /**
     * 工序状态
     */
    status?: 'IN_PROGRESS' | 'PENDING' | 'COMPLETED';
    /**
     * 操作工用户 ID
     */
    operatorUserId?: number;
    /**
     * 可访问图纸列表
     */
    drawings?: Array<ProcessDrawing>;
    /**
     * 总数
     */
    totalCount?: number;
    /**
     * Redis 缓存是否命中
     */
    cacheHit?: boolean;
    /**
     * 查询时间
     */
    queriedAt?: string;
};

