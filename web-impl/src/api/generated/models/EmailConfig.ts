/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type EmailConfig = {
    smtpHost?: string;
    smtpPort?: number;
    useSsl?: boolean;
    fromAddress?: string;
    /**
     * KMS 注入（不返回给前端）
     */
    authCode?: string;
    retryPolicy?: Array<string>;
    dailyQuota?: number;
    quotaWarnThreshold?: number;
    logRetentionDays?: number;
    attachmentMaxSizeMb?: number;
};

