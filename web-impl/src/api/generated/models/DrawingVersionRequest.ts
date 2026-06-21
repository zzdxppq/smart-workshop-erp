/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type DrawingVersionRequest = {
    /**
     * 版本号 v\d+ 严格递增
     */
    version: string;
    changeReason?: string;
    pdfPath?: string;
    signatureScanPath?: string;
};

