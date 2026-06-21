/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { PageResponsePrintLog } from '../models/PageResponsePrintLog';
import type { PdfA4PrintRequest } from '../models/PdfA4PrintRequest';
import type { PrintLogResponse } from '../models/PrintLogResponse';
import type { PrintStatisticsBucket } from '../models/PrintStatisticsBucket';
import type { ReprintRequest } from '../models/ReprintRequest';
import type { Result } from '../models/Result';
import type { ZplPrintRequest } from '../models/ZplPrintRequest';
import type { ZplPrintResult } from '../models/ZplPrintResult';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E12PrintService {
    /**
     * 模式一 ZPL/TSPL 直连打印（Socket 9100 · 3s 硬性超时）
     * @param requestBody
     * @returns any ZPL 发送成功 · 异步 · logNo 即时返回
     * @throws ApiError
     */
    public static printLabelsZpl(
        requestBody: ZplPrintRequest,
    ): CancelablePromise<(Result & {
        data?: ZplPrintResult;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/print/labels/zpl',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `50201 PRINTER_OFFLINE · 50202 PROTOCOL_UNSUPPORTED`,
                422: `40001/40003 参数校验失败`,
            },
        });
    }
    /**
     * 模式二 A4 PDF（3×9=27 标签/页 · base64 返回）
     * @param requestBody
     * @returns any PDF 生成成功 · 含 base64 + printLogId
     * @throws ApiError
     */
    public static printLabelsPdfA4(
        requestBody: PdfA4PrintRequest,
    ): CancelablePromise<(Result & {
        data?: {
            printLogId?: number;
            logNo?: string;
            pdfBase64?: string;
            bytes?: number;
            contentType?: string;
            filename?: string;
        };
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/print/labels/pdf-a4',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                422: `items 超过 30 → 40003`,
            },
        });
    }
    /**
     * 打印历史查询（分页 + 多维过滤）
     * @param codeType
     * @param mode
     * @param status
     * @param operatorId
     * @param codeValue
     * @param dateFrom
     * @param dateTo
     * @param page
     * @param size
     * @param tenantId
     * @returns any 历史分页
     * @throws ApiError
     */
    public static listPrintLogs(
        codeType?: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL' | 'DRAWING',
        mode?: 'ZPL_DIRECT' | 'PDF_BROWSER',
        status?: 'SUCCESS' | 'FAILED' | 'PENDING',
        operatorId?: number,
        codeValue?: string,
        dateFrom?: string,
        dateTo?: string,
        page: number = 1,
        size: number = 20,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: PageResponsePrintLog;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/print/logs',
            query: {
                'codeType': codeType,
                'mode': mode,
                'status': status,
                'operatorId': operatorId,
                'codeValue': codeValue,
                'dateFrom': dateFrom,
                'dateTo': dateTo,
                'page': page,
                'size': size,
                'tenantId': tenantId,
            },
        });
    }
    /**
     * 最近打印记录（按用户 · limit N · 默认 20）
     * @param limit
     * @param tenantId
     * @returns any 最近打印列表
     * @throws ApiError
     */
    public static listRecentPrintLogs(
        limit: number = 20,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: Array<PrintLogResponse>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/print/logs/recent',
            query: {
                'limit': limit,
                'tenantId': tenantId,
            },
        });
    }
    /**
     * 单条打印日志详情
     * @param id
     * @param tenantId
     * @returns any 单条详情
     * @throws ApiError
     */
    public static getPrintLog(
        id: number,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: PrintLogResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/print/logs/{id}',
            path: {
                'id': id,
            },
            query: {
                'tenantId': tenantId,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
    /**
     * 补打（同模式/换模式）· 防 reference 递归
     * @param id
     * @param requestBody
     * @returns Result 补打成功 · 新 sys_print_log reference_log_id=id
     * @throws ApiError
     */
    public static replayPrintLog(
        id: number,
        requestBody?: ReprintRequest,
    ): CancelablePromise<Result> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/print/logs/{id}/replay',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                409: `40954 PRINT_REPLAY_FORBIDDEN · FAILED 不可补打 / 补打记录不可再补打`,
            },
        });
    }
    /**
     * 打印统计（groupBy 聚合）
     * @param groupBy
     * @param dateFrom
     * @param dateTo
     * @param tenantId
     * @returns any 聚合结果
     * @throws ApiError
     */
    public static printStatistics(
        groupBy: 'month' | 'day' | 'operator_id' | 'code_type' | 'mode' = 'month',
        dateFrom?: string,
        dateTo?: string,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: Array<PrintStatisticsBucket>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/print/statistics',
            query: {
                'groupBy': groupBy,
                'dateFrom': dateFrom,
                'dateTo': dateTo,
                'tenantId': tenantId,
            },
        });
    }
}
