/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 图纸关联新增请求（13.3 POST links）
 */
export type DrawingLinkCreateRequest = {
    /**
     * 业务类型 · 5 类
     */
    bizType: 'ORDER' | 'PO' | 'INBOUND' | 'INSPECTION' | 'WORKORDER_PROCESS';
    /**
     * 业务单据 ID
     */
    bizId: number;
    remark?: string | null;
};

