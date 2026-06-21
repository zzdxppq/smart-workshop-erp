/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Result } from '../models/Result';
import type { WorkflowEventStats } from '../models/WorkflowEventStats';
import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';
export class V138WorkflowService {
    /**
     * 审批事件统计（Story 10.3）
     * @param workflowCode
     * @param approverRole
     * @param startDate
     * @param endDate
     * @returns any 成功
     * @throws ApiError
     */
    public static getWorkflowEventStats(
        workflowCode: string,
        approverRole?: string,
        startDate?: string,
        endDate?: string,
    ): CancelablePromise<(Result & {
        data?: WorkflowEventStats;
    })> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/workflow/events/stats',
            query: {
                'workflow_code': workflowCode,
                'approver_role': approverRole,
                'start_date': startDate,
                'end_date': endDate,
            },
        });
    }
}
