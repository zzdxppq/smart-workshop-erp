/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ApprovalCreateRequest = {
    bizType: 'QUOTE' | 'ORDER' | 'PURCHASE' | 'PAYMENT' | 'OTHER';
    bizId: string;
    amount: number;
    applicantUserId: number;
    /**
     * 可选，默认按 bizType 选
     */
    workflowCode?: string;
    comment?: string;
};

