/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DrawingReleaseRequest } from '../models/DrawingReleaseRequest';
import type { DrawingVersion } from '../models/DrawingVersion';
import type { DrawingVersionRequest } from '../models/DrawingVersionRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3DrawingFlowService {
    /**
     * 5. 新增版本（AC-3.1.2 · v1→v2→v3 严格递增 · P1 修补）
     * @param id
     * @param operatorUserId
     * @param requestBody
     * @returns DrawingVersion 成功
     * @throws ApiError
     */
    public static addDrawingVersion(
        id: number,
        operatorUserId: number,
        requestBody: DrawingVersionRequest,
    ): CancelablePromise<DrawingVersion> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings/{id}/versions',
            path: {
                'id': id,
            },
            query: {
                'operatorUserId': operatorUserId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 6. 发布审批（AC-3.1.3 · 4 阈值 + > 20万 二次密码）
     * @param id
     * @param operatorUserId
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static releaseDrawing(
        id: number,
        operatorUserId: number,
        requestBody?: DrawingReleaseRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings/{id}/release',
            path: {
                'id': id,
            },
            query: {
                'operatorUserId': operatorUserId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                401: `ADMIN_PASSWORD_REQUIRED · > 20万 FA 件必填`,
                409: `DRAWING_STATE_INVALID / DRAWING_BLACKLIST`,
            },
        });
    }
    /**
     * 7. 归档（AC-3.1.3 · RELEASED → ARCHIVED）
     * @param id
     * @param operatorUserId
     * @returns any 成功
     * @throws ApiError
     */
    public static archiveDrawing(
        id: number,
        operatorUserId: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings/{id}/archive',
            path: {
                'id': id,
            },
            query: {
                'operatorUserId': operatorUserId,
            },
            errors: {
                409: `DRAWING_STATE_INVALID`,
            },
        });
    }
}
