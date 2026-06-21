/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { OutsourceOrder } from '../models/OutsourceOrder';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E6OutsubService {
    /**
     * 【采购】选厂商创建 WW- 单
     * **V1.3.7 改版**：采购从此接口创建 WW- 单，自动发 163 邮箱通知厂商。
     * **V1.3.7 红线**：此接口**不允许指定 decision / processSeq**，只接受 allocationId + vendorId。
     *
     * @param requestBody
     * @returns any 成功（已自动发 163 邮箱）
     * @throws ApiError
     */
    public static createOutsubOrder(
        requestBody: {
            /**
             * 来自 /allocations/pending
             */
            allocationId: number;
            vendorId: number;
            unitPrice: number;
            deliveryDate: string;
            /**
             * 加工图纸 ID（Epic 3 · 下单前确认）
             */
            drawingId: number;
        },
    ): CancelablePromise<(Result & {
        data?: OutsourceOrder;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsub/orders',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 状态机转换（V1.3.4 7 状态机）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static transitionOutsub(
        id: number,
        requestBody: {
            toStatus: 'PENDING_SHIP' | 'SHIPPING' | 'PENDING_INSPECTION' | 'INSPECTING' | 'QUALIFIED_STORAGE' | 'STORED' | 'REPAIR_REQUESTED' | 'NOTIFIED_REPAIR';
            /**
             * 不良品必填
             */
            reason?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsub/orders/{id}/state-transition',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40904: `状态机不匹配（守卫拒绝）'`,
            },
        });
    }
    /**
     * 生成返修单（V1.3.4 返修闭环）
     * rework_count 自动累加；≥ 阈值（默认 2）自动推高层 + 采购 + 生管。
     *
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createRework(
        requestBody: {
            originalOutsourceOrderId: number;
            vendorId: number;
            reason?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsub/rework',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 历史交期预估（V1.3.4）
     * 返回最近 3 次中位数 + 50%/80%/100% 分位数
     * @param vendorId
     * @param processCode
     * @returns any 成功
     * @throws ApiError
     */
    public static getOutsubEta(
        vendorId: number,
        processCode: string,
    ): CancelablePromise<(Result & {
        data?: {
            medianDays?: number;
            p50?: number;
            p80?: number;
            p100?: number;
        };
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/outsub/eta/{vendorId}/{processCode}',
            path: {
                'vendorId': vendorId,
                'processCode': processCode,
            },
        });
    }
}
