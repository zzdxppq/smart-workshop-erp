/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { AccessibleDrawingListResponse } from '../models/AccessibleDrawingListResponse';
import type { DrawingLinkCreateRequest } from '../models/DrawingLinkCreateRequest';
import type { DrawingLinkListResponse } from '../models/DrawingLinkListResponse';
import type { OperatorProcessDrawingResponse } from '../models/OperatorProcessDrawingResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3DrawingLinkQueryService {
    /**
     * 图纸新增关联业务单据（13.3 · admin/owner）
     * @param id
     * @param requestBody
     * @returns any 关联创建成功
     * @throws ApiError
     */
    public static createDrawingLink(
        id: number,
        requestBody: DrawingLinkCreateRequest,
    ): CancelablePromise<(Result & {
        data?: DrawingLinkListResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/drawings/{id}/links',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `INVALID_BIZ_TYPE（40001）`,
                403: `40303 DRAWING_LINK_FORBIDDEN（非 admin/owner）`,
                404: `DRAWING_NOT_FOUND（40401）`,
                409: `40903 DRAWING_LINK_DUPLICATE（已关联）`,
            },
        });
    }
    /**
     * 删除图纸关联（13.3 · admin/owner）
     * @param id
     * @param linkId
     * @returns Result 删除成功
     * @throws ApiError
     */
    public static deleteDrawingLink(
        id: number,
        linkId: number,
    ): CancelablePromise<Result> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/drawings/{id}/links/{linkId}',
            path: {
                'id': id,
                'linkId': linkId,
            },
            errors: {
                403: `40303 DRAWING_LINK_FORBIDDEN`,
                404: `40401 DRAWING_LINK_NOT_FOUND`,
            },
        });
    }
    /**
     * 图纸 → 关联业务单据 ID 列表（13.3 端点 1 · 按 biz_type 过滤 · 无缓存）
     * @param id crm_drawing.id
     * @param bizType 业务类型（5 类枚举）
     * @returns any 关联业务单据列表
     * @throws ApiError
     */
    public static getDrawingLinks(
        id: number,
        bizType: 'ORDER' | 'PO' | 'INCOMING' | 'INSPECTION' | 'WORKORDER_PROCESS',
    ): CancelablePromise<(Result & {
        data?: DrawingLinkListResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/{id}/links',
            path: {
                'id': id,
            },
            query: {
                'biz_type': bizType,
            },
            errors: {
                400: `INVALID_BIZ_TYPE（40001）`,
                403: `DRAWING_FORBIDDEN（40304 · 角色与 biz_type 不匹配）`,
                404: `DRAWING_NOT_FOUND（40401）`,
            },
        });
    }
    /**
     * 业务单据 → 可访问图纸列表（13.3 端点 2 · Redis 5min 缓存）
     * @param bizType 业务类型（5 类枚举）
     * @param bizId 业务单据 ID
     * @returns any 可访问图纸列表
     * @throws ApiError
     */
    public static getAccessibleDrawings(
        bizType: 'ORDER' | 'PO' | 'INCOMING' | 'INSPECTION' | 'WORKORDER_PROCESS',
        bizId: number,
    ): CancelablePromise<(Result & {
        data?: AccessibleDrawingListResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/accessible',
            query: {
                'biz_type': bizType,
                'biz_id': bizId,
            },
            errors: {
                400: `INVALID_BIZ_TYPE（40001）`,
                403: `DRAWING_FORBIDDEN（40304）`,
                404: `BIZ_DOC_NOT_FOUND（40401）`,
            },
        });
    }
    /**
     * OPERATOR 工序 → 可访问图纸列表（13.3 端点 3 · Redis 5min 缓存 · 手机端扫码）
     * @param processId 工序 ID（crm_workorder_process.id）
     * @returns any 工序可访问图纸列表
     * @throws ApiError
     */
    public static getOperatorProcessDrawings(
        processId: number,
    ): CancelablePromise<(Result & {
        data?: OperatorProcessDrawingResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/process/{processId}',
            path: {
                'processId': processId,
            },
            errors: {
                403: `PROCESS_FORBIDDEN（40304 · 工序非本人操作）`,
                404: `PROCESS_NOT_FOUND（40402 · 工序不存在或非 IN_PROGRESS）`,
            },
        });
    }
}
