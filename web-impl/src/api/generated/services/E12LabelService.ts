/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LabelPreviewRequest } from '../models/LabelPreviewRequest';
import type { LabelPreviewResponse } from '../models/LabelPreviewResponse';
import type { LabelTemplateListResponse } from '../models/LabelTemplateListResponse';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E12LabelService {
    /**
     * 查标签模板元数据（type 可选 · 不传返回 5 种）
     * @param type
     * @param tenantId
     * @returns any 模板列表（GD/LZ/SB/WW/WL · SB 由代码层 fallback 注入）
     * @throws ApiError
     */
    public static listLabelTemplates(
        type?: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL',
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: LabelTemplateListResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/label-templates',
            query: {
                'type': type,
                'tenantId': tenantId,
            },
            errors: {
                400: `type 不支持（40002）`,
            },
        });
    }
    /**
     * 生成标签预览 PNG base64（ZXing + 三区布局）
     * @param requestBody
     * @param tenantId
     * @returns any 预览生成成功（base64 PNG）
     * @throws ApiError
     */
    public static previewLabel(
        requestBody: LabelPreviewRequest,
        tenantId: number = 1,
    ): CancelablePromise<(Result & {
        data?: LabelPreviewResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/label-templates/preview',
            query: {
                'tenantId': tenantId,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `参数无效（40001 · lines 超 6 / qr_content 空）`,
                422: `qr_content 超 200 字符 · factory_name 超 20 字符（42201）`,
            },
        });
    }
}
