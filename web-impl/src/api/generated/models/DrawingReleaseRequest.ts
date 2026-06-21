/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 发布请求 · > 20万 走二次密码
 */
export type DrawingReleaseRequest = {
    /**
     * 管理员二次密码（FA 件 > 20万 必填）
     */
    adminPassword?: string;
    /**
     * OR 会签候选人
     */
    candidates?: Array<number>;
    comment?: string;
};

