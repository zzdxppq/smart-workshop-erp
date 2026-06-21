/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 3 P1 修补说明
 */
export type DrawingP1Fixes = {
    /**
     * 图号 + 版本 唯一复合索引 (drawing_no, version) - 数据库层兜底
     */
    uniqueDrawingNoIndex?: string;
    /**
     * 版本号严格递增校验 v1 < v2 < v3，禁止跳跃和回退
     */
    strictlyIncreasingVersion?: string;
    /**
     * AES-256-GCM 加密签字扫描件（V1.3.6 红线 · 128-bit GCM tag · IV 唯一）
     */
    aes256GcmEncryption?: string;
};

