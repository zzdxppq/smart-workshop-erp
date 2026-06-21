/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E2QuoteExportService {
    /**
     * 导出 PDF / Excel（1h 缓存 + 审计留痕）
     * @param id
     * @param format
     * @returns binary 二进制流
     * @throws ApiError
     */
    public static exportQuote(
        id: number,
        format: 'pdf' | 'excel' = 'pdf',
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/quotes/export/{id}',
            path: {
                'id': id,
            },
            query: {
                'format': format,
            },
            errors: {
                40401: `报价不存在`,
            },
        });
    }
}
