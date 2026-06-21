/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { InspectionItemDTO } from './InspectionItemDTO';
/**
 * 品质检验单完整 DTO（V1.3.7 1.28-1.30 + V1.3.9 Sprint 13.1 补齐）
 */
export type InspectionDTO = {
    id?: number;
    /**
     * 检验单号 · INSP-yyyyMMdd-NNNN
     */
    inspectionNo?: string;
    /**
     * 物料码 · WL-... 前缀
     */
    materialCode?: string;
    /**
     * 物料名 · 关联 mtl_material.name
     */
    materialName?: string | null;
    /**
     * 批次号 · 关联 wms_inbound.batch_no
     */
    batchNo?: string | null;
    /**
     * 检验类型 · V1.3.7 1.28 5 类
     */
    inspectionType?: 'INCOMING' | 'IN_PROCESS' | 'OUTGOING' | 'FA' | 'CMM';
    /**
     * 质检结果 · 与 Sprint 1.4 MaterialBarcodeParseResponse.qualityStatus 一致
     */
    qualityStatus?: 'PASS' | 'FAIL' | 'PENDING' | 'REWORK';
    /**
     * 检验项明细
     */
    inspectItems?: Array<InspectionItemDTO>;
    /**
     * 检验人 userId
     */
    inspectorUserId?: number;
    /**
     * 检验人姓名 · 关联 sys_user.full_name
     */
    inspectorName?: string | null;
    /**
     * 实际检验日期
     */
    inspectionDate?: string;
    /**
     * 实际检验完成时间 · nullable（进行中为空）
     */
    actualInspectionDate?: string | null;
    remark?: string | null;
    /**
     * 多租户隔离
     */
    tenantId?: number;
    createdAt?: string;
    updatedAt?: string;
};

