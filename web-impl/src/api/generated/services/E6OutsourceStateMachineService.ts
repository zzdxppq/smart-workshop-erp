/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { OutsourceOrder } from '../models/OutsourceOrder';
import type { OutsourceStateAdvanceRequest } from '../models/OutsourceStateAdvanceRequest';
import type { OutsourceStateHistory } from '../models/OutsourceStateHistory';
import type { OutsourceStateRollbackRequest } from '../models/OutsourceStateRollbackRequest';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E6OutsourceStateMachineService {
    /**
     * 委外状态机推进（AC-6.2.1/AC-6.2.2 · 40904 状态守卫）
     * **V1.3.7 Story 1.22** 委外 7 状态机推进：状态守卫 40904 OUTSOURCE_STATE_INVALID
     * 生管/采购/品检/财务 分工严格分离（V1.3.7 AD-1）
     * 终态 CLOSED 不可再开
     *
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static advanceOutsourceState(
        requestBody: OutsourceStateAdvanceRequest,
    ): CancelablePromise<(Result & {
        data?: OutsourceOrder;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsource-states/advance',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40301: `操作员角色越权（AD-1）`,
                40904: `状态机不匹配（守卫拒绝）`,
            },
        });
    }
    /**
     * 委外状态机回退（含 REWORK 返修路径）
     * 回退原因必填，任意非终态 → REWORK
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static rollbackOutsourceState(
        requestBody: OutsourceStateRollbackRequest,
    ): CancelablePromise<(Result & {
        data?: OutsourceOrder;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsource-states/rollback',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `回退原因缺失`,
                40904: `终态不可再开`,
            },
        });
    }
    /**
     * 委外状态机历史（AC-6.2.3 · 100% 留痕）
     * @param outsourceId
     * @returns any 成功
     * @throws ApiError
     */
    public static getOutsourceStateHistory(
        outsourceId: number,
    ): CancelablePromise<(Result & {
        data?: Array<OutsourceStateHistory>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/outsource-states/{outsourceId}/history',
            path: {
                'outsourceId': outsourceId,
            },
        });
    }
    /**
     * 获取委外单当前状态
     * @param outsourceId
     * @returns any 成功
     * @throws ApiError
     */
    public static getOutsourceCurrentState(
        outsourceId: number,
    ): CancelablePromise<(Result & {
        data?: OutsourceOrder;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/outsource-states/{outsourceId}',
            path: {
                'outsourceId': outsourceId,
            },
        });
    }
    /**
     * 7 状态转换矩阵（OpenAPI 元数据）
     * 7 状态：DRAFT/SUBMITTED/ACCEPTED/IN_PROGRESS/COMPLETED/REWORK + 终态 CLOSED
     * @returns any 成功
     * @throws ApiError
     */
    public static getOutsourceStateMatrix(): CancelablePromise<(Result & {
        data?: {
            states?: Array<string>;
            transitions?: Record<string, any>;
            terminalStates?: Array<string>;
            operatorRoles?: Record<string, any>;
        };
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/outsource-states/matrix',
        });
    }
}
