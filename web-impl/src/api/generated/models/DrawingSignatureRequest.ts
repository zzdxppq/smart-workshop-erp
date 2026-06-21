/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type DrawingSignatureRequest = {
    version?: string;
    signerUserId?: number;
    /**
     * 签字图片（AES-256-GCM 加密后存储）
     */
    signatureImagePath?: string;
    /**
     * GCM 初始化向量（IV 唯一）
     */
    iv?: string;
};

