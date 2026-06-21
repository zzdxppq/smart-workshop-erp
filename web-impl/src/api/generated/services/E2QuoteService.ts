/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuoteConvertRequest } from '../models/QuoteConvertRequest';
import type { QuoteCreateRequest } from '../models/QuoteCreateRequest';
import type { QuoteUpdateRequest } from '../models/QuoteUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E2QuoteService {
    /**
     * 报价列表（6 维过滤：status/customerId/owner/dateFrom/dateTo/page/size）
     * @param pageNum
     * @param pageSize
     * @param status
     * @param customerId
     * @param owner
     * @param dateFrom
     * @param dateTo
     * @returns any 成功
     * @throws ApiError
     */
    public static listQuotes(
        pageNum: number = 1,
        pageSize: number = 20,
        status?: 'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'CONVERTED',
        customerId?: number,
        owner?: number,
        dateFrom?: string,
        dateTo?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/quotes',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'status': status,
                'customerId': customerId,
                'owner': owner,
                'dateFrom': dateFrom,
                'dateTo': dateTo,
            },
            errors: {
                40303: `无权限`,
            },
        });
    }
    /**
     * 创建报价（多级审批：< 5万 业务员；5-20万 部门经理；> 20万 总经理 + 财务总监双签）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createQuote(
        requestBody: QuoteCreateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quotes',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `字段校验失败（QUOTE_ITEMS_EMPTY/QUOTE_DELIVERY_DATE_INVALID）`,
                40902: `黑名单客户 CUSTOMER_BLACKLIST`,
            },
        });
    }
    /**
     * 查询报价详情（含历史变更记录）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getQuote(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/quotes/{id}',
            path: {
                'id': id,
            },
            errors: {
                40401: `报价不存在`,
            },
        });
    }
    /**
     * 修改报价（仅 DRAFT 状态可改）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateQuote(
        id: number,
        requestBody: QuoteUpdateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/quotes/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40903: `QUOTE_NOT_EDITABLE（SUBMITTED/APPROVED 状态不可改）`,
            },
        });
    }
    /**
     * 报价转订单（APPROVED → CONVERTED，生成 XS{yyyyMMdd}{seq:4} 订单号）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static convertQuoteToOrder(
        id: number,
        requestBody?: QuoteConvertRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quotes/{id}/convert-to-order',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `QUOTE_STATE_INVALID（非 APPROVED 状态）`,
            },
        });
    }
}
