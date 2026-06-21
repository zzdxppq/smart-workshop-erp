/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 标签预览请求（12.3 previewLabel 输入）
 */
export type LabelPreviewRequest = {
    type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL';
    data: {
        /**
         * 纯文本 · APP 扫码壳按前缀路由
         */
        qrContent: string;
        lines?: Array<string>;
        factoryName?: string;
    };
    format?: 'PNG' | 'PDF';
};

