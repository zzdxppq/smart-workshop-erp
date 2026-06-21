/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type OutsourceOrder = {
    id?: number;
    outsourceNo?: string;
    workorderId?: number;
    vendorId?: number;
    processCode?: string;
    qty?: number;
    unitPrice?: number;
    totalAmount?: number;
    /**
     * V1.3.4 升级 7 状态机
     */
    status?: 'PENDING_SHIP' | 'SHIPPING' | 'PENDING_INSPECTION' | 'INSPECTING' | 'QUALIFIED_STORAGE' | 'STORED' | 'REPAIR_REQUESTED' | 'NOTIFIED_REPAIR';
    /**
     * V1.3.4 返修次数
     */
    reworkCount?: number;
    /**
     * V1.3.4 返修关联原单
     */
    originalOutsourceOrderId?: number;
    isReworkReinspection?: boolean;
    deliveryDate?: string;
    actualDeliveryDate?: string;
};

