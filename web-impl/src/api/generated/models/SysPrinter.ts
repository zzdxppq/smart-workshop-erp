/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 打印机配置模型（V1.3.9 Sprint 12 Story 12.2 · sys_printer 表）
 */
export type SysPrinter = {
    id?: number;
    /**
     * 唯一名称 · 1-50 字符
     */
    name: string;
    /**
     * 普通（NORMAL · OS 打印队列） / 标签（LABEL · Socket 9100）
     */
    type: 'NORMAL' | 'LABEL';
    /**
     * IPv4 · LABEL 必填 · NORMAL 可空
     */
    ip?: string | null;
    port?: number;
    /**
     * 协议 · 由设备型号决定
     */
    protocol?: 'ZPL' | 'TSPL' | 'PDF_BROWSER';
    modelSuggestion?: 'DELI_DL888B' | 'ZEBRA_ZD420' | 'TSC_TTP244PRO' | 'OTHER';
    enabled?: 0 | 1;
    /**
     * 心跳状态
     */
    status?: 'ONLINE' | 'OFFLINE' | 'UNKNOWN';
    /**
     * 连续失败次数 · 达 2 标 OFFLINE
     */
    failCount?: number;
    lastHeartbeatAt?: string | null;
    createdAt?: string;
    updatedAt?: string;
    tenantId?: number;
};

