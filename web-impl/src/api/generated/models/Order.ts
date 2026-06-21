/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * V1.3.7 Story 1.6 订单主表（crm_order）
 */
export type Order = {
    id?: number;
    /**
     * XS+YYYYMMDD+NNNN（继承 1.5 DocNoGenerator）
     */
    orderNo?: string;
    /**
     * 来源报价ID(1.5 转单时非空)
     */
    quoteId?: number;
    customerId?: number;
    customerName?: string;
    ownerUserId?: number;
    deptId?: number;
    currency?: string;
    /**
     * 由 items 自动计算（只读）
     */
    totalAmount?: number;
    deliveryDate?: string;
    isFa?: boolean;
    isNew?: boolean;
    /**
     * V1.3.7 加急
     */
    isUrgent?: boolean;
    /**
     * V1.3.7 §附录-b 7 状态机
     */
    status?: 'DRAFT' | 'CONFIRMED' | 'PRODUCING' | 'PARTIAL_SHIPPED' | 'SHIPPED' | 'SETTLED' | 'CLOSED' | 'CANCELLED';
    /**
     * 当前审批节点 1/2/3
     */
    currentNode?: number;
    comment?: string;
    /**
     * 转生产时生成（Epic 5）
     */
    productionOrderNo?: string;
    /**
     * 转委外时生成（Epic 6）
     */
    outsourceOrderNo?: string;
    /**
     * 0=未通过, 1=通过, -1=无限制
     */
    creditLimitCheck?: number;
    isDeleted?: number;
    createdAt?: string;
    updatedAt?: string;
};

