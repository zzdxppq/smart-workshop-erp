/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApprovalRouteRequest } from '../models/ApprovalRouteRequest';
import type { ApprovalRouteResponse } from '../models/ApprovalRouteResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138ApprovalService {
    /**
     * 审批路由预览（Story 4.2）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static previewApprovalRoute(
        requestBody: ApprovalRouteRequest,
    ): CancelablePromise<(Result & {
        data?: ApprovalRouteResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approval/route-preview',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 采购主管权限查询（Story 4.2）
     * @returns any 成功
     * @throws ApiError
     */
    public static getProcurementManagerPerms(): CancelablePromise<(Result & {
        data?: Array<string>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/roles/procurement-manager-perms',
        });
    }
}
