/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
export type InspectionItemDTO = {
    /**
     * 检验项名称（如"外观"/"尺寸"/"硬度"）
     */
    itemName?: string;
    /**
     * 检验标准
     */
    standard?: string | null;
    /**
     * 实测值
     */
    measuredValue?: string | null;
    /**
     * 单项结果
     */
    result?: 'OK' | 'NG' | 'NA';
    sortOrder?: number;
};

