/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 标签模板元数据（V1.3.9 Sprint 12 Story 12.3 · label_template 表）
 */
export type LabelTemplate = {
    type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL';
    name?: string;
    prefix?: string;
    /**
     * 色条 HEX
     */
    colorStrip: string;
    /**
     * SB=GD · 其他 null
     */
    reuseFrom?: string | null;
    layout?: {
        topBarH?: number;
        qrAreaH?: number;
        textAreaH?: number;
        fontSize?: number;
        qrSizePx?: number;
        qrSizeMm?: number;
        widthMm?: number;
        heightMm?: number;
        colorStripWidthMm?: number;
    };
    dpi?: 203 | 300;
    enabled?: boolean;
    qrExample?: string;
    createdAt?: string | null;
    updatedAt?: string | null;
};

