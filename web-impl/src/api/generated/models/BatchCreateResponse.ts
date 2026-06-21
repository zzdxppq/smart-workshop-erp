/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BatchInfo } from './BatchInfo';
export type BatchCreateResponse = {
    batches?: Array<BatchInfo>;
    poStatusAfter?: 'PENDING_SHIP' | 'PARTIAL_ARRIVED' | 'ALL_ARRIVED' | 'CANCELLED';
    qualityOrders?: Array<string>;
};

