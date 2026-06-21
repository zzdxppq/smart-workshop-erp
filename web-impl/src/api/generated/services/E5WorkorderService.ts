/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Result } from '../models/Result';
import type { Workorder } from '../models/Workorder';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E5WorkorderService {
    /**
     * 工单列表
     * @param pageNum
     * @param pageSize
     * @param status
     * @returns any 成功
     * @throws ApiError
     */
    public static listWorkorders(
        pageNum: number = 1,
        pageSize: number = 20,
        status?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/workorders',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'status': status,
            },
        });
    }
    /**
     * 创建工单
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createWorkorder(
        requestBody: Workorder,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/workorders',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 当前用户进行中的工序（E5-S6 首页）
     * @returns any 成功
     * @throws ApiError
     */
    public static getCurrentProcess(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/workorders/current-process',
        });
    }
    /**
     * 排产（Gantt 拖拽）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static scheduleWorkorder(
        id: number,
        requestBody?: {
            machineId?: number;
            planStart?: string;
            planEnd?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/workorders/{id}/schedule',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * MRP 计算
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static mrpCalculate(
        id: number,
    ): CancelablePromise<(Result & {
        data?: {
            shortageList?: Array<{
                materialCode?: string;
                requiredQty?: number;
                onHandQty?: number;
                shortageQty?: number;
            }>;
        };
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/workorders/{id}/mrp',
            path: {
                'id': id,
            },
        });
    }
}
