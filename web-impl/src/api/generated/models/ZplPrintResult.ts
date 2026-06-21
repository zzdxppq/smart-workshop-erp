/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * ZPL/TSPL 直连打印响应
 */
export type ZplPrintResult = {
    printLogId?: number;
    logNo?: string;
    bytesSent?: number;
    latencyMs?: number;
    protocol?: 'ZPL' | 'TSPL';
};

