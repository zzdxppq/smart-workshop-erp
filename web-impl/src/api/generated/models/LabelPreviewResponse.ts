/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 标签预览响应（base64 PNG / PDF）
 */
export type LabelPreviewResponse = {
    type?: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL';
    format?: string;
    /**
     * data:image/png;base64,...
     */
    base64?: string;
    contentType?: string;
    /**
     * PNG 字节数 · < 5120 (5KB)
     */
    sizeBytes?: number;
    renderedAt?: string;
};

