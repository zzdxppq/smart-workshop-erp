/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E1DictService {
    /**
     * 字典查询
     * @param type
     * @returns any 成功
     * @throws ApiError
     */
    public static getDict(
        type: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/dict/{type}',
            path: {
                'type': type,
            },
        });
    }
    /**
     * 单据号生成
     * @param bizType
     * @returns any 成功
     * @throws ApiError
     */
    public static generateSerial(
        bizType: 'GD' | 'BJ' | 'XS' | 'WW' | 'FA',
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/system/serial/{bizType}',
            path: {
                'bizType': bizType,
            },
        });
    }
}
