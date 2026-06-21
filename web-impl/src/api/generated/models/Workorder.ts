/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type Workorder = {
    id?: number;
    workorderNo?: string;
    orderId?: number;
    productCode?: string;
    qty?: number;
    planStart?: string;
    planEnd?: string;
    status?: 'DRAFT' | 'PENDING_SCHEDULE' | 'SCHEDULED' | 'IN_PROGRESS' | 'REPORTED' | 'INSPECTED' | 'STORED' | 'CLOSED' | 'CANCELLED';
    machineId?: number;
};

