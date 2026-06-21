/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PageResponse } from '../models/PageResponse';
import type { PrinterAvailableResponse } from '../models/PrinterAvailableResponse';
import type { PrinterHeartbeatResult } from '../models/PrinterHeartbeatResult';
import type { Result } from '../models/Result';
import type { SysPrinter } from '../models/SysPrinter';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E12PrinterService {
    /**
     * 查打印机列表（分页 + 多维过滤）
     * @param type
     * @param status
     * @param enabled
     * @param pageNum
     * @param pageSize
     * @param tenantId
     * @returns any 打印机列表
     * @throws ApiError
     */
    public static listPrinters(
        type?: 'NORMAL' | 'LABEL',
        status?: 'ONLINE' | 'OFFLINE' | 'UNKNOWN',
        enabled?: 0 | 1,
        pageNum: number = 1,
        pageSize: number = 20,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: PageResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/printers',
            query: {
                'type': type,
                'status': status,
                'enabled': enabled,
                'pageNum': pageNum,
                'pageSize': pageSize,
                'tenantId': tenantId,
            },
            errors: {
                403: `无权限`,
            },
        });
    }
    /**
     * 新增打印机配置
     * @param requestBody
     * @param operatorUserId
     * @param tenantId
     * @returns any 创建成功
     * @throws ApiError
     */
    public static createPrinter(
        requestBody: SysPrinter,
        operatorUserId: number = 1,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: SysPrinter;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/printers',
            query: {
                'operatorUserId': operatorUserId,
                'tenantId': tenantId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `无权限`,
                409: `打印机名称已存在`,
                422: `字段校验失败（LABEL 缺 ip / 端口非法）`,
            },
        });
    }
    /**
     * 修改打印机配置
     * @param id
     * @param requestBody
     * @param operatorUserId
     * @returns any 修改成功
     * @throws ApiError
     */
    public static updatePrinter(
        id: number,
        requestBody: SysPrinter,
        operatorUserId: number = 1,
    ): CancelablePromise<(Result & {
        data?: SysPrinter;
    })> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/printers/{id}',
            path: {
                'id': id,
            },
            query: {
                'operatorUserId': operatorUserId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                403: `无权限`,
                404: `资源不存在`,
            },
        });
    }
    /**
     * 删除打印机配置
     * @param id
     * @returns Result 删除成功
     * @throws ApiError
     */
    public static deletePrinter(
        id: number,
    ): CancelablePromise<Result> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/printers/{id}',
            path: {
                'id': id,
            },
            errors: {
                403: `无权限`,
                404: `资源不存在`,
                409: `打印机已被使用 · 改为 enabled=0`,
            },
        });
    }
    /**
     * 测试打印机连接（TCP Socket 探活 · 2 秒超时）
     * @param id
     * @returns any 探活结果
     * @throws ApiError
     */
    public static testPrinterConnection(
        id: number,
    ): CancelablePromise<(Result & {
        data?: PrinterHeartbeatResult;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/printers/{id}/test',
            path: {
                'id': id,
            },
            errors: {
                403: `无权限`,
            },
        });
    }
    /**
     * 查询可用同类型打印机（前端打印入口用）
     * @param type
     * @param tenantId
     * @returns any 可用打印机列表
     * @throws ApiError
     */
    public static getAvailablePrinters(
        type: 'NORMAL' | 'LABEL',
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: PrinterAvailableResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/printers/available',
            query: {
                'type': type,
                'tenantId': tenantId,
            },
        });
    }
}
