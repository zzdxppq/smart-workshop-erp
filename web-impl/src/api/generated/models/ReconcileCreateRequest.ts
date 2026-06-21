/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ReconcileItem } from './ReconcileItem';
export type ReconcileCreateRequest = {
    vendorId: number;
    vendorName: string;
    periodYear: number;
    periodMonth: number;
    items?: Array<ReconcileItem>;
};

