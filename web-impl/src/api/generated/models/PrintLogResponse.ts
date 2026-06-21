/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 打印日志响应（12.4 sys_print_log · V57 Flyway）
 */
export type PrintLogResponse = {
    id?: number;
    logNo?: string;
    operatorUserId?: number;
    operatorName?: string;
    printedAt?: string;
    codeType?: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL' | 'DRAWING';
    codeValue?: string;
    copies?: number;
    printerId?: number | null;
    printerNameSnapshot?: string | null;
    printerIpSnapshot?: string | null;
    printMode?: 'ZPL_DIRECT' | 'PDF_BROWSER';
    status?: 'SUCCESS' | 'FAILED' | 'PENDING';
    errorMsg?: string | null;
    /**
     * 补打时指向原始日志 · 防递归
     */
    referenceLogId?: number | null;
    remark?: string | null;
    tenantId?: number;
};

