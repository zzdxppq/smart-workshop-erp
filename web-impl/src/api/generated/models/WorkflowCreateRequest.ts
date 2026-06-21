/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type WorkflowCreateRequest = {
    workflowCode: string;
    /**
     * 节点 JSON 数组（1..20 节点）
     */
    nodesJson: string;
    /**
     * 条件 JSON（amount_field 必填）
     */
    conditionsJson?: string;
};

