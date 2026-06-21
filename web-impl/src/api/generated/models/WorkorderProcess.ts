/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type WorkorderProcess = {
    id?: number;
    workorderId?: number;
    processSeq?: number;
    processCode?: string;
    qty?: number;
    /**
     * V1.3.7 新增
     */
    isOutsource?: boolean;
    /**
     * V1.3.7 生管 ID
     */
    assignedBy?: number;
    assignedAt?: string;
    status?: 'PENDING' | 'DISPATCHED' | 'IN_PROGRESS' | 'REPORTED' | 'INSPECTED' | 'STORED' | 'CLOSED';
};

