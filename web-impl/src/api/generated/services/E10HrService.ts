/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E10HrService {
    /**
     * 创建员工
     * @returns any 成功
     * @throws ApiError
     */
    public static createEmployee(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/employees',
        });
    }
    /**
     * 考勤打卡（APP · WiFi/蓝牙定位）
     * @returns any 成功
     * @throws ApiError
     */
    public static attendance(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/attendance',
        });
    }
    /**
     * 薪酬核算
     * @returns any 成功
     * @throws ApiError
     */
    public static salaryCalculate(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/salary/calculate',
        });
    }
    /**
     * 招聘计划
     * @returns any 成功
     * @throws ApiError
     */
    public static createRecruitmentPlan(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/recruitment/plans',
        });
    }
}
