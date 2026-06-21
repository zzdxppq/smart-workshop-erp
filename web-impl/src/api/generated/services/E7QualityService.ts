/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { InspectionCreateRequest } from '../models/InspectionCreateRequest';
import type { InspectionResponse } from '../models/InspectionResponse';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E7QualityService {
    /**
     * 创建检验单
     * @returns any 成功
     * @throws ApiError
     */
    public static createInspection(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/inspections',
        });
    }
    /**
     * 创建品质检验单（V1.3.9 Sprint 13.1 · 补 InspectionDTO schema）
     * @param requestBody
     * @returns InspectionResponse 检验单创建成功
     * @throws ApiError
     */
    public static createInspectionV1389(
        requestBody: InspectionCreateRequest,
    ): CancelablePromise<InspectionResponse> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/quality/inspections',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `字段校验失败（code: 40001 INVALID_FIELD · 字段名 details.field）`,
                403: `无 QC 权限（40301 QC_REQUIRED）`,
                409: `物料码已被检验（40901 INSPECTION_DUPLICATE）`,
            },
        });
    }
    /**
     * FA 双签（品检 + 工程师）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static faSign(
        id: number,
        requestBody?: {
            inspectorId?: number;
            engineerId?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/inspections/fa/{id}/sign',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 不良品返工
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static reworkDefect(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/defects/{id}/rework',
            path: {
                'id': id,
            },
        });
    }
}
