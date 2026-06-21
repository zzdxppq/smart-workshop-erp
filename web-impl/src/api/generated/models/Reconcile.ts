/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * V1.3.7 AD-2 红线：不含"线下"动作。状态机：DRAFT(1) → VENDOR_CONFIRMED(2) → BOTH_CONFIRMED(3) → FINANCE_CONFIRMED(4) → CLOSED
 */
export type Reconcile = {
    id?: number;
    /**
     * RC{yyyyMM}{seq:4}（按月隔离）
     */
    reconcileNo?: string;
    vendorId?: number;
    vendorName?: string;
    periodYear?: number;
    /**
     * 1-12
     */
    periodMonth?: number;
    totalAmount?: number;
    status?: 'DRAFT' | 'VENDOR_CONFIRMED' | 'BOTH_CONFIRMED' | 'FINANCE_CONFIRMED' | 'CLOSED';
    /**
     * 1-4 步
     */
    currentStep?: number;
    /**
     * 对账期锁定
     */
    isLocked?: boolean;
    createdBy?: number;
    createdAt?: string;
    updatedAt?: string;
};

