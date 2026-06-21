/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type Vendor = {
    id?: number;
    vendorCode?: string;
    vendorName: string;
    capabilities?: Array<string>;
    creditLevel?: 'A' | 'B' | 'C' | 'D';
    /**
     * V1.3.7 仍必填（163 邮箱推送）
     */
    contactEmail: string;
    /**
     * V1.3.7 改选填（短信下线后非关键路径）
     */
    contactPhone?: string;
    /**
     * V1.3.7 收窄为单 163 邮箱
     */
    notifyChannel?: 'email_163';
    /**
     * V1.3.6 对账单邮箱
     */
    defaultReconEmail?: string;
    /**
     * V1.3.4 近 3 次中位数
     */
    avgDeliveryDays?: number;
};

