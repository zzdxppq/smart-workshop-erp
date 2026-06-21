/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LoginRequest } from '../models/LoginRequest';
import type { LoginResponse } from '../models/LoginResponse';
import type { PageResponse } from '../models/PageResponse';
import type { Result } from '../models/Result';
import type { User } from '../models/User';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E1AuthService {
    /**
     * 登录
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static login(
        requestBody: LoginRequest,
    ): CancelablePromise<(Result & {
        data?: LoginResponse;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/auth/login',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                401: `未登录`,
            },
        });
    }
    /**
     * 登出
     * @returns any 未登录
     * @throws ApiError
     */
    public static logout(): CancelablePromise<(Result & {
        code?: any;
    })> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/auth/logout',
        });
    }
    /**
     * 用户列表
     * @param pageNum
     * @param pageSize
     * @param deptId
     * @param status
     * @returns any 成功
     * @throws ApiError
     */
    public static listUsers(
        pageNum: number = 1,
        pageSize: number = 20,
        deptId?: number,
        status?: 'ACTIVE' | 'DISABLED',
    ): CancelablePromise<(Result & {
        data?: PageResponse;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/users',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'deptId': deptId,
                'status': status,
            },
        });
    }
    /**
     * 创建用户
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createUser(
        requestBody: User,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/users',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                400: `参数错误`,
            },
        });
    }
    /**
     * 查询用户
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getUser(
        id: number,
    ): CancelablePromise<(Result & {
        data?: User;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/users/{id}',
            path: {
                'id': id,
            },
            errors: {
                404: `资源不存在`,
            },
        });
    }
    /**
     * 更新用户
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateUser(
        id: number,
        requestBody: User,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/users/{id}',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 角色列表
     * @returns any 成功
     * @throws ApiError
     */
    public static listRoles(): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/roles',
        });
    }
    /**
     * 创建角色（含金额阈值）
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createRole(
        requestBody: {
            roleCode?: string;
            roleName?: string;
            dataScope?: 'SELF' | 'DEPT' | 'ALL' | 'CUSTOM';
            amountThreshold?: number;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/roles',
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 分配权限
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static assignPermissions(
        id: number,
        requestBody: {
            menuIds?: Array<number>;
            actions?: Array<string>;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/roles/{id}/permissions',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
}
