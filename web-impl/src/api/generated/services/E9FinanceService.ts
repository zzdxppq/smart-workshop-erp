/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E9FinanceService {
    /**
     * 应收账款
     * @returns any 成功
     * @throws ApiError
     */
    public static receivables(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/finance/receivables',
        });
    }
    /**
     * 账龄分析（30/60/90/90+）
     * @returns any 成功
     * @throws ApiError
     */
    public static aging(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/finance/aging',
        });
    }
    /**
     * 成本核算
     * @returns any 成功
     * @throws ApiError
     */
    public static costCalculate(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/cost/calculate',
        });
    }
    /**
     * 付款申请（V1.3.6 三条件触发）
     * @returns any 成功
     * @throws ApiError
     */
    public static createPayment(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/payments',
        });
    }
    /**
     * 客户利润（V1.3.3 利润率预警：< 10% 黄 / < 5% 红 / < 0 深红）
     * @returns any 成功
     * @throws ApiError
     */
    public static customerProfit(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/profit/customers',
        });
    }
}
