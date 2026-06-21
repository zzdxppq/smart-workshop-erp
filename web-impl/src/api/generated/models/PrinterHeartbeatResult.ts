/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 心跳测试响应（TCP Socket 2s 探活结果）
 */
export type PrinterHeartbeatResult = {
    status?: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
    /**
     * 延迟 ms
     */
    latencyMs?: number;
    protocolDetected?: 'ZPL' | 'TSPL' | 'UNKNOWN';
    /**
     * 失败时填充
     */
    error?: string | null;
    hint?: string | null;
};

