/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E4WarehouseService {
    /**
     * 生成物料码 WL-
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static generateBarcode(
        id: number,
        requestBody?: {
            batchNo?: string;
            qty?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/materials/{id}/barcodes',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 入库（APP 扫 WL-）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static stockIn(
        requestBody: {
            barcode: string;
            type: 'PURCHASE' | 'PRODUCTION' | 'OUTSOURCE' | 'OTHER';
            warehouseId: number;
            locationId?: number;
            qty: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/stock/in',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 出库（APP 扫 WL-）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static stockOut(
        requestBody: {
            barcode: string;
            type: 'ISSUE' | 'SHIP' | 'OUTSOURCE' | 'OTHER';
            warehouseId: number;
            qty: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/stock/out',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 库存查询（实时 = 在库 + 在途 - 冻结）
     * @param materialCode
     * @param warehouseId
     * @param batchNo
     * @returns any 成功
     * @throws ApiError
     */
    public static queryStock(
        materialCode?: string,
        warehouseId?: number,
        batchNo?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/stock',
            query: {
                'materialCode': materialCode,
                'warehouseId': warehouseId,
                'batchNo': batchNo,
            },
        });
    }
}
