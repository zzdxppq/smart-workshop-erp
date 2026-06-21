/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Result } from '../models/Result';
import type { Rework } from '../models/Rework';
import type { ReworkAlert } from '../models/ReworkAlert';
import type { ReworkCreateRequest } from '../models/ReworkCreateRequest';
import type { ReworkHistory } from '../models/ReworkHistory';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E6ReworkService {
    /**
     * 创建返修单（AC-6.3.1 · 返修次数 ≤ 3）
     * **V1.3.7 Story 1.23** 委外返修闭环
     * **3 P1 修补**：
     * - 返修次数 ≤ 3（40905 REWORK_COUNT_EXCEED_MAX_3）
     * - 返修原因必填
     * - 返修成本计入月度对账
     *
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createRework(
        requestBody: ReworkCreateRequest,
    ): CancelablePromise<(Result & {
        data?: Rework;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reworks',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `返修原因缺失 / 成本非负`,
                40404: `委外单不存在`,
                40905: `返修次数超过 3 次`,
            },
        });
    }
    /**
     * 完成返修（AC-6.3.2）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static finishRework(
        id: number,
    ): CancelablePromise<(Result & {
        data?: Rework;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reworks/{id}/finish',
            path: {
                'id': id,
            },
            errors: {
                40903: `返修已完成/已取消`,
            },
        });
    }
    /**
     * 返修历史（AC-6.3.2 · 完整时间线）
     * @param outsourceId
     * @returns any 成功
     * @throws ApiError
     */
    public static getReworkHistory(
        outsourceId: number,
    ): CancelablePromise<(Result & {
        data?: Array<ReworkHistory>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/reworks/{outsourceId}/history',
            path: {
                'outsourceId': outsourceId,
            },
        });
    }
    /**
     * 返修次数预警（4 级别：INFO/WARN/CRITICAL/EXCEED）
     * **4 级别预警阈值**：
     * - 0-1 次 → INFO（首次返修）
     * - 2 次 → WARN（接近上限）
     * - 3 次 → CRITICAL（已达上限）
     * - > 3 次 → EXCEED（超限）
     *
     * @param outsourceId
     * @returns any 成功
     * @throws ApiError
     */
    public static getReworkAlert(
        outsourceId: number,
    ): CancelablePromise<(Result & {
        data?: {
            outsourceId?: number;
            outsourceNo?: string;
            reworkCount?: number;
            maxAllowed?: number;
            level?: 'INFO' | 'WARN' | 'CRITICAL' | 'EXCEED';
            message?: string;
            history?: Array<ReworkAlert>;
        };
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/reworks/{outsourceId}/alert',
            path: {
                'outsourceId': outsourceId,
            },
        });
    }
}
