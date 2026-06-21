/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E11DashboardService {
    /**
     * 生产工作台
     * @returns any 成功
     * @throws ApiError
     */
    public static dashboardProduction(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/dashboard/production',
        });
    }
    /**
     * 委外面板（V1.3.4 7 状态机 + 返修高亮）
     * @returns any 成功
     * @throws ApiError
     */
    public static dashboardOutsource(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/dashboard/outsource',
        });
    }
    /**
     * 销售龙虎榜
     * @returns any 成功
     * @throws ApiError
     */
    public static salesRanking(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/dashboard/sales-ranking',
        });
    }
}
