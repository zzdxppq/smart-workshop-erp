/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ProcessAllocation = {
    id?: number;
    workorderId: number;
    processSeq: number;
    decision: 'INHOUSE' | 'OUTSOURCE';
    /**
     * 生管 ID
     */
    decidedByUserId: number;
    decidedAt?: string;
};

