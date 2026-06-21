/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { GmSummaryResponse } from '../models/GmSummaryResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138ReportService {
    /**
     * 总经理汇总报表（Story 4.3）
     * @param period
     * @param startDate
     * @param endDate
     * @returns any 成功（仅 GM + ADMIN 可见）
     * @throws ApiError
     */
    public static getGmSummary(
        period?: 'LAST_7D' | 'LAST_30D' | 'LAST_90D' | 'CUSTOM',
        startDate?: string,
        endDate?: string,
    ): CancelablePromise<(Result & {
        data?: GmSummaryResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/reports/gm-summary',
            query: {
                'period': period,
                'start_date': startDate,
                'end_date': endDate,
            },
        });
    }
}
