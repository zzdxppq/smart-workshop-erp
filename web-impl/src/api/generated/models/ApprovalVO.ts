/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ApprovalVO = {
    id?: number;
    bizType?: 'QUOTE' | 'ORDER' | 'PURCHASE' | 'PAYMENT' | 'OTHER';
    bizId?: string;
    workflowCode?: string;
    currentNodeIndex?: number;
    currentApproverUserId?: number;
    candidates?: Array<number>;
    /**
     * V1.3.7 P1 修补
     */
    orSignRequired?: boolean;
    status?: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SKIPPED' | 'WAITING';
    skipReason?: 'ON_LEAVE' | 'ON_TRIP' | 'DISABLED' | 'RESIGNED';
    timeoutAt?: string;
    isOverdue?: boolean;
    nodeSkipped?: boolean;
};

