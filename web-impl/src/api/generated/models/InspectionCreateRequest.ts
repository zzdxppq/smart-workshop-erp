/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { InspectionItemDTO } from './InspectionItemDTO';
export type InspectionCreateRequest = {
    /**
     * 物料码 · 必填
     */
    materialCode: string;
    inspectionType: 'INCOMING' | 'IN_PROCESS' | 'OUTGOING' | 'FA' | 'CMM';
    qualityStatus: 'PASS' | 'FAIL' | 'PENDING' | 'REWORK';
    inspectItems: Array<InspectionItemDTO>;
    remark?: string | null;
};

