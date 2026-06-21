/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ReconcileVendorConfirmRequest = {
    vendorAmounts: Array<{
        itemId: number;
        /**
         * 厂商确认金额（与 amount 一致才能通过 · 40905）
         */
        vendorAmount: number;
    }>;
};

