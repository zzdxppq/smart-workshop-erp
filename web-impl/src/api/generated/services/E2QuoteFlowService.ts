/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { QuoteApproveRequest } from '../models/QuoteApproveRequest';
import type { QuoteRejectRequest } from '../models/QuoteRejectRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E2QuoteFlowService {
    /**
     * 提交审批（DRAFT → SUBMITTED，触发 stream:notify 4 通道推送）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static submitQuote(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quotes/{id}/submit',
            path: {
                'id': id,
            },
            errors: {
                40904: `QUOTE_STATE_INVALID（状态机越权）`,
            },
        });
    }
    /**
     * 审批通过（SUBMITTED → APPROVED，OR 会签 + 跳过请假 + 二次密码 > 20万）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static approveQuote(
        id: number,
        requestBody?: QuoteApproveRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quotes/{id}/approve',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `状态机越权`,
            },
        });
    }
    /**
     * 驳回（SUBMITTED → REJECTED，含 reason）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static rejectQuote(
        id: number,
        requestBody: QuoteRejectRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quotes/{id}/reject',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `状态机越权`,
            },
        });
    }
}
