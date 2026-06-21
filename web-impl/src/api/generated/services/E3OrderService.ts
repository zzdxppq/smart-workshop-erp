/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { OrderCreateRequest } from '../models/OrderCreateRequest';
import type { OrderUpdateRequest } from '../models/OrderUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3OrderService {
    /**
     * 订单列表（按权限过滤：业务员 owner / 经理 dept / 总经理 all）
     * @param page
     * @param size
     * @param status
     * @param customerId
     * @param owner
     * @param role
     * @param deptId
     * @returns any 成功
     * @throws ApiError
     */
    public static listOrders(
        page: number = 1,
        size: number = 20,
        status?: 'DRAFT' | 'CONFIRMED' | 'PRODUCING' | 'PARTIAL_SHIPPED' | 'SHIPPED' | 'SETTLED' | 'CLOSED' | 'CANCELLED',
        customerId?: number,
        owner?: number,
        role: 'salesperson' | 'dept_manager' | 'gm' = 'gm',
        deptId?: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/orders',
            query: {
                'page': page,
                'size': size,
                'status': status,
                'customerId': customerId,
                'owner': owner,
                'role': role,
                'deptId': deptId,
            },
            errors: {
                40302: `DATA_SCOPE（越权访问）`,
            },
        });
    }
    /**
     * 创建订单（XS{yyyyMMdd}{seq:4} 单号 + 信用额度校验 40909 + 黑名单 40902）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createOrder(
        requestBody: OrderCreateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/orders',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `ORDER_CUSTOMER_REQUIRED / ORDER_ITEMS_EMPTY / ORDER_DELIVERY_DATE_INVALID`,
                40003: `ORDER_QUANTITY_INVALID / ORDER_UNIT_PRICE_INVALID / ORDER_QUANTITY_ADJUSTMENT_INVALID`,
                40902: `CUSTOMER_BLACKLIST（黑名单优先）`,
                40909: `CREDIT_LIMIT_EXCEEDED（信用额度超限）`,
            },
        });
    }
    /**
     * 查询订单详情（含状态机 + 历史 + 转生产/委外链路）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getOrder(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/orders/{id}',
            path: {
                'id': id,
            },
            errors: {
                40401: `ORDER_NOT_FOUND`,
            },
        });
    }
    /**
     * 修改订单（仅 DRAFT 状态可改）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateOrder(
        id: number,
        requestBody: OrderUpdateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/orders/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40903: `ORDER_NOT_EDITABLE（仅 DRAFT 可改）`,
            },
        });
    }
}
