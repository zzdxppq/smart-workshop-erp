/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ApproveRequest } from '../models/ApproveRequest';
import type { RejectRequest } from '../models/RejectRequest';
import type { WorkflowCreateRequest } from '../models/WorkflowCreateRequest';
import type { WorkflowTestRequest } from '../models/WorkflowTestRequest';
import type { WorkflowUpdateRequest } from '../models/WorkflowUpdateRequest';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class E1WorkflowService {
    /**
     * 创建工作流
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static createWorkflow(
        requestBody: WorkflowCreateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/workflows',
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `节点阈值不单调`,
                40005: `节点数 2-20 之外`,
                40905: `workflow_code 重复`,
            },
        });
    }
    /**
     * 工作流列表（分页）
     * @param pageNum
     * @param pageSize
     * @param status
     * @param keyword
     * @returns any 成功
     * @throws ApiError
     */
    public static listWorkflows(
        pageNum: number = 1,
        pageSize: number = 20,
        status?: 'ACTIVE' | 'INACTIVE' | 'DELETED',
        keyword?: string,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/workflows',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'status': status,
                'keyword': keyword,
            },
        });
    }
    /**
     * 修改工作流（V1.3.7 · 物理替换节点链）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static updateWorkflow(
        id: number,
        requestBody: WorkflowUpdateRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'PUT',
            url: '/workflows',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40001: `节点阈值不单调`,
                40906: `工作流正在被使用`,
            },
        });
    }
    /**
     * 删除工作流（内置模板 40904）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static deleteWorkflow(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'DELETE',
            url: '/workflows',
            path: {
                'id': id,
            },
            errors: {
                40904: `内置工作流不可删除`,
            },
        });
    }
    /**
     * 工作流详情（含 nodes 列表）
     * @param id
     * @returns any 成功
     * @throws ApiError
     */
    public static getWorkflow(
        id: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/workflows/{id}',
            path: {
                'id': id,
            },
            errors: {
                40401: `工作流不存在`,
            },
        });
    }
    /**
     * 试跑工作流（不落库）
     * @param id
     * @param requestBody
     * @returns any 成功，返回 matchedNode/matchedRole/candidates/orSignRequired/trace
     * @throws ApiError
     */
    public static testWorkflow(
        id: number,
        requestBody: WorkflowTestRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/workflows/{id}/test',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }
    /**
     * 待办列表（作为审批人）
     * @param approverUserId
     * @param pageNum
     * @param pageSize
     * @returns any 成功
     * @throws ApiError
     */
    public static pendingApprovals(
        approverUserId: number,
        pageNum: number = 1,
        pageSize: number = 20,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/approvals/pending',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'approverUserId': approverUserId,
            },
        });
    }
    /**
     * 我的待办（作为申请人，看自己提交的单子进度）
     * @param applicantUserId
     * @param pageNum
     * @param pageSize
     * @returns any 成功
     * @throws ApiError
     */
    public static myPendingApprovals(
        applicantUserId: number,
        pageNum: number = 1,
        pageSize: number = 20,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/approvals/my-pending',
            query: {
                'pageNum': pageNum,
                'pageSize': pageSize,
                'applicantUserId': applicantUserId,
            },
        });
    }
    /**
     * 审批通过（V1.3.7 P1 修补 OR 会签）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static approve(
        id: number,
        requestBody?: ApproveRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approvals/{id}/approve',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40303: `审批人不在 candidates 列表`,
                40904: `审批单已结束 / 重复 approve`,
            },
        });
    }
    /**
     * 审批驳回（reason 必填）
     * @param id
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static reject(
        id: number,
        requestBody: RejectRequest,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approvals/{id}/reject',
            path: {
                'id': id,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40009: `驳回 reason 为空`,
                40904: `状态机不匹配`,
            },
        });
    }
    /**
     * 催办（V1.3.7 · 不重置 timeout_at）
     * @param id
     * @param operatorUserId
     * @returns any 成功
     * @throws ApiError
     */
    public static urge(
        id: number,
        operatorUserId: number,
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approvals/{id}/urge',
            path: {
                'id': id,
            },
            query: {
                'operatorUserId': operatorUserId,
            },
        });
    }
    /**
     * 报价转订单触发自动审批（Service Token 守）
     * @param xServiceToken
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static callbackQuoteToOrder(
        xServiceToken: string,
        requestBody: {
            quoteId?: string;
            orderId?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approvals/callback/quote-to-order',
            headers: {
                'X-Service-Token': xServiceToken,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40101: `Service Token 无效`,
            },
        });
    }
    /**
     * 订单取消反向同步审批（Service Token 守）
     * @param xServiceToken
     * @param requestBody
     * @returns any 成功
     * @throws ApiError
     */
    public static callbackOrderCancel(
        xServiceToken: string,
        requestBody: {
            orderId?: string;
            bizType?: string;
        },
    ): CancelablePromise<any> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/approvals/callback/order-cancel',
            headers: {
                'X-Service-Token': xServiceToken,
            },
            body: requestBody,
            mediaType: 'application/json',
            errors: {
                40101: `Service Token 无效`,
            },
        });
    }
}
