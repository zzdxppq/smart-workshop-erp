/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChangeLogEntry } from '../models/ChangeLogEntry';
import type { MaterialDetailResponse } from '../models/MaterialDetailResponse';
import type { PriceTrendPoint } from '../models/PriceTrendPoint';
import type { ProcessRouteStep } from '../models/ProcessRouteStep';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138MaterialService {
    /**
     * 料号详情聚合（Story 2.1 · 7 Tab）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getMaterialDetail(
        id: number,
    ): CancelablePromise<(Result & {
        data?: MaterialDetailResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/materials/{id}/detail',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 价格走势（Story 2.1）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getPriceHistory(
        id: number,
    ): CancelablePromise<(Result & {
        data?: Array<PriceTrendPoint>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/materials/{id}/price-history',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 工艺路线（Story 2.1）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getProcessRoute(
        id: number,
    ): CancelablePromise<(Result & {
        data?: Array<ProcessRouteStep>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/materials/{id}/process-route',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 历史变更（Story 2.1）
     * @param id
     * @param limit
     * @returns any 成功
     * @throws ApiError
     */
    public static getChangeLog(
        id: number,
        limit: number = 50,
    ): CancelablePromise<(Result & {
        data?: Array<ChangeLogEntry>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/materials/{id}/change-log',
            path: {
                'id': id,
            },
            query: {
                'limit': limit,
            },
        });
    }
}
