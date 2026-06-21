/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { Order } from './Order';
import type { OrderItem } from './OrderItem';
/**
 * 订单创建请求（继承 1.5 quantityAdjustment 字段）
 */
export type OrderCreateRequest = {
    order: Order;
    items: Array<OrderItem>;
};

