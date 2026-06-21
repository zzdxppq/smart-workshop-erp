/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { MaterialBarcodeParseResponse } from '../models/MaterialBarcodeParseResponse';
import type { MaterialBarcodeRequest } from '../models/MaterialBarcodeRequest';
import type { MaterialBarcodeResponse } from '../models/MaterialBarcodeResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138MaterialBarcodeService {
    /**
     * 复合物料码生成（Story 3.2）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static generateMaterialBarcode(
        requestBody: MaterialBarcodeRequest,
    ): CancelablePromise<(Result & {
        data?: MaterialBarcodeResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/material-barcode/generate',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 物料码解析 APP 扫码（Story 3.2）
     * @param barcode
     * @returns any 成功
     * @throws ApiError
     */
    public static parseMaterialBarcode(
        barcode: string,
    ): CancelablePromise<(Result & {
        data?: MaterialBarcodeParseResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/material-barcode/parse',
            query: {
                'barcode': barcode,
            },
        });
    }
}
