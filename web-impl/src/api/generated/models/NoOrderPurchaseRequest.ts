/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoOrderPurchaseItem } from './NoOrderPurchaseItem';
export type NoOrderPurchaseRequest = {
    purchaseReason: 'URGENT_REPLENISH' | 'CUSTOMER_ADD' | 'STOCK_SWAP' | 'OTHER';
    items: Array<NoOrderPurchaseItem>;
    supplierId: number;
    remark?: string;
};

