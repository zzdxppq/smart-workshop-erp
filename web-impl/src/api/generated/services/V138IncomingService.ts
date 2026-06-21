/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BatchCreateRequest } from '../models/BatchCreateRequest';
import type { BatchCreateResponse } from '../models/BatchCreateResponse';
import type { PoStatusResponse } from '../models/PoStatusResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138IncomingService {
    /**
     * 按物料粒度批次到货（Story 3.1）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createIncomingBatch(
        requestBody: BatchCreateRequest,
    ): CancelablePromise<(Result & {
        data?: BatchCreateResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/incoming/batch-create',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 按物料粒度 PO 状态查询（Story 3.1）
     * @param poId
     * @returns any 成功
     * @throws ApiError
     */
    public static getPoStatus(
        poId: number,
    ): CancelablePromise<(Result & {
        data?: PoStatusResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/incoming/po-status/{poId}',
            path: {
                'poId': poId,
            },
        });
    }
}
