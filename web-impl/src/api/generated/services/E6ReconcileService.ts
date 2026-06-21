/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Reconcile } from '../models/Reconcile';
import type { ReconcileItem } from '../models/ReconcileItem';
import type { ReconcileItemRequest } from '../models/ReconcileItemRequest';
import type { ReconcileSignature } from '../models/ReconcileSignature';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E6ReconcileService {
    /**
     * 采购生成对账单 PDF
     * **V1.3.6 改版**：不含"采购带纸去厂商处"等线下动作
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createReconcile(
        requestBody: {
            vendorId: number;
            period: string;
        },
    ): CancelablePromise<(Result & {
        data?: Reconcile;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 发送对账单邮件（163 邮箱）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static sendReconcileEmail(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/send-email',
            path: {
                'id': id,
            },
            errors: {
                50203: `163 SMTP 失败`,
                50204: `邮件额度耗尽'`,
            },
        });
    }
    /**
     * 上传厂商签字扫描件（V1.3.6 · AES-256-GCM 加密）
     * @param id
     * @param formData
     * @returns any 成功（自动 AES-256-GCM 加密）'
     * @throws ApiError
     */
    public static uploadSignedScan(
        id: number,
        formData: {
            /**
             * PDF / JPG / PNG · ≤ 10MB
             */
            file?: Blob;
            /**
             * 不能早于对账月最后一天
             */
            confirmDate?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/upload-signed-scan',
            path: {
                'id': id,
            },
            formData: formData,
            mediaType: 'multipart/form-data',
        });
    }
    /**
     * 对账已确认 → 触发付款申请（V1.3.6 三条件）
     * **触发条件**（缺一不可）：
     * 1. 对账已确认
     * 2. 签字扫描件已上传
     * 3. 双方对账金额一致
     *
     * @param id
     * @returns any 成功（生成付款申请）'
     * @throws ApiError
     */
    public static confirmReconcile(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/confirm',
            path: {
                'id': id,
            },
            errors: {
                40905: `对账金额不一致'`,
            },
        });
    }
    /**
     * 对账列表（按 vendor/period/status 过滤）
     * @param vendorId
     * @param periodYear
     * @param periodMonth
     * @param status
     * @param page
     * @param size
     * @returns any 成功
     * @throws ApiError
     */
    public static listReconciles(
        vendorId?: number,
        periodYear?: number,
        periodMonth?: number,
        status?: 'DRAFT' | 'VENDOR_CONFIRMED' | 'BOTH_CONFIRMED' | 'FINANCE_CONFIRMED' | 'CLOSED',
        page?: number,
        size: number = 20,
    ): CancelablePromise<(Result & {
        data?: {
            list?: Array<Reconcile>;
            page?: number;
            size?: number;
        };
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/reconciles/list',
            query: {
                'vendorId': vendorId,
                'periodYear': periodYear,
                'periodMonth': periodMonth,
                'status': status,
                'page': page,
                'size': size,
            },
        });
    }
    /**
     * 对账单详情（主单 + 明细 + 签字）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getReconcileDetail(
        id: number,
    ): CancelablePromise<(Result & {
        data?: {
            reconcile?: Reconcile;
            items?: Array<ReconcileItem>;
            signatures?: Array<ReconcileSignature>;
        };
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/reconciles/{id}/detail',
            path: {
                'id': id,
            },
        });
    }
    /**
     * 追加对账明细（AC-6.1.1）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static addReconcileItem(
        id: number,
        requestBody: ReconcileItemRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/items',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `参数缺失`,
                40404: `对账单不存在`,
            },
        });
    }
    /**
     * 上传厂商签字扫描件（V1.3.7 AD-2 · 不含"线下" · AES-256-GCM 加密）
     * **V1.3.7 AD-2 红线**：通过电子方式上传，不允许线下传递
     * **加密**：AES-256-GCM · IV 唯一 · 128-bit GCM tag
     * **厂商签字必传**（P1 修补 3）
     *
     * @param id
     * @param formData
     * @returns any 成功（自动 AES-256-GCM 加密）
     * @throws ApiError
     */
    public static uploadReconcileSignature(
        id: number,
        formData: {
            /**
             * PDF / JPG / PNG · ≤ 10MB
             */
            file: Blob;
            /**
             * 厂商签字人姓名
             */
            signerName: string;
        },
    ): CancelablePromise<(Result & {
        data?: ReconcileSignature;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/upload-signature',
            path: {
                'id': id,
            },
            formData: formData,
            mediaType: 'multipart/form-data',
        });
    }
    /**
     * 双方对账确认 → step 3
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static bothConfirmReconcile(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/both-confirm',
            path: {
                'id': id,
            },
            errors: {
                40903: `状态非法（需 VENDOR_CONFIRMED）`,
            },
        });
    }
    /**
     * 财务对账确认（AC-6.1.4 · step 4 → CLOSED）
     * @param id
     * @returns any 成功 → CLOSED + isLocked=true
     * @throws ApiError
     */
    public static financeConfirmReconcile(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/reconciles/{id}/finance-confirm',
            path: {
                'id': id,
            },
            errors: {
                40903: `状态非法（需 BOTH_CONFIRMED）`,
            },
        });
    }
}
