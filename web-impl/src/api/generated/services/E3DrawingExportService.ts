/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3DrawingExportService {
    /**
     * 8. 导出 PDF（AC-3.1.4 · 含签字扫描件解密嵌入 · 1h 缓存 · AES-256-GCM P1 修补）
     * @param id
     * @param format
     * @returns binary 成功
     * @throws ApiError
     */
    public static exportDrawingPdf(
        id: number,
        format: 'pdf' | 'txt' = 'pdf',
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/export/{id}',
            path: {
                'id': id,
            },
            query: {
                'format': format,
            },
        });
    }
}
