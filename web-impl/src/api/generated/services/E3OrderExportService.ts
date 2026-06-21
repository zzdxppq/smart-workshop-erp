/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3OrderExportService {
    /**
     * 导出 PDF/Excel（PDF 1h 缓存，OpenPDF 1.3.34 / POI 5.2.5）
     * @param id
     * @param format
     * @returns binary 二进制流
     * @throws ApiError
     */
    public static exportOrder(
        id: number,
        format: 'pdf' | 'excel' = 'pdf',
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/orders/export/{id}',
            path: {
                'id': id,
            },
            query: {
                'format': format,
            },
            errors: {
                40401: `ORDER_NOT_FOUND`,
            },
        });
    }
    /**
     * 订单利润分析（利润 = 订单金额 - 生产成本 - 委外成本 - 材料成本，负利润告警）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static orderProfit(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/orders/{id}/profit',
            path: {
                'id': id,
            },
            errors: {
                40401: `ORDER_NOT_FOUND`,
            },
        });
    }
    /**
     * 订单利润分析 PDF 导出
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static exportOrderProfit(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/orders/{id}/profit/export',
            path: {
                'id': id,
            },
        });
    }
}
