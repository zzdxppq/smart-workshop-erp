/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 图纸关联业务单据列表响应（13.3 端点 1）
 */
export type DrawingLinkListResponse = {
    /**
     * 图纸 ID
     */
    drawingId?: number;
    /**
     * 业务类型
     */
    bizType?: 'ORDER' | 'PO' | 'INCOMING' | 'INSPECTION' | 'WORKORDER_PROCESS';
    /**
     * 关联业务单据 ID 列表
     */
    bizIds?: Array<number>;
    /**
     * 总数
     */
    totalCount?: number;
    /**
     * 数据来源
     */
    querySource?: 'DB_REAL' | 'CACHE';
    /**
     * 查询时间
     */
    queriedAt?: string;
};

