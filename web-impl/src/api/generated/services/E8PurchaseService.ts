/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Vendor } from '../models/Vendor';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E8PurchaseService {
    /**
     * 询价单
     * @returns any 成功
     * @throws ApiError
     */
    public static createRfq(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/rfqs',
        });
    }
    /**
     * 采购订单
     * @returns any 成功
     * @throws ApiError
     */
    public static createPurchaseOrder(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/purchase-orders',
        });
    }
    /**
     * 待到货列表
     * @returns any 成功
     * @throws ApiError
     */
    public static incomingList(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/purchase-orders/incoming',
        });
    }
    /**
     * 厂商列表
     * @returns any 成功
     * @throws ApiError
     */
    public static listVendors(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/vendors',
        });
    }
    /**
     * 创建厂商（V1.3.7 邮箱必填 + 电话选填）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createVendor(
        requestBody: Vendor,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/vendors',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `邮箱缺失（V1.3.7 仍必填）'`,
            },
        });
    }
}
