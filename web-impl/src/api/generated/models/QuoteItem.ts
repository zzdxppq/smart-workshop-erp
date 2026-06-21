/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type QuoteItem = {
    id?: number;
    quoteId?: number;
    drawingNo?: string;
    material?: string;
    spec?: string;
    quantity?: number;
    unitPrice?: number;
    /**
     * = quantity * unitPrice
     */
    amount?: number;
    isFa?: boolean;
    isNew?: boolean;
    sort?: number;
};

