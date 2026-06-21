/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E1AppService {
    /**
     * APP 离线数据批量同步
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static appSync(
        requestBody: {
            deviceId?: string;
            records?: Array<Record<string, any>>;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/app/sync',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * APP 消息中心
     * @param unreadOnly
     * @returns any 成功
     * @throws ApiError
     */
    public static appMessages(
        unreadOnly: boolean = false,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/app/messages',
            query: {
                'unreadOnly': unreadOnly,
            },
        });
    }
}
