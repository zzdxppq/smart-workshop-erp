/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Order } from './Order';
import type { OrderItem } from './OrderItem';
/**
 * 订单更新请求（仅 DRAFT 状态可改）
 */
export type OrderUpdateRequest = {
    order?: Order;
    items?: Array<OrderItem>;
};

