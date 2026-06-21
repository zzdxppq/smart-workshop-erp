/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ReconcileItem = {
    id?: number;
    reconcileId?: number;
    outsourceOrderId?: number;
    outsourceOrderNo?: string;
    itemName?: string;
    quantity?: number;
    unitPrice?: number;
    amount?: number;
    /**
     * 厂商确认金额（AC-6.1.2）
     */
    vendorAmount?: number;
    /**
     * 最终对账金额
     */
    finalAmount?: number;
    /**
     * 金额是否一致（40905 校验）
     */
    isConsistent?: boolean;
    sort?: number;
};

