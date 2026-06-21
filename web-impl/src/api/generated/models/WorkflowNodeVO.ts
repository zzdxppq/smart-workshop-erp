/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type WorkflowNodeVO = {
    nodeIndex?: number;
    nodeType?: 'START' | 'APPROVAL' | 'CC' | 'END';
    roleCode?: string;
    threshold?: number;
    /**
     * V1.3.7 P1 修补 OR 会签
     */
    orSignRequired?: boolean;
};

