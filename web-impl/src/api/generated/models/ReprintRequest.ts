/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 补打请求（12.4 replayPrintLog 输入）
 */
export type ReprintRequest = {
    /**
     * 同模式 / 换模式 / SAME（沿用原模式）
     */
    targetMode?: 'ZPL_DIRECT' | 'PDF_BROWSER' | 'SAME';
    /**
     * targetMode=ZPL_DIRECT 时必填
     */
    printerId?: number | null;
    remark?: string;
};

