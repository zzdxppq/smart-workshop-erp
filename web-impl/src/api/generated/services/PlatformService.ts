/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class PlatformService {
    /**
     * 文件上传（含 AES-256-GCM 加密）
     * @param formData
     * @returns any 成功
     * @throws ApiError
     */
    public static uploadFile(
        formData: {
            file?: Blob;
            bucket?: 'drawings' | 'contracts' | 'reports' | 'signed_scan';
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/platform/files/upload',
            formData: formData,
            mediaType: 'multipart/form-data',
        });
    }
    /**
     * 文件下载（签字件需 3 角色 + 审计 · V1.3.6）
     * @param id
     * @returns binary 成功
     * @throws ApiError
     */
    public static downloadFile(
        id: number,
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/platform/files/{id}/download',
            path: {
                'id': id,
            },
            errors: {
                403: `无权限（签字件仅 3 角色可下载）'`,
            },
        });
    }
    /**
     * 在线预览（签名 URL 5 分钟过期）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static previewFile(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/platform/files/{id}/preview',
            path: {
                'id': id,
            },
        });
    }
}
