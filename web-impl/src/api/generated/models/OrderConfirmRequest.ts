/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
/**
 * 订单确认请求（DRAFT → CONFIRMED，> 20万 必填二次密码）
 */
export type OrderConfirmRequest = {
    /**
     * > 20万 二次密码确认
     */
    secondPassword?: string;
    comment?: string;
};

