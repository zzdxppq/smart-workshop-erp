/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { OutsourceArriveRequest } from '../models/OutsourceArriveRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E12ReceiveService {
    /**
     * 【仓管】扫 WW- 到货（V1.3.5 新增）
     * APP 端"到货扫码"入口；扫委外订单二维码，触发 SHIPPING → PENDING_INSPECTION。
     * **V1.3.5 改版**：替代原送货员角色，由仓管代替。
     *
     * @param id
     * @param requestBody
     * @returns any 成功（自动通知生管 + 品质）'
     * @throws ApiError
     */
    public static arriveOutsub(
        id: number,
        requestBody: OutsourceArriveRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsub/orders/{id}/arrive',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 【仓管】到货扫码（V1.3.5 替代送货员角色）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static receiveOutsource(
        id: number,
        requestBody?: OutsourceArriveRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/outsource/orders/{id}/receive',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
