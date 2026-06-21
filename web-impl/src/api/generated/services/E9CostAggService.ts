/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CostAggregate } from '../models/CostAggregate';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E9CostAggService {
    /**
     * 料号 5 段成本（V1.3.4）
     * 返回材料/工时/表处/外协/总成本 5 段；权限隔离（管理层看全量）
     * @param materialCode
     * @param period
     * @param customerId
     * @returns any 成功
     * @throws ApiError
     */
    public static getCostAggregate(
        materialCode: string,
        period?: string,
        customerId?: number,
    ): CancelablePromise<(Result & {
        data?: CostAggregate;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/cost-aggregator/{materialCode}',
            path: {
                'materialCode': materialCode,
            },
            query: {
                'period': period,
                'customerId': customerId,
            },
        });
    }
    /**
     * 导出料号成本（Excel / PDF）
     * @param materialCode
     * @param format
     * @returns binary 成功
     * @throws ApiError
     */
    public static exportCostAggregate(
        materialCode: string,
        format: 'xlsx' | 'pdf' = 'xlsx',
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/cost-aggregator/{materialCode}/export',
            path: {
                'materialCode': materialCode,
            },
            query: {
                'format': format,
            },
        });
    }
}
