/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { GmTrendPoint } from './GmTrendPoint';
export type GmSummaryResponse = {
    period?: string;
    noOrderPoCount?: number;
    noOrderPoAmount?: number;
    urgentReplenishCount?: number;
    amountThresholdPassedRate?: number;
    procurementManagerWorkload?: number;
    outsourceCostRatio?: number;
    trendChart?: Array<GmTrendPoint>;
};

