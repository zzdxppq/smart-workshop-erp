/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Customer } from '../models/Customer';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E2CrmService {
    /**
     * 客户列表
     * @param pageNum
     * @param pageSize
     * @param ownerId
     * @param status
     * @returns any 成功
     * @throws ApiError
     */
    public static listCustomers(
        pageNum: number = 1,
        pageSize: number = 20,
        ownerId?: number,
        status?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/customers',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'ownerId': ownerId,
                'status': status,
            },
        });
    }
    /**
     * 创建客户
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createCustomer(
        requestBody: Customer,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/customers',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 客户领用（30 天保护期）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static claimCustomer(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/customers/{id}/claim',
            path: {
                'id': id,
            },
            errors: {
                40903: `保护期内`,
            },
        });
    }
}
