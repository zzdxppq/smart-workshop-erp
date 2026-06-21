/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { NoOrderPurchaseRequest } from '../models/NoOrderPurchaseRequest';
import type { NoOrderPurchaseResponse } from '../models/NoOrderPurchaseResponse';
import type { PurchaseReason } from '../models/PurchaseReason';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138PurchaseService {
    /**
     * 无订单采购创建 PO（Story 4.1）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createNoOrderPurchase(
        requestBody: NoOrderPurchaseRequest,
    ): CancelablePromise<(Result & {
        data?: NoOrderPurchaseResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/purchase/no-order',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 采购理由字典查询（Story 4.1）
     * @returns any 成功
     * @throws ApiError
     */
    public static getPurchaseReasons(): CancelablePromise<(Result & {
        data?: Array<PurchaseReason>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/purchase/reasons',
        });
    }
}
