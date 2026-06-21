/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Drawing } from '../models/Drawing';
import type { DrawingCreateRequest } from '../models/DrawingCreateRequest';
import type { DrawingUpdateRequest } from '../models/DrawingUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3DrawingService {
    /**
     * 1. 创建图纸（AC-3.1.1）
     * @param operatorUserId
     * @param requestBody
     * @returns Drawing 成功
     * @throws ApiError
     */
    public static createDrawing(
        operatorUserId: number,
        requestBody: DrawingCreateRequest,
    ): CancelablePromise<Drawing> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings',
            query: {
                'operatorUserId': operatorUserId,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 4. 列表查询 6 维过滤（AC-3.1.4）
     * @param keyword
     * @param version
     * @param category
     * @param customerId
     * @param isFa
     * @param status
     * @param page
     * @param size
     * @returns any 成功
     * @throws ApiError
     */
    public static listDrawings(
        keyword?: string,
        version?: string,
        category?: string,
        customerId?: number,
        isFa?: 0 | 1,
        status?: 'DRAFT' | 'RELEASED' | 'ARCHIVED' | 'OBSOLETE',
        page?: number,
        size: number = 20,
    ): CancelablePromise<{
        list?: Array<Drawing>;
        total?: number;
        page?: number;
        size?: number;
    }> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings',
            query: {
                'keyword': keyword,
                'version': version,
                'category': category,
                'customerId': customerId,
                'isFa': isFa,
                'status': status,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * 上传图纸文件（PDF / DWG / STEP · Story 10.1 fix：原 /drawings multipart 迁到子路径以避免与 createDrawing POST 冲突）
     * @param formData
     * @returns any 成功
     * @throws ApiError
     */
    public static uploadDrawing(
        formData: {
            file?: Blob;
            drawingNo?: string;
            version?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings/upload',
            formData: formData,
            mediaType: 'multipart/form-data',
        });
    }
    /**
     * 创建 BOM
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createBom(
        requestBody: {
            productCode?: string;
            bomVersion?: string;
            lines?: Array<{
                parentCode?: string;
                childCode?: string;
                qty?: number;
                lossRate?: number;
                processNo?: string;
            }>;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/boms',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 查询产品工艺路线
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getProductRoute(
        id: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/products/{id}/routes',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 创建/覆盖产品工艺路线
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createProductRoute(
        id: string,
        requestBody?: {
            changeReason?: string;
            processes?: Array<{
                processSeq?: number;
                processCode?: string;
                stdTimeMin?: number;
                isOutsource?: boolean;
            }>;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/products/{id}/routes',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 从历史产品复制工艺路线
     * @param id
     * @param srcProductId
     * @returns any 成功
     * @throws ApiError
     */
    public static copyProductRoute(
        id: string,
        srcProductId: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/products/{id}/routes/copy-from/{srcProductId}',
            path: {
                'id': id,
                'srcProductId': srcProductId,
            },
        });
    }
    /**
     * 2. 查询详情（AC-3.1.1）
     * @param id
     * @returns Drawing 成功
     * @throws ApiError
     */
    public static getDrawing(
        id: number,
    ): CancelablePromise<Drawing> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/{id}',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 3. 修改图纸（AC-3.1.1 · 仅 DRAFT 状态）
     * @param id
     * @param operatorUserId
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateDrawing(
        id: number,
        operatorUserId: number,
        requestBody: DrawingUpdateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/drawings/{id}',
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
}
