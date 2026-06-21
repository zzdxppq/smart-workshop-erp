/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { StatsPeriod } from './StatsPeriod';
export type WorkflowEventStats = {
    totalCount?: number;
    byEventType?: Record<string, number>;
    byApproverRole?: Record<string, number>;
    period?: StatsPeriod;
};

