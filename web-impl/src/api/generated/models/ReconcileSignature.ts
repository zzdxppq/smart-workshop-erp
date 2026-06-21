/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type ReconcileSignature = {
    id?: number;
    reconcileId?: number;
    signerUserId?: number;
    signerName?: string;
    signatureImagePath?: string;
    /**
     * AES-256-GCM 加密 Base64
     */
    encryptedData?: string;
    /**
     * 12 字节 IV 唯一（Base64）
     */
    iv?: string;
    /**
     * 128-bit GCM tag（Base64）
     */
    authTag?: string;
    signedAt?: string;
};

