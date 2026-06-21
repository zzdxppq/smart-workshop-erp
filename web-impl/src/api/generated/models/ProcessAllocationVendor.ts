/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ProcessAllocationVendor = {
    id?: number;
    allocationId?: number;
    /**
     * 采购选
     */
    vendorId?: number;
    unitPrice?: number;
    deliveryDate?: string;
    /**
     * 采购 ID
     */
    selectedByUserId?: number;
    selectedAt?: string;
    status?: 'PENDING' | 'CONFIRMED' | 'REJECTED';
};

