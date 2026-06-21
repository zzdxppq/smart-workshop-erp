/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccessibleDrawing } from './AccessibleDrawing';
/**
 * 业务单据可访问图纸列表响应（13.3 端点 2 · Redis 5min 缓存）
 */
export type AccessibleDrawingListResponse = {
    /**
     * 业务类型
     */
    bizType?: string;
    /**
     * 业务单据 ID
     */
    bizId?: number;
    /**
     * 可访问图纸列表
     */
    drawings?: Array<AccessibleDrawing>;
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

