/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { LabelTemplate } from './LabelTemplate';
/**
 * 标签模板列表响应（12.3 listLabelTemplates 返回）
 */
export type LabelTemplateListResponse = {
    templates?: Array<LabelTemplate>;
    /**
     * 厂名 · 读 sys_dict.COMPANY_NAME
     */
    companyName?: string;
};

