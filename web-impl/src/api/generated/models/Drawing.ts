/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 图纸主表 · 4 状态机（DRAFT/RELEASED/ARCHIVED/OBSOLETE）
 */
export type Drawing = {
    id?: number;
    /**
     * 图号 DWG-YYYYMMDD-NNNN · 唯一索引（P1 修补）
     */
    drawingNo?: string;
    /**
     * 版本 v1 < v2 < v3 严格递增（P1 修补）
     */
    version?: string;
    title?: string;
    /**
     * 物料编码 WL-XXXX 唯一索引
     */
    materialCode?: string;
    /**
     * 工艺路线 JSON（5 段成本聚合 · V1.3.4 留 1.9 BOM）
     */
    processRoute?: string;
    status?: 'DRAFT' | 'RELEASED' | 'ARCHIVED' | 'OBSOLETE';
    pdfPath?: string;
    /**
     * 签字扫描件路径（AES-256-GCM 加密存储 · V1.3.6 红线）
     */
    signatureScanPath?: string;
    /**
     * 加密标记（P1 修补 · AES-256-GCM）
     */
    isEncrypted?: 0 | 1;
    /**
     * FA 件 · > 20万 二次密码
     */
    isFa?: 0 | 1;
    isNew?: 0 | 1;
    ownerUserId?: number;
    deptId?: number;
    createdAt?: string;
    updatedAt?: string;
};

