/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ProcessAllocation } from '../models/ProcessAllocation';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E5AllocationService {
    /**
     * 【生管】分配工序归属（自制/委外）
     * **V1.3.7 红线**：此接口只能由生管调用，**不接受 vendorId 字段**。
     * 勾"委外"的工序会自动推送给采购的"待委外清单"。
     *
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createAllocation(
        requestBody: {
            workorderId: number;
            processSeq: number;
            decision: 'INHOUSE' | 'OUTSOURCE';
            qty?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/production/allocations',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40304: `工序分配越权（调用方非生管角色）'`,
            },
        });
    }
    /**
     * 【采购】取待委外工序清单
     * **V1.3.7 红线**：此接口只能由采购调用，**不允许修改 decision 字段**。
     * 采购只能为每道工序选厂商。
     *
     * @returns any 成功
     * @throws ApiError
     */
    public static pendingAllocations(): CancelablePromise<(Result & {
        data?: Array<(ProcessAllocation & {
            workorderNo?: string;
            productCode?: string;
            processName?: string;
        })>;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/production/allocations/pending',
        });
    }
}
