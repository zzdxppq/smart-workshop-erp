/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type DrawingCreateRequest = {
    /**
     * 图号（自动生成）
     */
    drawingNo?: string;
    title: string;
    materialCode: string;
    /**
     * 工艺路线 JSON（至少 1 工序）
     */
    processRoute: string;
    isFa?: 0 | 1;
    isNew?: 0 | 1;
    pdfPath?: string;
    signatureScanPath?: string;
    comment?: string;
};

