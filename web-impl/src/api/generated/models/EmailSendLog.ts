/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type EmailSendLog = {
    id?: number;
    toAddress?: string;
    subject?: string;
    /**
     * 附件 SHA-256
     */
    attachmentHash?: string;
    smtpResponse?: string;
    status?: 'PENDING' | 'SENT' | 'FAILED' | 'RETRY_1H' | 'RETRY_6H' | 'RETRY_24H' | 'DEAD';
    retryCount?: number;
    sentAt?: string;
};

