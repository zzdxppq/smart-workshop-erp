/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E5ScanService {
    /**
     * 扫码开工（APP 扫 GD-）
     * @param barcode
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static scanStart(
        barcode: string,
        requestBody?: {
            /**
             * 可选 SB-
             */
            machineBarcode?: string;
            operatorId?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/app/workorders/{barcode}/start',
            path: {
                'barcode': barcode,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 扫码报工（APP 扫 GD-）
     * @param barcode
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static scanReport(
        barcode: string,
        requestBody: {
            qtyDone: number;
            qtyOk: number;
            qtyScrap: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/app/workorders/{barcode}/report',
            path: {
                'barcode': barcode,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 扫码过站（APP 扫 LZ-）
     * @param barcode
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static scanTransfer(
        barcode: string,
        requestBody?: {
            workorderNo?: string;
            fromStepNo?: number;
            toStepNo?: number;
            toProcessCode?: string;
            qty?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/app/transfer/{barcode}/next',
            path: {
                'barcode': barcode,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
