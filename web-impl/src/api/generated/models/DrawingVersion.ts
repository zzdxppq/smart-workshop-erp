/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 图纸版本历史
 */
export type DrawingVersion = {
    id?: number;
    drawingId?: number;
    version?: string;
    pdfPath?: string;
    signatureScanPath?: string;
    isEncrypted?: 0 | 1;
    changeReason?: string;
    changedBy?: number;
    changedAt?: string;
};

