/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E5MachineService {
    /**
     * 设备列表
     * @returns any 成功
     * @throws ApiError
     */
    public static listMachines(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/machines',
        });
    }
    /**
     * 设备机台负荷
     * @param id
     * @param date
     * @returns any 成功
     * @throws ApiError
     */
    public static machineLoad(
        id: number,
        date?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/machines/{id}/load',
            path: {
                'id': id,
            },
            query: {
                'date': date,
            },
        });
    }
}
