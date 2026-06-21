/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * ZPL/TSPL 直连打印请求（12.4 模式一）
 */
export type ZplPrintRequest = {
    templateCode: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL';
    qrContent: string;
    lines: Array<string>;
    printerId: number;
    count: number;
    colorBarHex?: string;
    remark?: string;
};

