/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * A4 PDF 打印请求（12.4 模式二 · 3×9=27 标签/页）
 */
export type PdfA4PrintRequest = {
    items: Array<{
        templateCode: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL';
        qrContent: string;
        lines: Array<string>;
        colorBarHex?: string;
    }>;
    remark?: string;
};

