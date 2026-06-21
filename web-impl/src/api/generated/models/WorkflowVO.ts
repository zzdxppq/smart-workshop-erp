/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { WorkflowNodeVO } from './WorkflowNodeVO';
export type WorkflowVO = {
    id?: number;
    workflowCode?: string;
    nodes?: Array<WorkflowNodeVO>;
    conditionsJson?: string;
    status?: 'ACTIVE' | 'INACTIVE' | 'DELETED';
    version?: number;
};

