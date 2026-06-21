/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type Quote = {
    id?: number;
    quoteNo?: string;
    customerId?: number;
    customerName?: string;
    ownerUserId?: number;
    deptId?: number;
    currency?: string;
    deliveryDate?: string;
    /**
     * 由 items 自动计算（只读）
     */
    totalAmount?: number;
    isFa?: boolean;
    isNew?: boolean;
    status?: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CONVERTED';
    /**
     * 当前审批节点 1/2/3
     */
    currentNode?: number;
    comment?: string;
    createdAt?: string;
    updatedAt?: string;
};

