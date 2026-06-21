/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { OrderCancelRequest } from '../models/OrderCancelRequest';
import type { OrderConfirmRequest } from '../models/OrderConfirmRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3OrderFlowService {
    /**
     * 确认订单（DRAFT → CONFIRMED，触发 4 通道推送）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static confirmOrder(
        id: number,
        requestBody?: OrderConfirmRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/confirm',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 审批通过（CONFIRMED → PRODUCING，4 阈值路由复用 1.5）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static approveOrder(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/approve',
            path: {
                'id': id,
            },
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 驳回（CONFIRMED → DRAFT，含 reason 必填）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static rejectOrder(
        id: number,
        requestBody: {
            /**
             * 驳回原因
             */
            reason?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/reject',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
    /**
     * 取消订单（→ CANCELLED）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static cancelOrder(
        id: number,
        requestBody: OrderCancelRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders/{id}/cancel',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `ORDER_STATE_INVALID`,
            },
        });
    }
}
