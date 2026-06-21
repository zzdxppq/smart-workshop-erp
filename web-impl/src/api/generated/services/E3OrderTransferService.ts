/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3OrderTransferService {
    /**
     * 转生产（CONFIRMED → PRODUCING，生成 GD{yyyyMMdd}{seq:4} 工单号，Epic 5 hook）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static convertOrderToProduction(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/convert-to-production',
            path: {
                'id': id,
            },
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 转委外（生成 WW{yyyyMMdd}{seq:4} 单号，Epic 6 hook）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static convertOrderToOutsource(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/convert-to-outsource',
            path: {
                'id': id,
            },
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 发货（PRODUCING → SHIPPED/PARTIAL_SHIPPED，累计 shippedQty）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static shipOrder(
        id: number,
        requestBody?: Record<string, number>,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/ship',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40003: `ORDER_SHIP_QTY_EXCEEDED`,
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 结算（SHIPPED → SETTLED，触发回款 + Epic 9 hook）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static settleOrder(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/settle',
            path: {
                'id': id,
            },
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 关闭（SETTLED → CLOSED 终态）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static closeOrder(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/close',
            path: {
                'id': id,
            },
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
}
