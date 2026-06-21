/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { EmailConfig } from '../models/EmailConfig';
import type { Result } from '../models/Result';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E1EmailService {
    /**
     * 获取邮件配置
     * @returns any 成功
     * @throws ApiError
     */
    public static getEmailConfig(): CancelablePromise<(Result & {
        data?: EmailConfig;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/email/config',
        });
    }
    /**
     * 更新邮件配置
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateEmailConfig(
        requestBody: EmailConfig,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/email/config',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 测试发送
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static testEmail(
        requestBody: {
            toAddress?: string;
            subject?: string;
            body?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/email/test',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                50203: `163 SMTP 失败`,
            },
        });
    }
    /**
     * 发送日志查询
     * @param pageNum
     * @param pageSize
     * @param status
     * @returns any 成功
     * @throws ApiError
     */
    public static emailLogs(
        pageNum: number = 1,
        pageSize: number = 20,
        status?: 'PENDING' | 'SENT' | 'FAILED' | 'DEAD',
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/email/logs',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'status': status,
            },
        });
    }
}
