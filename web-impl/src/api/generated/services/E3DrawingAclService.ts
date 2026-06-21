/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { DrawingPermissionDTO } from '../models/DrawingPermissionDTO';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E3DrawingAclService {
    /**
     * 查询当前用户对图纸的权限位（任意角色可调 · FINANCE 也返 200 + 全 false）
     * @param id crm_drawing.id
     * @returns any 权限位返回（任意角色 · 包括 FINANCE scope=NONE）
     * @throws ApiError
     */
    public static getDrawingPermission(
        id: number,
    ): CancelablePromise<(Result & {
        data?: DrawingPermissionDTO;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/{id}/permission',
            path: {
                'id': id,
            },
            errors: {
                404: `图纸不存在`,
            },
        });
    }
    /**
     * 图纸预览（带 ACL · 自动审计 · @PreAuthorize @drawingAuthz.canView）
     * @param id crm_drawing.id
     * @param resolution
     * @returns binary PDF 流（带水印 · 防止截屏外发）
     * @throws ApiError
     */
    public static previewDrawing(
        id: number,
        resolution: 'LOW' | 'MEDIUM' | 'HIGH' = 'MEDIUM',
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/{id}/preview',
            path: {
                'id': id,
            },
            query: {
                'resolution': resolution,
            },
            errors: {
                403: `ACL 拒绝（错误码统一 40304）`,
                404: `图纸不存在`,
                410: `图纸已归档（OBSOLETE/ARCHIVED · 默认不可预览）`,
            },
        });
    }
    /**
     * 下载图纸原文件（仅 ENGINEER · @PreAuthorize @drawingAuthz.canDownload）
     * @param id crm_drawing.id
     * @returns binary 原文件流（PDF/签字扫描件 ZIP · application/octet-stream）
     * @throws ApiError
     */
    public static downloadDrawing(
        id: number,
    ): CancelablePromise<Blob> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/drawings/{id}/download',
            path: {
                'id': id,
            },
            errors: {
                403: `非 ENGINEER 角色拒绝（40304）`,
                404: `图纸不存在`,
            },
        });
    }
}
