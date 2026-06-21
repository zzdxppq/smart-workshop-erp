/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ReworkHistory = {
    id?: number;
    reworkId?: number;
    operation?: 'CREATE' | 'FINISH' | 'CANCEL';
    /**
     * 变更前快照
     */
    beforeJson?: string;
    /**
     * 变更后快照
     */
    afterJson?: string;
    changedBy?: number;
    changedAt?: string;
};

