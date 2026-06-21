/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type NoOrderPurchaseResponse = {
    poId?: number;
    poNo?: string;
    sourceType?: 'FROM_ORDER' | 'FROM_MRP' | 'NO_ORDER';
    purchaseReason?: string;
    approvalRoute?: string;
    estimatedTotal?: number;
};

